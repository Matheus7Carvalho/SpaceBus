package com.spacebus.ops.domain.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Referência curta a uma entidade relacionada (rota, veículo, motorista). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Resumo {
    private String id;
    private String nome;
    /** Identificador legível quando existir (ex.: placa do veículo). */
    private String codigo;
}
