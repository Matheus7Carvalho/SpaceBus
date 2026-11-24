package com.spacebus.ops.domain.service.notificacao;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class NotificacaoListenerTest {

    /** Erro ao gerar (banco, canal) fica no log: não volta para a operação de origem (webhook, cancelamento). */
    @Test
    void erroAoGerarNaoPropaga() {
        var service = mock(INotificacaoService.class);
        var evento = NotificacaoSolicitada.confirmada("rs1");
        doThrow(new IllegalStateException("banco fora")).when(service).processar(evento);

        assertThatCode(() -> new NotificacaoListener(service).aoConfirmarOrigem(evento)).doesNotThrowAnyException();
        verify(service).processar(evento);
    }
}
