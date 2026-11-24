import { CalendarClockIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { Skeleton } from '@/components/ui/skeleton'
import { ViagemMotoristaCard } from '@/features/localizacao/components/viagem-motorista-card'
import { useMinhasViagensMotorista } from '@/features/localizacao/hooks'

/** Tela inicial do motorista (celular no suporte): viagens de hoje, iniciar, transmitir e encerrar. */
export function MotoristaViagensPage() {
  const viagens = useMinhasViagensMotorista()

  return (
    <>
      <PageHeader title="Minhas viagens" />
      {viagens.isPending && <Skeleton className="h-40 w-full max-w-2xl" />}
      {!viagens.data && viagens.isError && <ErrorState onRetry={() => viagens.refetch()} />}
      {viagens.data?.length === 0 && <EmptyState icon={CalendarClockIcon} title="Nenhuma viagem para hoje." />}
      {!!viagens.data?.length && (
        <ul aria-label="Viagens de hoje" className="grid max-w-2xl gap-3">
          {viagens.data.map((v) => (
            <ViagemMotoristaCard key={v.id} viagem={v} />
          ))}
        </ul>
      )}
    </>
  )
}
