package com.spacebus.ops.domain.service.pagamento;

import java.math.BigDecimal;

/**
 * Provedor externo de pagamento. O SpaceBus só cria a cobrança e recebe o retorno (aprovado/recusado):
 * o passageiro paga na página do provedor e nenhum dado de cartão ou conta passa por aqui.
 * Trocar de provedor = outra implementação desta interface. Nunca chamar direto: use {@link ClienteProvedorPagamento},
 * que impõe o tempo limite, e nunca com trava de banco segura.
 */
public interface ProvedorPagamento {

    /** Método oferecido ao passageiro (ex.: "PIX_SIMULADO"). */
    String metodo();

    /**
     * Cria a cobrança no provedor. A mesma {@code chaveIdempotencia} devolve a mesma cobrança (duas chamadas
     * simultâneas não geram duas cobranças). Falha de comunicação: {@link ProvedorPagamentoException}.
     */
    Cobranca criarCobranca(String chaveIdempotencia, String referencia, BigDecimal valor);

    /** Devolve integralmente um pagamento aprovado; repetir não devolve em dobro. Falha: {@link ProvedorPagamentoException}. */
    void reembolsar(String provedorId);

    record Cobranca(String provedorId, String urlPagamento) {
    }
}
