import { z } from 'zod'
import { LIMITE } from './limites'
import type { TipoVeiculo, Veiculo, VeiculoRequest } from './types'

/** Mesmo padrão do VeiculoService: antigo (ABC1234) ou Mercosul (ABC1D23). */
const PLACA = /^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$/

export const normalizarPlaca = (placa: string) => placa.replace(/[\s-]/g, '').toUpperCase()

export const veiculoSchema = z.object({
  placa: z
    .string()
    .trim()
    .min(1, 'Informe a placa.')
    .max(LIMITE.placa, `Use até ${LIMITE.placa} caracteres.`)
    .refine((p) => PLACA.test(normalizarPlaca(p)), 'Informe uma placa válida (ex.: ABC1D23).'),
  descricao: z.string().trim().min(1, 'Informe a descrição.').max(LIMITE.descricao, `Use até ${LIMITE.descricao} caracteres.`),
  tipo: z.string({ error: 'Selecione o tipo.' }).min(1, 'Selecione o tipo.'),
  capacidade: z
    .number({ error: 'Informe a capacidade.' })
    .int('Informe um número inteiro.')
    .min(LIMITE.capacidadeMin, `A capacidade deve ser entre ${LIMITE.capacidadeMin} e ${LIMITE.capacidadeMax} lugares.`)
    .max(LIMITE.capacidadeMax, `A capacidade deve ser entre ${LIMITE.capacidadeMin} e ${LIMITE.capacidadeMax} lugares.`),
  ativo: z.boolean(),
})

export type VeiculoForm = z.infer<typeof veiculoSchema>

export function paraFormulario(v?: Veiculo): VeiculoForm {
  return {
    placa: v?.placa ?? '',
    descricao: v?.descricao ?? '',
    tipo: v?.tipo ?? 'VAN',
    capacidade: v?.capacidade ?? 15,
    ativo: v?.ativo ?? true,
  }
}

export function paraRequisicao(form: VeiculoForm): VeiculoRequest {
  return { ...form, placa: normalizarPlaca(form.placa), tipo: form.tipo as TipoVeiculo }
}
