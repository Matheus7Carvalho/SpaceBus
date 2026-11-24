package com.spacebus.ops.domain.service.notificacao;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Lembrete de partida (1 h antes). Duas execuções juntas não duplicam: a chave da notificação é única. */
@Slf4j
@Component
@ConditionalOnProperty(name = "spacebus.agendamentos.ativos", havingValue = "true", matchIfMissing = true)
public class AgendadorNotificacoes {

    private final INotificacaoService service;

    public AgendadorNotificacoes(INotificacaoService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${spacebus.notificacao.lembrete-ms:60000}")
    public void lembrar() {
        try {
            var novas = service.lembrar();
            if (novas > 0) log.info("Lembretes gerados: {}.", novas);
        } catch (RuntimeException e) {
            log.error("Falha ao gerar lembretes.", e);
        }
    }
}
