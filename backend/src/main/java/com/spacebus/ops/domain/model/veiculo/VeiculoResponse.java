package com.spacebus.ops.domain.model.veiculo;

import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VeiculoResponse {
    private String id;
    private String placa;
    private String descricao;
    private TipoVeiculo tipo;
    private Integer capacidade;
    private Boolean ativo;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
