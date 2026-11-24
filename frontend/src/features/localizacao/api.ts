import { api } from '@/lib/api'
import type { Localizacao, LocalizacaoOperacao, Posicao, ResultadoEnvio, ViagemMotorista } from './types'

export const localizacaoApi = {
  daReserva: (reservaId: string) => api.get<Localizacao>(`/reserva/${reservaId}/localizacao`),
  daViagem: (viagemId: string) => api.get<LocalizacaoOperacao>(`/viagem/${viagemId}/localizacao`),
  minhasViagens: () => api.get<ViagemMotorista[]>('/motorista/viagem'),
  iniciar: (viagemId: string) => api.post<ViagemMotorista>(`/motorista/viagem/${viagemId}/iniciar`),
  encerrar: (viagemId: string) => api.post<ViagemMotorista>(`/motorista/viagem/${viagemId}/encerrar`),
  enviar: (viagemId: string, posicoes: Posicao[]) => api.post<ResultadoEnvio>(`/motorista/viagem/${viagemId}/localizacao`, { posicoes }),
}
