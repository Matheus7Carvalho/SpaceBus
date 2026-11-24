import { zodResolver } from '@hookform/resolvers/zod'
import { CalendarClockIcon } from 'lucide-react'
import { useForm, useWatch, type Resolver } from 'react-hook-form'
import { Link } from 'react-router'
import { CadastroForm } from '@/components/shared/cadastro-form'
import { LimitedInput } from '@/components/shared/limited-input'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { formatarDataHora } from '@/lib/format'
import { LIMITE } from '../limites'
import { rotaSchema, type RotaForm as Valores } from '../schemas'
import type { Rota } from '../types'
import { PontosEditor } from './pontos-editor'
import { RotinaCampos } from './rotina-campos'
import { TipoRotaField } from './tipo-rota-field'

type RotaFormProps = {
  /** Rota gravada (edição); ausente no cadastro novo. */
  rota?: Rota
  defaultValues: Valores
  salvando: boolean
  erro?: string
  onSubmit: (dados: Valores) => Promise<Valores | undefined>
  onSalvo: () => void
  onCancel: () => void
}

export function RotaForm({ rota, defaultValues, ...acoes }: RotaFormProps) {
  // O schema valida a rotina só na Rota Fixa (superRefine); o tipo do formulário é o RotaForm.
  const form = useForm<Valores>({ resolver: zodResolver(rotaSchema) as unknown as Resolver<Valores>, defaultValues })
  const { errors } = form.formState
  const [nome, origem, destino, tipo] = useWatch({ control: form.control, name: ['nome', 'origem', 'destino', 'tipo'] })

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
      <TipoRotaField />
      {tipo === 'FIXA' && (
        <>
          <RotinaCampos veiculoAtual={rota?.rotina?.veiculoId} />
          {rota?.tipo === 'FIXA' && rota.rotina?.geradaEm && <ResultadoGeracao rota={rota} />}
        </>
      )}
      <PontosEditor />
    </CadastroForm>
  )
}

/** Resultado da última geração da rotina gravada, com atalho para as viagens da rota. */
function ResultadoGeracao({ rota }: { rota: Rota }) {
  const r = rota.rotina!
  return (
    <div role="status" className="flex flex-wrap items-center justify-between gap-3 rounded-lg bg-muted/50 px-4 py-3 text-sm">
      <p>
        Última geração em {formatarDataHora(r.geradaEm)}: <strong>{r.geradas ?? 0}</strong> {r.geradas === 1 ? 'viagem criada' : 'viagens criadas'}
        {!!r.conflitos && (
          <>
            {' '}
            · <strong className="text-destructive">{r.conflitos}</strong> {r.conflitos === 1 ? 'horário pulado' : 'horários pulados'} (veículo já tinha viagem)
          </>
        )}
        .
      </p>
      <Button asChild variant="outline" size="sm">
        <Link to={`/operacao/viagens?rota=${rota.id}`}>
          <CalendarClockIcon aria-hidden />
          Ver viagens da rota
        </Link>
      </Button>
    </div>
  )
}
