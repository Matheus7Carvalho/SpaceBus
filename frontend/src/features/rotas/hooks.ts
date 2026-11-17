import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { rotasApi } from './api'
import type { RotaRequest } from './types'

export const rotaKeys = {
  all: ['rotas'] as const,
  detalhe: (id: string) => ['rotas', id] as const,
}

export function useRotas() {
  return useQuery({ queryKey: rotaKeys.all, queryFn: rotasApi.listar })
}

export function useRota(id: string | undefined) {
  return useQuery({ queryKey: rotaKeys.detalhe(id ?? ''), queryFn: () => rotasApi.buscar(id!), enabled: !!id })
}

export function useSalvarRota(id: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dados: RotaRequest) => (id ? rotasApi.atualizar(id, dados) : rotasApi.criar(dados)),
    onSuccess: (salva) => {
      queryClient.setQueryData(rotaKeys.detalhe(salva.id), salva)
      // Nomes de pontos aparecem na agenda e na busca do passageiro (resultados e sugestões de locais).
      return Promise.all([
        queryClient.invalidateQueries({ queryKey: rotaKeys.all, exact: true }),
        queryClient.invalidateQueries({ queryKey: ['viagens', 'agenda'] }),
        queryClient.invalidateQueries({ queryKey: ['busca'] }),
      ])
    },
  })
}

export function useExcluirRota() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: rotasApi.excluir,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: rotaKeys.all }),
  })
}
