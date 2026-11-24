package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.enums.CanalEnvio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Não envia nada: só registra. Com spacebus.notificacao.simular-falha=true, todo envio falha (testes). */
@Slf4j
@Component
public class CanalNotificacaoSimulado implements CanalNotificacao {

    private final boolean simularFalha;

    public CanalNotificacaoSimulado(@Value("${spacebus.notificacao.simular-falha:false}") boolean simularFalha) {
        this.simularFalha = simularFalha;
    }

    @Override
    public void enviar(CanalEnvio canal, Mensagem mensagem) {
        if (simularFalha) throw new IllegalStateException("Falha simulada do canal.");
        log.info("Envio simulado: notificacao={} canal={}", mensagem.notificacaoId(), canal);
    }
}
