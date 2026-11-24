package com.spacebus.ops.domain.service.pagamento;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Única porta para o provedor de pagamento: cada chamada tem tempo limite, e qualquer falha (inclusive o tempo
 * esgotado) vira {@link ProvedorPagamentoException}. Vale para qualquer implementação, simulada ou real.
 */
@Component
public class ClienteProvedorPagamento {

    private final ProvedorPagamento provedor;
    private final long limiteMs;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ClienteProvedorPagamento(ProvedorPagamento provedor, @Value("${spacebus.pagamento.timeout-ms:5000}") long limiteMs) {
        this.provedor = provedor;
        this.limiteMs = limiteMs;
    }

    public String metodo() {
        return provedor.metodo();
    }

    public ProvedorPagamento.Cobranca criarCobranca(String chaveIdempotencia, String referencia, BigDecimal valor) {
        return comLimite(() -> provedor.criarCobranca(chaveIdempotencia, referencia, valor));
    }

    public void reembolsar(String provedorId) {
        comLimite(() -> {
            provedor.reembolsar(provedorId);
            return null;
        });
    }

    private <T> T comLimite(Callable<T> chamada) {
        var futuro = executor.submit(chamada);
        try {
            return futuro.get(limiteMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new ProvedorPagamentoException("O provedor não respondeu em " + limiteMs + " ms.", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ProvedorPagamentoException falha) throw falha;
            throw new ProvedorPagamentoException("Falha ao chamar o provedor.", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProvedorPagamentoException("Chamada ao provedor interrompida.", e);
        }
    }

    @PreDestroy
    void encerrar() {
        executor.shutdownNow();
    }
}
