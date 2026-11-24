package com.spacebus.ops.domain.model.busca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Parâmetros da busca do passageiro (query string). */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuscaViagemRequest {

    @NotBlank(message = "Informe a origem.")
    @Size(max = 100, message = "A origem deve ter até 100 caracteres.")
    private String origem;

    @NotBlank(message = "Informe o destino.")
    @Size(max = 100, message = "O destino deve ter até 100 caracteres.")
    private String destino;

    @NotNull(message = "Informe a data.")
    private LocalDate data;
}
