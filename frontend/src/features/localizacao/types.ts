import type { StatusViagem } from '@/features/viagens/types'

/** Espelha LocalizacaoResponse (passageiro). "Desatualizada" vem do servidor, não do relógio do aparelho. */
export type Localizacao = {
  situacaoViagem: StatusViagem
  /** Publicada, dentro da janela de início. */
  aguardandoInicio: boolean
  disponivel: boolean
  latitude: number | null
  longitude: number | null
  precisaoM: number | null
  /** LocalDateTime sem fuso (fuso da operação). */
  registradaEm: string | null
  segundosDesdeAtualizacao: number | null
  desatualizada: boolean
  simulada: boolean
}

export type MotivoRejeicao = 'ADIANTADA' | 'ANTIGA' | 'IMPRECISA'

export const motivoRejeicaoLabel: Record<MotivoRejeicao, string> = {
  ADIANTADA: 'aparelho adiantado',
  ANTIGA: 'antiga',
  IMPRECISA: 'imprecisa',
}

/** Espelha DiagnosticoLocalizacaoResponse. */
export type Diagnostico = {
  iniciadaEm: string | null
  encerradaEm: string | null
  ultimaRecebidaEm: string | null
  aceitas: number
  rejeitadasAdiantada: number
  rejeitadasAntiga: number
  rejeitadasImprecisa: number
  ignoradas: number
  lacunas: number
  maiorIntervaloSegundos: number
  ultimoMotivoRejeicao: MotivoRejeicao | null
  ultimaRejeicaoEm: string | null
}

/** Espelha LocalizacaoOperacaoResponse. */
export type LocalizacaoOperacao = Localizacao & { semMotorista: boolean; diagnostico: Diagnostico }

/** Espelha ViagemMotoristaResponse. */
export type ViagemMotorista = {
  id: string
  rotaNome: string
  origem: string
  destino: string
  partida: string
  veiculoPlaca: string
  veiculoDescricao: string
  status: StatusViagem
  iniciadaEm: string | null
  podeIniciar: boolean
  motivo: string | null
  ultimaPosicaoEm: string | null
}

/** Espelha PosicaoRequest. Hora sempre com fuso (ISO-8601). */
export type Posicao = { latitude: number; longitude: number; precisaoM: number; registradaEm: string }

/** Espelha ResultadoEnvioResponse. */
export type ResultadoEnvio = { aceitas: number; rejeitadas: number; ignoradas: number }
