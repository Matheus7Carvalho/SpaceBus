package com.spacebus.ops.domain.service.pagamento;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClienteProvedorPagamentoTest {

    /** Provedor que demora mais que o limite em toda operação. */
    private static final class ProvedorLento implements ProvedorPagamento {
        @Override
        public String metodo() {
            return "LENTO";
        }

        @Override
        public Cobranca criarCobranca(String chave, String referencia, BigDecimal valor) {
            dormir();
            return new Cobranca("x", "/x");
        }

        @Override
        public void reembolsar(String provedorId) {
            dormir();
        }

        private static void dormir() {
            try {
                Thread.sleep(Duration.ofSeconds(5));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void provedorQueNaoRespondeNoLimiteViraFalhaDoProvedor() {
        var cliente = new ClienteProvedorPagamento(new ProvedorLento(), 100);
        var inicio = System.nanoTime();

        assertThatThrownBy(() -> cliente.criarCobranca("k", "SB-1", BigDecimal.TEN))
                .isInstanceOf(ProvedorPagamentoException.class)
                .hasMessage("O provedor não respondeu em 100 ms.");
        assertThatThrownBy(() -> cliente.reembolsar("x")).isInstanceOf(ProvedorPagamentoException.class);

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void erroInesperadoDoProvedorViraFalhaDoProvedor() {
        var cliente = new ClienteProvedorPagamento(new ProvedorPagamentoSimulado() {
            @Override
            public void reembolsar(String provedorId) {
                throw new IllegalStateException("resposta inválida");
            }
        }, 1_000);

        assertThatThrownBy(() -> cliente.reembolsar("x"))
                .isInstanceOf(ProvedorPagamentoException.class)
                .hasRootCauseMessage("resposta inválida");
    }

    @Test
    void simuladoDevolveAMesmaCobrancaParaAMesmaChave() {
        var cliente = new ClienteProvedorPagamento(new ProvedorPagamentoSimulado(), 1_000);

        var primeira = cliente.criarCobranca("rs1-0", "SB-1", BigDecimal.TEN);

        assertThat(cliente.criarCobranca("rs1-0", "SB-1", BigDecimal.TEN)).isEqualTo(primeira);
        assertThat(cliente.criarCobranca("rs1-1", "SB-1", BigDecimal.TEN)).isNotEqualTo(primeira);
    }
}
