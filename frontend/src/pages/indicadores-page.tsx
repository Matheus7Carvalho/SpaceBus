import { useState } from 'react'
import { ErrorState } from '@/components/shared/error-state'
import { BarraFiltros, FiltroData, FiltroSelect } from '@/components/shared/filtros'
import { PageHeader } from '@/components/shared/page-header'
import { SemResultado } from '@/components/shared/sem-resultado'
import { Skeleton } from '@/components/ui/skeleton'
import { CartoesIndicadores } from '@/features/indicadores/components/cartoes-indicadores'
import { TabelaHorarios } from '@/features/indicadores/components/tabela-horarios'
import { TabelaViagensIndicadores } from '@/features/indicadores/components/tabela-viagens-indicadores'
import { useIndicadoresPorViagem, useResumoIndicadores } from '@/features/indicadores/hooks'
import { useRotas } from '@/features/rotas/hooks'
import { formatar } from '@/lib/datas'
import { useFiltrosUrl } from '@/lib/filtros'

const FILTROS = ['de', 'ate', 'rota'] as const

export function IndicadoresPage() {
  const rotas = useRotas()
  const filtros = useFiltrosUrl(FILTROS)
  const f = filtros.valores
  const filtro = { inicio: f.de, fim: f.ate, rotaId: f.rota }
  const [pagina, setPagina] = useState(0)
  const resumo = useResumoIndicadores(filtro)
  const porViagem = useIndicadoresPorViagem(filtro, pagina)
  // Filtro novo recomeça da primeira página.
  const definir = (chave: (typeof FILTROS)[number], valor: string) => {
    setPagina(0)
    filtros.definir(chave, valor)
  }
  const limpar = () => {
    setPagina(0)
    filtros.limpar()
  }
  const r = resumo.data

  return (
    <>
      <PageHeader title="Indicadores" />
      <BarraFiltros ativos={filtros.ativos} onLimpar={limpar}>
        <FiltroData id="filtro-de" label="De" valor={f.de} onChange={(v) => definir('de', v)} />
        <FiltroData id="filtro-ate" label="Até" valor={f.ate} onChange={(v) => definir('ate', v)} />
        <FiltroSelect
          id="filtro-rota"
          label="Rota"
          valor={f.rota}
          onChange={(v) => definir('rota', v)}
          opcoes={(rotas.data ?? []).map((rota) => ({ valor: rota.id, rotulo: rota.nome }))}
          todos="Todas"
          className="sm:w-56"
        />
      </BarraFiltros>
      {resumo.isPending && <Skeleton className="h-48 w-full" />}
      {resumo.isError && <ErrorState message={resumo.error.message || undefined} onRetry={() => resumo.refetch()} />}
      {r && !resumo.isError && (
        <>
          <p className="text-sm text-muted-foreground">
            Partidas de {formatar.data(r.inicio)} a {formatar.data(r.fim)}
          </p>
          <CartoesIndicadores resumo={r} />
          {r.viagens.total === 0 ? (
            <SemResultado titulo="Nenhuma viagem no período." onLimpar={limpar} />
          ) : (
            <>
              {r.horarios.length > 0 && <TabelaHorarios horarios={r.horarios} />}
              {porViagem.isError && <ErrorState onRetry={() => porViagem.refetch()} />}
              {porViagem.data && (
                <TabelaViagensIndicadores
                  viagens={porViagem.data.content}
                  total={porViagem.data.page.totalElements}
                  pagina={pagina}
                  paginas={porViagem.data.page.totalPages}
                  onPagina={setPagina}
                />
              )}
            </>
          )}
        </>
      )}
    </>
  )
}
