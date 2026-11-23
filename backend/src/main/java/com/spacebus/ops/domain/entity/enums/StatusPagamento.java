package com.spacebus.ops.domain.entity.enums;

/**
 * PENDENTE → APROVADO ou RECUSADO (pelo provedor).
 * APROVADO → REEMBOLSADO (cancelamento ou aprovação sem lugar); se o provedor falhar no reembolso,
 * fica REEMBOLSO_PENDENTE até a nova tentativa automática conseguir.
 */
public enum StatusPagamento {
    PENDENTE,
    APROVADO,
    RECUSADO,
    REEMBOLSO_PENDENTE,
    REEMBOLSADO
}
