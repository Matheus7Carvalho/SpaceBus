package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.Notificacao;
import com.spacebus.ops.domain.entity.NotificacaoEnvio;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.StatusEnvio;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoNotificacao;
import com.spacebus.ops.domain.model.notificacao.NotificacaoResponse;
import com.spacebus.ops.domain.model.notificacao.PreferenciasResponse;
import com.spacebus.ops.domain.repository.notificacao.NotificacaoEnvioRepository;
import com.spacebus.ops.domain.repository.notificacao.NotificacaoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cada etapa numa transação nova e curta: gerar (insert condicional na chave única) e, para cada notificação nova,
 * ler o destinatário, chamar o canal fora de transação e gravar o envio. Nunca trava viagem ou reserva.
 */
@Slf4j
@Service
public class NotificacaoService implements INotificacaoService {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<StatusReserva> ATIVAS = Set.of(StatusReserva.AGUARDANDO_PAGAMENTO, StatusReserva.CONFIRMADA);
    static final String NAO_ENCONTRADA = "Notificação não encontrada.";
    static final String MOTIVO_FALHA = "Falha no envio pelo canal (simulado).";

    private final NotificacaoRepository repository;
    private final NotificacaoEnvioRepository envioRepository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CanalNotificacao canal;
    private final TransactionTemplate transacao;
    private final TransactionTemplate leitura;
    private final Clock clock;
    private final int lembreteMinutos;

    public NotificacaoService(NotificacaoRepository repository, NotificacaoEnvioRepository envioRepository,
                              ReservaRepository reservaRepository, UsuarioRepository usuarioRepository, CanalNotificacao canal,
                              PlatformTransactionManager transacoes, Clock clock,
                              @Value("${spacebus.notificacao.lembrete-minutos:60}") int lembreteMinutos) {
        this.repository = repository;
        this.envioRepository = envioRepository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.canal = canal;
        // Transação nova: o listener roda depois do commit da origem, que não pode receber escrita.
        this.transacao = new TransactionTemplate(transacoes);
        this.transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.leitura.setReadOnly(true);
        this.clock = clock;
        this.lembreteMinutos = lembreteMinutos;
    }

    // --- Geração ---

    @Override
    public void processar(NotificacaoSolicitada evento) {
        var novas = transacao.execute(s -> gerar(evento));
        novas.forEach(this::enviarSemInterromper);
    }

    @Override
    public int lembrar() {
        var novas = transacao.execute(s -> {
            var agora = agora();
            var ids = new ArrayList<String>();
            reservaRepository.confirmadasComPartidaEntre(agora, agora.plusMinutes(lembreteMinutos), StatusReserva.CONFIRMADA, StatusViagem.PUBLICADA)
                    .forEach(r -> {
                        var hora = r.getViagem().getPartida().format(HORA);
                        criar(r, TipoNotificacao.LEMBRETE, "LEMBRETE:" + r.getId(), "Sua viagem parte às %s.".formatted(hora),
                                "%s · %s · embarque em %s.".formatted(trecho(r), hora, r.getEmbarqueNome()))
                                .ifPresent(ids::add);
                    });
            return ids;
        });
        novas.forEach(this::enviarSemInterromper);
        return novas.size();
    }

    /** Destinatários do evento e textos; retorna os ids das notificações realmente criadas. */
    private List<String> gerar(NotificacaoSolicitada e) {
        var ids = new ArrayList<String>();
        switch (e.tipo()) {
            case RESERVA_CONFIRMADA -> reserva(e.alvoId()).filter(r -> r.getStatus() == StatusReserva.CONFIRMADA)
                    .flatMap(r -> criar(r, e.tipo(), "CONFIRMADA:" + r.getId(), "Reserva %s confirmada".formatted(r.getCodigo()),
                            "%s · %s · %s.".formatted(trecho(r), r.getViagem().getPartida().format(DATA_HORA), lugares(r))))
                    .ifPresent(ids::add);
            case VIAGEM_CANCELADA -> reserva(e.alvoId())
                    .flatMap(r -> criar(r, e.tipo(), "CANCELADA:" + r.getId(), "Viagem cancelada: reserva %s".formatted(r.getCodigo()),
                            e.reembolso() ? "O valor será reembolsado." : "A reserva foi cancelada."))
                    .ifPresent(ids::add);
            case VIAGEM_ALTERADA -> reservaRepository.findByViagemIdOrderByDataCriacao(e.alvoId()).stream()
                    .filter(r -> ATIVAS.contains(r.getStatus()))
                    .forEach(r -> {
                        var nova = r.getViagem().getPartida();
                        criar(r, e.tipo(), "ALTERADA:%s:%s".formatted(r.getId(), nova), "Horário alterado: reserva %s".formatted(r.getCodigo()),
                                "Nova partida: %s (antes %s).".formatted(nova.format(DATA_HORA), e.partidaAnterior().format(DATA_HORA)))
                                .ifPresent(ids::add);
                    });
            case VIAGEM_INICIADA -> reservaRepository.findByViagemIdOrderByDataCriacao(e.alvoId()).stream()
                    .filter(r -> r.getStatus() == StatusReserva.CONFIRMADA)
                    .forEach(r -> criar(r, e.tipo(), "INICIADA:" + r.getId(), "A van saiu", "Acompanhe a localização.").ifPresent(ids::add));
            case LEMBRETE -> throw new IllegalArgumentException("Lembrete vem da tarefa agendada.");
        }
        return ids;
    }

