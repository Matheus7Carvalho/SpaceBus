import { Link } from 'react-router'
import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { StatusBadge } from '@/components/shared/status-badge'
import { formatar, mesmoDia } from '@/lib/datas'
import { cn } from '@/lib/utils'
import { useLerNotificacao } from '../hooks'
import { canalLabel, situacaoEnvio, type Notificacao } from '../types'

/** "hoje 07:12" ou "24/11 18:40". */
function quando(criadaEm: string, agora: Date) {
  const data = new Date(criadaEm)
  if (mesmoDia(data, agora)) return `hoje ${formatar.hora(data)}`
  return `${String(data.getDate()).padStart(2, '0')}/${String(data.getMonth() + 1).padStart(2, '0')} ${formatar.hora(data)}`
}

/** Item da lista: a área inteira abre a reserva e marca como lida. */
export function NotificacaoItem({ notificacao: n, agora }: { notificacao: Notificacao; agora: Date }) {
  const ler = useLerNotificacao()
  const naoLida = !n.lidaEm
  return (
    <li className={cn('relative grid gap-1 rounded-lg border p-3 hover:bg-muted/40', naoLida && 'border-primary/40 bg-primary/5')} data-lida={!naoLida}>
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className={cn('text-sm', naoLida ? 'font-semibold' : 'font-medium')}>
          <Link to={n.link} className="after:absolute after:inset-0 focus-visible:outline-none" onClick={() => naoLida && ler.mutate(n.id)}>
            {n.titulo}
          </Link>
          {naoLida && <span className="sr-only"> (não lida)</span>}
        </h2>
        <span className="text-xs text-muted-foreground tabular-nums">{quando(n.criadaEm, agora)}</span>
      </div>
      <p className="text-sm text-muted-foreground">{n.mensagem}</p>
      {n.envios.length > 0 && (
        <ul aria-label="Envios" className="flex flex-wrap items-center gap-2 text-xs">
          {n.envios.map((e) => (
            <li key={e.canal} className="flex items-center gap-1">
              {canalLabel[e.canal]} · <StatusBadge tom={situacaoEnvio[e.status].tom}>{situacaoEnvio[e.status].rotulo}</StatusBadge>
            </li>
          ))}
          <li className="relative z-10">
            <SimulatedBadge />
          </li>
        </ul>
      )}
    </li>
  )
}
