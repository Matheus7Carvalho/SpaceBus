/**
 * Janelas e limites da localização. Iguais a spacebus.localizacao.* no backend (LocalizacaoPropriedades),
 * ao README e ao .env.example; premissas em docs/modulo-04-localizacao.md.
 */
export const LOCALIZACAO = {
  /** Iniciar abre este tanto antes da partida (inclusivo)… */
  inicioAntesMin: 30,
  /** …e fecha este tanto depois dela (inclusivo). */
  inicioDepoisMin: 120,
  /** Intervalo mínimo entre envios do celular do motorista (a posição mais recente). */
  envioSegundos: 15,
  /** Atualização do mapa do passageiro e da operação (polling). */
  atualizacaoSegundos: 15,
  /** Posições guardadas no aparelho sem internet; cheia, descarta a mais antiga. */
  filaMax: 20,
  /** Posição mais velha que isso é descartada antes do envio (o servidor também a rejeitaria). */
  posicaoMaxIdadeSegundos: 600,
  /** Intervalo acima disso conta como lacuna no diagnóstico. */
  lacunaSegundos: 60,
} as const
