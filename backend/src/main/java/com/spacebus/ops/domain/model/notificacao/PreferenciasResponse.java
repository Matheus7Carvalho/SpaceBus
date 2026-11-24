package com.spacebus.ops.domain.model.notificacao;

import com.spacebus.ops.domain.entity.Usuario;

import java.time.LocalDateTime;

public record PreferenciasResponse(boolean whatsapp, LocalDateTime whatsappConsentidoEm) {

    public static PreferenciasResponse de(Usuario u) {
        return new PreferenciasResponse(u.getWhatsappConsentidoEm() != null, u.getWhatsappConsentidoEm());
    }
}
