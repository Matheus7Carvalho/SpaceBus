package com.spacebus.ops.domain.service.reserva;

import com.spacebus.ops.domain.entity.enums.OrigemAcao;

/** Publicado dentro da transação que decidiu reembolsar; o provedor só é chamado depois do commit. */
public record ReembolsoSolicitado(String pagamentoId, OrigemAcao origem, String responsavelId) {
}
