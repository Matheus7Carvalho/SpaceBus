package com.spacebus.ops.domain.entity.enums;

/**
 * RASCUNHO → PUBLICADA ⇄ SUSPENSA; qualquer uma → CANCELADA (definitivo).
 * Só PUBLICADA aparece para o passageiro. EM_ANDAMENTO/CONCLUIDA entram com o rastreamento.
 */
public enum StatusViagem {
    RASCUNHO,
    PUBLICADA,
    SUSPENSA,
    CANCELADA
}
