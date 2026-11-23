package com.spacebus.ops.domain.model.reserva;

import com.spacebus.ops.domain.entity.Pagamento;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Situação da cobrança; a URL do provedor só vai para o passageiro enquanto o pagamento está pendente. */
public record PagamentoResponse(String provedorId, String metodo, BigDecimal valor, StatusPagamento status,
                                String urlPagamento, LocalDateTime criadoEm) {

    public static PagamentoResponse de(Pagamento p) {
        if (p == null) return null;
        var url = p.getStatus() == StatusPagamento.PENDENTE ? p.getUrlPagamento() : null;
        return new PagamentoResponse(p.getProvedorId(), p.getMetodo(), p.getValor(), p.getStatus(), url, p.getDataCriacao());
    }
}
