package com.spacebus.ops.domain.service.rotina;

import com.spacebus.ops.domain.repository.rota.RotinaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/** Uma vez por dia, estende a geração das rotinas vigentes para manter o horizonte de 30 dias. */
@Slf4j
@Component
@ConditionalOnProperty(name = "spacebus.agendamentos.ativos", havingValue = "true", matchIfMissing = true)
public class AgendadorRotinas {

    private final RotinaRepository rotinaRepository;
    private final GeradorViagens gerador;
    private final Clock clock;

    public AgendadorRotinas(RotinaRepository rotinaRepository, GeradorViagens gerador, Clock clock) {
        this.rotinaRepository = rotinaRepository;
        this.gerador = gerador;
        this.clock = clock;
    }

    /** Sem transação aqui: cada rotina gera na sua (gerarPorId); uma falha é registrada e as outras seguem. */
    @Scheduled(cron = "${spacebus.rotina.cron:0 15 2 * * *}")
    public void estenderRotinas() {
        var ids = rotinaRepository.vigentes(LocalDate.now(clock)).stream().map(r -> r.getId()).toList();
        var falhas = 0;
        for (var id : ids) {
            try {
                gerador.gerarPorId(id);
            } catch (RuntimeException e) {
                falhas++;
                log.error("Falha ao estender a rotina {}; tenta de novo na próxima execução.", id, e);
            }
        }
        log.info("Rotinas estendidas: {} (falhas: {}).", ids.size() - falhas, falhas);
    }
}
