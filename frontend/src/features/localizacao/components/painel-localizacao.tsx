import { lazy, Suspense } from 'react'
import { Skeleton } from '@/components/ui/skeleton'
import type { Localizacao } from '../types'
import { SituacaoLocalizacao } from './situacao-localizacao'

/** O Leaflet só é baixado nas telas que mostram o mapa. */
const MapaVan = lazy(() => import('./mapa-van'))

type PainelLocalizacaoProps = {
  localizacao: Localizacao
  falhaAoAtualizar: boolean
}

/** Situação da posição (sempre visível) e, quando há posição, o mapa. Igual para passageiro e operação. */
export function PainelLocalizacao({ localizacao: l, falhaAoAtualizar }: PainelLocalizacaoProps) {
  const temPosicao = l.disponivel && l.latitude != null && l.longitude != null
  return (
    <div className="grid gap-3">
      <SituacaoLocalizacao localizacao={l} falhaAoAtualizar={falhaAoAtualizar} />
      {temPosicao && (
        <Suspense fallback={<Skeleton className="h-64 w-full sm:h-80" />}>
          <MapaVan latitude={l.latitude!} longitude={l.longitude!} precisaoM={l.precisaoM ?? 0} esmaecido={l.desatualizada} />
        </Suspense>
      )}
    </div>
  )
}
