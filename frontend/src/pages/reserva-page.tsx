import { ArrowLeftIcon, FileQuestionIcon } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { HistoricoReserva } from '@/features/reservas/components/historico-reserva'
import { ReservaAcoes } from '@/features/reservas/components/reserva-acoes'
import { useReserva } from '@/features/reservas/hooks'
import { POLITICA, situacaoDoPagamento, situacaoReserva, type Reserva } from '@/features/reservas/types'
import { formatarPreco } from '@/features/viagens/types'
import { ApiError } from '@/lib/api'
import { formatar } from '@/lib/datas'
import { formatarDataHora } from '@/lib/format'

/** Aviso curto sobre o que acontece agora com a reserva. */
function Aviso({ reserva }: { reserva: Reserva }) {
  if (reserva.status === 'AGUARDANDO_PAGAMENTO') {
    if (!reserva.pagavel) return <p className="text-sm text-muted-foreground">O prazo para pagar acabou.</p>
    if (reserva.pagamento?.status === 'PENDENTE') return <p className="text-sm">Aguardando a confirmação do pagamento pelo provedor…</p>
    if (reserva.pagamento?.status === 'RECUSADO') return <p className="text-sm text-destructive">Pagamento recusado. Tente de novo até {formatar.hora(reserva.expiraEm)}.</p>
    return <p className="text-sm">Pague até <strong>{formatar.hora(reserva.expiraEm)}</strong> para garantir os lugares.</p>
  }
  if (reserva.status === 'CANCELADA' && reserva.motivoCancelamento === 'VIAGEM_CANCELADA') {
    return <p className="text-sm text-destructive">A transportadora cancelou esta viagem. O valor pago é reembolsado.</p>
  }
  return null
}

export function ReservaPage() {
  const { id } = useParams()
  const consulta = useReserva(id)
  const voltar = (
    <Button asChild variant="outline">
      <Link to="/reservas">
        <ArrowLeftIcon aria-hidden />
        Minhas reservas
      </Link>
    </Button>
  )

  if (consulta.isPending) return <Skeleton className="h-64 w-full max-w-3xl" />
  if (consulta.isError) {
    const naoEncontrada = consulta.error instanceof ApiError && consulta.error.status === 404
    return (
      <>
        <PageHeader title="Reserva" actions={voltar} />
        {naoEncontrada ? <EmptyState icon={FileQuestionIcon} title="Reserva não encontrada." /> : <ErrorState onRetry={() => consulta.refetch()} />}
      </>
    )
  }

  const r = consulta.data
  const situacao = situacaoReserva[r.status]
  const pagamento = r.pagamento ? situacaoDoPagamento(r.status, r.pagamento.status) : null
  const dados: [string, string][] = [
    ['Viagem', `${formatarDataHora(r.partida)} · ${r.rotaNome}`],
    ['Embarque', r.embarqueReferencia ? `${r.embarqueNome} (${r.embarqueReferencia})` : r.embarqueNome],
    ['Desembarque', r.desembarqueNome],
    ['Lugares', String(r.quantidade)],
    ['Total', formatarPreco(r.valorTotal)],
  ]

  return (
    <>
      <PageHeader title={`Reserva ${r.codigo}`} actions={voltar} />
      <section aria-label="Situação da reserva" className="grid max-w-3xl gap-4 rounded-lg border p-4">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
          {pagamento && (
            <span className="flex items-center gap-2 text-sm text-muted-foreground">
              Pagamento <StatusBadge tom={pagamento.tom}>{pagamento.rotulo}</StatusBadge>
              <SimulatedBadge />
            </span>
          )}
        </div>
        <Aviso reserva={r} />
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
          {dados.map(([rotulo, valor]) => (
            <div key={rotulo} className="contents">
              <dt className="text-muted-foreground">{rotulo}</dt>
              <dd className="font-medium">{valor}</dd>
            </div>
          ))}
        </dl>
        <ReservaAcoes reserva={r} />
        <p className="text-xs text-muted-foreground">{POLITICA.texto}</p>
        <HistoricoReserva eventos={r.historico} />
      </section>
    </>
  )
}
