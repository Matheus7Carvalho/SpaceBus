import { CalendarClockIcon } from 'lucide-react'
import { useState } from 'react'
import { BotaoNovo } from '@/components/shared/botao-novo'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { ExcluirDialog } from '@/components/shared/excluir-dialog'
import { BarraFiltros, FiltroData, FiltroSelect } from '@/components/shared/filtros'
import { PageHeader } from '@/components/shared/page-header'
import { SemResultado } from '@/components/shared/sem-resultado'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useRotas } from '@/features/rotas/hooks'
import { ViagemTable } from '@/features/viagens/components/viagem-table'
import { useAgendaViagens, useExcluirViagem } from '@/features/viagens/hooks'
import { situacoes, type StatusViagem, type Viagem } from '@/features/viagens/types'
import { formatarDataHora } from '@/lib/format'
import { useFiltrosUrl } from '@/lib/filtros'

const FILTROS = ['dia', 'rota', 'situacao'] as const

export function ViagensPage() {
  const rotas = useRotas()
  const excluir = useExcluirViagem()
  const [excluindo, setExcluindo] = useState<Viagem | null>(null)
  const filtros = useFiltrosUrl(FILTROS)
  const f = filtros.valores

  const agenda = useAgendaViagens({ dia: f.dia, rotaId: f.rota, status: (f.situacao || undefined) as StatusViagem | undefined })
  const viagens = agenda.data?.pages.flatMap((p) => p.content) ?? []
  const total = agenda.data?.pages[0]?.page.totalElements ?? 0
  const nova = <BotaoNovo para="nova">Nova viagem</BotaoNovo>

  return (
    <>
      <PageHeader title="Viagens" actions={nova} />
      <BarraFiltros ativos={filtros.ativos} onLimpar={() => filtros.limpar()}>
        <FiltroData id="filtro-dia" label="Dia" valor={f.dia} onChange={(v) => filtros.definir('dia', v)} />
        <FiltroSelect
          id="filtro-rota"
          label="Rota"
          valor={f.rota}
          onChange={(v) => filtros.definir('rota', v)}
          opcoes={(rotas.data ?? []).map((r) => ({ valor: r.id, rotulo: r.nome }))}
          todos="Todas"
          className="sm:w-56"
        />
        <FiltroSelect id="filtro-situacao" label="Situação" valor={f.situacao} onChange={(v) => filtros.definir('situacao', v)} opcoes={situacoes} todos="Todas" />
      </BarraFiltros>
      {agenda.isPending && <Skeleton className="h-48 w-full" />}
      {agenda.isError && <ErrorState onRetry={() => agenda.refetch()} />}
      {agenda.isSuccess && total === 0 && filtros.ativos === 0 && (
        <EmptyState title="Nenhuma viagem programada de hoje em diante" icon={CalendarClockIcon} action={nova} />
      )}
      {agenda.isSuccess && total === 0 && filtros.ativos > 0 && <SemResultado onLimpar={() => filtros.limpar()} />}
      {viagens.length > 0 && (
        <section aria-label="Lista de viagens" aria-busy={agenda.isFetching} className="grid gap-3">
          <p className="text-sm text-muted-foreground">
            {f.dia ? 'Viagens do dia' : 'De hoje em diante'} · {viagens.length < total ? `${viagens.length} de ${total}` : total}{' '}
            {total === 1 ? 'viagem' : 'viagens'}
          </p>
          <ViagemTable viagens={viagens} onExcluir={setExcluindo} />
          {agenda.hasNextPage && (
            <Button type="button" variant="outline" className="justify-self-center" disabled={agenda.isFetchingNextPage} onClick={() => agenda.fetchNextPage()}>
              Mostrar mais
            </Button>
          )}
        </section>
      )}
      <ExcluirDialog
        registro={excluindo && { id: excluindo.id, nome: `${excluindo.rotaNome} · ${formatarDataHora(excluindo.partida)}` }}
        titulo="Excluir viagem?"
        mensagemSucesso="Viagem excluída."
        mutacao={excluir}
        onClose={() => setExcluindo(null)}
      />
    </>
  )
}
