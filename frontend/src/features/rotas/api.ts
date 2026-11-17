import { api, type Page } from '@/lib/api'
import type { Rota, RotaRequest } from './types'

export const rotasApi = {
  listar: () => api.get<Page<Rota>>('/rota', { sort: 'nome', size: 500 }).then((p) => p.content),
  buscar: (id: string) => api.get<Rota>(`/rota/${id}`),
  criar: (dados: RotaRequest) => api.post<Rota>('/rota', dados),
  atualizar: (id: string, dados: RotaRequest) => api.put<Rota>(`/rota/${id}`, dados),
  excluir: (id: string) => api.delete(`/rota/${id}`),
}
