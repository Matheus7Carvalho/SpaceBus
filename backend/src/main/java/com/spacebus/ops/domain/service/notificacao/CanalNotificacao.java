package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.entity.enums.CanalEnvio;

/**
 * Envio externo (e-mail, WhatsApp). No MVP, {@link CanalNotificacaoSimulado}; a integração real troca esta
 * implementação. Falha = exceção; quem chama grava FALHOU e segue (a reserva nunca muda por falha de entrega).
 */
public interface CanalNotificacao {

    record Mensagem(String notificacaoId, String destino, String titulo, String texto) {
    }

    void enviar(CanalEnvio canal, Mensagem mensagem);
}
