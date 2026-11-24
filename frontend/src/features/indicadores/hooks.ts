import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { indicadoresApi } from './api'
import type { FiltroIndicadores } from './types'

export function useResumoIndicadores(filtro: FiltroIndicadores) {
  return useQuery({ queryKey: ['indicadores', 'resumo', filtro], queryFn: () => indicadoresApi.resumo(filtro), placeholderData: keepPreviousData })
}

export function useIndicadoresPorViagem(filtro: FiltroIndicadores, page: number) {
  return useQuery({
    queryKey: ['indicadores', 'viagem', filtro, page],
    queryFn: () => indicadoresApi.porViagem(filtro, page),
    placeholderData: keepPreviousData,
  })
}
