import type { ReactNode } from 'react'
import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { formatarPreco } from '@/features/viagens/types'
import { formatarOcupacao, type ResumoIndicadores } from '../types'

function Cartao({ titulo, valor, detalhe }: { titulo: string; valor: ReactNode; detalhe?: ReactNode }) {
  return (
    <section aria-label={titulo} className="grid gap-1 rounded-lg border p-4">
      <h2 className="text-sm text-muted-foreground">{titulo}</h2>
      <p data-valor className="text-2xl font-semibold tabular-nums">{valor}</p>
      {detalhe && <p className="text-xs text-muted-foreground">{detalhe}</p>}
    </section>
  )
}

/** Viagens, lugares vendidos, ocupação ponderada e receita (pagamento simulado). */
export function CartoesIndicadores({ resumo: r }: { resumo: ResumoIndicadores }) {
  const v = r.viagens
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <Cartao titulo="Viagens" valor={v.total} detalhe={`${v.publicadas} publicadas · ${v.realizadas} realizadas · ${v.canceladas} canceladas · ${v.comReserva} com reserva`} />
      <Cartao titulo="Lugares vendidos" valor={r.vendidos} detalhe={`de ${r.oferecidos} oferecidos`} />
      <Cartao titulo="Ocupação" valor={formatarOcupacao(r.ocupacao)} />
      <Cartao
        titulo="Receita"
        valor={formatarPreco(r.receita)}
        detalhe={
          <span className="inline-flex items-center gap-1">
            Reservas confirmadas <SimulatedBadge />
          </span>
        }
      />
    </div>
  )
}
