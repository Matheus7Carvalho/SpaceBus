package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.StatusViagem;
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
import java.time.LocalDateTime;

/** Uma partida de uma rota, num horário, com veículo, preço e capacidade. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "VIAGEM")
public class Viagem extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "ROTA_ID", nullable = false)
    private Rota rota;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "VEICULO_ID", nullable = false)
    private Veiculo veiculo;

    /** Opcional até a publicação: a transportadora pode escalar o motorista depois. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MOTORISTA_ID")
    private Usuario motorista;

    @Column(name = "DATA_HORA_PARTIDA", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime partida;

    @Column(name = "PRECO", nullable = false, precision = 6, scale = 2)
    private BigDecimal preco;

    /** Lugares à venda nesta viagem; no máximo a capacidade do veículo. */
    @Column(name = "CAPACIDADE", nullable = false)
    private Integer capacidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    private StatusViagem status;

    /** Rotina (Rota Fixa) que gerou a viagem; nulo se programada à mão. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ROTINA_ID")
    private Rotina rotina;

    public Viagem(Rota rota, Veiculo veiculo, Usuario motorista, LocalDateTime partida, BigDecimal preco, Integer capacidade, StatusViagem status) {
        this(rota, veiculo, motorista, partida, preco, capacidade, status, null);
    }
}
