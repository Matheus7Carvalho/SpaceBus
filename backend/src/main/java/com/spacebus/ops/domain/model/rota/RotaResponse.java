package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.entity.enums.TipoRota;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotaResponse {
    private String id;
    private String nome;
    private String origem;
    private String destino;
    private TipoRota tipo;
    /** Só na Rota Fixa. */
    private RotinaResponse rotina;
    private List<PontoParadaResponse> pontos;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
