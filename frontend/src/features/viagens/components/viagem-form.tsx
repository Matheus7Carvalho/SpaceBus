import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { CadastroForm } from '@/components/shared/cadastro-form'
import { SelectField } from '@/components/shared/select-field'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { useRotas } from '@/features/rotas/hooks'
import { useVeiculos } from '@/features/veiculos/hooks'
import { formatarPlaca } from '@/features/veiculos/types'
import { useMotoristas } from '../hooks'
import { LIMITE } from '../limites'
import { SEM_MOTORISTA, viagemSchema, type ViagemForm as Valores } from '../schemas'
import type { Viagem } from '../types'
import { ViagemAcoes } from './viagem-acoes'

type ViagemFormProps = {
  /** Viagem gravada (edição); ausente no cadastro novo. */
  viagem?: Viagem
  defaultValues: Valores
  salvando: boolean
  erro?: string
  onSubmit: (dados: Valores) => Promise<Valores | undefined>
  onSalvo: () => void
  onCancel: () => void
}

export function ViagemForm({ viagem, defaultValues, ...acoes }: ViagemFormProps) {
  const form = useForm<Valores>({ resolver: zodResolver(viagemSchema), defaultValues })
  const { errors, isDirty } = form.formState
  const veiculoId = useWatch({ control: form.control, name: 'veiculoId' })
  const rotas = useRotas()
  const veiculos = useVeiculos()
  const motoristas = useMotoristas()

  const veiculoAtual = veiculos.data?.find((v) => v.id === veiculoId)
  // Inativos não entram em viagens novas, mas o já escolhido continua visível.
  const opcoesVeiculo = (veiculos.data ?? [])
    .filter((v) => v.ativo || v.id === defaultValues.veiculoId)
    .map((v) => ({ valor: v.id, rotulo: `${formatarPlaca(v.placa)} · ${v.descricao} · ${v.capacidade} lugares${v.ativo ? '' : ' (inativo)'}` }))

  // Ao trocar o veículo, a capacidade acompanha se estiver vazia ou acima do que o veículo comporta.
  useEffect(() => {
    return form.subscribe({
      name: 'veiculoId',
      formState: { values: true },
      callback: ({ values }) => {
        const escolhido = veiculos.data?.find((v) => v.id === values.veiculoId)
        const atual = values.capacidade
        if (escolhido && (Number.isNaN(atual) || atual > escolhido.capacidade)) {
          form.setValue('capacidade', escolhido.capacidade, { shouldDirty: true, shouldValidate: true })
        }
      },
    })
  }, [form, veiculos.data])

  const carregando = (pendente: boolean) => (pendente ? 'Carregando…' : 'Selecione')

  return (
    <CadastroForm form={form} rotulo="Viagem" className="max-w-4xl" somenteLeitura={viagem?.status === 'CANCELADA'} {...acoes}>
      {viagem && <ViagemAcoes viagem={viagem} pendente={isDirty} />}

      <div className="grid gap-4 md:grid-cols-2">
        <SelectField<Valores>
          name="rotaId"
          id="vi-rota"
          label="Rota"
          opcoes={(rotas.data ?? []).map((r) => ({ valor: r.id, rotulo: `${r.nome} (${r.origem} → ${r.destino})` }))}
          placeholder={carregando(rotas.isPending)}
        />
        <SelectField<Valores> name="veiculoId" id="vi-veiculo" label="Veículo" opcoes={opcoesVeiculo} placeholder={carregando(veiculos.isPending)} />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 md:grid-cols-4">
        <Field data-invalid={!!errors.partida} className="gap-1.5 sm:col-span-2 md:col-span-1">
          <FieldLabel htmlFor="vi-partida">Partida</FieldLabel>
          <Input id="vi-partida" type="datetime-local" required aria-invalid={!!errors.partida} {...form.register('partida')} />
          <FieldError errors={[errors.partida]} />
        </Field>
        <Field data-invalid={!!errors.preco} className="gap-1.5">
          <FieldLabel htmlFor="vi-preco">Preço (R$)</FieldLabel>
          <Input
            id="vi-preco"
            type="number"
            inputMode="decimal"
            required
            min={LIMITE.precoMin}
            max={LIMITE.precoMax}
            step={0.01}
            aria-invalid={!!errors.preco}
            {...form.register('preco', { valueAsNumber: true })}
          />
          <FieldError errors={[errors.preco]} />
        </Field>
        <Field data-invalid={!!errors.capacidade} className="gap-1.5">
          <FieldLabel htmlFor="vi-capacidade">Capacidade (lugares)</FieldLabel>
          <Input
            id="vi-capacidade"
            type="number"
            required
            min={LIMITE.capacidadeMin}
            max={veiculoAtual?.capacidade ?? LIMITE.capacidadeMax}
            step={1}
            aria-invalid={!!errors.capacidade}
            {...form.register('capacidade', { valueAsNumber: true })}
          />
          <FieldError errors={[errors.capacidade]} />
        </Field>
        <SelectField<Valores>
          name="motoristaId"
          id="vi-motorista"
          label="Motorista"
          opcoes={[{ valor: SEM_MOTORISTA, rotulo: 'A definir' }, ...(motoristas.data ?? []).map((m) => ({ valor: m.id, rotulo: m.nome }))]}
        />
      </div>
    </CadastroForm>
  )
}
