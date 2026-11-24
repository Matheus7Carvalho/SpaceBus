package com.spacebus.ops.api.controller.reserva;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.Reserva;
import com.spacebus.ops.domain.model.reserva.EventoReservaResponse;
import com.spacebus.ops.domain.model.reserva.PagamentoResponse;
import com.spacebus.ops.domain.model.reserva.ReservaDaViagemResponse;
import com.spacebus.ops.domain.model.reserva.ReservaResponse;
import com.spacebus.ops.domain.service.reserva.IReservaService;
import org.springframework.stereotype.Component;

/** Monta as respostas de reserva (precisa do serviço para último pagamento e prazos). */
@Component
public class ReservaRespostas {

    private final IReservaService service;

    public ReservaRespostas(IReservaService service) {
        this.service = service;
    }

    public ReservaResponse doPassageiro(Reserva r) {
        var viagem = r.getViagem();
        return ReservaResponse.builder()
                .id(r.getId())
                .codigo(r.getCodigo())
                .status(r.getStatus())
                .motivoCancelamento(r.getMotivoCancelamento())
                .viagemId(viagem.getId())
                .rotaNome(viagem.getRota().getNome())
                .situacaoViagem(viagem.getStatus())
                .partida(viagem.getPartida())
                .embarqueNome(r.getEmbarqueNome())
                .embarqueReferencia(r.getEmbarqueReferencia())
                .desembarqueNome(r.getDesembarqueNome())
                .quantidade(r.getQuantidade())
                .valorTotal(r.getValorTotal())
                .expiraEm(r.getExpiraEm())
                .confirmadaEm(r.getConfirmadaEm())
                .canceladaEm(r.getCanceladaEm())
                .cancelavel(service.cancelavel(r))
                .pagavel(service.pagavel(r))
                .pagamento(PagamentoResponse.de(service.ultimoPagamento(r.getId()).orElse(null)))
                .historico(service.historico(r.getId()).stream().map(EventoReservaResponse::de).toList())
                .criadaEm(r.getDataCriacao())
                .build();
    }

    public ReservaDaViagemResponse daViagem(Reserva r) {
        var pagamento = service.ultimoPagamento(r.getId()).map(Pagamento::getStatus).orElse(null);
        return new ReservaDaViagemResponse(r.getId(), r.getCodigo(), r.getPassageiro().getNome(), r.getPassageiro().getEmail(),
                r.getEmbarqueNome(), r.getDesembarqueNome(), r.getQuantidade(), r.getValorTotal(), r.getStatus(),
                r.getMotivoCancelamento(), pagamento);
    }
}
