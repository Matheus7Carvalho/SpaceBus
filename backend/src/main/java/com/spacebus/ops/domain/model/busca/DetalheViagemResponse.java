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
import java.util.List;

/** Detalhe público de uma viagem publicada. Sem dados do motorista nem internos da operação. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalheViagemResponse {
    private String id;
    private String transportadora;
    private String rotaNome;
    private String origem;
    private String destino;
    private LocalDateTime partida;
    private BigDecimal preco;
    private TipoVeiculo tipoVeiculo;
    private String veiculoDescricao;
    private Integer lugaresDisponiveis;
    private List<PontoParadaResponse> pontos;
    /** Última alteração feita pela transportadora (ou o cadastro, se nunca alterada). */
    private LocalDateTime atualizadaEm;
}
