/** Iguais ao RotaRequest/PontoParadaRequest do backend e às colunas do banco. */
export const LIMITE = {
  nome: 100,
  origem: 100,
  destino: 100,
  pontoNome: 100,
  pontoReferencia: 150,
  pontosMin: 2,
  pontosMax: 30,
  /** RotinaRequest.MAX_HORARIOS. */
  horariosMax: 12,
} as const
