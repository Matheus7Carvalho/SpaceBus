package com.spacebus.ops.domain.model.reserva;

import com.spacebus.ops.domain.entity.ReservaHistorico;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;

import java.time.LocalDateTime;

/** Linha do histórico mostrada ao passageiro (sem o id de quem agiu). */
public record EventoReservaResponse(AcaoReserva acao, OrigemAcao origem, LocalDateTime data, String detalhe) {

    public static EventoReservaResponse de(ReservaHistorico h) {
        return new EventoReservaResponse(h.getAcao(), h.getOrigem(), h.getDataCriacao(), h.getDetalhe());
    }
}
