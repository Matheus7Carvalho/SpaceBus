import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { veiculosApi } from './api'
import type { VeiculoRequest } from './types'

export const veiculoKeys = {
  all: ['veiculos'] as const,
  detalhe: (id: string) => ['veiculos', id] as const,
}

export function useVeiculos() {
  return useQuery({ queryKey: veiculoKeys.all, queryFn: veiculosApi.listar })
}

export function useVeiculo(id: string | undefined) {
  return useQuery({ queryKey: veiculoKeys.detalhe(id ?? ''), queryFn: () => veiculosApi.buscar(id!), enabled: !!id })
}

export function useSalvarVeiculo(id: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dados: VeiculoRequest) => (id ? veiculosApi.atualizar(id, dados) : veiculosApi.criar(dados)),
    onSuccess: (salvo) => {
      queryClient.setQueryData(veiculoKeys.detalhe(salvo.id), salvo)
      return queryClient.invalidateQueries({ queryKey: veiculoKeys.all, exact: true })
    },
  })
}

export function useExcluirVeiculo() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: veiculosApi.excluir,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: veiculoKeys.all }),
  })
}
