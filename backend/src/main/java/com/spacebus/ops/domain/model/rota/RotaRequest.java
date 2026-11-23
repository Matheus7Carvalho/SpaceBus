package com.spacebus.ops.domain.model.rota;

import com.spacebus.ops.domain.entity.enums.TipoRota;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RotaRequest {

    public static final int MIN_PONTOS = 2;
    public static final int MAX_PONTOS = 30;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 100, message = "O nome deve ter até 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe a origem.")
    @Size(max = 100, message = "A origem deve ter até 100 caracteres.")
    private String origem;

    @NotBlank(message = "Informe o destino.")
    @Size(max = 100, message = "O destino deve ter até 100 caracteres.")
    private String destino;

    @NotNull(message = "Selecione o tipo da rota.")
    @Builder.Default
    private TipoRota tipo = TipoRota.UNICA;

    /** Obrigatória na Rota Fixa; ignorada na Rota Única. */
    @Valid
    private RotinaRequest rotina;

    /** A ordem da lista é a ordem do percurso. */
    @NotNull(message = "Adicione os pontos de parada.")
    @Size(min = MIN_PONTOS, max = MAX_PONTOS, message = "A rota deve ter de 2 a 30 pontos de parada.")
    @Builder.Default
    private List<@Valid PontoParadaRequest> pontos = new ArrayList<>();
}
