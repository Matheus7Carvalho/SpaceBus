package com.spacebus.ops.domain.entity.enums;

/** Por que uma posição enviada pelo celular do motorista não foi gravada (conta como falha de sinal). */
public enum MotivoRejeicaoPosicao {
    /** Hora à frente do servidor além da tolerância (relógio do aparelho adiantado). */
    ADIANTADA,
    /** Hora mais antiga que o limite (ficou presa na fila). */
    ANTIGA,
    /** Precisão pior que o limite. */
    IMPRECISA
}
