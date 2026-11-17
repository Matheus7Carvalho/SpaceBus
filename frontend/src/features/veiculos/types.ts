export type TipoVeiculo = 'VAN' | 'ONIBUS'

export const tipos: { valor: TipoVeiculo; rotulo: string }[] = [
  { valor: 'VAN', rotulo: 'Van' },
  { valor: 'ONIBUS', rotulo: 'Ônibus' },
]

export const tipoLabel = Object.fromEntries(tipos.map((t) => [t.valor, t.rotulo])) as Record<TipoVeiculo, string>

/** Espelha VeiculoResponse. */
export type Veiculo = {
  id: string
  placa: string
  descricao: string
  tipo: TipoVeiculo
  capacidade: number
  ativo: boolean
  dataCriacao: string
  dataAtualizacao: string | null
}

/** Espelha VeiculoRequest. */
export type VeiculoRequest = Omit<Veiculo, 'id' | 'dataCriacao' | 'dataAtualizacao'>

/** "SBV1A23" → "SBV-1A23", como na placa física. */
export function formatarPlaca(placa: string) {
  return placa.length === 7 ? `${placa.slice(0, 3)}-${placa.slice(3)}` : placa
}
