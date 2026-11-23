import { StatusBadge } from '@/components/shared/status-badge'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { ErrorState } from '@/components/shared/error-state'
import { useReservasDaViagem } from '../hooks'
import { situacaoDoPagamento, situacaoReserva, type ReservaDaViagem } from '../types'

const ocupa = (r: ReservaDaViagem) => r.status === 'CONFIRMADA' || r.status === 'AGUARDANDO_PAGAMENTO'

/** Lista de passageiros da viagem para o operador; cancelados pela viagem aparecem como "A avisar". */
export function ReservasDaViagem({ viagemId, capacidade }: { viagemId: string; capacidade: number }) {
  const reservas = useReservasDaViagem(viagemId)
  const lista = reservas.data ?? []
  const confirmados = lista.filter((r) => r.status === 'CONFIRMADA').reduce((s, r) => s + r.quantidade, 0)
  const bloqueados = lista.filter((r) => r.status === 'AGUARDANDO_PAGAMENTO').reduce((s, r) => s + r.quantidade, 0)
  const avisar = lista.filter((r) => r.motivoCancelamento === 'VIAGEM_CANCELADA')

  return (
    <section aria-labelledby="passageiros-titulo" className="grid max-w-4xl gap-3">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="passageiros-titulo" className="text-base font-semibold">
          Passageiros
        </h2>
        {reservas.isSuccess && (
          <p className="text-sm text-muted-foreground">
            {confirmados} confirmados · {bloqueados} aguardando pagamento · {Math.max(0, capacidade - confirmados - bloqueados)} livres de {capacidade}
          </p>
        )}
      </div>
      {reservas.isPending && <Skeleton className="h-24 w-full" />}
      {reservas.isError && <ErrorState onRetry={() => reservas.refetch()} />}
      {avisar.length > 0 && (
        <p role="status" className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
          {avisar.length} {avisar.length === 1 ? 'passageiro precisa ser avisado' : 'passageiros precisam ser avisados'} do cancelamento.
        </p>
      )}
      {reservas.isSuccess && lista.length === 0 && <p className="text-sm text-muted-foreground">Nenhuma reserva nesta viagem.</p>}
      {lista.length > 0 && (
        <div className="rounded-lg border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Passageiro</TableHead>
                <TableHead className="hidden md:table-cell">Trecho</TableHead>
                <TableHead className="w-16 text-right">Lugares</TableHead>
                <TableHead className="w-44">Situação</TableHead>
                <TableHead className="hidden w-36 sm:table-cell">Pagamento</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {lista.map((r) => {
                const situacao = situacaoReserva[r.status]
                const pagamento = r.pagamento ? situacaoDoPagamento(r.status, r.pagamento) : null
                return (
                  <TableRow key={r.id} className={ocupa(r) ? undefined : 'text-muted-foreground'}>
                    <TableCell className="whitespace-normal">
                      <span className="font-medium">{r.passageiroNome}</span>
                      <span className="block text-xs text-muted-foreground">
                        {r.passageiroEmail} · <span className="font-mono">{r.codigo}</span>
                      </span>
                    </TableCell>
                    <TableCell className="hidden whitespace-normal md:table-cell">
                      {r.embarqueNome} → {r.desembarqueNome}
                    </TableCell>
                    <TableCell className="text-right tabular-nums">{r.quantidade}</TableCell>
                    <TableCell>
                      <div className="flex flex-wrap gap-1">
                        <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
                        {r.motivoCancelamento === 'VIAGEM_CANCELADA' && <StatusBadge tom="andamento">A avisar</StatusBadge>}
                      </div>
                    </TableCell>
                    <TableCell className="hidden sm:table-cell">{pagamento && <StatusBadge tom={pagamento.tom}>{pagamento.rotulo}</StatusBadge>}</TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  )
}
