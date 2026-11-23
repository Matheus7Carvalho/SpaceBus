package com.spacebus.ops.domain.service.busca;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.model.busca.BuscaViagemRequest;
import com.spacebus.ops.domain.model.busca.DetalheViagemResponse;
import com.spacebus.ops.domain.model.busca.ViagemEncontradaResponse;
import com.spacebus.ops.domain.model.rota.PontoParadaResponse;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class BuscaViagemService implements IBuscaViagemService {

    public static final String INDISPONIVEL = "Esta viagem não está mais disponível.";

    private final ViagemRepository viagemRepository;
    private final IReservaService reservas;
    private final Clock clock;
    private final String transportadora;

    public BuscaViagemService(final ViagemRepository viagemRepository, final IReservaService reservas, final Clock clock,
                              @Value("${spacebus.transportadora.nome:Transportadora piloto}") final String transportadora) {
        this.viagemRepository = viagemRepository;
        this.reservas = reservas;
        this.clock = clock;
        this.transportadora = transportadora;
    }

    @Override
    public List<ViagemEncontradaResponse> buscar(BuscaViagemRequest busca) {
        var agora = LocalDateTime.now(clock);
        if (busca.getData().isBefore(agora.toLocalDate())) {
            throw new DomainException("Escolha hoje ou uma data futura.");
        }
        var inicio = busca.getData().atStartOfDay();
        return viagemRepository.publicadasEntre(inicio, inicio.plusDays(1), agora, StatusViagem.PUBLICADA).stream()
                .map(v -> trecho(v, busca.getOrigem(), busca.getDestino()).map(t -> encontrada(v, t)))
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public DetalheViagemResponse detalhar(String id) {
        var viagem = viagemRepository.findById(id)
                .filter(v -> v.getStatus() == StatusViagem.PUBLICADA && v.getPartida().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> new EntityNotFoundException(INDISPONIVEL));
        var rota = viagem.getRota();
        return DetalheViagemResponse.builder()
                .id(viagem.getId())
                .transportadora(transportadora)
                .rotaNome(rota.getNome())
                .origem(rota.getOrigem())
                .destino(rota.getDestino())
                .partida(viagem.getPartida())
                .preco(viagem.getPreco())
                .tipoVeiculo(viagem.getVeiculo().getTipo())
                .veiculoDescricao(viagem.getVeiculo().getDescricao())
                .lugaresDisponiveis(lugaresDisponiveis(viagem))
                .pontos(rota.getPontos().stream().map(BuscaViagemService::ponto).toList())
                .atualizadaEm(viagem.getDataAtualizacao() != null ? viagem.getDataAtualizacao() : viagem.getDataCriacao())
                .build();
    }

    @Override
    public List<String> locais() {
        var collator = Collator.getInstance(Locale.of("pt", "BR"));
        collator.setStrength(Collator.PRIMARY); // "Praça" e "praca" contam como o mesmo local
        var nomes = new TreeSet<String>(collator);
        viagemRepository.publicadasFuturas(LocalDateTime.now(clock), StatusViagem.PUBLICADA).stream()
                .map(Viagem::getRota)
                .distinct()
                .flatMap(r -> Stream.concat(Stream.of(r.getOrigem(), r.getDestino()), r.getPontos().stream().map(PontoParada::getNome)))
                .forEach(nomes::add);
        return List.copyOf(nomes);
    }

    /** Capacidade menos reservas confirmadas e bloqueios vigentes (mesma conta da reserva). */
    private int lugaresDisponiveis(Viagem viagem) {
        return reservas.lugaresDisponiveis(viagem);
    }

    private record Trecho(PontoParada embarque, PontoParada desembarque) {
    }

    /**
     * Melhor par embarque → desembarque (embarque antes no percurso). Cada ponto recebe uma nota pela
     * relevância do nome (igual &gt; começa com &gt; contém); a origem da rota identifica o primeiro ponto e o
     * destino da rota o último, com a menor nota. Vence a maior soma; no empate, o embarque mais cedo e o
     * desembarque mais próximo.
     */
    private static Optional<Trecho> trecho(Viagem viagem, String origem, String destino) {
        var rota = viagem.getRota();
        var pontos = rota.getPontos();
        Trecho melhor = null;
        var melhorNota = 0;
        for (int i = 0; i < pontos.size(); i++) {
            var notaOrigem = nota(pontos, i, rota.getOrigem(), rota.getDestino(), origem);
            if (notaOrigem == 0) continue;
            for (int j = i + 1; j < pontos.size(); j++) {
                var notaDestino = nota(pontos, j, rota.getOrigem(), rota.getDestino(), destino);
                if (notaDestino > 0 && notaOrigem + notaDestino > melhorNota) {
                    melhorNota = notaOrigem + notaDestino;
                    melhor = new Trecho(pontos.get(i), pontos.get(j));
                }
            }
        }
        return Optional.ofNullable(melhor);
    }

    private static final int NOTA_ROTA = 1;
    private static final int NOTA_CONTEM = 2;
    private static final int NOTA_COMECA = 3;
    private static final int NOTA_IGUAL = 4;

    private static int nota(List<PontoParada> pontos, int i, String origemRota, String destinoRota, String busca) {
        var nota = relevancia(pontos.get(i).getNome(), busca);
        var apelido = i == 0 ? origemRota : i == pontos.size() - 1 ? destinoRota : null;
        if (nota == 0 && apelido != null && relevancia(apelido, busca) > 0) nota = NOTA_ROTA;
        return nota;
    }

    /** 0 se não casa; sem diferenciar maiúsculas nem acentos. */
    static int relevancia(String texto, String busca) {
        var t = normalizar(texto);
        var b = normalizar(busca);
        if (b.isEmpty() || !t.contains(b)) return 0;
        if (t.equals(b)) return NOTA_IGUAL;
        return t.startsWith(b) ? NOTA_COMECA : NOTA_CONTEM;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private ViagemEncontradaResponse encontrada(Viagem viagem, Trecho trecho) {
        return ViagemEncontradaResponse.builder()
                .id(viagem.getId())
                .transportadora(transportadora)
                .rotaNome(viagem.getRota().getNome())
                .partida(viagem.getPartida())
                .preco(viagem.getPreco())
                .tipoVeiculo(viagem.getVeiculo().getTipo())
                .embarque(ponto(trecho.embarque()))
                .desembarque(ponto(trecho.desembarque()))
                .lugaresDisponiveis(lugaresDisponiveis(viagem))
                .build();
    }

    private static PontoParadaResponse ponto(PontoParada p) {
        return new PontoParadaResponse(p.getId(), p.getOrdem(), p.getNome(), p.getReferencia());
    }
}
