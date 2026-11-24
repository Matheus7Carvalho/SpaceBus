import { BellIcon } from 'lucide-react'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { NotificacaoItem } from '@/features/notificacoes/components/notificacao-item'
import { PreferenciaWhatsapp } from '@/features/notificacoes/components/preferencia-whatsapp'
import { useLerTodas, useNaoLidas, useNotificacoes } from '@/features/notificacoes/hooks'
import { useAgora } from '@/lib/use-agora'

export function NotificacoesPage() {
  const lista = useNotificacoes()
  const naoLidas = useNaoLidas(true)
  const lerTodas = useLerTodas()
  const agora = useAgora()
  const itens = lista.data?.pages.flatMap((p) => p.content) ?? []
  const marcar = (naoLidas.data?.quantidade ?? 0) > 0 && (
    <Button variant="outline" disabled={lerTodas.isPending} onClick={() => lerTodas.mutate()}>
      Marcar todas como lidas
    </Button>
  )

  return (
    <>
      <PageHeader title="Notificações" actions={marcar || undefined} />
      <PreferenciaWhatsapp />
      {lista.isPending && <Skeleton className="h-32 w-full max-w-3xl" />}
      {!lista.data && lista.isError && <ErrorState onRetry={() => lista.refetch()} />}
      {lista.isSuccess && itens.length === 0 && <EmptyState icon={BellIcon} title="Nenhuma notificação" />}
      {itens.length > 0 && (
        <section aria-label="Lista de notificações" className="grid max-w-3xl gap-3">
          <ul className="grid gap-2">
            {itens.map((n) => (
              <NotificacaoItem key={n.id} notificacao={n} agora={agora} />
            ))}
          </ul>
          {lista.hasNextPage && (
            <Button variant="outline" className="justify-self-center" disabled={lista.isFetchingNextPage} onClick={() => lista.fetchNextPage()}>
              Mostrar mais
            </Button>
          )}
        </section>
      )}
    </>
  )
}
