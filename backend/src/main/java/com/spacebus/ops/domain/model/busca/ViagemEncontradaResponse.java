package com.spacebus.ops.domain.model.busca;

import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import com.spacebus.ops.domain.model.rota.PontoParadaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Uma viagem que atende à busca, com o trecho do passageiro (embarque → desembarque). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViagemEncontradaResponse {
    private String id;
    private String transportadora;
    private String rotaNome;
    private LocalDateTime partida;
    private BigDecimal preco;
    private TipoVeiculo tipoVeiculo;
    private PontoParadaResponse embarque;
    private PontoParadaResponse desembarque;
    private Integer lugaresDisponiveis;
}
