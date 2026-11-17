import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import { CadastroForm } from '@/components/shared/cadastro-form'
import { LimitedInput } from '@/components/shared/limited-input'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { LIMITE } from '../limites'
import { rotaSchema, type RotaForm as Valores } from '../schemas'
import { PontosEditor } from './pontos-editor'

type RotaFormProps = {
  defaultValues: Valores
  salvando: boolean
  erro?: string
  onSubmit: (dados: Valores) => Promise<Valores | undefined>
  onSalvo: () => void
  onCancel: () => void
}

export function RotaForm({ defaultValues, ...acoes }: RotaFormProps) {
  const form = useForm<Valores>({ resolver: zodResolver(rotaSchema), defaultValues })
  const { errors } = form.formState
  const [nome, origem, destino] = useWatch({ control: form.control, name: ['nome', 'origem', 'destino'] })

  return (
    <CadastroForm form={form} rotulo="Rota" {...acoes}>
      <div className="grid gap-4 md:grid-cols-3">
        <Field data-invalid={!!errors.nome} className="gap-1.5">
          <FieldLabel htmlFor="ro-nome">Nome</FieldLabel>
          <LimitedInput id="ro-nome" required maxLength={LIMITE.nome} length={nome?.length ?? 0} counter="always" aria-invalid={!!errors.nome} {...form.register('nome')} />
          <FieldError errors={[errors.nome]} />
        </Field>
        <Field data-invalid={!!errors.origem} className="gap-1.5">
          <FieldLabel htmlFor="ro-origem">Origem</FieldLabel>
          <LimitedInput id="ro-origem" required maxLength={LIMITE.origem} length={origem?.length ?? 0} aria-invalid={!!errors.origem} {...form.register('origem')} />
          <FieldError errors={[errors.origem]} />
        </Field>
        <Field data-invalid={!!errors.destino} className="gap-1.5">
          <FieldLabel htmlFor="ro-destino">Destino</FieldLabel>
          <LimitedInput id="ro-destino" required maxLength={LIMITE.destino} length={destino?.length ?? 0} aria-invalid={!!errors.destino} {...form.register('destino')} />
          <FieldError errors={[errors.destino]} />
        </Field>
      </div>
      <PontosEditor />
    </CadastroForm>
  )
}
