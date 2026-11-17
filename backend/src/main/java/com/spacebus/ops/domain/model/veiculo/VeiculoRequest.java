package com.spacebus.ops.domain.model.veiculo;

import com.spacebus.ops.domain.entity.enums.TipoVeiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class VeiculoRequest {

    /** Aceita com hífen ("ABC-1D23"); o serviço normaliza para 7 caracteres. */
    @NotBlank(message = "Informe a placa.")
    @Size(max = 8, message = "A placa deve ter até 8 caracteres.")
    private String placa;

    @NotBlank(message = "Informe a descrição.")
    @Size(max = 100, message = "A descrição deve ter até 100 caracteres.")
    private String descricao;

    @NotNull(message = "Selecione o tipo.")
    private TipoVeiculo tipo;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade deve ser entre 1 e 60 lugares.")
    @Max(value = 60, message = "A capacidade deve ser entre 1 e 60 lugares.")
    private Integer capacidade;

    @Builder.Default
    private Boolean ativo = Boolean.TRUE;
}
