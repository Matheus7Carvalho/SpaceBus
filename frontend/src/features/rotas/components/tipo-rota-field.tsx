import { useFormContext, useWatch } from 'react-hook-form'
import { cn } from '@/lib/utils'
import type { RotaForm } from '../schemas'
import { tiposRota } from '../types'

/** Rota Única ou Rota Fixa, como dois cartões de rádio nativos (teclado e leitor de tela de graça). */
export function TipoRotaField() {
  const { register, control } = useFormContext<RotaForm>()
  const tipo = useWatch({ control, name: 'tipo' })

  return (
    <fieldset className="grid gap-2">
      <legend className="mb-1.5 text-sm font-medium">Tipo da rota</legend>
      <div className="grid gap-2 sm:grid-cols-2">
        {tiposRota.map((t) => (
          <label
            key={t.valor}
            className={cn(
              'flex cursor-pointer items-start gap-3 rounded-lg border p-3 has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-ring',
              tipo === t.valor && 'border-primary bg-primary/5',
            )}
          >
            <input type="radio" value={t.valor} className="mt-1 accent-primary" {...register('tipo')} />
            <span className="grid gap-0.5">
              <span className="text-sm font-medium">{t.rotulo}</span>
              <span className="text-xs text-muted-foreground">{t.descricao}</span>
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}
