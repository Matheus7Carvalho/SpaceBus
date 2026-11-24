package com.spacebus.ops.domain.service.localizacao;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Janelas e limites da localização (premissas do piloto em docs/modulo-04-localizacao.md). Os mesmos valores estão em
 * frontend/src/features/localizacao/limites.ts, no README e no .env.example.
 *
 * @param inicioAntesMin              iniciar abre este tanto antes da partida (inclusivo)
 * @param inicioDepoisMin             e fecha este tanto depois dela (inclusivo)
 * @param encerramentoAutomaticoHoras viagem esquecida em andamento é encerrada depois disso
 * @param retencaoHoras               coordenadas apagadas este tanto depois do encerramento
 * @param desatualizadaSegundos       última posição mais velha que isso é "Desatualizada"
 * @param lacunaSegundos              intervalo entre posições acima disso conta como lacuna
 * @param intervaloMinimoSegundos     posição a menos disso da última gravada é ignorada (frequente)
 * @param adiantadaMaxSegundos        hora do aparelho à frente do servidor além disso é rejeitada
 * @param antigaMaxSegundos           posição mais velha que isso ao chegar é rejeitada
 * @param precisaoMaxM                precisão pior que isso é rejeitada
 */
@ConfigurationProperties("spacebus.localizacao")
public record LocalizacaoPropriedades(
        @DefaultValue("30") int inicioAntesMin,
        @DefaultValue("120") int inicioDepoisMin,
        @DefaultValue("6") int encerramentoAutomaticoHoras,
        @DefaultValue("24") int retencaoHoras,
        @DefaultValue("120") int desatualizadaSegundos,
        @DefaultValue("60") int lacunaSegundos,
        @DefaultValue("10") int intervaloMinimoSegundos,
        @DefaultValue("120") int adiantadaMaxSegundos,
        @DefaultValue("600") int antigaMaxSegundos,
        @DefaultValue("200") int precisaoMaxM) {

    /** Valores padrão do piloto (testes). */
    public static LocalizacaoPropriedades padrao() {
        return new LocalizacaoPropriedades(30, 120, 6, 24, 120, 60, 10, 120, 600, 200);
    }
}