    /** Insert condicional na chave: repetido ou simultâneo não duplica (e não é erro). */
    private Optional<String> criar(Reserva r, TipoNotificacao tipo, String chave, String titulo, String mensagem) {
        var id = UUID.randomUUID().toString();
        var criadas = repository.inserirSeNova(id, agora(), r.getPassageiro().getId(), r.getId(), tipo.name(), limitar(titulo, 120),
                limitar(mensagem, 300), "/reservas/" + r.getId(), chave);
        return criadas == 1 ? Optional.of(id) : Optional.empty();
    }

    private Optional<Reserva> reserva(String id) {
        return reservaRepository.findById(id);
    }

    // --- Envio externo (simulado) ---

    private record Destino(Notificacao notificacao, String email, boolean whatsapp) {
    }

    /** Erro no envio de uma notificação (ex.: banco) não impede o envio das seguintes; fica no log, sem dados pessoais. */
    private void enviarSemInterromper(String notificacaoId) {
        try {
            enviar(notificacaoId);
        } catch (RuntimeException e) {
            log.error("Falha ao enviar notificação: notificacao={} erro={}", notificacaoId, e.getClass().getSimpleName());
        }
    }

    /** O canal é chamado fora de transação; falha grava FALHOU e vai para o log sem e-mail ou telefone. */
    private void enviar(String notificacaoId) {
        var destino = leitura.execute(s -> {
            var n = repository.findById(notificacaoId).orElseThrow();
            var u = usuarioRepository.findById(n.getUsuarioId()).orElseThrow();
            return new Destino(n, u.getEmail(), u.getWhatsappConsentidoEm() != null);
        });
        enviarPor(CanalEnvio.EMAIL, destino);
        if (destino.whatsapp()) enviarPor(CanalEnvio.WHATSAPP, destino);
    }

    private void enviarPor(CanalEnvio canalEnvio, Destino d) {
        var n = d.notificacao();
        StatusEnvio status;
        String motivo = null;
        try {
            canal.enviar(canalEnvio, new CanalNotificacao.Mensagem(n.getId(), canalEnvio == CanalEnvio.EMAIL ? d.email() : null,
                    n.getTitulo(), n.getMensagem()));
            status = StatusEnvio.ENVIADA;
        } catch (RuntimeException ex) {
            status = StatusEnvio.FALHOU;
            motivo = MOTIVO_FALHA;
            log.warn("Envio de notificação falhou: notificacao={} canal={} erro={}", n.getId(), canalEnvio, ex.getClass().getSimpleName());
        }
        var registro = new NotificacaoEnvio(n, canalEnvio, status, motivo);
        transacao.executeWithoutResult(s -> envioRepository.save(registro));
    }

    // --- Leitura pelo passageiro ---

    @Override
    public Page<NotificacaoResponse> minhas(String usuarioId, Pageable pageable) {
        return leitura.execute(s -> repository.findByUsuarioIdOrderByDataCriacaoDescIdDesc(usuarioId, pageable).map(NotificacaoResponse::de));
    }

    @Override
    public long naoLidas(String usuarioId) {
        return repository.countByUsuarioIdAndLidaEmIsNull(usuarioId);
    }

    @Override
    public NotificacaoResponse ler(String id, String usuarioId) {
        return transacao.execute(s -> {
            var n = repository.findByIdAndUsuarioId(id, usuarioId).orElseThrow(() -> new EntityNotFoundException(NAO_ENCONTRADA));
            if (n.getLidaEm() == null) n.setLidaEm(agora());
            return NotificacaoResponse.de(n);
        });
    }

    @Override
    public int lerTodas(String usuarioId) {
        return transacao.execute(s -> repository.marcarTodasComoLidas(usuarioId, agora()));
    }

    @Override
    public PreferenciasResponse preferencias(String usuarioId) {
        return PreferenciasResponse.de(usuario(usuarioId));
    }

    @Override
    public PreferenciasResponse definirWhatsapp(String usuarioId, boolean whatsapp) {
        return transacao.execute(s -> {
            var u = usuario(usuarioId);
            if (whatsapp && u.getWhatsappConsentidoEm() == null) u.setWhatsappConsentidoEm(agora());
            if (!whatsapp) u.setWhatsappConsentidoEm(null);
            return PreferenciasResponse.de(usuarioRepository.save(u));
        });
    }

    private Usuario usuario(String id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    // --- Texto ---

    private static String trecho(Reserva r) {
        return r.getEmbarqueNome() + " → " + r.getDesembarqueNome();
    }

    private static String lugares(Reserva r) {
        return r.getQuantidade() == 1 ? "1 lugar" : r.getQuantidade() + " lugares";
    }

    private static String limitar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(clock);
    }
}
