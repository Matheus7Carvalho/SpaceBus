import { api } from '@/lib/api'
import type { CobrancaSimulada, Pagamento, Reserva, ReservaDaViagem, ReservaRequest } from './types'

export const reservasApi = {
  reservar: (dados: ReservaRequest) => api.post<Reserva>('/reserva', dados),
  minhas: () => api.get<Reserva[]>('/reserva'),
  buscar: (id: string) => api.get<Reserva>(`/reserva/${id}`),
  pagar: (id: string) => api.post<Pagamento>(`/reserva/${id}/pagamento`),
  cancelar: (id: string) => api.post<Reserva>(`/reserva/${id}/cancelar`),
  daViagem: (viagemId: string) => api.get<ReservaDaViagem[]>(`/viagem/${viagemId}/reservas`),
  cobranca: (provedorId: string) => api.get<CobrancaSimulada>(`/provedor-simulado/cobrancas/${provedorId}`),
  responderCobranca: (provedorId: string, acao: 'aprovar' | 'recusar') => api.post<void>(`/provedor-simulado/cobrancas/${provedorId}/${acao}`),
}
