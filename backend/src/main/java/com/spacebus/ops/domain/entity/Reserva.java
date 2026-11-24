package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.MotivoCancelamento;
import com.spacebus.ops.domain.entity.enums.StatusReserva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lugares de um passageiro numa viagem, num trecho da rota. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "RESERVA")
public class Reserva extends BaseEntity {

    /** Ex.: SB-7K3Q9M. */
    @Column(name = "CODIGO", nullable = false, length = 9, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "VIAGEM_ID", nullable = false, updatable = false)
    private Viagem viagem;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "PASSAGEIRO_ID", nullable = false, updatable = false)
    private Usuario passageiro;

    @Column(name = "EMBARQUE_ORDEM", nullable = false)
    private Integer embarqueOrdem;

    @Column(name = "EMBARQUE_NOME", nullable = false, length = 100)
    private String embarqueNome;

    @Column(name = "EMBARQUE_REFERENCIA", length = 150)
    private String embarqueReferencia;

    @Column(name = "DESEMBARQUE_ORDEM", nullable = false)
    private Integer desembarqueOrdem;

    @Column(name = "DESEMBARQUE_NOME", nullable = false, length = 100)
    private String desembarqueNome;

    @Column(name = "QUANTIDADE", nullable = false)
    private Integer quantidade;

    @Column(name = "VALOR_TOTAL", nullable = false, precision = 8, scale = 2)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StatusReserva status;

    /** Fim do bloqueio dos lugares enquanto aguarda pagamento. */
    @Column(name = "EXPIRA_EM", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime expiraEm;

    @Column(name = "CONFIRMADA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime confirmadaEm;

    @Column(name = "CANCELADA_EM", columnDefinition = "TIMESTAMP")
    private LocalDateTime canceladaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "MOTIVO_CANCELAMENTO", length = 16)
    private MotivoCancelamento motivoCancelamento;

    /** Ainda segura lugares: confirmada, ou aguardando pagamento dentro do prazo. */
    public boolean ocupaLugares(LocalDateTime agora) {
        return status == StatusReserva.CONFIRMADA || (status == StatusReserva.AGUARDANDO_PAGAMENTO && expiraEm.isAfter(agora));
    }
}
