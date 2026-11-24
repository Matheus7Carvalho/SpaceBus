import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { localizacaoApi } from './api'
import { LOCALIZACAO } from './limites'
import type { Localizacao } from './types'

export const localizacaoKeys = {
  daReserva: (reservaId: string) => ['reservas', reservaId, 'localizacao'] as const,
  daViagem: (viagemId: string) => ['viagens', viagemId, 'localizacao'] as const,
  minhasViagens: ['motorista', 'viagens'] as const,
}

const ATUALIZACAO_MS = LOCALIZACAO.atualizacaoSegundos * 1000
/** Viagem que ainda não está perto de começar: confere de vez em quando, sem gastar a bateria do passageiro. */
const ESPERA_MS = 60_000

/** Polling (sem WebSocket): rápido com a viagem em andamento ou prestes a começar; com erro, mantém a última leitura. */
function intervalo(dados: Localizacao | undefined) {
  if (!dados) return ATUALIZACAO_MS
  if (dados.situacaoViagem === 'EM_ANDAMENTO' || dados.aguardandoInicio) return ATUALIZACAO_MS
  return dados.situacaoViagem === 'PUBLICADA' ? ESPERA_MS : false
}

export function useLocalizacaoReserva(reservaId: string, ativa: boolean) {
  return useQuery({
    queryKey: localizacaoKeys.daReserva(reservaId),
    queryFn: () => localizacaoApi.daReserva(reservaId),
    enabled: ativa,
    staleTime: 0,
    refetchInterval: (q) => intervalo(q.state.data),
  })
}

export function useLocalizacaoViagem(viagemId: string) {
  return useQuery({
    queryKey: localizacaoKeys.daViagem(viagemId),
    queryFn: () => localizacaoApi.daViagem(viagemId),
    staleTime: 0,
    refetchInterval: (q) => intervalo(q.state.data),
  })
}

export function useMinhasViagensMotorista() {
  return useQuery({ queryKey: localizacaoKeys.minhasViagens, queryFn: localizacaoApi.minhasViagens, refetchInterval: 30_000 })
}

/** Iniciar ou encerrar pelo motorista. */
export function useTransicaoMotorista(viagemId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (acao: 'iniciar' | 'encerrar') => localizacaoApi[acao](viagemId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: localizacaoKeys.minhasViagens }),
  })
}
