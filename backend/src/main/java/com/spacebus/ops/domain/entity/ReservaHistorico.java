package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.AcaoReserva;
import com.spacebus.ops.domain.entity.enums.OrigemAcao;
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

/** Uma linha do histórico da reserva: o que mudou, quem causou e quando (data = dataCriacao). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "RESERVA_HISTORICO")
public class ReservaHistorico extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RESERVA_ID", nullable = false, updatable = false)
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACAO", nullable = false, length = 20, updatable = false)
    private AcaoReserva acao;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORIGEM", nullable = false, length = 10, updatable = false)
    private OrigemAcao origem;

    /** Nulo quando a origem é o provedor ou o sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESPONSAVEL_ID", updatable = false)
    private Usuario responsavel;

    @Column(name = "DETALHE", length = 200, updatable = false)
    private String detalhe;

    /** Sequência do banco (identity): ordem exata em que as ações foram gravadas. */
    @Column(name = "ORDEM", insertable = false, updatable = false)
    private Long ordem;

    public ReservaHistorico(Reserva reserva, AcaoReserva acao, OrigemAcao origem, Usuario responsavel, String detalhe) {
        this(reserva, acao, origem, responsavel, detalhe, null);
    }
}
