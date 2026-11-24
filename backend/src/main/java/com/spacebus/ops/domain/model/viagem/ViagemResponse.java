package com.spacebus.ops.domain.model.viagem;

import com.spacebus.ops.domain.entity.enums.StatusViagem;
import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViagemResponse {
    private String id;
    private String rotaId;
    private String rotaNome;
    private String origem;
    private String destino;
    private String veiculoId;
    private String veiculoPlaca;
    private String veiculoDescricao;
    private TipoVeiculo tipoVeiculo;
    private String motoristaId;
    private String motoristaNome;
    private LocalDateTime partida;
    private BigDecimal preco;
    private Integer capacidade;
    private StatusViagem status;
    /** Rotina de Rota Fixa que gerou a viagem; nulo se programada à mão. */
    private String rotinaId;
    private LocalDateTime iniciadaEm;
    private LocalDateTime encerradaEm;
    /** Em andamento com localização desatualizada ou indisponível (só na agenda; nulo nas outras respostas). */
    private Boolean semSinal;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
