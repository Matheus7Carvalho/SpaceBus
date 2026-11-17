package com.spacebus.ops.domain.entity;

import com.spacebus.ops.domain.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Local de embarque/desembarque de uma rota, na ordem do percurso. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PONTO_PARADA")
public class PontoParada extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ROTA_ID", nullable = false)
    private Rota rota;

    @Column(name = "ORDEM", nullable = false)
    private Integer ordem;

    @Column(name = "NOME", nullable = false, length = 100)
    private String nome;

    /** Referência para o passageiro achar o ponto (ex.: "Portão B"). Opcional. */
    @Column(name = "REFERENCIA", length = 150)
    private String referencia;

    public PontoParada(String nome, String referencia) {
        this.nome = nome;
        this.referencia = referencia;
    }
}
