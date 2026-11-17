package com.spacebus.ops.domain.model.rota;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PontoParadaResponse {
    private String id;
    private Integer ordem;
    private String nome;
    private String referencia;
}
