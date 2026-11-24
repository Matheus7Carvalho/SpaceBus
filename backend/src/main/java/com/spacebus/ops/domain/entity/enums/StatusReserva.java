package com.spacebus.ops.domain.entity.enums;

/**
 * AGUARDANDO_PAGAMENTO (lugares bloqueados até expirar) → CONFIRMADA (só após o provedor aprovar);
 * → EXPIRADA (prazo vencido) ou CANCELADA (passageiro ou viagem cancelada).
 */
public enum StatusReserva {
    AGUARDANDO_PAGAMENTO,
    CONFIRMADA,
    EXPIRADA,
    CANCELADA
}
