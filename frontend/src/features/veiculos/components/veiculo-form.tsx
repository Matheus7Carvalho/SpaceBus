import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm, useWatch } from 'react-hook-form'
import { CadastroForm } from '@/components/shared/cadastro-form'
import { LimitedInput } from '@/components/shared/limited-input'
import { SelectField } from '@/components/shared/select-field'
import { Checkbox } from '@/components/ui/checkbox'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { LIMITE } from '../limites'
import { veiculoSchema, type VeiculoForm as Valores } from '../schemas'
import { tipos } from '../types'

type VeiculoFormProps = {
  defaultValues: Valores
  salvando: boolean
  erro?: string
  onSubmit: (dados: Valores) => Promise<Valores | undefined>
  onSalvo: () => void
  onCancel: () => void
}

export function VeiculoForm({ defaultValues, ...acoes }: VeiculoFormProps) {
  const form = useForm<Valores>({ resolver: zodResolver(veiculoSchema), defaultValues })
  const { errors } = form.formState
  const [placa, descricao] = useWatch({ control: form.control, name: ['placa', 'descricao'] })

  return (
    <CadastroForm form={form} rotulo="Veículo" className="max-w-3xl" {...acoes}>
      <div className="grid gap-4 sm:grid-cols-[minmax(0,10rem)_minmax(0,1fr)]">
        <Field data-invalid={!!errors.placa} className="gap-1.5">
          <FieldLabel htmlFor="ve-placa">Placa</FieldLabel>
          <LimitedInput
            id="ve-placa"
            required
            maxLength={LIMITE.placa}
            length={placa?.length ?? 0}
            className="uppercase"
            autoCapitalize="characters"
            placeholder="ABC1D23"
            aria-invalid={!!errors.placa}
            {...form.register('placa')}
          />
          <FieldError errors={[errors.placa]} />
        </Field>
        <Field data-invalid={!!errors.descricao} className="gap-1.5">
          <FieldLabel htmlFor="ve-descricao">Descrição</FieldLabel>
          <LimitedInput
            id="ve-descricao"
            required
            maxLength={LIMITE.descricao}
            length={descricao?.length ?? 0}
            placeholder="Modelo"
            aria-invalid={!!errors.descricao}
            {...form.register('descricao')}
          />
          <FieldError errors={[errors.descricao]} />
        </Field>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <SelectField<Valores> name="tipo" id="ve-tipo" label="Tipo" opcoes={tipos} />
        <Field data-invalid={!!errors.capacidade} className="gap-1.5">
          <FieldLabel htmlFor="ve-capacidade">Capacidade (lugares)</FieldLabel>
          <Input
            id="ve-capacidade"
            type="number"
            required
            min={LIMITE.capacidadeMin}
            max={LIMITE.capacidadeMax}
            step={1}
            aria-invalid={!!errors.capacidade}
            {...form.register('capacidade', { valueAsNumber: true })}
          />
          <FieldError errors={[errors.capacidade]} />
        </Field>
        <Controller
          control={form.control}
          name="ativo"
          render={({ field }) => (
            <Field orientation="horizontal" className="gap-2 self-end pb-2">
              <Checkbox id="ve-ativo" checked={field.value} onCheckedChange={(v) => field.onChange(v === true)} onBlur={field.onBlur} />
              <FieldLabel htmlFor="ve-ativo">Ativo</FieldLabel>
            </Field>
          )}
        />
      </div>
    </CadastroForm>
  )
}
