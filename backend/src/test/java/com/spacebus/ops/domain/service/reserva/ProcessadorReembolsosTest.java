package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.repository.reserva.PagamentoRepository;
import com.spacebus.ops.domain.repository.reserva.ReservaHistoricoRepository;
import com.spacebus.ops.domain.repository.usuario.UsuarioRepository;
import com.spacebus.ops.domain.service.pagamento.ClienteProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamento;
import com.spacebus.ops.domain.service.pagamento.ProvedorPagamentoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessadorReembolsosTest {

    private static final int MAX_TENTATIVAS = 3;

    @Mock private PagamentoRepository pagamentoRepository;
    @Mock private ReservaHistoricoRepository historicoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProvedorPagamento provedor;
    @Mock private PlatformTransactionManager transacoes;

    private ProcessadorReembolsos processador;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        processador = new ProcessadorReembolsos(pagamentoRepository, historicoRepository, usuarioRepository,
                new ClienteProvedorPagamento(provedor, 1_000), transacoes, MAX_TENTATIVAS);
        var reserva = new Reserva();
        reserva.setId("rs1");
        pagamento = new Pagamento(reserva, "sim_1", "PIX_SIMULADO", new BigDecimal("37.00"), StatusPagamento.REEMBOLSO_PENDENTE, "/x");
        pagamento.setId("pg1");
        lenient().when(pagamentoRepository.findById("pg1")).thenReturn(Optional.of(pagamento));
        lenient().when(pagamentoRepository.travar("pg1")).thenReturn(Optional.of(pagamento));
    }

    @Test
    void reembolsaDepoisDoCommitERegistraQuemPediu() {
        processador.aoSolicitar(new ReembolsoSolicitado("pg1", OrigemAcao.PASSAGEIRO, "u1"));

        verify(provedor).reembolsar("sim_1");
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThat(acoes()).containsExactly(AcaoReserva.REEMBOLSADA);
        assertThat(historico().getFirst().getOrigem()).isEqualTo(OrigemAcao.PASSAGEIRO);
        verify(usuarioRepository).getReferenceById("u1");
    }

    /** QA N2: a chamada externa fica entre duas transações; só o resultado trava a cobrança. */
    @Test
    void provedorEChamadoSemTravaSegura() {
        processador.tentar("pg1", OrigemAcao.SISTEMA, null);

        var ordem = inOrder(transacoes, provedor, pagamentoRepository);
        ordem.verify(transacoes).commit(any());
        ordem.verify(provedor).reembolsar("sim_1");
        ordem.verify(transacoes).getTransaction(any());
        ordem.verify(pagamentoRepository).travar("pg1");
        ordem.verify(transacoes).commit(any());
    }

    @Test
    void falhaDoProvedorDeixaPendenteEAvisaUmaVezSo() {
        doThrow(new ProvedorPagamentoException("fora do ar", null)).when(provedor).reembolsar("sim_1");

        assertThat(processador.tentar("pg1", OrigemAcao.PASSAGEIRO, "u1")).isFalse();
        assertThat(processador.tentar("pg1", OrigemAcao.SISTEMA, null)).isFalse();

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REEMBOLSO_PENDENTE);
        assertThat(pagamento.getTentativasReembolso()).isEqualTo(2);
        assertThat(acoes()).containsExactly(AcaoReserva.REEMBOLSO_PENDENTE);
    }

    @Test
    void erroInesperadoNaoDerrubaAOperacaoJaGravada() {
        when(pagamentoRepository.findById("pg1")).thenThrow(new IllegalStateException("banco fora"));

        processador.aoSolicitar(new ReembolsoSolicitado("pg1", OrigemAcao.PASSAGEIRO, "u1"));

        verify(provedor, never()).reembolsar(anyString());
    }

    @Test
    void tentativasSaoLimitadas() {
        pagamento.setTentativasReembolso(MAX_TENTATIVAS);

        assertThat(processador.tentar("pg1", OrigemAcao.SISTEMA, null)).isFalse();

        verify(provedor, never()).reembolsar(anyString());
    }

    @Test
    void novaTentativaAgendadaConcluiOsPendentes() {
        when(pagamentoRepository.reembolsosPendentes(MAX_TENTATIVAS)).thenReturn(List.of("pg1"));

        assertThat(processador.tentarPendentes()).isEqualTo(1);

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REEMBOLSADO);
        assertThat(historico().getFirst().getOrigem()).isEqualTo(OrigemAcao.SISTEMA);
    }

    /** Duas tentativas ao mesmo tempo (depois do commit e a agendada): o provedor é idempotente e o histórico não duplica. */
    @Test
    void reembolsoJaConcluidoNaoChamaOProvedorDeNovo() {
        pagamento.setStatus(StatusPagamento.REEMBOLSADO);

        assertThat(processador.tentar("pg1", OrigemAcao.SISTEMA, null)).isFalse();

        verify(provedor, never()).reembolsar(anyString());
        verify(historicoRepository, never()).save(any());
    }

    @Test
    void outraTentativaQueConcluiuAntesNaoDuplicaHistorico() {
        org.mockito.Mockito.doAnswer(inv -> {
            pagamento.setStatus(StatusPagamento.REEMBOLSADO); // a tentativa concorrente gravou primeiro
            return null;
        }).when(provedor).reembolsar("sim_1");

        assertThat(processador.tentar("pg1", OrigemAcao.SISTEMA, null)).isFalse();

        verify(historicoRepository, never()).save(any());
    }

    private List<ReservaHistorico> historico() {
        var captor = ArgumentCaptor.forClass(ReservaHistorico.class);
        verify(historicoRepository, atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    private List<AcaoReserva> acoes() {
        return historico().stream().map(ReservaHistorico::getAcao).toList();
    }
}
