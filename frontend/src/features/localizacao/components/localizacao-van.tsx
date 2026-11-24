import { useEffect, useRef } from 'react'
import { useLocation } from 'react-router'
import { ErrorState } from '@/components/shared/error-state'
import { Skeleton } from '@/components/ui/skeleton'
import type { Reserva } from '@/features/reservas/types'
import { useLocalizacaoReserva } from '../hooks'
import type { Localizacao } from '../types'
import { PainelLocalizacao } from './painel-localizacao'

/** Id do bloco: "Acompanhar" (Minhas reservas) abre a reserva aqui. */
export const ANCORA_LOCALIZACAO = 'localizacao'

/** Em andamento, encerrada ou perto de começar; fora disso, o bloco não aparece. */
const exibir = (l: Localizacao) => l.situacaoViagem === 'EM_ANDAMENTO' || l.situacaoViagem === 'ENCERRADA' || l.aguardandoInicio

/** Localização da van na tela da reserva confirmada do passageiro (atualiza sozinha). */
export function LocalizacaoVan({ reserva }: { reserva: Reserva }) {
  const consulta = useLocalizacaoReserva(reserva.id, reserva.status === 'CONFIRMADA')
  const secao = useRef<HTMLElement>(null)
  const { hash } = useLocation()
  const visivel = !!consulta.data && exibir(consulta.data)

  useEffect(() => {
    if (visivel && hash === `#${ANCORA_LOCALIZACAO}`) secao.current?.scrollIntoView({ block: 'start' })
  }, [visivel, hash])

  if (reserva.status !== 'CONFIRMADA') return null
  if (consulta.isPending) return <Skeleton className="h-24 w-full max-w-3xl" />
  if (!consulta.data) return <ErrorState message="Não foi possível carregar a localização." onRetry={() => consulta.refetch()} />
  if (!visivel) return null

  return (
    <section ref={secao} id={ANCORA_LOCALIZACAO} aria-labelledby="localizacao-titulo" className="grid max-w-3xl scroll-mt-4 gap-3 rounded-lg border p-4">
      <h2 id="localizacao-titulo" className="text-base font-semibold">
        Localização da van
      </h2>
      <PainelLocalizacao localizacao={consulta.data} falhaAoAtualizar={consulta.isError} />
    </section>
  )
}
