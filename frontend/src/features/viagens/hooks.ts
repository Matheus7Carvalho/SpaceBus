import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { viagensApi } from './api'
import type { AcaoViagem, ViagemRequest } from './types'

export const viagemKeys = {
  all: ['viagens'] as const,
  detalhe: (id: string) => ['viagens', id] as const,
  motoristas: ['motoristas'] as const,
}

export function useViagens() {
  return useQuery({ queryKey: viagemKeys.all, queryFn: viagensApi.listar })
}

export function useViagem(id: string | undefined) {
  return useQuery({ queryKey: viagemKeys.detalhe(id ?? ''), queryFn: () => viagensApi.buscar(id!), enabled: !!id })
}

export function useMotoristas() {
  return useQuery({ queryKey: viagemKeys.motoristas, queryFn: viagensApi.motoristas })
}

export function useSalvarViagem(id: string | undefined) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (dados: ViagemRequest) => (id ? viagensApi.atualizar(id, dados) : viagensApi.criar(dados)),
    onSuccess: (salva) => {
      queryClient.setQueryData(viagemKeys.detalhe(salva.id), salva)
      return queryClient.invalidateQueries({ queryKey: viagemKeys.all, exact: true })
    },
  })
}

/** Publicar, suspender ou cancelar. */
export function useTransicaoViagem(id: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (acao: AcaoViagem) => viagensApi.transicao(id, acao),
    onSuccess: (salva) => {
      queryClient.setQueryData(viagemKeys.detalhe(salva.id), salva)
      return queryClient.invalidateQueries({ queryKey: viagemKeys.all, exact: true })
    },
  })
}

export function useExcluirViagem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: viagensApi.excluir,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: viagemKeys.all }),
  })
}
