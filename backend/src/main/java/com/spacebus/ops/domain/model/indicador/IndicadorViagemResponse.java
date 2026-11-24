package com.spacebus.ops.domain.model.indicador;

import com.spacebus.ops.domain.entity.enums.StatusViagem;

import java.time.LocalDateTime;

/** Ocupação de uma viagem. Cancelada: disponíveis e ocupação nulos ("—"). */
public record IndicadorViagemResponse(String id, LocalDateTime partida, String rotaNome, String veiculoPlaca, StatusViagem status,
                                      int capacidade, long vendidos, Long disponiveis, Double ocupacao) {
}
