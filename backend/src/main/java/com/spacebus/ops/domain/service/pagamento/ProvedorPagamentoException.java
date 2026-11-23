package com.spacebus.ops.domain.service.pagamento;

/** O provedor não respondeu ou recusou a operação técnica (não é recusa do pagamento pelo passageiro). */
public class ProvedorPagamentoException extends RuntimeException {
    public ProvedorPagamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
