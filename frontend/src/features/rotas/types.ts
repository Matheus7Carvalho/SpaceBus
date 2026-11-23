/** Espelha PontoParadaResponse. */
export type PontoParada = {
  id: string
  ordem: number
  nome: string
  referencia: string | null
}

export type TipoRota = 'UNICA' | 'FIXA'
export type FrequenciaRotina = 'DIARIA' | 'SEMANAL'
/** java.time.DayOfWeek. */
export type DiaSemana = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export const tiposRota: { valor: TipoRota; rotulo: string; descricao: string }[] = [
  { valor: 'UNICA', rotulo: 'Rota Única', descricao: 'Viagens programadas uma a uma.' },
  { valor: 'FIXA', rotulo: 'Rota Fixa', descricao: 'Rotina diária ou semanal que gera as viagens.' },
]

export const frequencias: { valor: FrequenciaRotina; rotulo: string }[] = [
  { valor: 'DIARIA', rotulo: 'Diária' },
  { valor: 'SEMANAL', rotulo: 'Semanal' },
]

export const diasSemana: { valor: DiaSemana; curto: string; rotulo: string }[] = [
  { valor: 'MONDAY', curto: 'Seg', rotulo: 'Segunda' },
  { valor: 'TUESDAY', curto: 'Ter', rotulo: 'Terça' },
  { valor: 'WEDNESDAY', curto: 'Qua', rotulo: 'Quarta' },
  { valor: 'THURSDAY', curto: 'Qui', rotulo: 'Quinta' },
  { valor: 'FRIDAY', curto: 'Sex', rotulo: 'Sexta' },
  { valor: 'SATURDAY', curto: 'Sáb', rotulo: 'Sábado' },
  { valor: 'SUNDAY', curto: 'Dom', rotulo: 'Domingo' },
]

/** Espelha RotinaResponse. Horários "HH:mm:ss", datas "AAAA-MM-DD". */
export type Rotina = {
  frequencia: FrequenciaRotina
  dias: DiaSemana[]
  horarios: string[]
  veiculoId: string
  veiculoPlaca: string
  motoristaId: string | null
  motoristaNome: string | null
  preco: number
  capacidade: number
  inicio: string
  fim: string | null
  publicar: boolean
  geradaEm: string | null
  geradas: number | null
  conflitos: number | null
}

/** Espelha RotaResponse. */
export type Rota = {
  id: string
  nome: string
  origem: string
  destino: string
  tipo: TipoRota
  rotina: Rotina | null
  pontos: PontoParada[]
  dataCriacao: string
  dataAtualizacao: string | null
}

/** Espelha RotinaRequest. */
export type RotinaRequest = {
  frequencia: FrequenciaRotina
  dias: DiaSemana[]
  horarios: string[]
  veiculoId: string
  motoristaId: string | null
  preco: number
  capacidade: number
  inicio: string
  fim: string | null
  publicar: boolean
}

/** Espelha RotaRequest (a ordem da lista é a ordem do percurso). */
export type RotaRequest = {
  nome: string
  origem: string
  destino: string
  tipo: TipoRota
  rotina: RotinaRequest | null
  pontos: { nome: string; referencia: string | null }[]
}
