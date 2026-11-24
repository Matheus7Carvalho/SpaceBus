import { api, type Page } from '@/lib/api'
import type { FiltroIndicadores, IndicadorViagem, ResumoIndicadores } from './types'

const parametros = ({ inicio, fim, rotaId }: FiltroIndicadores) => ({ inicio: inicio || undefined, fim: fim || undefined, rotaId: rotaId || undefined })

export const indicadoresApi = {
  resumo: (filtro: FiltroIndicadores) => api.get<ResumoIndicadores>('/indicador', parametros(filtro)),
  porViagem: (filtro: FiltroIndicadores, page: number) => api.get<Page<IndicadorViagem>>('/indicador/viagem', { ...parametros(filtro), page }),
}
