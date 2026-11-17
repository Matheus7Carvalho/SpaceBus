package com.spacebus.ops.domain.model.rota;

import jakarta.validation.constraints.NotBlank;
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
public class PontoParadaRequest {

    @NotBlank(message = "Informe o nome do ponto.")
    @Size(max = 100, message = "O nome do ponto deve ter até 100 caracteres.")
    private String nome;

    @Size(max = 150, message = "A referência deve ter até 150 caracteres.")
    private String referencia;
}
