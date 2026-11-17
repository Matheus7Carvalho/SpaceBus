/** Espelha PontoParadaResponse. */
export type PontoParada = {
  id: string
  ordem: number
  nome: string
  referencia: string | null
}

/** Espelha RotaResponse. */
export type Rota = {
  id: string
  nome: string
  origem: string
  destino: string
  pontos: PontoParada[]
  dataCriacao: string
  dataAtualizacao: string | null
}

/** Espelha RotaRequest (a ordem da lista é a ordem do percurso). */
export type RotaRequest = {
  nome: string
  origem: string
  destino: string
  pontos: { nome: string; referencia: string | null }[]
}
