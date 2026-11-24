package com.spacebus.ops.domain.model.localizacao;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A mesma localização do passageiro, mais o diagnóstico do sinal. Sem dados do motorista. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalizacaoOperacaoResponse {
    @JsonUnwrapped
    private LocalizacaoResponse localizacao;
    /** Viagem sem motorista atribuído: ninguém transmite a posição. */
    private boolean semMotorista;
    private DiagnosticoLocalizacaoResponse diagnostico;
}
