import type { Tom } from '@/components/shared/status-badge'

export type TipoNotificacao = 'RESERVA_CONFIRMADA' | 'VIAGEM_ALTERADA' | 'VIAGEM_CANCELADA' | 'VIAGEM_INICIADA' | 'LEMBRETE'
export type CanalEnvio = 'EMAIL' | 'WHATSAPP'
export type StatusEnvio = 'ENVIADA' | 'FALHOU'

export const canalLabel: Record<CanalEnvio, string> = { EMAIL: 'E-mail', WHATSAPP: 'WhatsApp' }
export const situacaoEnvio: Record<StatusEnvio, { rotulo: string; tom: Tom }> = {
  ENVIADA: { rotulo: 'Enviado', tom: 'sucesso' },
  FALHOU: { rotulo: 'Falhou', tom: 'perigo' },
}

/** Espelha NotificacaoResponse.Envio. */
export type Envio = { canal: CanalEnvio; status: StatusEnvio; motivo: string | null; em: string }

/** Espelha NotificacaoResponse. */
export type Notificacao = {
  id: string
  tipo: TipoNotificacao
  titulo: string
  mensagem: string
  link: string
  reservaId: string
  criadaEm: string
  lidaEm: string | null
  envios: Envio[]
}

/** Espelha PreferenciasResponse. */
export type Preferencias = { whatsapp: boolean; whatsappConsentidoEm: string | null }
