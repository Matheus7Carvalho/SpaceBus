package com.spacebus.ops.domain.model.notificacao;

import com.spacebus.ops.domain.entity.Notificacao;
import com.spacebus.ops.domain.entity.NotificacaoEnvio;
import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import com.spacebus.ops.domain.entity.enums.StatusEnvio;
import com.spacebus.ops.domain.entity.enums.TipoNotificacao;

import java.time.LocalDateTime;
import java.util.List;

/** Notificação do passageiro, com os envios externos (sem e-mail ou telefone). */
public record NotificacaoResponse(String id, TipoNotificacao tipo, String titulo, String mensagem, String link, String reservaId,
                                  LocalDateTime criadaEm, LocalDateTime lidaEm, List<Envio> envios) {

    public record Envio(CanalEnvio canal, StatusEnvio status, String motivo, LocalDateTime em) {
        static Envio de(NotificacaoEnvio e) {
            return new Envio(e.getCanal(), e.getStatus(), e.getMotivo(), e.getDataCriacao());
        }
    }

    public static NotificacaoResponse de(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getMensagem(), n.getLink(), n.getReservaId(),
                n.getDataCriacao(), n.getLidaEm(), n.getEnvios().stream().map(Envio::de).toList());
    }
}
