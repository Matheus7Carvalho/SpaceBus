package com.spacebus.ops.domain.model.reserva;

import com.spacebus.ops.domain.entity.enums.MotivoCancelamento;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;

import java.math.BigDecimal;

/**
 * Linha da lista de passageiros da viagem, para o operador. Inclui contato para avisar
 * o passageiro em alteração ou cancelamento (escopo 4.4 e requisito 7).
 */
public record ReservaDaViagemResponse(String id, String codigo, String passageiroNome, String passageiroEmail,
                                      String embarqueNome, String desembarqueNome, Integer quantidade,
                                      BigDecimal valorTotal, StatusReserva status, MotivoCancelamento motivoCancelamento,
                                      StatusPagamento pagamento) {
}
