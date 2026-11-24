import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { notificacoesApi } from './api'

export const notificacaoKeys = {
  all: ['notificacoes'] as const,
  lista: ['notificacoes', 'lista'] as const,
  naoLidas: ['notificacoes', 'nao-lidas'] as const,
  preferencias: ['notificacoes', 'preferencias'] as const,
}

/** Contagem do sino: a cada 30 s e ao voltar para a aba. */
export function useNaoLidas(ativo: boolean) {
  return useQuery({
    queryKey: notificacaoKeys.naoLidas,
    queryFn: notificacoesApi.naoLidas,
    enabled: ativo,
    staleTime: 0,
    refetchInterval: 30_000,
    refetchOnWindowFocus: true,
  })
}

export function useNotificacoes() {
  return useInfiniteQuery({
    queryKey: notificacaoKeys.lista,
    queryFn: ({ pageParam }) => notificacoesApi.listar(pageParam),
    initialPageParam: 0,
    getNextPageParam: (ultima) => (ultima.page.number + 1 < ultima.page.totalPages ? ultima.page.number + 1 : undefined),
    staleTime: 0,
    refetchOnWindowFocus: true,
  })
}

function useAoLer() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: notificacaoKeys.all })
}

export function useLerNotificacao() {
  const aoLer = useAoLer()
  return useMutation({ mutationFn: notificacoesApi.ler, onSuccess: aoLer })
}

export function useLerTodas() {
  const aoLer = useAoLer()
  return useMutation({ mutationFn: notificacoesApi.lerTodas, onSuccess: aoLer })
}

export function usePreferencias() {
  return useQuery({ queryKey: notificacaoKeys.preferencias, queryFn: notificacoesApi.preferencias })
}

export function useDefinirWhatsapp() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificacoesApi.definirWhatsapp,
    onSuccess: (prefs) => queryClient.setQueryData(notificacaoKeys.preferencias, prefs),
  })
}
