import { z } from 'zod'
import { LIMITE } from './limites'
import type { Viagem, ViagemRequest } from './types'

/** Valor do Select para "sem motorista" (o Select não aceita valor vazio). */
export const SEM_MOTORISTA = '__sem__'

const faixaPreco = `O preço deve ser entre R$ 0,01 e R$ 9.999,99.`
const faixaCapacidade = `A capacidade deve ser entre ${LIMITE.capacidadeMin} e ${LIMITE.capacidadeMax} lugares.`

export const viagemSchema = z.object({
  rotaId: z.string({ error: 'Selecione a rota.' }).min(1, 'Selecione a rota.'),
  veiculoId: z.string({ error: 'Selecione o veículo.' }).min(1, 'Selecione o veículo.'),
  motoristaId: z.string(),
  /** Valor do input datetime-local: "AAAA-MM-DDTHH:mm". */
  partida: z.string().min(1, 'Informe a partida.'),
  preco: z
    .number({ error: 'Informe o preço.' })
    .min(LIMITE.precoMin, faixaPreco)
    .max(LIMITE.precoMax, faixaPreco)
    .refine((v) => Math.abs(v * 100 - Math.round(v * 100)) < 1e-6, 'Use no máximo 2 casas decimais.'),
  capacidade: z
    .number({ error: 'Informe a capacidade.' })
    .int('Informe um número inteiro.')
    .min(LIMITE.capacidadeMin, faixaCapacidade)
    .max(LIMITE.capacidadeMax, faixaCapacidade),
})

export type ViagemForm = z.infer<typeof viagemSchema>

export function paraFormulario(v?: Viagem): ViagemForm {
  return {
    rotaId: v?.rotaId ?? '',
    veiculoId: v?.veiculoId ?? '',
    motoristaId: v?.motoristaId ?? SEM_MOTORISTA,
    partida: v ? v.partida.slice(0, 16) : '',
    preco: v?.preco ?? Number.NaN,
    capacidade: v?.capacidade ?? Number.NaN,
  }
}

export function paraRequisicao(form: ViagemForm): ViagemRequest {
  return {
    ...form,
    motoristaId: form.motoristaId === SEM_MOTORISTA ? null : form.motoristaId,
    partida: `${form.partida}:00`,
  }
}
