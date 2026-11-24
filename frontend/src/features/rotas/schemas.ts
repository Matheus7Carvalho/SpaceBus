import { z } from 'zod'
import { LIMITE as LIMITE_VIAGEM } from '@/features/viagens/limites'
import { SEM_MOTORISTA } from '@/features/viagens/schemas'
import { LIMITE } from './limites'
import type { DiaSemana, FrequenciaRotina, Rota, RotaRequest, TipoRota } from './types'

const texto = (max: number, vazio: string) => z.string().trim().min(1, vazio).max(max, `Use até ${max} caracteres.`)

const pontoSchema = z.object({
  nome: texto(LIMITE.pontoNome, 'Informe o nome do ponto.'),
  referencia: z.string().trim().max(LIMITE.pontoReferencia, `Use até ${LIMITE.pontoReferencia} caracteres.`),
})

const faixaPreco = 'O preço deve ser entre R$ 0,01 e R$ 9.999,99.'
const faixaCapacidade = `A capacidade deve ser entre ${LIMITE_VIAGEM.capacidadeMin} e ${LIMITE_VIAGEM.capacidadeMax} lugares.`

/** Campos da rotina. Ficam no formulário mesmo na Rota Única (para não perder o que foi digitado) e só são validados na Fixa. */
const rotinaSchema = z
  .object({
    frequencia: z.enum(['DIARIA', 'SEMANAL']),
    dias: z.array(z.string()),
    horarios: z
      .array(z.object({ valor: z.string().min(1, 'Informe o horário.') }))
      .min(1, 'Adicione ao menos um horário.')
      .max(LIMITE.horariosMax, `A rotina pode ter até ${LIMITE.horariosMax} horários.`),
    veiculoId: z.string().min(1, 'Selecione o veículo.'),
    motoristaId: z.string(),
    preco: z.number({ error: 'Informe o preço.' }).min(LIMITE_VIAGEM.precoMin, faixaPreco).max(LIMITE_VIAGEM.precoMax, faixaPreco),
    capacidade: z
      .number({ error: 'Informe a capacidade.' })
      .int('Informe um número inteiro.')
      .min(LIMITE_VIAGEM.capacidadeMin, faixaCapacidade)
      .max(LIMITE_VIAGEM.capacidadeMax, faixaCapacidade),
    inicio: z.string().min(1, 'Informe o início da vigência.'),
    fim: z.string(),
    publicar: z.boolean(),
  })
  .superRefine((r, ctx) => {
    if (r.frequencia === 'SEMANAL' && r.dias.length === 0) ctx.addIssue({ code: 'custom', path: ['dias'], message: 'Escolha ao menos um dia da semana.' })
    if (r.fim && r.inicio && r.fim < r.inicio) ctx.addIssue({ code: 'custom', path: ['fim'], message: 'O fim não pode ser antes do início.' })
    const repetidos = r.horarios.map((h) => h.valor).filter((h, i, todos) => h && todos.indexOf(h) !== i)
    if (repetidos.length) ctx.addIssue({ code: 'custom', path: ['horarios'], message: `Horário repetido: ${repetidos[0]}.` })
  })

const rotaBase = z.object({
  nome: texto(LIMITE.nome, 'Informe o nome.'),
  origem: texto(LIMITE.origem, 'Informe a origem.'),
  destino: texto(LIMITE.destino, 'Informe o destino.'),
  tipo: z.enum(['UNICA', 'FIXA']),
  rotina: z.any(),
  pontos: z
    .array(pontoSchema)
    .min(LIMITE.pontosMin, `Adicione pelo menos ${LIMITE.pontosMin} pontos de parada.`)
    .max(LIMITE.pontosMax, `A rota pode ter até ${LIMITE.pontosMax} pontos de parada.`),
})

export const rotaSchema = rotaBase.superRefine((rota, ctx) => {
  if (rota.tipo !== 'FIXA') return
  const resultado = rotinaSchema.safeParse(rota.rotina)
  if (!resultado.success) resultado.error.issues.forEach((issue) => ctx.addIssue({ ...issue, path: ['rotina', ...issue.path] }))
})

export type RotinaForm = {
  frequencia: FrequenciaRotina
  dias: DiaSemana[]
  horarios: { valor: string }[]
  veiculoId: string
  motoristaId: string
  preco: number
  capacidade: number
  inicio: string
  fim: string
  publicar: boolean
}

export type RotaForm = Omit<z.infer<typeof rotaBase>, 'tipo' | 'rotina'> & { tipo: TipoRota; rotina: RotinaForm }

export const novoPonto = (): RotaForm['pontos'][number] => ({ nome: '', referencia: '' })

function rotinaParaFormulario(r: Rota['rotina'], hoje: string): RotinaForm {
  return {
    frequencia: r?.frequencia ?? 'DIARIA',
    dias: r?.dias ?? [],
    horarios: r ? r.horarios.map((h) => ({ valor: h.slice(0, 5) })) : [{ valor: '' }],
    veiculoId: r?.veiculoId ?? '',
    motoristaId: r?.motoristaId ?? SEM_MOTORISTA,
    preco: r?.preco ?? Number.NaN,
    capacidade: r?.capacidade ?? Number.NaN,
    inicio: r?.inicio ?? hoje,
    fim: r?.fim ?? '',
    publicar: r?.publicar ?? false,
  }
}

/** `hoje` ("AAAA-MM-DD") é o início padrão da vigência numa rotina nova. */
export function paraFormulario(r: Rota | undefined, hoje: string): RotaForm {
  return {
    nome: r?.nome ?? '',
    origem: r?.origem ?? '',
    destino: r?.destino ?? '',
    tipo: r?.tipo ?? 'UNICA',
    rotina: rotinaParaFormulario(r?.rotina ?? null, hoje),
    pontos: r ? r.pontos.map((p) => ({ nome: p.nome, referencia: p.referencia ?? '' })) : [novoPonto(), novoPonto()],
  }
}

export function paraRequisicao(form: RotaForm): RotaRequest {
  const r = form.rotina
  return {
    nome: form.nome,
    origem: form.origem,
    destino: form.destino,
    tipo: form.tipo,
    rotina:
      form.tipo === 'FIXA'
        ? {
            frequencia: r.frequencia,
            dias: r.frequencia === 'SEMANAL' ? r.dias : [],
            horarios: r.horarios.map((h) => h.valor),
            veiculoId: r.veiculoId,
            motoristaId: r.motoristaId === SEM_MOTORISTA ? null : r.motoristaId,
            preco: r.preco,
            capacidade: r.capacidade,
            inicio: r.inicio,
            fim: r.fim || null,
            publicar: r.publicar,
          }
        : null,
    pontos: form.pontos.map((p) => ({ nome: p.nome, referencia: p.referencia || null })),
  }
}
