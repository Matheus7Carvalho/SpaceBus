import { api, type Page } from '@/lib/api'
import type { AcaoViagem, Motorista, StatusViagem, Viagem, ViagemRequest } from './types'

/** Filtros da agenda do operador (GET /api/viagem/agenda). Sem dia: de hoje em diante. */
export type FiltroAgenda = { dia?: string; rotaId?: string; status?: StatusViagem }

export const viagensApi = {
  /** Uma página (100 viagens, por partida; tamanho e ordem definidos pelo servidor). */
  agenda: ({ dia, rotaId, status }: FiltroAgenda, page: number) =>
    api.get<Page<Viagem>>('/viagem/agenda', { dia: dia || undefined, rotaId: rotaId || undefined, status: status || undefined, page }),
  buscar: (id: string) => api.get<Viagem>(`/viagem/${id}`),
  criar: (dados: ViagemRequest) => api.post<Viagem>('/viagem', dados),
  atualizar: (id: string, dados: ViagemRequest) => api.put<Viagem>(`/viagem/${id}`, dados),
  excluir: (id: string) => api.delete(`/viagem/${id}`),
  transicao: (id: string, acao: AcaoViagem) => api.post<Viagem>(`/viagem/${id}/${acao}`),
  motoristas: () => api.get<Motorista[]>('/usuario/motoristas'),
}
