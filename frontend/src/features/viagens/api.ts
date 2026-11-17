import { api, type Page } from '@/lib/api'
import type { AcaoViagem, Motorista, Viagem, ViagemRequest } from './types'

export const viagensApi = {
  listar: () => api.get<Page<Viagem>>('/viagem', { sort: 'partida', size: 500 }).then((p) => p.content),
  buscar: (id: string) => api.get<Viagem>(`/viagem/${id}`),
  criar: (dados: ViagemRequest) => api.post<Viagem>('/viagem', dados),
  atualizar: (id: string, dados: ViagemRequest) => api.put<Viagem>(`/viagem/${id}`, dados),
  excluir: (id: string) => api.delete(`/viagem/${id}`),
  transicao: (id: string, acao: AcaoViagem) => api.post<Viagem>(`/viagem/${id}/${acao}`),
  motoristas: () => api.get<Motorista[]>('/usuario/motoristas'),
}
