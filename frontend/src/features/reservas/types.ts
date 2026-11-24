import type { Tom } from '@/components/shared/status-badge'
import type { StatusViagem } from '@/features/viagens/types'

export type StatusReserva = 'AGUARDANDO_PAGAMENTO' | 'CONFIRMADA' | 'EXPIRADA' | 'CANCELADA'
export type StatusPagamento = 'PENDENTE' | 'APROVADO' | 'RECUSADO' | 'REEMBOLSO_PENDENTE' | 'REEMBOLSADO'
export type MotivoCancelamento = 'PASSAGEIRO' | 'VIAGEM_CANCELADA'

export const situacaoReserva: Record<StatusReserva, { rotulo: string; tom: Tom }> = {
  AGUARDANDO_PAGAMENTO: { rotulo: 'Aguardando pagamento', tom: 'atencao' },
  CONFIRMADA: { rotulo: 'Confirmada', tom: 'sucesso' },
  EXPIRADA: { rotulo: 'Expirada', tom: 'neutro' },
  CANCELADA: { rotulo: 'Cancelada', tom: 'perigo' },
}

export const situacaoPagamento: Record<StatusPagamento, { rotulo: string; tom: Tom }> = {
  PENDENTE: { rotulo: 'Pendente', tom: 'info' },
  APROVADO: { rotulo: 'Aprovado', tom: 'sucesso' },
  RECUSADO: { rotulo: 'Recusado', tom: 'perigo' },
  REEMBOLSO_PENDENTE: { rotulo: 'Reembolso pendente', tom: 'andamento' },
  REEMBOLSADO: { rotulo: 'Reembolsado', tom: 'neutro' },
}

/** Situação do pagamento como o passageiro e o operador veem (o servidor já marca reembolso pendente). */
export function situacaoDoPagamento(_reserva: StatusReserva, pagamento: StatusPagamento) {
  return situacaoPagamento[pagamento]
}

export type AcaoReserva =
  | 'RESERVADA'
  | 'PAGAMENTO_INICIADO'
  | 'PAGAMENTO_RECUSADO'
  | 'PAGAMENTO_APROVADO'
  | 'CONFIRMADA'
  | 'EXPIRADA'
  | 'CANCELADA'
  | 'REEMBOLSO_PENDENTE'
  | 'REEMBOLSADA'
export type OrigemAcao = 'PASSAGEIRO' | 'OPERADOR' | 'PROVEDOR' | 'SISTEMA'

export const acaoLabel: Record<AcaoReserva, string> = {
  RESERVADA: 'Reserva feita',
  PAGAMENTO_INICIADO: 'Pagamento iniciado',
  PAGAMENTO_RECUSADO: 'Pagamento recusado',
  PAGAMENTO_APROVADO: 'Pagamento aprovado',
  CONFIRMADA: 'Reserva confirmada',
  EXPIRADA: 'Prazo de pagamento vencido',
  CANCELADA: 'Reserva cancelada',
  REEMBOLSO_PENDENTE: 'Reembolso pendente',
  REEMBOLSADA: 'Valor reembolsado',
}

export const origemLabel: Record<OrigemAcao, string> = {
  PASSAGEIRO: 'por você',
  OPERADOR: 'pela transportadora',
  PROVEDOR: 'pelo provedor de pagamento',
  SISTEMA: 'automaticamente',
}

/** Espelha EventoReservaResponse. */
export type EventoReserva = { acao: AcaoReserva; origem: OrigemAcao; data: string; detalhe: string | null }

/**
 * Política publicada ao passageiro (premissa do piloto; docs/modulo-03-reserva-pagamento.md).
 * Mesmos valores de spacebus.reserva.* no backend.
 */
export const POLITICA = {
  bloqueioMinutos: 10,
  cancelamentoHoras: 2,
  maxLugares: 6,
  texto: 'Cancelamento gratuito até 2 horas antes da partida, com reembolso integral. Depois disso, a reserva não pode ser cancelada.',
} as const

/** Espelha PagamentoResponse. */
export type Pagamento = {
  provedorId: string
  metodo: string
  valor: number
  status: StatusPagamento
  urlPagamento: string | null
  criadoEm: string
}

/** Espelha ReservaResponse. */
export type Reserva = {
  id: string
  codigo: string
  status: StatusReserva
  motivoCancelamento: MotivoCancelamento | null
  viagemId: string
  rotaNome: string
  /** Situação da viagem (em andamento: "Acompanhar"). */
  situacaoViagem: StatusViagem
  partida: string
  embarqueNome: string
  embarqueReferencia: string | null
  desembarqueNome: string
  quantidade: number
  valorTotal: number
  expiraEm: string
  confirmadaEm: string | null
  canceladaEm: string | null
  cancelavel: boolean
  pagavel: boolean
  pagamento: Pagamento | null
  historico: EventoReserva[]
  criadaEm: string
}

/** Espelha ReservaRequest. */
export type ReservaRequest = { viagemId: string; embarqueOrdem: number; desembarqueOrdem: number; quantidade: number }

/** Espelha ReservaDaViagemResponse (lista de passageiros do operador). */
export type ReservaDaViagem = {
  id: string
  codigo: string
  passageiroNome: string
  passageiroEmail: string
  embarqueNome: string
  desembarqueNome: string
  quantidade: number
  valorTotal: number
  status: StatusReserva
  motivoCancelamento: MotivoCancelamento | null
  pagamento: StatusPagamento | null
}

/** Espelha ProvedorSimuladoController.CobrancaSimulada. */
export type CobrancaSimulada = { provedorId: string; reservaId: string; reservaCodigo: string; valor: number; status: StatusPagamento }
