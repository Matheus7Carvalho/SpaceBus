import { api } from '@/lib/api'
import type { Busca, DetalheViagem, ViagemEncontrada } from './types'

export const buscaApi = {
  buscar: (busca: Busca) => api.get<ViagemEncontrada[]>('/busca/viagens', busca),
  detalhar: (id: string) => api.get<DetalheViagem>(`/busca/viagens/${id}`),
  locais: () => api.get<string[]>('/busca/locais'),
}
