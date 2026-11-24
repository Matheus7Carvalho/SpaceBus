import { ArrowLeftIcon, CalendarX2Icon } from 'lucide-react'
import { Link, useParams, useSearchParams } from 'react-router'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useSessao } from '@/features/auth/hooks'
import { Percurso } from '@/features/busca/components/percurso'
import { ReservarPainel } from '@/features/reservas/components/reservar-painel'
import { useDetalheViagem } from '@/features/busca/hooks'
import { tipoLabel } from '@/features/veiculos/types'
import { formatarPreco } from '@/features/viagens/types'
import { ApiError } from '@/lib/api'
import { formatar } from '@/lib/datas'
import { formatarDataHora } from '@/lib/format'
import { useVoltarPara } from '@/lib/navegacao'

/** Número da query string ou undefined (trecho escolhido na busca). */
function ordem(valor: string | null) {
  const n = Number(valor)
  return valor && Number.isInteger(n) ? n : undefined
}

export function BuscaDetalhePage() {
  const { id } = useParams()
  const [params] = useSearchParams()
  const detalhe = useDetalheViagem(id)
  const { data: usuario } = useSessao()
  const voltarPara = useVoltarPara('/buscar')
  const voltar = (
    <Button asChild variant="outline">
      <Link to={voltarPara}>
        <ArrowLeftIcon aria-hidden />
        Voltar à busca
      </Link>
    </Button>
  )

  if (detalhe.isPending) {
    return (
      <>
        <PageHeader title="Detalhes da viagem" />
        <Skeleton className="h-64 w-full max-w-3xl" />
      </>
    )
  }
  if (detalhe.isError) {
    const indisponivel = detalhe.error instanceof ApiError && detalhe.error.status === 404
    return (
      <>
        <PageHeader title="Detalhes da viagem" actions={voltar} />
        {indisponivel ? <EmptyState icon={CalendarX2Icon} title={detalhe.error.message} /> : <ErrorState onRetry={() => detalhe.refetch()} />}
      </>
    )
  }

  const v = detalhe.data
  const itens: [string, string][] = [
    ['Data', formatar.data(v.partida)],
    ['Partida', formatar.hora(v.partida)],
    ['Preço', formatarPreco(v.preco)],
    ['Lugares disponíveis', String(v.lugaresDisponiveis)],
    ['Veículo', `${tipoLabel[v.tipoVeiculo]} · ${v.veiculoDescricao}`],
    ['Transportadora', v.transportadora],
  ]

  return (
    <>
      <PageHeader title={v.rotaNome} actions={voltar} />
      <div className="grid max-w-3xl gap-6 md:grid-cols-[1fr_1fr]">
        {usuario?.perfil === 'PASSAGEIRO' && (
          <div className="md:col-span-2">
            <ReservarPainel
              viagemId={v.id}
              preco={v.preco}
              lugaresDisponiveis={v.lugaresDisponiveis}
              embarque={ordem(params.get('embarque'))}
              desembarque={ordem(params.get('desembarque'))}
            />
          </div>
        )}
        <section aria-labelledby="dados-titulo" className="grid content-start gap-3 rounded-lg border p-4">
          <h2 id="dados-titulo" className="text-base font-semibold">
            Viagem
          </h2>
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
            {itens.map(([rotulo, valor]) => (
              <div key={rotulo} className="contents">
                <dt className="text-muted-foreground">{rotulo}</dt>
                <dd className="font-medium">{valor}</dd>
              </div>
            ))}
          </dl>
          <p className="text-xs text-muted-foreground">Atualizada pela transportadora em {formatarDataHora(v.atualizadaEm)}</p>
        </section>
        <section aria-labelledby="percurso-titulo" className="grid content-start gap-3 rounded-lg border p-4">
          <h2 id="percurso-titulo" className="text-base font-semibold">
            Percurso
          </h2>
          <Percurso pontos={v.pontos} embarque={ordem(params.get('embarque'))} desembarque={ordem(params.get('desembarque'))} />
        </section>
      </div>
    </>
  )
}
