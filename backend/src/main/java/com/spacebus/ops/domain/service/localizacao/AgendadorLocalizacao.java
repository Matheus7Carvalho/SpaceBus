package com.spacebus.ops.domain.service.localizacao;

import com.spacebus.ops.domain.service.viagem.IViagemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Encerra viagens esquecidas em andamento e apaga coordenadas vencidas (retenção). */
@Slf4j
@Component
@ConditionalOnProperty(name = "spacebus.agendamentos.ativos", havingValue = "true", matchIfMissing = true)
public class AgendadorLocalizacao {

    private final IViagemService viagens;
    private final ILocalizacaoService localizacao;

    public AgendadorLocalizacao(IViagemService viagens, ILocalizacaoService localizacao) {
        this.viagens = viagens;
        this.localizacao = localizacao;
    }

    @Scheduled(fixedDelayString = "${spacebus.localizacao.encerramento-ms:60000}")
    public void encerrarEsquecidas() {
        var encerradas = viagens.encerrarEsquecidas();
        if (encerradas > 0) log.info("Viagens encerradas automaticamente: {}.", encerradas);
    }

    @Scheduled(fixedDelayString = "${spacebus.localizacao.expurgo-ms:600000}")
    public void expurgar() {
        var expurgadas = localizacao.expurgar();
        if (expurgadas > 0) log.info("Localizações expurgadas: {}.", expurgadas);
    }
}
