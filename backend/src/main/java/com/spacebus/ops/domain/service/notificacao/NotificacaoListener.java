package com.spacebus.ops.domain.service.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gera as notificações só depois do commit da origem. Qualquer erro fica no log: a reserva, o pagamento, a viagem e a
 * resposta da operação de origem não mudam por causa da notificação.
 */
@Slf4j
@Component
public class NotificacaoListener {

    private final INotificacaoService service;

    public NotificacaoListener(INotificacaoService service) {
        this.service = service;
    }

    @TransactionalEventListener
    public void aoConfirmarOrigem(NotificacaoSolicitada evento) {
        try {
            service.processar(evento);
        } catch (RuntimeException e) {
            log.error("Falha ao gerar notificação: tipo={} alvo={}", evento.tipo(), evento.alvoId(), e);
        }
    }
}
