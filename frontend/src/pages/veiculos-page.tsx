import { BusIcon } from 'lucide-react'
import { useState } from 'react'
import { BotaoNovo } from '@/components/shared/botao-novo'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { ExcluirDialog } from '@/components/shared/excluir-dialog'
import { BarraFiltros, FiltroSelect, FiltroTexto } from '@/components/shared/filtros'
import { PageHeader } from '@/components/shared/page-header'
import { SemResultado } from '@/components/shared/sem-resultado'
import { Skeleton } from '@/components/ui/skeleton'
import { VeiculoTable } from '@/features/veiculos/components/veiculo-table'
import { useExcluirVeiculo, useVeiculos } from '@/features/veiculos/hooks'
import { normalizarPlaca } from '@/features/veiculos/schemas'
import { formatarPlaca, tipos, type Veiculo } from '@/features/veiculos/types'
import { contem, useFiltrosUrl } from '@/lib/filtros'

const FILTROS = ['busca', 'tipo', 'situacao'] as const
const situacoes = [
  { valor: 'ativo', rotulo: 'Ativo' },
  { valor: 'inativo', rotulo: 'Inativo' },
]

export function VeiculosPage() {
  const { data: veiculos, isPending, isError, refetch } = useVeiculos()
  const excluir = useExcluirVeiculo()
  const [excluindo, setExcluindo] = useState<Veiculo | null>(null)
  const filtros = useFiltrosUrl(FILTROS)
  const f = filtros.valores

  const visiveis = (veiculos ?? []).filter(
    (v) =>
      (contem(v.descricao, f.busca) || contem(v.placa, normalizarPlaca(f.busca))) &&
      (!f.tipo || v.tipo === f.tipo) &&
      (!f.situacao || v.ativo === (f.situacao === 'ativo')),
  )
  const novo = <BotaoNovo para="novo">Novo veículo</BotaoNovo>
  const temCadastro = !!veiculos && veiculos.length > 0

  return (
    <>
      <PageHeader title="Veículos" actions={temCadastro && novo} />
      {temCadastro && (
        <BarraFiltros ativos={filtros.ativos} onLimpar={() => filtros.limpar()}>
          <FiltroTexto id="filtro-busca" label="Placa ou descrição" valor={f.busca} onChange={(v) => filtros.definir('busca', v)} />
          <FiltroSelect id="filtro-tipo" label="Tipo" valor={f.tipo} onChange={(v) => filtros.definir('tipo', v)} opcoes={tipos} />
          <FiltroSelect id="filtro-situacao" label="Situação" valor={f.situacao} onChange={(v) => filtros.definir('situacao', v)} opcoes={situacoes} todos="Todas" />
        </BarraFiltros>
      )}
      {isPending && <Skeleton className="h-48 w-full" />}
      {isError && <ErrorState onRetry={() => refetch()} />}
      {veiculos?.length === 0 && <EmptyState title="Nenhum veículo cadastrado" icon={BusIcon} action={novo} />}
      {temCadastro && visiveis.length === 0 && <SemResultado onLimpar={() => filtros.limpar()} />}
      {visiveis.length > 0 && <VeiculoTable veiculos={visiveis} onExcluir={setExcluindo} />}
      <ExcluirDialog
        registro={excluindo && { id: excluindo.id, nome: `${formatarPlaca(excluindo.placa)} · ${excluindo.descricao}` }}
        titulo="Excluir veículo?"
        mensagemSucesso="Veículo excluído."
        mutacao={excluir}
        onClose={() => setExcluindo(null)}
      />
    </>
  )
}
