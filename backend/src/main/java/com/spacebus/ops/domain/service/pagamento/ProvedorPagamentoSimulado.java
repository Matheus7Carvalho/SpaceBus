package com.spacebus.ops.domain.service.pagamento;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provedor de teste (premissa do piloto: transações simuladas). A "página do provedor" é a tela
 * /reservas/pagamento/{id} do próprio frontend, que aprova ou recusa pelo ProvedorSimuladoController.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spacebus.pagamento.provedor", havingValue = "simulado")
public class ProvedorPagamentoSimulado implements ProvedorPagamento {

    public static final String PREFIXO = "sim_";

    /** Idempotência como num provedor real: mesma chave, mesma cobrança. */
    private final Map<String, Cobranca> porChave = new ConcurrentHashMap<>();

    @Override
    public String metodo() {
        return "PIX_SIMULADO";
    }

    @Override
    public Cobranca criarCobranca(String chaveIdempotencia, String referencia, BigDecimal valor) {
        return porChave.computeIfAbsent(chaveIdempotencia, chave -> {
            var id = PREFIXO + UUID.randomUUID().toString().replace("-", "");
            log.info("Cobrança simulada {} criada para {} (R$ {}).", id, referencia, valor);
            return new Cobranca(id, "/reservas/pagamento/" + id);
        });
    }

    @Override
    public void reembolsar(String provedorId) {
        log.info("Reembolso simulado de {}.", provedorId);
    }
}
