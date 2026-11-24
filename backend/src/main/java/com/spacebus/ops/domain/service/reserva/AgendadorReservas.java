package com.spacebus.ops.domain.service.reserva;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marca como expiradas as reservas vencidas. A conta de lugares já ignora as vencidas;
 * isto só deixa a situação gravada coerente para telas e relatórios.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spacebus.agendamentos.ativos", havingValue = "true", matchIfMissing = true)
public class AgendadorReservas {

    private final IReservaService service;
    private final ProcessadorReembolsos reembolsos;

    public AgendadorReservas(IReservaService service, ProcessadorReembolsos reembolsos) {
        this.service = service;
        this.reembolsos = reembolsos;
    }

    @Scheduled(fixedDelayString = "${spacebus.reserva.expiracao-ms:60000}")
    public void expirar() {
        var expiradas = service.expirarVencidas();
        if (expiradas > 0) log.info("Reservas expiradas: {}.", expiradas);
    }

    /** Reembolsos que o provedor não concluiu na hora são tentados de novo, até o limite de tentativas. */
    @Scheduled(fixedDelayString = "${spacebus.reserva.reembolso-ms:300000}")
    public void reembolsar() {
        var concluidos = reembolsos.tentarPendentes();
        if (concluidos > 0) log.info("Reembolsos pendentes concluídos: {}.", concluidos);
    }
}
