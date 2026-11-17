import { api, type Page } from '@/lib/api'
import type { Veiculo, VeiculoRequest } from './types'

export const veiculosApi = {
  listar: () => api.get<Page<Veiculo>>('/veiculo', { sort: 'placa', size: 500 }).then((p) => p.content),
  buscar: (id: string) => api.get<Veiculo>(`/veiculo/${id}`),
  criar: (dados: VeiculoRequest) => api.post<Veiculo>('/veiculo', dados),
  atualizar: (id: string, dados: VeiculoRequest) => api.put<Veiculo>(`/veiculo/${id}`, dados),
  excluir: (id: string) => api.delete(`/veiculo/${id}`),
}
