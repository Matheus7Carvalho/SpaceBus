package com.spacebus.ops.api.config.tempo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tarefas agendadas (rotinas, expiração de reservas). Desligáveis com spacebus.agendamentos.ativos=false. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spacebus.agendamentos.ativos", havingValue = "true", matchIfMissing = true)
public class AgendamentoConfiguration {
}
