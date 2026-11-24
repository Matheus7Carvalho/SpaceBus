package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.Notificacao;
import com.spacebus.ops.domain.entity.NotificacaoEnvio;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusEnvio;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoNotificacao;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.repository.notificacao.NotificacaoEnvioRepository;
import com.spacebus.ops.domain.repository.notificacao.NotificacaoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** H18 e H20: quem recebe, texto, chave única, envios simulados e falha do canal. */
@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);
    private static final LocalDateTime PARTIDA = LocalDateTime.of(2026, 11, 25, 7, 30);

    @Mock private NotificacaoRepository repository;
    @Mock private NotificacaoEnvioRepository envioRepository;
    @Mock private ReservaRepository reservaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CanalNotificacao canal;

    private NotificacaoService service;
    private final Usuario paula = new Usuario("Paula", "passageiro@spacebus.local", "h", Perfil.PASSAGEIRO, true);
    private Viagem viagem;

    @BeforeEach
    void setUp() {
        service = new NotificacaoService(repository, envioRepository, reservaRepository, usuarioRepository, canal,
                mock(PlatformTransactionManager.class), CLOCK, 60);
        paula.setId("p1");
        viagem = new Viagem(new Rota("Centro – Aeroporto", "Centro", "Aeroporto", List.of()),
                new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true), null, PARTIDA, new BigDecimal("18.50"), 15, StatusViagem.PUBLICADA);
        viagem.setId("vg1");
        lenient().when(repository.inserirSeNova(anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
        lenient().when(repository.findById(anyString())).thenAnswer(inv -> Optional.of(notificacao(inv.getArgument(0))));
        lenient().when(usuarioRepository.findById("p1")).thenReturn(Optional.of(paula));
    }

    private static Notificacao notificacao(String id) {
        var n = new Notificacao();
        n.setId(id);
        n.setUsuarioId("p1");
        n.setTitulo("t");
        n.setMensagem("m");
        return n;
    }

    private Reserva reserva(String id, StatusReserva status, int lugares) {
        var r = new Reserva();
        r.setId(id);
        r.setCodigo("SB-AB12CD");
        r.setStatus(status);
        r.setViagem(viagem);
        r.setPassageiro(paula);
        r.setEmbarqueNome("Praça Central");
        r.setDesembarqueNome("Aeroporto");
        r.setQuantidade(lugares);
        lenient().when(reservaRepository.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private void verificarCriada(TipoNotificacao tipo, String chave, String titulo, String mensagem, String reservaId) {
        verify(repository).inserirSeNova(anyString(), eq(AGORA), eq("p1"), eq(reservaId), eq(tipo.name()), eq(titulo), eq(mensagem),
                eq("/reservas/" + reservaId), eq(chave));
    }

    private List<NotificacaoEnvio> envios() {
        var captor = ArgumentCaptor.forClass(NotificacaoEnvio.class);
        verify(envioRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void confirmacaoGeraUmaNotificacaoComTextoLinkEChaveEEnviaPorEmail() {
        reserva("rs1", StatusReserva.CONFIRMADA, 2);

        service.processar(NotificacaoSolicitada.confirmada("rs1"));

        verificarCriada(TipoNotificacao.RESERVA_CONFIRMADA, "CONFIRMADA:rs1", "Reserva SB-AB12CD confirmada",
                "Praça Central → Aeroporto · 25/11 07:30 · 2 lugares.", "rs1");
        assertThat(envios()).extracting(NotificacaoEnvio::getCanal, NotificacaoEnvio::getStatus).containsExactly(
                org.assertj.core.groups.Tuple.tuple(CanalEnvio.EMAIL, StatusEnvio.ENVIADA));
        verify(canal).enviar(eq(CanalEnvio.EMAIL), any());
    }

    @Test
    void comOptInTambemEnviaPorWhatsapp() {
        paula.setWhatsappConsentidoEm(AGORA.minusDays(1));
        reserva("rs1", StatusReserva.CONFIRMADA, 1);

        service.processar(NotificacaoSolicitada.confirmada("rs1"));

        assertThat(envios()).extracting(NotificacaoEnvio::getCanal).containsExactly(CanalEnvio.EMAIL, CanalEnvio.WHATSAPP);
    }

    @Test
    void chaveRepetidaNaoGeraNemEnvia() {
        reserva("rs1", StatusReserva.CONFIRMADA, 1);
        when(repository.inserirSeNova(anyString(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        service.processar(NotificacaoSolicitada.confirmada("rs1"));

        verify(canal, never()).enviar(any(), any());
        verify(envioRepository, never()).save(any());
    }

    @Test
    void confirmacaoDeReservaQueNaoEstaConfirmadaNaoGera() {
        reserva("rs1", StatusReserva.EXPIRADA, 1);

        service.processar(NotificacaoSolicitada.confirmada("rs1"));

        verify(repository, never()).inserirSeNova(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void falhaDoCanalGravaFalhouSemPropagar() {
        reserva("rs1", StatusReserva.CONFIRMADA, 1);
        doThrow(new IllegalStateException("caiu")).when(canal).enviar(any(), any());

        service.processar(NotificacaoSolicitada.confirmada("rs1"));

        var envio = envios().get(0);
        assertThat(envio.getStatus()).isEqualTo(StatusEnvio.FALHOU);
        assertThat(envio.getMotivo()).isEqualTo("Falha no envio pelo canal (simulado).").doesNotContain("@");
    }

    @Test
    void partidaAlteradaAvisaSoReservasAtivasComANovaPartidaNaChave() {
        var ativa = reserva("rs1", StatusReserva.CONFIRMADA, 1);
        var aguardando = reserva("rs2", StatusReserva.AGUARDANDO_PAGAMENTO, 1);
        var cancelada = reserva("rs3", StatusReserva.CANCELADA, 1);
        when(reservaRepository.findByViagemIdOrderByDataCriacao("vg1")).thenReturn(List.of(ativa, aguardando, cancelada));

        service.processar(NotificacaoSolicitada.alterada("vg1", LocalDateTime.of(2026, 11, 25, 7, 0)));

        verificarCriada(TipoNotificacao.VIAGEM_ALTERADA, "ALTERADA:rs1:2026-11-25T07:30", "Horário alterado: reserva SB-AB12CD",
                "Nova partida: 25/11 07:30 (antes 25/11 07:00).", "rs1");
        verify(repository).inserirSeNova(anyString(), any(), any(), eq("rs2"), any(), any(), any(), any(), eq("ALTERADA:rs2:2026-11-25T07:30"));
        verify(repository, never()).inserirSeNova(anyString(), any(), any(), eq("rs3"), any(), any(), any(), any(), any());
    }

    @Test
    void viagemSemReservasNaoGeraNada() {
        when(reservaRepository.findByViagemIdOrderByDataCriacao("vg1")).thenReturn(List.of());

        service.processar(NotificacaoSolicitada.iniciada("vg1"));
        service.processar(NotificacaoSolicitada.alterada("vg1", PARTIDA.minusHours(1)));

        verify(repository, never()).inserirSeNova(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void cancelamentoDaViagemDizSeHaReembolso() {
        reserva("rs1", StatusReserva.CANCELADA, 1);
        reserva("rs2", StatusReserva.CANCELADA, 1);

        service.processar(NotificacaoSolicitada.cancelada("rs1", true));
        service.processar(NotificacaoSolicitada.cancelada("rs2", false));

        verificarCriada(TipoNotificacao.VIAGEM_CANCELADA, "CANCELADA:rs1", "Viagem cancelada: reserva SB-AB12CD", "O valor será reembolsado.", "rs1");
        verificarCriada(TipoNotificacao.VIAGEM_CANCELADA, "CANCELADA:rs2", "Viagem cancelada: reserva SB-AB12CD", "A reserva foi cancelada.", "rs2");
    }

    @Test
    void inicioAvisaSoAsConfirmadas() {
        var confirmada = reserva("rs1", StatusReserva.CONFIRMADA, 1);
        var aguardando = reserva("rs2", StatusReserva.AGUARDANDO_PAGAMENTO, 1);
        when(reservaRepository.findByViagemIdOrderByDataCriacao("vg1")).thenReturn(List.of(confirmada, aguardando));

        service.processar(NotificacaoSolicitada.iniciada("vg1"));

        verificarCriada(TipoNotificacao.VIAGEM_INICIADA, "INICIADA:rs1", "A van saiu", "Acompanhe a localização.", "rs1");
        verify(repository, times(1)).inserirSeNova(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    /** A janela (agora, agora + 60 min] e a situação PUBLICADA ficam na consulta; aqui, o que é pedido e o texto. */
    @Test
    void lembreteConsultaAProximaHoraDeViagensPublicadasEUsaAChavePorReserva() {
        var r = reserva("rs1", StatusReserva.CONFIRMADA, 1);
        when(reservaRepository.confirmadasComPartidaEntre(AGORA, AGORA.plusMinutes(60), StatusReserva.CONFIRMADA, StatusViagem.PUBLICADA))
                .thenReturn(List.of(r));

        assertThat(service.lembrar()).isEqualTo(1);

        verificarCriada(TipoNotificacao.LEMBRETE, "LEMBRETE:rs1", "Sua viagem parte às 07:30.",
                "Praça Central → Aeroporto · 07:30 · embarque em Praça Central.", "rs1");
    }

    /** QA R4: erro ao enviar uma notificação não interrompe as seguintes. */
    @Test
    void erroNoEnvioDeUmaNotificacaoNaoInterrompeAsSeguintes() {
        var primeira = reserva("rs1", StatusReserva.CONFIRMADA, 1);
        var segunda = reserva("rs2", StatusReserva.CONFIRMADA, 1);
        when(reservaRepository.findByViagemIdOrderByDataCriacao("vg1")).thenReturn(List.of(primeira, segunda));
        when(repository.findById(anyString()))
                .thenThrow(new IllegalStateException("banco fora"))
                .thenAnswer(inv -> Optional.of(notificacao(inv.getArgument(0))));

        org.assertj.core.api.Assertions.assertThatCode(() -> service.processar(NotificacaoSolicitada.iniciada("vg1"))).doesNotThrowAnyException();

        verify(canal, times(1)).enviar(eq(CanalEnvio.EMAIL), any());
        assertThat(envios()).hasSize(1);
    }

    @Test
    void lerNotificacaoDeOutroPassageiroNaoEncontra() {
        when(repository.findByIdAndUsuarioId("n1", "p2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ler("n1", "p2")).isInstanceOf(EntityNotFoundException.class).hasMessage("Notificação não encontrada.");
    }

    @Test
    void lerMarcaUmaVezSo() {
        var n = notificacao("n1");
        when(repository.findByIdAndUsuarioId("n1", "p1")).thenReturn(Optional.of(n));

        service.ler("n1", "p1");
        var primeira = n.getLidaEm();
        service.ler("n1", "p1");

        assertThat(primeira).isEqualTo(AGORA);
        assertThat(n.getLidaEm()).isEqualTo(primeira);
    }

    @Test
    void lerTodasMarcaAsDoPassageiro() {
        when(repository.marcarTodasComoLidas("p1", AGORA)).thenReturn(3);

        assertThat(service.lerTodas("p1")).isEqualTo(3);
    }

    @Test
    void ligarWhatsappGravaODiaDoConsentimentoEDesligarApaga() {
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var ligado = service.definirWhatsapp("p1", true);
        assertThat(ligado.whatsapp()).isTrue();
        assertThat(ligado.whatsappConsentidoEm()).isEqualTo(AGORA);

        var desligado = service.definirWhatsapp("p1", false);
        assertThat(desligado.whatsapp()).isFalse();
        assertThat(paula.getWhatsappConsentidoEm()).isNull();
    }
}
