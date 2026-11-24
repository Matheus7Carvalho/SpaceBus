package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.core.exception.DomainException;
import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest.Resultado;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaHistoricoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamento;
import com.spacebus.ops.domain.service.viagem.IViagemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Concorrência com PostgreSQL real (travas SELECT ... FOR UPDATE de verdade). Reproduz os cenários do QA:
 * duas reservas da última vaga; retorno do provedor enquanto o operador muda a capacidade ou cancela a viagem;
 * retorno do provedor enquanto o passageiro cancela; expiração em lote junto do retorno e do cancelamento da viagem;
 * provedor lento sem segurar a viagem (N2). Sem Docker, a classe é pulada.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.docker.compose.enabled=false",
        "spring.liquibase.contexts=teste",
        "spacebus.agendamentos.ativos=false",
        "spacebus.pagamento.provedor=simulado",
        "spacebus.pagamento.webhook-segredo=teste",
})
@ActiveProfiles("teste")
class ReservaConcorrenciaIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private IReservaService reservas;
    @Autowired private IViagemService viagens;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagamentoRepository pagamentoRepository;
    @Autowired private ReservaHistoricoRepository historicoRepository;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TransactionTemplate transacao;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private ProvedorPagamento provedor;

    private String passageiroId;
    private String operadorId;
    private String viagemId;

    @BeforeEach
    void setUp() {
        var sufixo = UUID.randomUUID().toString().substring(0, 8);
        passageiroId = usuarioRepository.save(new Usuario("Paula", "p-" + sufixo + "@teste", "h", Perfil.PASSAGEIRO, true)).getId();
        operadorId = usuarioRepository.save(new Usuario("Ana", "o-" + sufixo + "@teste", "h", Perfil.OPERADOR, true)).getId();
        var placa = "T" + sufixo.substring(0, 2).toUpperCase().replaceAll("[^A-Z]", "X") + "1A" + (10 + (int) (Math.random() * 89));
        var van = veiculoRepository.save(new Veiculo(placa.substring(0, 7), "Van", TipoVeiculo.VAN, 15, true));
        var rota = rotaRepository.save(new Rota("Rota " + sufixo, "A", "B", List.of(ponto("A", 1), ponto("B", 2))));
        viagemId = viagemRepository.save(new Viagem(rota, van, null, LocalDateTime.now().plusDays(5), new BigDecimal("15.00"), 10,
                StatusViagem.PUBLICADA)).getId();
    }

    private static PontoParada ponto(String nome, int ordem) {
        var p = new PontoParada(nome, null);
        p.setOrdem(ordem);
        return p;
    }

    private ReservaRequest pedido(int quantidade) {
        return new ReservaRequest(viagemId, 1, 2, quantidade);
    }

    /** Reserva com pagamento pendente; {@code vencida} coloca o prazo no passado. */
    private Pagamento reservaComPagamentoPendente(int quantidade, boolean vencida) {
        var reserva = reservas.reservar(passageiroId, pedido(quantidade));
        if (vencida) {
            jdbc.update("update reserva set expira_em = ? where id = ?", LocalDateTime.now().minusMinutes(1), reserva.getId());
        }
        var pagamento = new Pagamento(reserva, "sim_" + UUID.randomUUID().toString().replace("-", ""), "PIX_SIMULADO",
                reserva.getValorTotal(), StatusPagamento.PENDENTE, "/x");
        return pagamentoRepository.save(pagamento);
    }

    /**
     * Segura a trava da viagem numa transação (fazendo {@code acao} dentro dela), dispara {@code concorrente}
     * em outra thread, espera o concorrente ficar bloqueado na trava do Postgres e só então confirma.
     */
    private void comTravaSegurando(Runnable acao, Runnable concorrente) throws Exception {
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var dono = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(s -> {
            viagemRepository.travar(viagemId);
            acao.run();
            travou.countDown();
            aguardar(liberar);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
        var outro = CompletableFuture.runAsync(concorrente);
        esperarAlguemBloqueado();
        liberar.countDown();
        dono.get(10, TimeUnit.SECONDS);
        outro.get(10, TimeUnit.SECONDS);
    }

    private void esperarAlguemBloqueado() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            var bloqueados = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where wait_event_type = 'Lock' and datname = current_database()", Integer.class);
            if (bloqueados != null && bloqueados > 0) return;
            Thread.sleep(50);
        }
        throw new AssertionError("O concorrente não chegou a esperar a trava da viagem.");
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tempo esgotado segurando a trava.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Reserva reserva(String id) {
        return reservaRepository.findById(id).orElseThrow();
    }

    @Test
    void duasReservasSimultaneasDaUltimaVagaUmaSoPassa() throws Exception {
        jdbc.update("update viagem set capacidade = 1 where id = ?", viagemId);
        var largada = new CountDownLatch(1);
        var tentativas = List.of(1, 2).stream().map(i -> CompletableFuture.supplyAsync(() -> {
            aguardar(largada);
            try {
                reservas.reservar(passageiroId, pedido(1));
                return "ok";
            } catch (DomainException e) {
                return e.getMessage();
            }
        })).toList();
        largada.countDown();

        var resultados = tentativas.stream().map(CompletableFuture::join).sorted().toList();

        assertThat(resultados).containsExactly("Não há lugares disponíveis nesta viagem.", "ok");
        assertThat(reservaRepository.findByViagemIdOrderByDataCriacao(viagemId)).hasSize(1);
    }

    /** QA D1 (A): aprovação fora do prazo decide com a capacidade já reduzida pelo operador, não com a antiga. */
    @Test
    void retornoForaDoPrazoUsaACapacidadeNovaEReembolsa() throws Exception {
        reservas.reservar(passageiroId, pedido(5)); // 5 lugares ocupados (aguardando no prazo)
        var pagamento = reservaComPagamentoPendente(2, true);

        comTravaSegurando(
                () -> jdbc.update("update viagem set capacidade = 5 where id = ?", viagemId),
                () -> reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO));

        var reservaId = pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getReserva().getId();
        assertThat(reserva(reservaId).getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThat(reservas.lugaresOcupados(viagemId)).isEqualTo(5);
    }

    /** QA D1 (B): aprovação chegando junto do cancelamento da viagem não confirma reserva em viagem cancelada. */
    @Test
    void retornoDuranteCancelamentoDaViagemReembolsa() throws Exception {
        var pagamento = reservaComPagamentoPendente(2, false);
        var reservaId = pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getReserva().getId();

        comTravaSegurando(
                () -> viagens.cancelar(viagemId, operadorId),
                () -> reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO));

        assertThat(reserva(reservaId).getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThat(historicoRepository.findByReservaIdOrderByOrdemAsc(reservaId))
                .extracting(h -> h.getAcao())
                .containsSubsequence(AcaoReserva.RESERVADA, AcaoReserva.CANCELADA, AcaoReserva.PAGAMENTO_APROVADO, AcaoReserva.REEMBOLSADA);
    }

    /** QA D3: passageiro cancelando enquanto o provedor aprova: o pagamento aprovado não fica sem reembolso. */
    @Test
    void retornoDuranteCancelamentoDoPassageiroReembolsa() throws Exception {
        var pagamento = reservaComPagamentoPendente(1, false);
        var reservaId = pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getReserva().getId();

        comTravaSegurando(
                () -> reservas.cancelar(reservaId, passageiroId),
                () -> reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO));

        assertThat(reserva(reservaId).getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
    }

    /** Segura numa transação a expiração em lote (travas das reservas vencidas) enquanto {@code concorrente} roda. */
    private void comExpiracaoSegurando(Runnable concorrente) throws Exception {
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var dono = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(s -> {
            assertThat(reservas.expirarVencidas()).isPositive();
            travou.countDown();
            aguardar(liberar);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
        var outro = CompletableFuture.runAsync(concorrente);
        esperarAlguemBloqueado();
        liberar.countDown();
        dono.get(10, TimeUnit.SECONDS);
        outro.get(10, TimeUnit.SECONDS);
    }

    /** Lacuna do QA: aprovação chegando durante a expiração em lote espera, e confirma se ainda há lugar. */
    @Test
    void retornoDuranteExpiracaoEmLoteConfirmaSeAindaHouverLugar() throws Exception {
        var pagamento = reservaComPagamentoPendente(2, true);
        var reservaId = pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getReserva().getId();

        comExpiracaoSegurando(() -> reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO));

        assertThat(reserva(reservaId).getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(historicoRepository.findByReservaIdOrderByOrdemAsc(reservaId)).extracting(h -> h.getAcao())
                .containsSubsequence(AcaoReserva.EXPIRADA, AcaoReserva.PAGAMENTO_APROVADO, AcaoReserva.CONFIRMADA);
    }

    /** Lacuna do QA: cancelar a viagem durante a expiração em lote não sobrescreve a reserva já expirada. */
    @Test
    void cancelamentoDaViagemDuranteExpiracaoNaoSobrescreve() throws Exception {
        var vencida = reservaComPagamentoPendente(1, true).getReserva().getId();
        var ativa = reservas.reservar(passageiroId, pedido(1)).getId();

        comExpiracaoSegurando(() -> viagens.cancelar(viagemId, operadorId));

        assertThat(reserva(vencida).getStatus()).isEqualTo(StatusReserva.EXPIRADA);
        assertThat(reserva(ativa).getStatus()).isEqualTo(StatusReserva.CANCELADA);
        assertThat(historicoRepository.findByReservaIdOrderByOrdemAsc(vencida)).extracting(h -> h.getAcao())
                .doesNotContain(AcaoReserva.CANCELADA);
    }

    /** Lacuna do QA, ordem inversa: a expiração em lote pula a reserva travada pelo cancelamento (SKIP LOCKED). */
    @Test
    void expiracaoDuranteCancelamentoDaViagemPulaAReservaTravada() throws Exception {
        var vencida = reservaComPagamentoPendente(1, true).getReserva().getId();
        var expiradas = new java.util.concurrent.atomic.AtomicInteger(-1);
        var travou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);

        var dono = CompletableFuture.runAsync(() -> transacao.executeWithoutResult(s -> {
            viagens.cancelar(viagemId, operadorId);
            travou.countDown();
            aguardar(liberar);
        }));
        assertThat(travou.await(10, TimeUnit.SECONDS)).isTrue();
        CompletableFuture.runAsync(() -> expiradas.set(reservas.expirarVencidas())).get(10, TimeUnit.SECONDS);
        liberar.countDown();
        dono.get(10, TimeUnit.SECONDS);

        assertThat(expiradas.get()).isZero();
        assertThat(reserva(vencida).getStatus()).isEqualTo(StatusReserva.CANCELADA);
    }

    /** QA N2: enquanto o provedor demora para criar a cobrança, a viagem não fica travada para outras reservas. */
    @Test
    void provedorLentoNaoSeguraAViagem() throws Exception {
        var reserva = reservas.reservar(passageiroId, pedido(1));
        var provedorChamado = new CountDownLatch(1);
        var liberarProvedor = new CountDownLatch(1);
        doAnswer(inv -> {
            provedorChamado.countDown();
            aguardar(liberarProvedor);
            return inv.callRealMethod();
        }).when(provedor).criarCobranca(anyString(), anyString(), any());

        var pagamento = CompletableFuture.supplyAsync(() -> reservas.iniciarPagamento(reserva.getId(), passageiroId));
        try {
            assertThat(provedorChamado.await(10, TimeUnit.SECONDS)).isTrue();
            // Com a trava segura durante a chamada, esta reserva ficaria esperando o provedor.
            var outra = CompletableFuture.supplyAsync(() -> reservas.reservar(passageiroId, pedido(1)));
            assertThat(outra.get(5, TimeUnit.SECONDS).getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
        } finally {
            liberarProvedor.countDown();
        }

        assertThat(pagamento.get(10, TimeUnit.SECONDS).getStatus()).isEqualTo(StatusPagamento.PENDENTE);
    }

    /** Duas chamadas de pagamento ao mesmo tempo: uma cobrança só, no provedor e no banco. */
    @Test
    void pagamentoIniciadoDuasVezesAoMesmoTempoGeraUmaCobranca() {
        var reserva = reservas.reservar(passageiroId, pedido(1));
        var largada = new CountDownLatch(1);

        var cobrancas = List.of(1, 2).stream().map(i -> CompletableFuture.supplyAsync(() -> {
            aguardar(largada);
            return reservas.iniciarPagamento(reserva.getId(), passageiroId).getProvedorId();
        })).toList();
        largada.countDown();

        assertThat(cobrancas.stream().map(CompletableFuture::join).distinct()).hasSize(1);
        assertThat(pagamentoRepository.countByReservaId(reserva.getId())).isEqualTo(1);
    }

    @Test
    void retornoRepetidoAoMesmoTempoAplicaUmaVez() {
        var pagamento = reservaComPagamentoPendente(1, false);
        var reservaId = pagamentoRepository.findByProvedorId(pagamento.getProvedorId()).orElseThrow().getReserva().getId();
        var largada = new CountDownLatch(1);

        var chamadas = List.of(1, 2, 3).stream().map(i -> CompletableFuture.runAsync(() -> {
            aguardar(largada);
            reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO);
        })).toList();
        largada.countDown();
        chamadas.forEach(CompletableFuture::join);

        assertThat(reserva(reservaId).getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(historicoRepository.findByReservaIdOrderByOrdemAsc(reservaId))
                .filteredOn(h -> h.getAcao() == AcaoReserva.PAGAMENTO_APROVADO).hasSize(1);
    }
}
