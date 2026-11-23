import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reservasApi } from './api'
import type { Reserva, ReservaRequest } from './types'

export const reservaKeys = {
  minhas: ['reservas'] as const,
  detalhe: (id: string) => ['reservas', id] as const,
  daViagem: (viagemId: string) => ['viagens', viagemId, 'reservas'] as const,
  cobranca: (provedorId: string) => ['cobranca', provedorId] as const,
}

/** Enquanto aguarda o retorno do provedor, a tela consulta de novo a cada 3 s (a confirmação vem do servidor). */
const aguardandoRetorno = (r?: Reserva) => r?.status === 'AGUARDANDO_PAGAMENTO' && r.pagamento?.status === 'PENDENTE'

export function useMinhasReservas() {
  return useQuery({ queryKey: reservaKeys.minhas, queryFn: reservasApi.minhas })
}

export function useReserva(id: string | undefined) {
  return useQuery({
    queryKey: reservaKeys.detalhe(id ?? ''),
    queryFn: () => reservasApi.buscar(id!),
    enabled: !!id,
    refetchInterval: (q) => (aguardandoRetorno(q.state.data) ? 3000 : false),
  })
}

/** Reserva/cancelamento mudam a disponibilidade mostrada na busca. */
function useAoMudar() {
  const queryClient = useQueryClient()
  return (reserva: Reserva) => {
    queryClient.setQueryData(reservaKeys.detalhe(reserva.id), reserva)
    return Promise.all([queryClient.invalidateQueries({ queryKey: reservaKeys.minhas, exact: true }), queryClient.invalidateQueries({ queryKey: ['busca'] })])
  }
}

export function useReservar() {
  const aoMudar = useAoMudar()
  return useMutation({ mutationFn: (dados: ReservaRequest) => reservasApi.reservar(dados), onSuccess: aoMudar })
}

export function useCancelarReserva(id: string) {
  const aoMudar = useAoMudar()
  return useMutation({ mutationFn: () => reservasApi.cancelar(id), onSuccess: aoMudar })
}

export function usePagar(id: string) {
  const queryClient = useQueryClient()
  return useMutation({ mutationFn: () => reservasApi.pagar(id), onSuccess: () => queryClient.invalidateQueries({ queryKey: reservaKeys.detalhe(id) }) })
}

export function useReservasDaViagem(viagemId: string) {
  return useQuery({ queryKey: reservaKeys.daViagem(viagemId), queryFn: () => reservasApi.daViagem(viagemId) })
}

export function useCobrancaSimulada(provedorId: string | undefined) {
  return useQuery({ queryKey: reservaKeys.cobranca(provedorId ?? ''), queryFn: () => reservasApi.cobranca(provedorId!), enabled: !!provedorId })
}

export function useResponderCobranca(provedorId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (acao: 'aprovar' | 'recusar') => reservasApi.responderCobranca(provedorId, acao),
    onSuccess: () => Promise.all([queryClient.invalidateQueries({ queryKey: ['reservas'] }), queryClient.invalidateQueries({ queryKey: reservaKeys.cobranca(provedorId) })]),
  })
}
