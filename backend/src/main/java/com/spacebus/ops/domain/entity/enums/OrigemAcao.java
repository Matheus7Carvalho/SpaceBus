package com.spacebus.ops.domain.entity.enums;

/** Quem causou a mudança registrada no histórico. */
public enum OrigemAcao {
    PASSAGEIRO,
    OPERADOR,
    /** Retorno do provedor de pagamento. */
    PROVEDOR,
    /** Tarefa agendada (expiração, nova tentativa de reembolso). */
    SISTEMA
}
