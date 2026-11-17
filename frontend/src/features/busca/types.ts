import type { PontoParada } from '@/features/rotas/types'
import type { TipoVeiculo } from '@/features/veiculos/types'

/** Espelha ViagemEncontradaResponse. */
export type ViagemEncontrada = {
  id: string
  transportadora: string
  rotaNome: string
  partida: string
  preco: number
  tipoVeiculo: TipoVeiculo
  embarque: PontoParada
  desembarque: PontoParada
  lugaresDisponiveis: number
}

/** Espelha DetalheViagemResponse. */
export type DetalheViagem = {
  id: string
  transportadora: string
  rotaNome: string
  origem: string
  destino: string
  partida: string
  preco: number
  tipoVeiculo: TipoVeiculo
  veiculoDescricao: string
  lugaresDisponiveis: number
  pontos: PontoParada[]
  atualizadaEm: string
}

/** Espelha BuscaViagemRequest (query string). */
export type Busca = { origem: string; destino: string; data: string }
