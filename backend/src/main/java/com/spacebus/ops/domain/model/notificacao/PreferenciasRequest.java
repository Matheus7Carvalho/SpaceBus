package com.spacebus.ops.domain.model.notificacao;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Só um booleano: ligar grava a data do consentimento; desligar apaga. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciasRequest {

    @NotNull(message = "Informe se quer receber por WhatsApp.")
    private Boolean whatsapp;
}
