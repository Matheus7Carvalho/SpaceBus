/** Iguais ao ViagemRequest do backend e às colunas do banco (NUMERIC(6,2), capacidade 1–60). */
export const LIMITE = {
  precoMin: 0.01,
  precoMax: 9999.99,
  capacidadeMin: 1,
  capacidadeMax: 60,
} as const
