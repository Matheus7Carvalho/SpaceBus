package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.enums.TipoNotificacao;

import java.time.LocalDateTime;

/**
 * Evento publicado dentro da transação que mudou a reserva ou a viagem. As notificações são geradas só depois do
 * commit ({@link NotificacaoListener}); rollback da origem não gera nada.
 *
 * @param alvoId          reserva (confirmada, cancelada) ou viagem (alterada, iniciada)
 * @param partidaAnterior só em VIAGEM_ALTERADA
 * @param reembolso       só em VIAGEM_CANCELADA: havia pagamento aprovado
 */
public record NotificacaoSolicitada(TipoNotificacao tipo, String alvoId, LocalDateTime partidaAnterior, boolean reembolso) {

    public static NotificacaoSolicitada confirmada(String reservaId) {
        return new NotificacaoSolicitada(TipoNotificacao.RESERVA_CONFIRMADA, reservaId, null, false);
    }

    public static NotificacaoSolicitada cancelada(String reservaId, boolean reembolso) {
        return new NotificacaoSolicitada(TipoNotificacao.VIAGEM_CANCELADA, reservaId, null, reembolso);
    }

    public static NotificacaoSolicitada alterada(String viagemId, LocalDateTime partidaAnterior) {
        return new NotificacaoSolicitada(TipoNotificacao.VIAGEM_ALTERADA, viagemId, partidaAnterior, false);
    }

    public static NotificacaoSolicitada iniciada(String viagemId) {
        return new NotificacaoSolicitada(TipoNotificacao.VIAGEM_INICIADA, viagemId, null, false);
    }
}
