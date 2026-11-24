package com.spacebus.ops.domain.model.localizacao;

/**
 * Resultado de um lote. Rejeitadas: aparelho adiantado, antiga ou imprecisa (contam como falha de sinal).
 * Ignoradas: fora de ordem ou frequente demais (não são falha).
 */
public record ResultadoEnvioResponse(int aceitas, int rejeitadas, int ignoradas) {
}
