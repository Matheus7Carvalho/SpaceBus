import { PlusIcon } from 'lucide-react'
import { useFieldArray, useFormContext } from 'react-hook-form'
import { Button } from '@/components/ui/button'
import { FieldError } from '@/components/ui/field'
import { cn } from '@/lib/utils'
import { LIMITE } from '../limites'
import { novoPonto, type RotaForm } from '../schemas'
import { PONTO_GRID, PontoRow } from './ponto-row'

/** Pontos de parada na ordem do percurso: adicionar no topo, mover ↑ ↓, remover. */
export function PontosEditor() {
  const { control, formState, setFocus } = useFormContext<RotaForm>()
  const { fields, append, move, remove } = useFieldArray({ control, name: 'pontos', keyName: 'chave' })
  const erroLista = formState.errors.pontos?.root ?? formState.errors.pontos
  const cheio = fields.length >= LIMITE.pontosMax

  const adicionar = () => {
    if (!cheio) append(novoPonto(), { focusName: `pontos.${fields.length}.nome` })
  }

  const aoEnter = (index: number) => {
    if (index < fields.length - 1) setFocus(`pontos.${index + 1}.nome`)
    else adicionar()
  }

  return (
    <section aria-labelledby="pontos-titulo" className="grid gap-3">
      <div className="flex items-center justify-between gap-2">
        <h2 id="pontos-titulo" className="text-base font-semibold">
          Pontos de parada{' '}
          <span className="font-normal text-muted-foreground">
            ({fields.length}/{LIMITE.pontosMax})
          </span>
        </h2>
        <Button type="button" variant="outline" size="sm" disabled={cheio} onClick={adicionar}>
          <PlusIcon aria-hidden />
          Adicionar ponto
        </Button>
      </div>

      <div className="grid gap-2 md:gap-0 md:rounded-lg md:border">
        <div aria-hidden className={cn(PONTO_GRID, 'hidden border-b bg-muted/50 px-2 py-2 text-xs font-medium text-muted-foreground md:grid')}>
          <span className="text-center">#</span>
          <span>Nome</span>
          <span>Referência</span>
          <span />
        </div>
        {fields.map((field, index) => (
          <PontoRow key={field.chave} index={index} total={fields.length} onMove={move} onRemove={remove} onEnter={aoEnter} />
        ))}
      </div>

      <FieldError errors={[erroLista?.message ? { message: erroLista.message } : undefined]} />
    </section>
  )
}
