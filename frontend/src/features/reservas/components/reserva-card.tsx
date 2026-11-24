import { Link } from 'react-router'
import { StatusBadge } from '@/components/shared/status-badge'
import { formatarDataHora } from '@/lib/format'
import { formatarPreco } from '@/features/viagens/types'
import { ANCORA_LOCALIZACAO } from '@/features/localizacao/components/localizacao-van'
import { situacaoReserva, type Reserva } from '../types'

/** Item de "Minhas reservas": a área inteira abre a reserva. */
export function ReservaCard({ reserva }: { reserva: Reserva }) {
  const situacao = situacaoReserva[reserva.status]
  const acompanhar = reserva.status === 'CONFIRMADA' && reserva.situacaoViagem === 'EM_ANDAMENTO'
  return (
    <li className="relative grid gap-2 rounded-lg border p-4 hover:bg-muted/40 sm:grid-cols-[1fr_auto] sm:items-center">
      <div className="grid gap-1 text-sm">
        <h2 className="font-medium">
          <Link to={`/reservas/${reserva.id}`} className="after:absolute after:inset-0 focus-visible:outline-none">
            {reserva.embarqueNome} → {reserva.desembarqueNome}
          </Link>
        </h2>
        <p className="text-muted-foreground">
          {formatarDataHora(reserva.partida)} · {reserva.rotaNome}
        </p>
        <p className="font-mono text-xs">{reserva.codigo}</p>
      </div>
      <div className="flex items-center justify-between gap-3 sm:flex-col sm:items-end">
        <div className="flex flex-wrap items-center gap-2 sm:justify-end">
          {acompanhar && <StatusBadge tom="andamento">Em andamento</StatusBadge>}
          <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
        </div>
        {acompanhar && (
          <Link to={`/reservas/${reserva.id}#${ANCORA_LOCALIZACAO}`} className="relative z-10 text-sm font-medium text-primary underline-offset-4 hover:underline">
            Acompanhar
          </Link>
        )}
        <p className="text-sm tabular-nums">
          {reserva.quantidade} {reserva.quantidade === 1 ? 'lugar' : 'lugares'} · {formatarPreco(reserva.valorTotal)}
        </p>
      </div>
    </li>
  )
}
