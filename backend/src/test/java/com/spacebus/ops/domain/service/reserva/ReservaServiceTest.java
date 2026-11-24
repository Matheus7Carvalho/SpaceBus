package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.MotivoCancelamento;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest.Resultado;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaHistoricoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.pagamento.ClienteProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamentoException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    /** 17/11/2026 09:00 no fuso da operação. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("America/Araguaina"));
    private static final LocalDateTime AGORA = LocalDateTime.now(CLOCK);

    @Mock private ReservaRepository reservaRepository;
    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private ReservaHistoricoRepository historicoRepository;
    @Mock private ViagemRepository viagemRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProvedorPagamento provedor;
    @Mock private ApplicationEventPublisher eventos;
    @Mock private PlatformTransactionManager transacoes;

    private ReservaService service;
    private Viagem viagem;
    private final Usuario paula = new Usuario("Paula", "passageiro@spacebus.local", "h", Perfil.PASSAGEIRO, true);

    @BeforeEach
    void setUp() {
        service = new ReservaService(reservaRepository, pagamentoRepository, historicoRepository, viagemRepository, usuarioRepository,
                new ClienteProvedorPagamento(provedor, 1_000), eventos, transacoes, CLOCK, 10, 2);
        var pontos = new ArrayList<PontoParada>();
        for (int i = 1; i <= 4; i++) {
            var p = new PontoParada("Ponto " + i, i == 1 ? "Plataforma 3" : null);
            p.setOrdem(i);
            pontos.add(p);
        }
        var rota = new Rota("Centro – Aeroporto", "Centro", "Aeroporto", pontos);
        viagem = new Viagem(rota, new Veiculo("SBV1A23", "Van", TipoVeiculo.VAN, 15, true), null,
                AGORA.plusDays(1), new BigDecimal("18.50"), 15, StatusViagem.PUBLICADA);
        viagem.setId("vg1");
        paula.setId("u1");
        lenient().when(viagemRepository.travar("vg1")).thenReturn(Optional.of(viagem));
        lenient().when(usuarioRepository.findById("u1")).thenReturn(Optional.of(paula));
        lenient().when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(pagamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(provedor.metodo()).thenReturn("PIX_SIMULADO");
    }

    private static ReservaRequest pedido(int embarque, int desembarque, int quantidade) {
        return new ReservaRequest("vg1", embarque, desembarque, quantidade);
    }

    private Reserva reserva(StatusReserva status, LocalDateTime expiraEm) {
        var r = new Reserva();
        r.setId("rs1");
        r.setCodigo("SB-ABC234");
        r.setViagem(viagem);
        r.setPassageiro(paula);
        r.setQuantidade(2);
        r.setValorTotal(new BigDecimal("37.00"));
        r.setStatus(status);
        r.setExpiraEm(expiraEm);
        lenient().when(reservaRepository.findByIdAndPassageiroId("rs1", "u1")).thenReturn(Optional.of(r));
        lenient().when(reservaRepository.viagemDaReservaDoPassageiro("rs1", "u1")).thenReturn(Optional.of("vg1"));
        lenient().when(reservaRepository.travar("rs1")).thenReturn(Optional.of(r));
        return r;
    }

    private Pagamento pagamento(Reserva r, StatusPagamento status) {
        var p = new Pagamento(r, "sim_1", "PIX_SIMULADO", r.getValorTotal(), status, "/reservas/pagamento/sim_1");
        p.setId("pg1");
        lenient().when(pagamentoRepository.travarPorProvedorId("sim_1")).thenReturn(Optional.of(p));
        lenient().when(pagamentoRepository.viagemEReservaDaCobranca("sim_1")).thenReturn(List.<Object[]>of(new Object[]{"vg1", "rs1"}));
        return p;
    }

    // --- reservar -----------------------------------------------------------------------------

    @Test
    void reservaBloqueiaOsLugaresPor10MinutosComCodigoTrechoETotal() {
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "")).thenReturn(13);

        var r = service.reservar("u1", pedido(1, 4, 2));

        assertThat(r.getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        assertThat(r.getExpiraEm()).isEqualTo(AGORA.plusMinutes(10));
        assertThat(r.getCodigo()).matches("SB-[A-HJ-KM-NP-Z2-9]{6}");
        assertThat(r.getEmbarqueNome()).isEqualTo("Ponto 1");
        assertThat(r.getEmbarqueReferencia()).isEqualTo("Plataforma 3");
        assertThat(r.getDesembarqueNome()).isEqualTo("Ponto 4");
        assertThat(r.getValorTotal()).isEqualByComparingTo("37.00");
        assertThat(r.getPassageiro()).isSameAs(paula);
        verify(viagemRepository).travar("vg1"); // a conta de lugares é feita com a viagem travada
    }

    @Test
    void naoVendeAlemDaCapacidade() {
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "")).thenReturn(14);

        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 2))).hasMessage("Não há lugares suficientes. Restam 1.");

        when(reservaRepository.lugaresOcupados("vg1", AGORA, "")).thenReturn(15);
        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 1))).hasMessage("Não há lugares disponíveis nesta viagem.");
        verify(reservaRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = StatusViagem.class, names = {"RASCUNHO", "SUSPENSA", "CANCELADA"})
    void soReservaViagemPublicada(StatusViagem status) {
        viagem.setStatus(status);

        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 1))).hasMessage("Esta viagem não está mais disponível.");
    }

    @Test
    void naoReservaViagemQueJaPartiuOuInexistente() {
        viagem.setPartida(AGORA);
        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 1))).hasMessage("Esta viagem não está mais disponível.");

        when(viagemRepository.travar("vg9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reservar("u1", new ReservaRequest("vg9", 1, 4, 1))).hasMessage("Esta viagem não está mais disponível.");
    }

    @Test
    void trechoPrecisaSeguirOPercursoEExistirNaRota() {
        assertThatThrownBy(() -> service.reservar("u1", pedido(3, 2, 1))).hasMessage("O embarque deve vir antes do desembarque no percurso.");
        assertThatThrownBy(() -> service.reservar("u1", pedido(2, 2, 1))).hasMessage("O embarque deve vir antes do desembarque no percurso.");
        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 9, 1))).hasMessage("Ponto de parada não encontrado nesta rota.");
    }

    @Test
    void quantidadeDe1A6() {
        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 7))).hasMessage("Reserve de 1 a 6 lugares.");
        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 0))).hasMessage("Reserve de 1 a 6 lugares.");
    }

    @Test
    void soPassageiroAtivoReserva() {
        paula.setPerfil(Perfil.OPERADOR);

        assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 1))).hasMessage("Só passageiros podem reservar.");
    }

    @Test
    void reservaDeOutroPassageiroNaoEEncontrada() {
        when(reservaRepository.findByIdAndPassageiroId("rs1", "outro")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.minha("rs1", "outro")).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.iniciarPagamento("rs1", "outro")).isInstanceOf(EntityNotFoundException.class);
        assertThatThrownBy(() -> service.cancelar("rs1", "outro")).isInstanceOf(EntityNotFoundException.class);
    }

    // --- pagamento ----------------------------------------------------------------------------

    @Test
    void iniciaPagamentoCriandoCobrancaNoProvedor() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(provedor.criarCobranca("rs1-0", "SB-ABC234", r.getValorTotal())).thenReturn(new ProvedorPagamento.Cobranca("sim_1", "/reservas/pagamento/sim_1"));

        var p = service.iniciarPagamento("rs1", "u1");

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(p.getProvedorId()).isEqualTo("sim_1");
        assertThat(p.getValor()).isEqualByComparingTo("37.00");
    }

    @Test
    void naoCriaSegundaCobrancaPendente() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var pendente = pagamento(r, StatusPagamento.PENDENTE);
        when(pagamentoRepository.findFirstByReservaIdAndStatus("rs1", StatusPagamento.PENDENTE)).thenReturn(Optional.of(pendente));

        assertThat(service.iniciarPagamento("rs1", "u1")).isSameAs(pendente);
        verify(provedor, never()).criarCobranca(anyString(), anyString(), any());
    }

    @Test
    void falhaDoProvedorViraMensagemENaoMudaNada() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(provedor.criarCobranca(anyString(), anyString(), any())).thenThrow(new ProvedorPagamentoException("timeout", null));

        assertThatThrownBy(() -> service.iniciarPagamento("rs1", "u1"))
                .isInstanceOf(DomainException.class)
                .hasMessage("Não foi possível iniciar o pagamento agora. Tente de novo em instantes.");
        assertThat(r.getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void naoPagaReservaVencidaOuConfirmada() {
        reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA);
        assertThatThrownBy(() -> service.iniciarPagamento("rs1", "u1")).hasMessage("O prazo para pagar esta reserva acabou. Faça uma nova reserva.");

        reserva(StatusReserva.CONFIRMADA, AGORA.plusMinutes(5));
        assertThatThrownBy(() -> service.iniciarPagamento("rs1", "u1")).hasMessage("Esta reserva não está aguardando pagamento.");
        verifyNoInteractions(provedor);
    }

    /** QA N2: o provedor nunca é chamado com a trava segura; trava e gravação são duas transações curtas. */
    @Test
    void provedorEChamadoEntreDuasTransacoesCurtas() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(provedor.criarCobranca(anyString(), anyString(), any())).thenReturn(new ProvedorPagamento.Cobranca("sim_1", "/x"));

        service.iniciarPagamento("rs1", "u1");

        var ordem = inOrder(transacoes, viagemRepository, provedor, pagamentoRepository);
        ordem.verify(transacoes).getTransaction(any());
        ordem.verify(viagemRepository).travar("vg1");
        ordem.verify(transacoes).commit(any());
        ordem.verify(provedor).criarCobranca("rs1-0", "SB-ABC234", r.getValorTotal());
        ordem.verify(transacoes).getTransaction(any());
        ordem.verify(viagemRepository).travar("vg1");
        ordem.verify(pagamentoRepository).save(any());
        ordem.verify(transacoes).commit(any());
    }

    @Test
    void chaveDeIdempotenciaMudaDepoisDeUmaCobrancaRecusada() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(pagamentoRepository.countByReservaId("rs1")).thenReturn(1L);
        when(provedor.criarCobranca("rs1-1", "SB-ABC234", r.getValorTotal())).thenReturn(new ProvedorPagamento.Cobranca("sim_2", "/x"));

        assertThat(service.iniciarPagamento("rs1", "u1").getProvedorId()).isEqualTo("sim_2");
    }

    /** Chamada simultânea com a mesma chave já gravou a cobrança: devolve a mesma, sem duplicar. */
    @Test
    void cobrancaJaGravadaPorChamadaSimultaneaEDevolvida() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var gravada = new Pagamento(r, "sim_1", "PIX_SIMULADO", r.getValorTotal(), StatusPagamento.PENDENTE, "/x");
        when(provedor.criarCobranca(anyString(), anyString(), any())).thenReturn(new ProvedorPagamento.Cobranca("sim_1", "/x"));
        when(pagamentoRepository.findByProvedorId("sim_1")).thenReturn(Optional.of(gravada));

        assertThat(service.iniciarPagamento("rs1", "u1")).isSameAs(gravada);
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void reservaQueVenceEnquantoOProvedorRespondeNaoGravaACobranca() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(provedor.criarCobranca(anyString(), anyString(), any())).thenAnswer(inv -> {
            r.setExpiraEm(AGORA);
            return new ProvedorPagamento.Cobranca("sim_1", "/x");
        });

        assertThatThrownBy(() -> service.iniciarPagamento("rs1", "u1")).hasMessage("O prazo para pagar esta reserva acabou. Faça uma nova reserva.");
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    void aprovacaoNoPrazoConfirmaAReserva() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(r.getConfirmadaEm()).isEqualTo(AGORA);
        verify(viagemRepository).travar("vg1");
    }

    @Test
    void retornoRepetidoNaoMudaNada() {
        var r = reserva(StatusReserva.CONFIRMADA, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.APROVADO);

        service.processarRetorno("sim_1", Resultado.RECUSADO);
        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        verifyNoInteractions(provedor);
    }

    @Test
    void recusaMantemAReservaAguardandoParaNovaTentativa() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.RECUSADO);

        assertThat(p.getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        assertThat(service.pagavel(r)).isTrue();
    }

    @Test
    void aprovacaoForaDoPrazoConfirmaSeAindaHouverLugar() {
        var r = reserva(StatusReserva.EXPIRADA, AGORA.minusMinutes(1));
        pagamento(r, StatusPagamento.PENDENTE);
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "rs1")).thenReturn(13);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void aprovacaoForaDoPrazoSemLugarExpiraEReembolsa() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.minusMinutes(1));
        var p = pagamento(r, StatusPagamento.PENDENTE);
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "rs1")).thenReturn(14);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        verify(eventos).publishEvent(new ReembolsoSolicitado("pg1", OrigemAcao.PROVEDOR, null));
        verify(provedor, never()).reembolsar(anyString());
    }

    @Test
    void aprovacaoDeReservaJaCanceladaReembolsa() {
        var r = reserva(StatusReserva.CANCELADA, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        verify(eventos).publishEvent(new ReembolsoSolicitado("pg1", OrigemAcao.PROVEDOR, null));
    }

    @Test
    void retornoDeCobrancaDesconhecida() {
        assertThatThrownBy(() -> service.processarRetorno("x", Resultado.APROVADO)).isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(viagemRepository);
    }

    // --- cancelamento -------------------------------------------------------------------------

    @Test
    void passageiroCancelaEReembolsaPagamentoAprovado() {
        var r = reserva(StatusReserva.CONFIRMADA, AGORA.minusMinutes(5));
        var p = pagamento(r, StatusPagamento.APROVADO);
        when(pagamentoRepository.findByReservaIdAndStatus("rs1", StatusPagamento.APROVADO)).thenReturn(List.of(p));

        service.cancelar("rs1", "u1");

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(r.getMotivoCancelamento()).isEqualTo(MotivoCancelamento.PASSAGEIRO);
        assertThat(r.getCanceladaEm()).isEqualTo(AGORA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        verify(eventos).publishEvent(new ReembolsoSolicitado("pg1", OrigemAcao.PASSAGEIRO, "u1"));
    }

    @Test
    void cancelamentoAteDuasHorasAntesInclusive() {
        viagem.setPartida(AGORA.plusHours(2).minusMinutes(1));
        reserva(StatusReserva.CONFIRMADA, AGORA.minusMinutes(5));

        assertThatThrownBy(() -> service.cancelar("rs1", "u1")).hasMessage("O cancelamento é permitido até 2 horas antes da partida.");

        viagem.setPartida(AGORA.plusHours(2)); // exatamente 2 horas antes ainda pode
        assertThat(service.cancelar("rs1", "u1").getStatus()).isEqualTo(StatusReserva.CANCELADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusReserva.class, names = {"EXPIRADA", "CANCELADA"})
    void naoCancelaReservaEncerrada(StatusReserva status) {
        reserva(status, AGORA.plusMinutes(5));

        assertThatThrownBy(() -> service.cancelar("rs1", "u1")).hasMessage("Esta reserva não pode mais ser cancelada.");
    }

    /** QA N2: o reembolso não é pedido com a trava segura; sai depois do commit (ProcessadorReembolsos). */
    @Test
    void cancelamentoPedeOReembolsoSoDepoisDoCommit() {
        var r = reserva(StatusReserva.CONFIRMADA, AGORA.minusMinutes(5));
        var p = pagamento(r, StatusPagamento.APROVADO);
        when(pagamentoRepository.findByReservaIdAndStatus("rs1", StatusPagamento.APROVADO)).thenReturn(List.of(p));

        service.cancelar("rs1", "u1");

        verifyNoInteractions(provedor);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        assertThat(acoesRegistradas()).containsExactly(AcaoReserva.CANCELADA);
    }

    @Test
    void cancelarPorViagemMarcaParaAvisar() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(reservaRepository.idsDaViagemComStatus(eq("vg1"), any())).thenReturn(List.of("rs1"));

        service.cancelarPorViagem("vg1", "op1");

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(r.getMotivoCancelamento()).isEqualTo(MotivoCancelamento.VIAGEM_CANCELADA);
        var status = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(reservaRepository).idsDaViagemComStatus(eq("vg1"), status.capture());
        assertThat(status.getValue()).containsExactlyInAnyOrder(StatusReserva.AGUARDANDO_PAGAMENTO, StatusReserva.CONFIRMADA);
    }

    // --- disponibilidade ----------------------------------------------------------------------

    @Test
    void lugaresDisponiveisNuncaNegativos() {
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "")).thenReturn(4, 20);

        assertThat(service.lugaresDisponiveis(viagem)).isEqualTo(11);
        assertThat(service.lugaresDisponiveis(viagem)).isZero();
    }

    @Test
    void expiraAsVencidasTravadasERegistraHistorico() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.minusMinutes(1));
        when(reservaRepository.travarVencidas(AGORA)).thenReturn(List.of(r));

        assertThat(service.expirarVencidas()).isEqualTo(1);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(acoesRegistradas()).containsExactly(AcaoReserva.EXPIRADA);
        assertThat(origensRegistradas()).containsExactly(OrigemAcao.SISTEMA);
    }

    // --- travas e histórico (QA D1/D2/D3) -------------------------------------------------------

    /** Retorno do provedor trava viagem, depois reserva, depois pagamento, antes de ler a situação. */
    @Test
    void retornoTravaViagemDepoisReservaDepoisPagamento() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        var ordem = inOrder(viagemRepository, reservaRepository, pagamentoRepository);
        ordem.verify(viagemRepository).travar("vg1");
        ordem.verify(reservaRepository).travar("rs1");
        ordem.verify(pagamentoRepository).travarPorProvedorId("sim_1");
    }

    @Test
    void cancelamentoDoPassageiroTravaViagemAntesDaReserva() {
        reserva(StatusReserva.CONFIRMADA, AGORA.minusMinutes(5));

        service.cancelar("rs1", "u1");

        var ordem = inOrder(viagemRepository, reservaRepository);
        ordem.verify(viagemRepository).travar("vg1");
        ordem.verify(reservaRepository).travar("rs1");
    }

    @Test
    void iniciarPagamentoTravaAViagem() {
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        when(provedor.criarCobranca(anyString(), anyString(), any())).thenReturn(new ProvedorPagamento.Cobranca("sim_1", "/x"));

        service.iniciarPagamento("rs1", "u1");

        verify(viagemRepository, org.mockito.Mockito.times(2)).travar("vg1"); // antes e depois da chamada ao provedor
        assertThat(acoesRegistradas()).containsExactly(AcaoReserva.PAGAMENTO_INICIADO);
        assertThat(r.getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void aprovacaoNoPrazoComViagemJaCanceladaReembolsa() {
        viagem.setStatus(StatusViagem.CANCELADA);
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isNotEqualTo(StatusReserva.CONFIRMADA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
    }

    /** QA N1: aprovação que chega depois da partida não confirma; o valor volta ao passageiro. */
    @Test
    void aprovacaoDepoisDaPartidaNaoConfirmaEReembolsa() {
        viagem.setPartida(AGORA.minusMinutes(1));
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5)); // ainda no prazo de pagamento
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
    }

    /** Módulo 4: viagem iniciada (ainda antes da partida) não confirma aprovação que chega depois: expira e reembolsa. */
    @Test
    void aprovacaoDepoisDoInicioDaViagemNaoConfirmaEReembolsa() {
        viagem.setStatus(StatusViagem.EM_ANDAMENTO);
        viagem.setPartida(AGORA.plusMinutes(20));
        var r = reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.plusMinutes(5));
        var p = pagamento(r, StatusPagamento.PENDENTE);

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(r.getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(p.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        assertThat(historicoGravado()).filteredOn(h -> h.getAcao() == AcaoReserva.EXPIRADA)
                .extracting(ReservaHistorico::getDetalhe).containsExactly("Pagamento aprovado depois do início da viagem.");
    }

    @Test
    void naoReservaViagemEmAndamentoOuEncerrada() {
        for (var status : List.of(StatusViagem.EM_ANDAMENTO, StatusViagem.ENCERRADA)) {
            viagem.setStatus(status);
            assertThatThrownBy(() -> service.reservar("u1", pedido(1, 4, 1))).hasMessage("Esta viagem não está mais disponível.");
        }
        verify(reservaRepository, never()).save(any());
    }

    /** QA N6: reserva vencida (ainda não marcada) explica o motivo certo. */
    @Test
    void cancelarReservaVencidaExplicaOPrazoDePagamento() {
        reserva(StatusReserva.AGUARDANDO_PAGAMENTO, AGORA.minusMinutes(1));

        assertThatThrownBy(() -> service.cancelar("rs1", "u1")).hasMessage("O prazo para pagar esta reserva acabou; ela já não segura lugares.");
    }

    @Test
    void historicoDaReservaEDoPagamento() {
        when(reservaRepository.lugaresOcupados("vg1", AGORA, "")).thenReturn(0);
        var r = service.reservar("u1", pedido(1, 4, 2));
        r.setId("rs1");
        pagamento(r, StatusPagamento.PENDENTE);
        lenient().when(reservaRepository.travar("rs1")).thenReturn(Optional.of(r));

        service.processarRetorno("sim_1", Resultado.APROVADO);

        assertThat(acoesRegistradas()).containsExactly(AcaoReserva.RESERVADA, AcaoReserva.PAGAMENTO_APROVADO, AcaoReserva.CONFIRMADA);
        assertThat(origensRegistradas()).containsExactly(OrigemAcao.PASSAGEIRO, OrigemAcao.PROVEDOR, OrigemAcao.PROVEDOR);
        verify(usuarioRepository).getReferenceById("u1");
    }

    private List<ReservaHistorico> historicoGravado() {
        var captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(historicoRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    private List<AcaoReserva> acoesRegistradas() {
        return historicoGravado().stream().map(ReservaHistorico::getAcao).toList();
    }

    private List<OrigemAcao> origensRegistradas() {
        return historicoGravado().stream().map(ReservaHistorico::getOrigem).toList();
    }
}
