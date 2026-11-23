import { TicketIcon } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Field, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { formatarPreco } from '@/features/viagens/types'
import { mensagemDeErro } from '@/lib/cadastro'
import { useReservar } from '../hooks'
import { POLITICA } from '../types'

type Props = {
  viagemId: string
  preco: number
  lugaresDisponiveis: number
  /** Trecho escolhido na busca; sem ele, não há como reservar. */
  embarque?: number
  desembarque?: number
}

/** Quantidade, total e política antes de reservar. Leva à reserva criada (aguardando pagamento). */
export function ReservarPainel({ viagemId, preco, lugaresDisponiveis, embarque, desembarque }: Props) {
  const navigate = useNavigate()
  const reservar = useReservar()
  const maximo = Math.min(POLITICA.maxLugares, lugaresDisponiveis)
  const [quantidade, setQuantidade] = useState(1)
  const valida = Number.isInteger(quantidade) && quantidade >= 1 && quantidade <= maximo

  const enviar = (e: FormEvent) => {
    e.preventDefault()
    if (!valida || embarque === undefined || desembarque === undefined) return
    reservar.mutate(
      { viagemId, embarqueOrdem: embarque, desembarqueOrdem: desembarque, quantidade },
      { onSuccess: (r) => navigate(`/reservas/${r.id}`) },
    )
  }

  return (
    <section aria-labelledby="reservar-titulo" className="grid content-start gap-3 rounded-lg border p-4">
      <h2 id="reservar-titulo" className="text-base font-semibold">
        Reservar
      </h2>
      {lugaresDisponiveis === 0 ? (
        <p className="text-sm text-muted-foreground">Viagem lotada. Não há lugares disponíveis.</p>
      ) : embarque === undefined || desembarque === undefined ? (
        <p className="text-sm text-muted-foreground">Abra a viagem pela busca para escolher o trecho.</p>
      ) : (
        <form onSubmit={enviar} aria-label="Reservar lugares" className="grid gap-3">
          <div className="flex flex-wrap items-end gap-4">
            <Field className="w-32 gap-1.5">
              <FieldLabel htmlFor="reserva-quantidade">Lugares</FieldLabel>
              <Input
                id="reserva-quantidade"
                type="number"
                required
                min={1}
                max={maximo}
                step={1}
                value={Number.isNaN(quantidade) ? '' : quantidade}
                onChange={(e) => setQuantidade(e.target.valueAsNumber)}
              />
            </Field>
            <p className="pb-2 text-sm">
              Total <strong className="text-lg tabular-nums">{valida ? formatarPreco(preco * quantidade) : '—'}</strong>
            </p>
          </div>
          <p className="text-xs text-muted-foreground">
            Os lugares ficam reservados por {POLITICA.bloqueioMinutos} minutos para o pagamento. {POLITICA.texto}
          </p>
          {reservar.error && (
            <Alert variant="destructive">
              <AlertDescription>{mensagemDeErro(reservar.error, 'Não foi possível reservar.')}</AlertDescription>
            </Alert>
          )}
          <Button type="submit" disabled={!valida || reservar.isPending} className="justify-self-start">
            <TicketIcon aria-hidden />
            Reservar
          </Button>
        </form>
      )}
    </section>
  )
}
