package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "VEICULO")
public class Veiculo extends BaseEntity {

    /** Sem hífen, maiúsculas (padrão antigo ABC1234 ou Mercosul ABC1D23). */
    @Column(name = "PLACA", nullable = false, length = 7)
    private String placa;

    @Column(name = "DESCRICAO", nullable = false, length = 100)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 10)
    private TipoVeiculo tipo;

    /** Lugares para passageiros. */
    @Column(name = "CAPACIDADE", nullable = false)
    private Integer capacidade;

    @Column(name = "ATIVO", nullable = false)
    private Boolean ativo;
}
