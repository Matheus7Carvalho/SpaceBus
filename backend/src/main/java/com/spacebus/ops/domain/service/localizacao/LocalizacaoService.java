package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.LocalizacaoViagem;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.AcaoAuditada;
import com.spacebus.ops.domain.entity.enums.MotivoRejeicaoPosicao;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.localizacao.DiagnosticoLocalizacaoResponse;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoOperacaoResponse;
import com.spacebus.ops.domain.model.localizacao.LocalizacaoResponse;
import com.spacebus.ops.domain.model.localizacao.PosicaoRequest;
import com.spacebus.ops.domain.model.localizacao.ResultadoEnvioResponse;
import com.spacebus.ops.domain.repository.localizacao.LocalizacaoViagemRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.auditoria.IAuditoriaService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class LocalizacaoService implements ILocalizacaoService {

    private static final String NAO_EM_ANDAMENTO = "A viagem não está em andamento.";

    private final LocalizacaoViagemRepository repository;
    private final ViagemRepository viagemRepository;
    private final ReservaRepository reservaRepository;
    private final IAuditoriaService auditoria;
    private final Clock clock;
    private final LocalizacaoPropriedades limites;

    public LocalizacaoService(LocalizacaoViagemRepository repository, ViagemRepository viagemRepository,
                              ReservaRepository reservaRepository, IAuditoriaService auditoria, Clock clock,
                              LocalizacaoPropriedades limites) {
        this.repository = repository;
        this.viagemRepository = viagemRepository;
        this.reservaRepository = reservaRepository;
        this.auditoria = auditoria;
        this.clock = clock;
        this.limites = limites;
    }

    @Override
    @Transactional
    public ResultadoEnvioResponse registrar(String viagemId, String motoristaId, List<PosicaoRequest> posicoes) {
        return gravar(viagemId, motoristaId, posicoes, false);
    }

    @Override
    @Transactional
    public ResultadoEnvioResponse registrarSimulada(String viagemId, PosicaoRequest posicao) {
        return gravar(viagemId, null, List.of(posicao), true);
    }

    private record Posicao(double latitude, double longitude, double precisao, LocalDateTime registradaEm) {
    }

    /**
     * Trava a viagem em modo compartilhado e confere situação e motorista; depois classifica e grava cada posição, da
     * mais antiga para a mais nova, com o UPDATE condicional (a mais antiga nunca sobrescreve a mais nova).
     */
    private ResultadoEnvioResponse gravar(String viagemId, String motoristaId, List<PosicaoRequest> pedidas, boolean simulada) {
        var posicoes = pedidas.stream().map(this::converter).sorted(Comparator.comparing(Posicao::registradaEm)).toList();
        var agora = agora();
        viagemRepository.travarCompartilhado(viagemId)
                .filter(s -> StatusViagem.EM_ANDAMENTO.name().equals(s.getStatus()))
                .filter(s -> simulada || (motoristaId != null && motoristaId.equals(s.getMotoristaId())))
                .orElseThrow(() -> new DomainException(NAO_EM_ANDAMENTO));
        repository.garantirLinha(UUID.randomUUID().toString(), viagemId, agora);

        var rejeitadas = new EnumMap<MotivoRejeicaoPosicao, Integer>(MotivoRejeicaoPosicao.class);
        MotivoRejeicaoPosicao ultimoMotivo = null;
        var aceitas = 0;
        var ignoradas = 0;
        for (var posicao : posicoes) {
            var motivo = motivoDeRejeicao(posicao, agora);
            if (motivo.isPresent()) {
                rejeitadas.merge(motivo.get(), 1, Integer::sum);
                ultimoMotivo = motivo.get();
            } else if (gravarSeMaisNova(viagemId, posicao, agora, simulada)) {
                aceitas++;
            } else {
                ignoradas++;
            }
        }
        contar(viagemId, rejeitadas, ultimoMotivo, ignoradas, agora);
        var totalRejeitadas = rejeitadas.values().stream().mapToInt(Integer::intValue).sum();
        return new ResultadoEnvioResponse(aceitas, totalRejeitadas, ignoradas);
    }

    private boolean gravarSeMaisNova(String viagemId, Posicao p, LocalDateTime agora, boolean simulada) {
        var limiteFrequente = p.registradaEm().minusSeconds(limites.intervaloMinimoSegundos());
        return repository.gravarSeMaisNova(viagemId, p.latitude(), p.longitude(), p.precisao(), p.registradaEm(),
                limiteFrequente, agora, simulada, limites.lacunaSegundos()) == 1;
    }

    /** Rejeição conta como falha de sinal. Log só com viagem, motivo e contagem: nada de coordenadas ou pessoas. */
    private void contar(String viagemId, Map<MotivoRejeicaoPosicao, Integer> rejeitadas, MotivoRejeicaoPosicao ultimoMotivo,
                        int ignoradas, LocalDateTime agora) {
        if (rejeitadas.isEmpty() && ignoradas == 0) return;
        rejeitadas.forEach((motivo, quantidade) ->
                log.warn("Posições rejeitadas: viagem={} motivo={} quantidade={}", viagemId, motivo, quantidade));
        repository.contar(viagemId, rejeitadas.getOrDefault(MotivoRejeicaoPosicao.ADIANTADA, 0),
                rejeitadas.getOrDefault(MotivoRejeicaoPosicao.ANTIGA, 0), rejeitadas.getOrDefault(MotivoRejeicaoPosicao.IMPRECISA, 0),
                ignoradas, ultimoMotivo == null ? null : ultimoMotivo.name(), agora);
    }

    private Optional<MotivoRejeicaoPosicao> motivoDeRejeicao(Posicao p, LocalDateTime agora) {
        if (p.registradaEm().isAfter(agora.plusSeconds(limites.adiantadaMaxSegundos()))) return Optional.of(MotivoRejeicaoPosicao.ADIANTADA);
        if (p.registradaEm().isBefore(agora.minusSeconds(limites.antigaMaxSegundos()))) return Optional.of(MotivoRejeicaoPosicao.ANTIGA);
        if (p.precisao() > limites.precisaoMaxM()) return Optional.of(MotivoRejeicaoPosicao.IMPRECISA);
        return Optional.empty();
    }

    /** Hora com fuso → relógio de parede do fuso da aplicação (o mesmo do Clock). */
    private Posicao converter(PosicaoRequest p) {
        try {
            var registrada = OffsetDateTime.parse(p.getRegistradaEm()).atZoneSameInstant(clock.getZone()).toLocalDateTime();
            return new Posicao(p.getLatitude(), p.getLongitude(), p.getPrecisaoM(), registrada.truncatedTo(ChronoUnit.MILLIS));
        } catch (DateTimeParseException e) {
            throw new DomainException("Informe a hora da posição com fuso (ISO-8601).");
        }
    }

    @Override
    public LocalizacaoResponse daReserva(String reservaId, String passageiroId) {
        var reserva = reservaRepository.findByIdAndPassageiroId(reservaId, passageiroId)
                .orElseThrow(() -> new EntityNotFoundException("Reserva não encontrada."));
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new DomainException("Localização disponível só para reservas confirmadas.");
        }
        var viagem = reserva.getViagem();
        return montar(viagem, repository.findByViagemId(viagem.getId()), agora());
    }

    @Override
    @Transactional
    public LocalizacaoOperacaoResponse daViagem(String viagemId, String usuarioId, Perfil perfil) {
        var viagem = viagemRepository.findById(viagemId).orElseThrow(() -> new EntityNotFoundException("Item não encontrado."));
        var agora = agora();
        if (perfil == Perfil.ADMINISTRADOR) {
            auditoria.registrarUmaVezPorHora(usuarioId, perfil, AcaoAuditada.CONSULTA_LOCALIZACAO_VIAGEM, viagemId, agora);
        }
        var localizacao = repository.findByViagemId(viagemId);
        return LocalizacaoOperacaoResponse.builder()
                .localizacao(montar(viagem, localizacao, agora))
                .semMotorista(viagem.getMotorista() == null)
                .diagnostico(diagnostico(viagem, localizacao.orElse(null)))
                .build();
    }

    @Override
    public Set<String> semSinal(Collection<Viagem> viagens) {
        var emAndamento = viagens.stream().filter(v -> v.getStatus() == StatusViagem.EM_ANDAMENTO).map(Viagem::getId).toList();
        if (emAndamento.isEmpty()) return Set.of();
        var agora = agora();
        var comSinal = repository.findByViagemIdIn(emAndamento).stream()
                .filter(l -> l.temPosicao() && !desatualizada(l, agora))
                .map(LocalizacaoViagem::getViagemId)
                .collect(Collectors.toSet());
        return emAndamento.stream().filter(id -> !comSinal.contains(id)).collect(Collectors.toSet());
    }

    @Override
    public Map<String, LocalDateTime> ultimasPosicoes(Collection<String> viagemIds) {
        if (viagemIds.isEmpty()) return Map.of();
        var mapa = new HashMap<String, LocalDateTime>();
        repository.findByViagemIdIn(viagemIds).stream()
                .filter(LocalizacaoViagem::temPosicao)
                .forEach(l -> mapa.put(l.getViagemId(), l.getRegistradaEm()));
        return mapa;
    }

    @Override
    @Transactional
    public int expurgar() {
        var agora = agora();
        return repository.expurgarEncerradasAte(agora.minusHours(limites.retencaoHoras()), agora);
    }

    /** Posição só com a viagem em andamento; nunca inventada (sem posição gravada, "indisponível"). */
    private LocalizacaoResponse montar(Viagem viagem, Optional<LocalizacaoViagem> localizacao, LocalDateTime agora) {
        var resposta = LocalizacaoResponse.builder()
                .situacaoViagem(viagem.getStatus())
                .aguardandoInicio(aguardandoInicio(viagem, agora));
        var posicao = localizacao.filter(l -> viagem.getStatus() == StatusViagem.EM_ANDAMENTO && l.temPosicao());
        if (posicao.isEmpty()) return resposta.build();
        var l = posicao.get();
        return resposta.disponivel(true)
                .latitude(l.getLatitude())
                .longitude(l.getLongitude())
                .precisaoM(l.getPrecisaoM())
                .registradaEm(l.getRegistradaEm())
                .segundosDesdeAtualizacao(segundosDesde(l, agora))
                .desatualizada(desatualizada(l, agora))
                .simulada(l.isSimulada())
                .build();
    }

    private boolean aguardandoInicio(Viagem viagem, LocalDateTime agora) {
        if (viagem.getStatus() != StatusViagem.PUBLICADA) return false;
        var partida = viagem.getPartida();
        return !agora.isBefore(partida.minusMinutes(limites.inicioAntesMin())) && !agora.isAfter(partida.plusMinutes(limites.inicioDepoisMin()));
    }

    private boolean desatualizada(LocalizacaoViagem l, LocalDateTime agora) {
        return segundosDesde(l, agora) > limites.desatualizadaSegundos();
    }

    /** Aparelho um pouco adiantado (dentro da tolerância) não gera idade negativa. */
    private static long segundosDesde(LocalizacaoViagem l, LocalDateTime agora) {
        return Math.max(0, Duration.between(l.getRegistradaEm(), agora).getSeconds());
    }

    private static DiagnosticoLocalizacaoResponse diagnostico(Viagem viagem, LocalizacaoViagem l) {
        var d = DiagnosticoLocalizacaoResponse.builder().iniciadaEm(viagem.getIniciadaEm()).encerradaEm(viagem.getEncerradaEm());
        if (l == null) return d.build();
        return d.ultimaRecebidaEm(l.getRecebidaEm())
                .aceitas(l.getAceitas())
                .rejeitadasAdiantada(l.getRejeitadasAdiantada())
                .rejeitadasAntiga(l.getRejeitadasAntiga())
                .rejeitadasImprecisa(l.getRejeitadasImprecisa())
                .ignoradas(l.getIgnoradas())
                .lacunas(l.getLacunas())
                .maiorIntervaloSegundos(l.getMaiorIntervaloSegundos())
                .ultimoMotivoRejeicao(l.getUltimoMotivoRejeicao())
                .ultimaRejeicaoEm(l.getUltimaRejeicaoEm())
                .build();
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
