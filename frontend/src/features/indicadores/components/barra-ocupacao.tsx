import { formatarOcupacao } from '../types'

/** Percentual com barra proporcional (sem biblioteca de gráfico). */
export function BarraOcupacao({ valor }: { valor: number | null }) {
  return (
    <div className="flex items-center justify-end gap-2">
      {valor != null && (
        <div aria-hidden className="hidden h-2 w-20 overflow-hidden rounded-full bg-muted sm:block">
          <div className="h-full bg-primary" style={{ width: `${Math.min(100, Math.round(valor * 100))}%` }} />
        </div>
      )}
      <span className="w-10 text-right tabular-nums">{formatarOcupacao(valor)}</span>
    </div>
  )
}
