package com.spacebus.ops.domain.model.localizacao;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Uma posição lida pelo celular do motorista (Geolocation API). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosicaoRequest {

    @NotNull(message = "Latitude deve estar entre -90 e 90.")
    @DecimalMin(value = "-90", message = "Latitude deve estar entre -90 e 90.")
    @DecimalMax(value = "90", message = "Latitude deve estar entre -90 e 90.")
    private Double latitude;

    @NotNull(message = "Longitude deve estar entre -180 e 180.")
    @DecimalMin(value = "-180", message = "Longitude deve estar entre -180 e 180.")
    @DecimalMax(value = "180", message = "Longitude deve estar entre -180 e 180.")
    private Double longitude;

    @NotNull(message = "Precisão deve estar entre 0 e 5000 m.")
    @DecimalMin(value = "0", message = "Precisão deve estar entre 0 e 5000 m.")
    @DecimalMax(value = "5000", message = "Precisão deve estar entre 0 e 5000 m.")
    private Double precisaoM;

    /**
     * Hora da posição no aparelho, ISO-8601 com fuso ("2026-11-24T10:00:00-03:00" ou "...Z"). Hora local sem fuso é
     * recusada: o relógio do aparelho pode estar em outro fuso. O servidor converte para o fuso da aplicação.
     */
    @NotBlank(message = "Informe a hora da posição.")
    @Size(max = 40, message = "Informe a hora da posição com fuso (ISO-8601).")
    @Pattern(regexp = "^\\s*$|.*(Z|[+-][0-9]{2}:[0-9]{2})$", message = "Informe a hora da posição com fuso (ISO-8601).")
    private String registradaEm;
}
