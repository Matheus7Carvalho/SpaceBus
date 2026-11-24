package com.spacebus.ops.domain.entity.enums;

/** O que aconteceu com a reserva ou com o pagamento dela (histórico). */
public enum AcaoReserva {
    RESERVADA,
    PAGAMENTO_INICIADO,
    PAGAMENTO_RECUSADO,
    PAGAMENTO_APROVADO,
    CONFIRMADA,
    EXPIRADA,
    CANCELADA,
    REEMBOLSO_PENDENTE,
    REEMBOLSADA
}
