package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.PontoParada;
import com.spacebus.ops.domain.entity.Rota;
import com.spacebus.ops.domain.entity.Usuario;
import com.spacebus.ops.domain.entity.Veiculo;
import com.spacebus.ops.domain.entity.Viagem;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.Perfil;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.reserva.ReservaRequest;
import com.spacebus.ops.domain.model.reserva.RetornoPagamentoRequest.Resultado;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaRepository;
import com.spacebus.ops.domain.repository.rota.RotaRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.repository.veiculo.VeiculoRepository;
import com.spacebus.ops.domain.repository.viagem.ViagemRepository;
import com.spacebus.ops.domain.service.reserva.IReservaService;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

/**
 * Notificações com PostgreSQL real (H18): geração depois do commit, rollback não gera, falha do canal não muda a
 * reserva, webhook repetido em paralelo e lembrete simultâneo geram uma notificação. Sem Docker, a classe é pulada.
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
class NotificacaoIntegracaoTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired private IReservaService reservas;
    @Autowired private IViagemService viagens;
    @Autowired private INotificacaoService notificacoes;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private PagamentoRepository pagamentoRepository;
    @Autowired private ViagemRepository viagemRepository;
    @Autowired private VeiculoRepository veiculoRepository;
    @Autowired private RotaRepository rotaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TransactionTemplate transacao;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private CanalNotificacao canal;

    private String passageiroId;
    private String operadorId;
    private String viagemId;

    @BeforeEach
    void setUp() {
        reset(canal);
        var sufixo = UUID.randomUUID().toString().substring(0, 8);
        passageiroId = usuarioRepository.save(new Usuario("Paula", "p-" + sufixo + "@teste", "h", Perfil.PASSAGEIRO, true)).getId();
        operadorId = usuarioRepository.save(new Usuario("Ana", "o-" + sufixo + "@teste", "h", Perfil.OPERADOR, true)).getId();
        var van = veiculoRepository.save(new Veiculo("N" + sufixo.substring(0, 6).toUpperCase(), "Van", TipoVeiculo.VAN, 15, true));
        var a = new PontoParada("A", null);
        a.setOrdem(1);
        var b = new PontoParada("B", null);
        b.setOrdem(2);
        var rota = rotaRepository.save(new Rota("Rota " + sufixo, "A", "B", List.of(a, b)));
        viagemId = viagemRepository.save(new Viagem(rota, van, null, LocalDateTime.now().plusDays(5).truncatedTo(ChronoUnit.MINUTES),
                new BigDecimal("15.00"), 10, StatusViagem.PUBLICADA)).getId();
    }

    private Pagamento reservaComPagamentoPendente() {
        var reserva = reservas.reservar(passageiroId, new ReservaRequest(viagemId, 1, 2, 1));
        return pagamentoRepository.save(new Pagamento(reserva, "sim_" + UUID.randomUUID().toString().replace("-", ""), "PIX_SIMULADO",
                reserva.getValorTotal(), StatusPagamento.PENDENTE, "/x"));
    }

    private int notificacoesDo(String tipo) {
        return jdbc.queryForObject("select count(*) from notificacao where usuario_id = ? and tipo = ?", Integer.class, passageiroId, tipo);
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Tempo esgotado.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void simultaneos(Runnable acao) {
        var largada = new CountDownLatch(1);
        var futuros = List.of(1, 2).stream().map(i -> CompletableFuture.runAsync(() -> {
            aguardar(largada);
            acao.run();
        })).toList();
        largada.countDown();
        futuros.forEach(CompletableFuture::join);
    }

    @Test
    void webhookDeAprovacaoRepetidoEmParaleloGeraUmaNotificacaoComEnvioPorEmail() {
        var pagamento = reservaComPagamentoPendente();

        simultaneos(() -> reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO));

        assertThat(notificacoesDo("RESERVA_CONFIRMADA")).isEqualTo(1);
        assertThat(jdbc.queryForList("select e.canal from notificacao_envio e join notificacao n on n.id = e.notificacao_id where n.usuario_id = ?",
                String.class, passageiroId)).containsExactly("EMAIL");
    }

    @Test
    void lembreteEmDuasExecucoesSimultaneasGeraUmPorReserva() {
        var reserva = reservaComPagamentoPendente().getReserva();
        jdbc.update("update reserva set status = 'CONFIRMADA' where id = ?", reserva.getId());
        jdbc.update("update viagem set data_hora_partida = ? where id = ?", LocalDateTime.now().plusMinutes(30), viagemId);

        simultaneos(notificacoes::lembrar);
        notificacoes.lembrar();

        assertThat(notificacoesDo("LEMBRETE")).isEqualTo(1);
    }

    @Test
    void rollbackDaOrigemNaoGeraNotificacao() {
        var reserva = reservaComPagamentoPendente().getReserva();

        assertThatThrownBy(() -> transacao.executeWithoutResult(s -> {
            viagens.cancelar(viagemId, operadorId);
            throw new IllegalStateException("falha depois do cancelamento");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(notificacoesDo("VIAGEM_CANCELADA")).isZero();
        assertThat(reservaRepository.findById(reserva.getId()).orElseThrow().getStatus()).isEqualTo(StatusReserva.AGUARDANDO_PAGAMENTO);
    }

    @Test
    void cancelamentoDaViagemAvisaDepoisDoCommit() {
        reservaComPagamentoPendente();

        viagens.cancelar(viagemId, operadorId);

        assertThat(notificacoesDo("VIAGEM_CANCELADA")).isEqualTo(1);
    }

    @Test
    void falhaDoCanalGravaFalhouENaoMudaAReserva() {
        doThrow(new IllegalStateException("canal fora")).when(canal).enviar(eq(CanalEnvio.EMAIL), any());
        var pagamento = reservaComPagamentoPendente();

        reservas.processarRetorno(pagamento.getProvedorId(), Resultado.APROVADO);

        assertThat(reservaRepository.findById(pagamento.getReserva().getId()).orElseThrow().getStatus()).isEqualTo(StatusReserva.CONFIRMADA);
        assertThat(notificacoesDo("RESERVA_CONFIRMADA")).isEqualTo(1);
        assertThat(jdbc.queryForList("select e.status from notificacao_envio e join notificacao n on n.id = e.notificacao_id where n.usuario_id = ?",
                String.class, passageiroId)).containsExactly("FALHOU");
    }

    @Test
    void mudarAPartidaDuasVezesGeraDuasNotificacoes() {
        reservaComPagamentoPendente();
        var viagem = viagemRepository.findById(viagemId).orElseThrow();

        for (var horas : List.of(1, 2)) {
            var alterada = new Viagem(viagem.getRota(), viagem.getVeiculo(), null, viagem.getPartida().plusHours(horas),
                    viagem.getPreco(), viagem.getCapacidade(), viagem.getStatus());
            viagens.update(alterada, viagemId);
        }

        assertThat(notificacoesDo("VIAGEM_ALTERADA")).isEqualTo(2);
    }
}
