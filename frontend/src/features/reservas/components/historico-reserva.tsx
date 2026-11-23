import { formatarDataHora } from '@/lib/format'
import { acaoLabel, origemLabel, type EventoReserva } from '../types'

/** O que aconteceu com a reserva e o pagamento, do mais antigo ao mais recente. */
export function HistoricoReserva({ eventos }: { eventos: EventoReserva[] }) {
  if (eventos.length === 0) return null
  return (
    <section aria-labelledby="historico-titulo" className="grid gap-2">
      <h2 id="historico-titulo" className="text-sm font-semibold">
        Histórico
      </h2>
      <ol aria-label="Histórico da reserva" className="grid gap-1 text-sm">
        {eventos.map((e, i) => (
          <li key={`${e.acao}-${i}`} className="grid grid-cols-[8rem_1fr] gap-2">
            <span className="text-muted-foreground tabular-nums">{formatarDataHora(e.data)}</span>
            <span>
              {acaoLabel[e.acao]} <span className="text-muted-foreground">{origemLabel[e.origem]}</span>
            </span>
          </li>
        ))}
      </ol>
    </section>
  )
}
