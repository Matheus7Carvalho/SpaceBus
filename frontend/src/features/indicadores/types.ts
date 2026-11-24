import type { StatusViagem } from '@/features/viagens/types'

/** Espelha ResumoIndicadoresResponse. Ocupação de 0 a 1; nula sem lugares oferecidos. */
export type ResumoIndicadores = {
  inicio: string
  fim: string
  viagens: { total: number; publicadas: number; realizadas: number; canceladas: number; comReserva: number }
  vendidos: number
  oferecidos: number
  ocupacao: number | null
  receita: number
  horarios: { horario: string; viagens: number; oferecidos: number; vendidos: number; ocupacao: number | null }[]
}

/** Espelha IndicadorViagemResponse. Cancelada: disponíveis e ocupação nulos. */
export type IndicadorViagem = {
  id: string
  partida: string
  rotaNome: string
  veiculoPlaca: string
  status: StatusViagem
  capacidade: number
  vendidos: number
  disponiveis: number | null
  ocupacao: number | null
}

export type FiltroIndicadores = { inicio?: string; fim?: string; rotaId?: string }

const percentual = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 0 })

export const formatarOcupacao = (valor: number | null) => (valor == null ? '—' : percentual.format(valor))
