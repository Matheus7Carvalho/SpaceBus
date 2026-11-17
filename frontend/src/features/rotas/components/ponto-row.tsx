import { ArrowDownIcon, ArrowUpIcon, Trash2Icon } from 'lucide-react'
import type { KeyboardEvent } from 'react'
import { useFormContext, useWatch } from 'react-hook-form'
import { LimitedInput } from '@/components/shared/limited-input'
import { Button } from '@/components/ui/button'
import { Field, FieldError, FieldLabel } from '@/components/ui/field'
import { LIMITE } from '../limites'
import type { RotaForm } from '../schemas'

/** Mesma grade no cabeçalho e nas linhas (desktop). */
export const PONTO_GRID = 'md:grid-cols-[2.5rem_minmax(0,1fr)_minmax(0,1fr)_6.5rem] md:items-start md:gap-2'

type PontoRowProps = {
  index: number
  total: number
  onMove: (de: number, para: number) => void
  onRemove: (index: number) => void
  onEnter: (index: number) => void
}

export function PontoRow({ index, total, onMove, onRemove, onEnter }: PontoRowProps) {
  const { register, formState, control } = useFormContext<RotaForm>()
  const erros = formState.errors.pontos?.[index]
  const [nome, referencia] = useWatch({ control, name: [`pontos.${index}.nome`, `pontos.${index}.referencia`] })
  const numero = index + 1
  const id = `ponto-${index}`

  // Enter avança para o próximo ponto (ou cria um novo) em vez de enviar o formulário.
  const aoTeclar = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key !== 'Enter') return
    e.preventDefault()
    onEnter(index)
  }

  return (
    <fieldset className={`group grid gap-2 rounded-lg border p-3 md:rounded-none md:border-0 md:border-b md:px-2 md:py-1.5 md:last:border-b-0 ${PONTO_GRID}`}>
      <legend className="sr-only">Ponto {numero}</legend>
      <span aria-hidden className="text-sm font-medium text-muted-foreground md:pt-2 md:text-center">
        <span className="md:hidden">Ponto </span>
        {numero}
      </span>
      <Field data-invalid={!!erros?.nome} className="gap-1">
        <FieldLabel htmlFor={`${id}-nome`} className="md:sr-only">
          Nome do ponto {numero}
        </FieldLabel>
        <LimitedInput
          id={`${id}-nome`}
          required
          maxLength={LIMITE.pontoNome}
          length={nome?.length ?? 0}
          aria-invalid={!!erros?.nome}
          onKeyDown={aoTeclar}
          {...register(`pontos.${index}.nome`)}
        />
        <FieldError errors={[erros?.nome]} />
      </Field>
      <Field data-invalid={!!erros?.referencia} className="gap-1">
        <FieldLabel htmlFor={`${id}-referencia`} className="md:sr-only">
          Referência do ponto {numero}
        </FieldLabel>
        <LimitedInput
          id={`${id}-referencia`}
          maxLength={LIMITE.pontoReferencia}
          length={referencia?.length ?? 0}
          placeholder="Opcional"
          aria-invalid={!!erros?.referencia}
          onKeyDown={aoTeclar}
          {...register(`pontos.${index}.referencia`)}
        />
        <FieldError errors={[erros?.referencia]} />
      </Field>
      <div className="flex justify-end gap-1 md:pt-0.5 md:opacity-0 md:group-focus-within:opacity-100 md:group-hover:opacity-100">
        <Button type="button" variant="ghost" size="icon-sm" aria-label={`Mover ponto ${numero} para cima`} disabled={index === 0} onClick={() => onMove(index, index - 1)}>
          <ArrowUpIcon aria-hidden />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={`Mover ponto ${numero} para baixo`}
          disabled={index === total - 1}
          onClick={() => onMove(index, index + 1)}
        >
          <ArrowDownIcon aria-hidden />
        </Button>
        <Button
          type="button"
          variant="ghost"
          size="icon-sm"
          aria-label={`Remover ponto ${numero}`}
          disabled={total <= LIMITE.pontosMin}
          onClick={() => onRemove(index)}
        >
          <Trash2Icon aria-hidden />
        </Button>
      </div>
    </fieldset>
  )
}
