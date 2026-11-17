import { RouteIcon } from 'lucide-react'
import { useState } from 'react'
import { BotaoNovo } from '@/components/shared/botao-novo'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { ExcluirDialog } from '@/components/shared/excluir-dialog'
import { BarraFiltros, FiltroTexto } from '@/components/shared/filtros'
import { PageHeader } from '@/components/shared/page-header'
import { SemResultado } from '@/components/shared/sem-resultado'
import { Skeleton } from '@/components/ui/skeleton'
import { RotaTable } from '@/features/rotas/components/rota-table'
import { useExcluirRota, useRotas } from '@/features/rotas/hooks'
import type { Rota } from '@/features/rotas/types'
import { contem, useFiltrosUrl } from '@/lib/filtros'

const FILTROS = ['nome', 'local'] as const

export function RotasPage() {
  const { data: rotas, isPending, isError, refetch } = useRotas()
  const excluir = useExcluirRota()
  const [excluindo, setExcluindo] = useState<Rota | null>(null)
  const filtros = useFiltrosUrl(FILTROS)
  const f = filtros.valores

  // "Local" encontra a rota pela origem, pelo destino ou por qualquer ponto de parada.
  const visiveis = (rotas ?? []).filter(
    (r) => contem(r.nome, f.nome) && contem([r.origem, r.destino, ...r.pontos.map((p) => p.nome)].join(' '), f.local),
  )
  const nova = <BotaoNovo para="nova">Nova rota</BotaoNovo>
  const temCadastro = !!rotas && rotas.length > 0

  return (
    <>
      <PageHeader title="Rotas" actions={temCadastro && nova} />
      {temCadastro && (
        <BarraFiltros ativos={filtros.ativos} onLimpar={() => filtros.limpar()}>
          <FiltroTexto id="filtro-nome" label="Nome" valor={f.nome} onChange={(v) => filtros.definir('nome', v)} />
          <FiltroTexto id="filtro-local" label="Local ou ponto" valor={f.local} onChange={(v) => filtros.definir('local', v)} />
        </BarraFiltros>
      )}
      {isPending && <Skeleton className="h-48 w-full" />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {rotas?.length === 0 && <EmptyState title="Nenhuma rota cadastrada" icon={RouteIcon} action={nova} />}
      {temCadastro && visiveis.length === 0 && <SemResultado onLimpar={() => filtros.limpar()} />}
      {visiveis.length > 0 && <RotaTable rotas={visiveis} onExcluir={setExcluindo} />}
      <ExcluirDialog registro={excluindo} titulo="Excluir rota?" mensagemSucesso="Rota excluída." mutacao={excluir} onClose={() => setExcluindo(null)} />
    </>
  )
}
