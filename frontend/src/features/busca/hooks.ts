import { useQuery } from '@tanstack/react-query'
import { buscaApi } from './api'
import type { Busca } from './types'

export const buscaKeys = {
  resultado: (busca: Busca) => ['busca', busca] as const,
  detalhe: (id: string) => ['busca', 'detalhe', id] as const,
  locais: ['busca', 'locais'] as const,
}

/** Só consulta com a busca completa (origem, destino e data). */
export function useBuscaViagens(busca: Busca) {
  return useQuery({
    queryKey: buscaKeys.resultado(busca),
    queryFn: () => buscaApi.buscar(busca),
    enabled: !!(busca.origem && busca.destino && busca.data),
  })
}

export function useDetalheViagem(id: string | undefined) {
  return useQuery({ queryKey: buscaKeys.detalhe(id ?? ''), queryFn: () => buscaApi.detalhar(id!), enabled: !!id })
}

export function useLocais() {
  return useQuery({ queryKey: buscaKeys.locais, queryFn: buscaApi.locais, staleTime: 5 * 60_000 })
}
