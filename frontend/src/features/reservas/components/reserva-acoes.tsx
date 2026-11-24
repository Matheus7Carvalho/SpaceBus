import { CreditCardIcon, XIcon } from 'lucide-react'
import { useState } from 'react'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { ConfirmDialog } from '@/components/shared/confirm-dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { mensagemDeErro } from '@/lib/cadastro'
import { useCancelarReserva, usePagar } from '../hooks'
import { POLITICA, type Reserva } from '../types'

/** Pagar (vai para a página do provedor) e cancelar (com a política). */
export function ReservaAcoes({ reserva }: { reserva: Reserva }) {
  const navigate = useNavigate()
  const pagar = usePagar(reserva.id)
  const cancelar = useCancelarReserva(reserva.id)
  const [confirmando, setConfirmando] = useState(false)
  const pago = reserva.pagamento?.status === 'APROVADO'

  const irParaPagamento = () =>
    pagar.mutate(undefined, {
      onSuccess: (p) => {
        if (p.urlPagamento) navigate(p.urlPagamento)
      },
    })

  if (!reserva.pagavel && !reserva.cancelavel) return null

  return (
    <div className="grid gap-3">
      {pagar.error && (
        <Alert variant="destructive">
          <AlertDescription>{mensagemDeErro(pagar.error, 'Não foi possível iniciar o pagamento.')}</AlertDescription>
        </Alert>
      )}
      <div className="flex flex-wrap gap-2">
        {reserva.pagavel && (
          <Button type="button" onClick={irParaPagamento} disabled={pagar.isPending}>
            <CreditCardIcon aria-hidden />
            {reserva.pagamento?.status === 'RECUSADO' ? 'Tentar pagar de novo' : 'Pagar'}
          </Button>
        )}
        {reserva.cancelavel && (
          <Button type="button" variant="outline" className="text-destructive" onClick={() => setConfirmando(true)} disabled={cancelar.isPending}>
            <XIcon aria-hidden />
            Cancelar reserva
          </Button>
        )}
      </div>
      <ConfirmDialog
        open={confirmando}
        onOpenChange={setConfirmando}
        title="Cancelar reserva?"
        description={pago ? `O valor pago será reembolsado integralmente. ${POLITICA.texto}` : 'Os lugares serão liberados.'}
        confirmLabel="Cancelar reserva"
        destructive
        onConfirm={() =>
          cancelar.mutate(undefined, {
            onSuccess: () => toast.success('Reserva cancelada.'),
            onError: (e) => toast.error(mensagemDeErro(e, 'Não foi possível cancelar.')),
          })
        }
      />
    </div>
  )
}
