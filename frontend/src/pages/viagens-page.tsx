import { CalendarClockIcon } from 'lucide-react'
import { useState } from 'react'
import { BotaoNovo } from '@/components/shared/botao-novo'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { ExcluirDialog } from '@/components/shared/excluir-dialog'
import { BarraFiltros, FiltroData, FiltroSelect } from '@/components/shared/filtros'
import { PageHeader } from '@/components/shared/page-header'
import { SemResultado } from '@/components/shared/sem-resultado'
import { Skeleton } from '@/components/ui/skeleton'
import { useRotas } from '@/features/rotas/hooks'
import { ViagemTable } from '@/features/viagens/components/viagem-table'
import { useExcluirViagem, useViagens } from '@/features/viagens/hooks'
import { situacoes, type Viagem } from '@/features/viagens/types'
import { formatarDataHora } from '@/lib/format'
import { useFiltrosUrl } from '@/lib/filtros'

const FILTROS = ['dia', 'rota', 'situacao'] as const

export function ViagensPage() {
  const { data: viagens, isPending, isError, refetch } = useViagens()
  const rotas = useRotas()
  const excluir = useExcluirViagem()
  const [excluindo, setExcluindo] = useState<Viagem | null>(null)
  const filtros = useFiltrosUrl(FILTROS)
  const f = filtros.valores

  const visiveis = (viagens ?? []).filter(
    (v) => (!f.dia || v.partida.startsWith(f.dia)) && (!f.rota || v.rotaId === f.rota) && (!f.situacao || v.status === f.situacao),
  )
  const nova = <BotaoNovo para="nova">Nova viagem</BotaoNovo>
  const temCadastro = !!viagens && viagens.length > 0

  return (
    <>
      <PageHeader title="Viagens" actions={temCadastro && nova} />
      {temCadastro && (
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
      )}
      {isPending && <Skeleton className="h-48 w-full" />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {viagens?.length === 0 && <EmptyState title="Nenhuma viagem cadastrada" icon={CalendarClockIcon} action={nova} />}
      {temCadastro && visiveis.length === 0 && <SemResultado onLimpar={() => filtros.limpar()} />}
      {visiveis.length > 0 && <ViagemTable viagens={visiveis} onExcluir={setExcluindo} />}
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
