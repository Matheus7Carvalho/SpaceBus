package com.spacebus.ops.domain.model.localizacao;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Lote de posições: a mais recente ou as que ficaram na fila do aparelho sem internet. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteLocalizacaoRequest {

    public static final int MAX_POSICOES = 20;

    @NotNull(message = "Envie de 1 a 20 posições.")
    @Size(min = 1, max = MAX_POSICOES, message = "Envie de 1 a 20 posições.")
    private List<@Valid @NotNull(message = "Envie de 1 a 20 posições.") PosicaoRequest> posicoes;
}
