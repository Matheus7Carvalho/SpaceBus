import { api, type Page } from '@/lib/api'
import type { Notificacao, Preferencias } from './types'

export const notificacoesApi = {
  listar: (page: number) => api.get<Page<Notificacao>>('/notificacao', { page }),
  naoLidas: () => api.get<{ quantidade: number }>('/notificacao/nao-lidas'),
  ler: (id: string) => api.post<Notificacao>(`/notificacao/${id}/ler`),
  lerTodas: () => api.post<{ lidas: number }>('/notificacao/ler-todas'),
  preferencias: () => api.get<Preferencias>('/notificacao/preferencias'),
  definirWhatsapp: (whatsapp: boolean) => api.put<Preferencias>('/notificacao/preferencias', { whatsapp }),
}
