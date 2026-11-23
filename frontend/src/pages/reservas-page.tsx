import { SearchIcon, TicketIcon } from 'lucide-react'
import { Link } from 'react-router'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ReservaCard } from '@/features/reservas/components/reserva-card'
import { useMinhasReservas } from '@/features/reservas/hooks'

export function ReservasPage() {
  const reservas = useMinhasReservas()
  const buscar = (
    <Button asChild variant="outline">
      <Link to="/buscar">
        <SearchIcon aria-hidden />
        Buscar viagens
      </Link>
    </Button>
  )

  return (
    <>
      <PageHeader title="Minhas reservas" />
      {reservas.isPending && <Skeleton className="h-32 w-full" />}
      {reservas.isError && <ErrorState onRetry={() => reservas.refetch()} />}
      {reservas.data?.length === 0 && <EmptyState icon={TicketIcon} title="Você ainda não tem reservas." action={buscar} />}
      {!!reservas.data?.length && (
        <ul className="grid max-w-3xl gap-3">
          {reservas.data.map((r) => (
            <ReservaCard key={r.id} reserva={r} />
          ))}
        </ul>
      )}
    </>
  )
}
