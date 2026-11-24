package com.spacebus.ops.domain.entity.enums;

/**
 * PENDENTE → APROVADO ou RECUSADO (pelo provedor).
 * APROVADO → REEMBOLSO_PENDENTE (cancelamento ou aprovação sem lugar) → REEMBOLSADO quando o provedor confirma.
 * O pedido ao provedor sai depois do commit; se falhar, a tarefa agendada tenta de novo, até o limite de tentativas.
 */
public enum StatusPagamento {
    PENDENTE,
    APROVADO,
    RECUSADO,
    REEMBOLSO_PENDENTE,
    REEMBOLSADO
}
