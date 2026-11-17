/** Iguais ao VeiculoRequest do backend e às colunas do banco. */
export const LIMITE = {
  /** Com hífen opcional ("ABC-1D23"); gravada com 7. */
  placa: 8,
  descricao: 100,
  capacidadeMin: 1,
  capacidadeMax: 60,
} as const
