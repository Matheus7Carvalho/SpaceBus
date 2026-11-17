import { z } from 'zod'
import { LIMITE } from './limites'
import type { Rota, RotaRequest } from './types'

const texto = (max: number, vazio: string) => z.string().trim().min(1, vazio).max(max, `Use até ${max} caracteres.`)

const pontoSchema = z.object({
  nome: texto(LIMITE.pontoNome, 'Informe o nome do ponto.'),
  referencia: z.string().trim().max(LIMITE.pontoReferencia, `Use até ${LIMITE.pontoReferencia} caracteres.`),
})

export const rotaSchema = z.object({
  nome: texto(LIMITE.nome, 'Informe o nome.'),
  origem: texto(LIMITE.origem, 'Informe a origem.'),
  destino: texto(LIMITE.destino, 'Informe o destino.'),
  pontos: z
    .array(pontoSchema)
    .min(LIMITE.pontosMin, `Adicione pelo menos ${LIMITE.pontosMin} pontos de parada.`)
    .max(LIMITE.pontosMax, `A rota pode ter até ${LIMITE.pontosMax} pontos de parada.`),
})

export type RotaForm = z.infer<typeof rotaSchema>

export const novoPonto = (): RotaForm['pontos'][number] => ({ nome: '', referencia: '' })

export function paraFormulario(r?: Rota): RotaForm {
  return {
    nome: r?.nome ?? '',
    origem: r?.origem ?? '',
    destino: r?.destino ?? '',
    pontos: r ? r.pontos.map((p) => ({ nome: p.nome, referencia: p.referencia ?? '' })) : [novoPonto(), novoPonto()],
  }
}

export function paraRequisicao(form: RotaForm): RotaRequest {
  return { ...form, pontos: form.pontos.map((p) => ({ nome: p.nome, referencia: p.referencia || null })) }
}
