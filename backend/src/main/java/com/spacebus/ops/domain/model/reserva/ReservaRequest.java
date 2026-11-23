package com.spacebus.ops.domain.model.reserva;

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

/** Reserva de lugares numa viagem publicada, no trecho escolhido na busca (ordem dos pontos). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaRequest {

    public static final int MAX_LUGARES = 6;

    @NotBlank(message = "Selecione a viagem.")
    @Size(max = 36, message = "Viagem inválida.")
    private String viagemId;

    @NotNull(message = "Informe o ponto de embarque.")
    @Min(value = 1, message = "Ponto de embarque inválido.")
    @Max(value = 30, message = "Ponto de embarque inválido.")
    private Integer embarqueOrdem;

    @NotNull(message = "Informe o ponto de desembarque.")
    @Min(value = 1, message = "Ponto de desembarque inválido.")
    @Max(value = 30, message = "Ponto de desembarque inválido.")
    private Integer desembarqueOrdem;

    @NotNull(message = "Informe a quantidade de lugares.")
    @Min(value = 1, message = "Reserve de 1 a 6 lugares.")
    @Max(value = MAX_LUGARES, message = "Reserve de 1 a 6 lugares.")
    private Integer quantidade;
}
