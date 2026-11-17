import { SearchXIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Skeleton } from '@/components/ui/skeleton'
import { BuscaForm } from '@/features/busca/components/busca-form'
import { ViagemEncontradaCard } from '@/features/busca/components/viagem-encontrada-card'
import { useBuscaViagens } from '@/features/busca/hooks'
import { ApiError } from '@/lib/api'
import { formatar, paraIsoData } from '@/lib/datas'
import { useFiltrosUrl } from '@/lib/filtros'
import { useAgora } from '@/lib/use-agora'

const CAMPOS = ['origem', 'destino', 'data'] as const

export function BuscaPage() {
  const hoje = paraIsoData(useAgora())
  const url = useFiltrosUrl(CAMPOS)
  const busca = { ...url.valores, data: url.valores.data || hoje }
  const buscou = !!(url.valores.origem && url.valores.destino && url.valores.data)
  const resultado = useBuscaViagens(buscou ? busca : { origem: '', destino: '', data: '' })
  const erroDeRegra = resultado.error instanceof ApiError && resultado.error.status === 400 ? resultado.error.message : undefined

  return (
    <>
      <PageHeader title="Buscar viagens" />
      {/* key: o formulário reflete a URL ao voltar do detalhe ou recarregar. */}
      <BuscaForm key={`${busca.origem}|${busca.destino}|${busca.data}`} inicial={busca} minData={hoje} onBuscar={(b) => url.atualizar(b)} />

      {buscou && (
        <section aria-labelledby="resultado-titulo" aria-busy={resultado.isFetching} className="grid gap-3">
          <h2 id="resultado-titulo" className="text-base font-semibold">
            {resultado.data
              ? `${resultado.data.length} ${resultado.data.length === 1 ? 'viagem' : 'viagens'} em ${formatar.data(busca.data)}`
              : `Viagens em ${formatar.data(busca.data)}`}
          </h2>
          {resultado.isPending && <Skeleton className="h-32 w-full" />}
          {erroDeRegra && (
            <Alert variant="destructive">
              <AlertDescription>{erroDeRegra}</AlertDescription>
            </Alert>
          )}
          {resultado.isError && !erroDeRegra && <ErrorState onRetry={() => resultado.refetch()} />}
          {resultado.data?.length === 0 && (
            <EmptyState icon={SearchXIcon} title="Nenhuma viagem encontrada. Tente outra data ou confira os locais de embarque e desembarque." />
          )}
          {!!resultado.data?.length && (
            <ul className="grid gap-3">
              {resultado.data.map((v) => (
                <ViagemEncontradaCard key={v.id} viagem={v} />
              ))}
            </ul>
          )}
        </section>
      )}
    </>
  )
}
