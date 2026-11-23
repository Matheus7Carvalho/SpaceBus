import { CheckIcon, FileQuestionIcon, XIcon } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router'
import { toast } from 'sonner'
import { EmptyState } from '@/components/shared/empty-state'
import { ErrorState } from '@/components/shared/error-state'
import { PageHeader } from '@/components/shared/page-header'
import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { useCobrancaSimulada, useResponderCobranca } from '@/features/reservas/hooks'
import { situacaoPagamento } from '@/features/reservas/types'
import { formatarPreco } from '@/features/viagens/types'
import { ApiError } from '@/lib/api'
import { mensagemDeErro } from '@/lib/cadastro'

/**
 * "Página do provedor" no pagamento simulado (piloto sem transações reais). O resultado volta ao
 * SpaceBus pelo mesmo caminho do retorno do provedor; a reserva só confirma depois disso.
 */
export function PagamentoSimuladoPage() {
  const { provedorId } = useParams()
  const navigate = useNavigate()
  const cobranca = useCobrancaSimulada(provedorId)
  const responder = useResponderCobranca(provedorId ?? '')

  if (cobranca.isPending) return <Skeleton className="h-48 w-full max-w-md" />
  if (cobranca.isError) {
    const naoEncontrada = cobranca.error instanceof ApiError && cobranca.error.status === 404
    return (
      <>
        <PageHeader title="Pagamento" />
        {naoEncontrada ? <EmptyState icon={FileQuestionIcon} title="Cobrança não encontrada." /> : <ErrorState onRetry={() => cobranca.refetch()} />}
      </>
    )
  }

  const c = cobranca.data
  const acao = (tipo: 'aprovar' | 'recusar') =>
    responder.mutate(tipo, {
      onSuccess: () => {
        toast.success(tipo === 'aprovar' ? 'Pagamento enviado.' : 'Pagamento recusado.')
        navigate(`/reservas/${c.reservaId}`, { replace: true })
      },
      onError: (e) => toast.error(mensagemDeErro(e, 'Não foi possível concluir.')),
    })

  return (
    <>
      <PageHeader title="Pagamento" />
      <section aria-label="Cobrança" className="grid max-w-md gap-4 rounded-lg border p-4">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm text-muted-foreground">Pix · provedor de teste</p>
          <SimulatedBadge />
        </div>
        <p className="text-3xl font-semibold tabular-nums">{formatarPreco(c.valor)}</p>
        <p className="text-sm">
          Reserva <span className="font-mono">{c.reservaCodigo}</span>
        </p>
        {c.status === 'PENDENTE' ? (
          <div className="flex flex-wrap gap-2">
            <Button type="button" onClick={() => acao('aprovar')} disabled={responder.isPending}>
              <CheckIcon aria-hidden />
              Aprovar pagamento
            </Button>
            <Button type="button" variant="outline" onClick={() => acao('recusar')} disabled={responder.isPending}>
              <XIcon aria-hidden />
              Recusar pagamento
            </Button>
          </div>
        ) : (
          <p className="text-sm">
            Esta cobrança já foi processada ({situacaoPagamento[c.status].rotulo.toLowerCase()}).{' '}
            <Link to={`/reservas/${c.reservaId}`} className="underline">
              Ver reserva
            </Link>
          </p>
        )}
      </section>
    </>
  )
}
