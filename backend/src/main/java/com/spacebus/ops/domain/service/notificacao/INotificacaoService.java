package com.spacebus.ops.domain.service.notificacao;

import com.spacebus.ops.domain.model.notificacao.NotificacaoResponse;
import com.spacebus.ops.domain.model.notificacao.PreferenciasResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Notificações do passageiro (escopo 4.5): na aplicação sempre; e-mail e WhatsApp (com opt-in) simulados. */
public interface INotificacaoService {

    /** Gera as notificações do evento (uma por reserva, sem duplicar) e faz os envios externos. Chamado após o commit. */
    void processar(NotificacaoSolicitada evento);

    /** Lembrete das reservas confirmadas com partida na próxima hora. Retorna quantas notificações novas. */
    int lembrar();

    Page<NotificacaoResponse> minhas(String usuarioId, Pageable pageable);

    long naoLidas(String usuarioId);

    /** Marca como lida (idempotente). De outro passageiro: não encontrada. */
    NotificacaoResponse ler(String id, String usuarioId);

    int lerTodas(String usuarioId);

    PreferenciasResponse preferencias(String usuarioId);

    /** Liga (grava a data do consentimento) ou desliga (apaga) o WhatsApp. */
    PreferenciasResponse definirWhatsapp(String usuarioId, boolean whatsapp);
}
