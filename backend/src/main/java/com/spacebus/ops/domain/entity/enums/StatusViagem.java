package com.spacebus.ops.domain.entity.enums;

/**
 * RASCUNHO → PUBLICADA ⇄ SUSPENSA; RASCUNHO, PUBLICADA, SUSPENSA → CANCELADA (definitivo).
 * PUBLICADA → EM_ANDAMENTO (iniciar) → ENCERRADA (encerrar, definitivo). Só PUBLICADA aparece para o passageiro.
 */
public enum StatusViagem {
    RASCUNHO,
    PUBLICADA,
    SUSPENSA,
    CANCELADA,
    EM_ANDAMENTO,
    ENCERRADA
}
