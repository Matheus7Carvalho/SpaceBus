import { keepPreviousData, useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { viagensApi, type FiltroAgenda } from './api'
import type { AcaoViagem, ViagemRequest } from './types'

export const viagemKeys = {
  all: ['viagens'] as const,
  agenda: ['viagens', 'agenda'] as const,
  agendaCom: (filtro: FiltroAgenda) => ['viagens', 'agenda', filtro] as const,
  detalhe: (id: string) => ['viagens', id] as const,
  motoristas: ['motoristas'] as const,
}

/**
 * Lista do operador filtrada e paginada no servidor. Cada "Mostrar mais" busca só a próxima página;
 * filtros novos (outra chave) recomeçam da primeira. Mantém a lista anterior enquanto a nova carrega.
 */
export function useAgendaViagens(filtro: FiltroAgenda) {
  return useInfiniteQuery({
    queryKey: viagemKeys.agendaCom(filtro),
    queryFn: ({ pageParam }) => viagensApi.agenda(filtro, pageParam),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.page.number + 1 < ultima.page.totalPages ? ultima.page.number + 1 : undefined),
    placeholderData: keepPreviousData,
  })
}

export function useViagem(id: string | undefined) {
  return useQuery({ queryKey: viagemKeys.detalhe(id ?? ''), queryFn: () => viagensApi.buscar(id!), enabled: !!id })
}

export function useMotoristas() {
  return useQuery({ queryKey: viagemKeys.motoristas, queryFn: viagensApi.motoristas })
}

/** Uma viagem gravada muda a agenda e a busca do passageiro. */
function useAoGravar() {
  const queryClient = useQueryClient()
  return async (salva: { id: string }) => {
    queryClient.setQueryData(viagemKeys.detalhe(salva.id), salva)
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: viagemKeys.agenda }),
      queryClient.invalidateQueries({ queryKey: ['busca'] }),
      // Cancelar a viagem cancela as reservas: a lista de passageiros muda junto.
      queryClient.invalidateQueries({ queryKey: ['viagens', salva.id, 'reservas'] }),
      // Iniciar e encerrar mudam o bloco Localização.
      queryClient.invalidateQueries({ queryKey: ['viagens', salva.id, 'localizacao'] }),
    ])
  }
}

export function useSalvarViagem(id: string | undefined) {
  const aoGravar = useAoGravar()
  return useMutation({
    mutationFn: (dados: ViagemRequest) => (id ? viagensApi.atualizar(id, dados) : viagensApi.criar(dados)),
    onSuccess: aoGravar,
  })
}

/** Publicar, suspender ou cancelar. */
export function useTransicaoViagem(id: string) {
  const aoGravar = useAoGravar()
  return useMutation({
    mutationFn: (acao: AcaoViagem) => viagensApi.transicao(id, acao),
    onSuccess: aoGravar,
  })
}

export function useExcluirViagem() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: viagensApi.excluir,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: viagemKeys.all }),
  })
}
