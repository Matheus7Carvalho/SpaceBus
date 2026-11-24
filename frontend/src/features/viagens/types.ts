import type { Tom } from '@/components/shared/status-badge'
import type { TipoVeiculo } from '@/features/veiculos/types'

export type StatusViagem = 'RASCUNHO' | 'PUBLICADA' | 'SUSPENSA' | 'CANCELADA' | 'EM_ANDAMENTO' | 'ENCERRADA'

export const situacoes: { valor: StatusViagem; rotulo: string; tom: Tom }[] = [
  { valor: 'RASCUNHO', rotulo: 'Rascunho', tom: 'neutro' },
  { valor: 'PUBLICADA', rotulo: 'Publicada', tom: 'sucesso' },
  { valor: 'SUSPENSA', rotulo: 'Suspensa', tom: 'atencao' },
  { valor: 'CANCELADA', rotulo: 'Cancelada', tom: 'perigo' },
  { valor: 'EM_ANDAMENTO', rotulo: 'Em andamento', tom: 'andamento' },
  { valor: 'ENCERRADA', rotulo: 'Encerrada', tom: 'info' },
]

export const situacaoInfo = Object.fromEntries(situacoes.map((s) => [s.valor, s])) as Record<StatusViagem, (typeof situacoes)[number]>

/** Endpoints de transição: POST /api/viagem/{id}/{acao}. */
export type AcaoViagem = 'publicar' | 'suspender' | 'cancelar' | 'iniciar' | 'encerrar'

/** Espelha ViagemResponse. */
export type Viagem = {
  id: string
  rotaId: string
  rotaNome: string
  origem: string
  destino: string
  veiculoId: string
  veiculoPlaca: string
  veiculoDescricao: string
  tipoVeiculo: TipoVeiculo
  motoristaId: string | null
  motoristaNome: string | null
  /** LocalDateTime sem fuso: "2026-11-18T07:00:00". */
  partida: string
  preco: number
  capacidade: number
  status: StatusViagem
  iniciadaEm: string | null
  encerradaEm: string | null
  /** Só na agenda: em andamento com localização desatualizada ou indisponível. */
  semSinal?: boolean | null
  dataCriacao: string
  dataAtualizacao: string | null
}

/** Espelha ViagemRequest (status só muda pelas ações). */
export type ViagemRequest = {
  rotaId: string
  veiculoId: string
  motoristaId: string | null
  partida: string
  preco: number
  capacidade: number
}

/** Espelha UsuarioResponse de /api/usuario/motoristas. */
export type Motorista = { id: string; nome: string }

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export const formatarPreco = (valor: number) => moeda.format(valor)
