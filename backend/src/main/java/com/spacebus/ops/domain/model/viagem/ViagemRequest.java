package com.spacebus.ops.domain.model.viagem;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** O status não vem aqui: muda só pelos endpoints de transição (publicar, suspender, cancelar). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViagemRequest {

    @NotBlank(message = "Selecione a rota.")
    @Size(max = 36, message = "Rota inválida.")
    private String rotaId;

    @NotBlank(message = "Selecione o veículo.")
    @Size(max = 36, message = "Veículo inválido.")
    private String veiculoId;

    @Size(max = 36, message = "Motorista inválido.")
    private String motoristaId;

    @NotNull(message = "Informe a partida.")
    private LocalDateTime partida;

    @NotNull(message = "Informe o preço.")
    @DecimalMin(value = "0.01", message = "O preço deve ser entre R$ 0,01 e R$ 9.999,99.")
    @DecimalMax(value = "9999.99", message = "O preço deve ser entre R$ 0,01 e R$ 9.999,99.")
    @Digits(integer = 4, fraction = 2, message = "Use no máximo 2 casas decimais no preço.")
    private BigDecimal preco;

    @NotNull(message = "Informe a capacidade.")
    @Min(value = 1, message = "A capacidade deve ser entre 1 e 60 lugares.")
    @Max(value = 60, message = "A capacidade deve ser entre 1 e 60 lugares.")
    private Integer capacidade;
}
