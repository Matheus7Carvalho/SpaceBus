package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Uma cobrança no provedor externo. Nenhum dado de cartão ou conta passa pelo SpaceBus. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PAGAMENTO")
public class Pagamento extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESERVA_ID", nullable = false, updatable = false)
    private Reserva reserva;

    /** Identificador da cobrança no provedor (chave do retorno/webhook). */
    @Column(name = "PROVEDOR_ID", nullable = false, length = 64, updatable = false)
    private String provedorId;

    @Column(name = "METODO", nullable = false, length = 20)
    private String metodo;

    @Column(name = "VALOR", nullable = false, precision = 8, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 18)
    private StatusPagamento status;

    /** Página do provedor onde o passageiro paga. */
    @Column(name = "URL_PAGAMENTO", nullable = false)
    private String urlPagamento;
}
