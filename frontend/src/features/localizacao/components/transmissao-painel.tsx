import { useQueryClient } from '@tanstack/react-query'
import { RadioIcon } from 'lucide-react'
import { toast } from 'sonner'
import { formatar } from '@/lib/datas'
import { cn } from '@/lib/utils'
import { localizacaoKeys } from '../hooks'
import type { ViagemMotorista } from '../types'
import { useTransmissao, type Transmissao } from '../use-transmissao'

/** Uma linha sobre o GPS ou a conexão; vazio quando tudo vai bem. */
function aviso(t: Transmissao) {
  if (t.estado === 'negada') return 'Permita a localização no navegador para transmitir.'
  if (t.estado === 'sem-sinal') return 'Sem sinal de GPS'
  if (t.estado === 'sem-suporte') return 'Este navegador não fornece a localização.'
  if (t.semConexao) return `Sem conexão · ${t.naFila} na fila`
  if (t.falhaEnvio) return `Falha ao enviar · ${t.naFila} na fila`
  if (t.recusadas) return 'Posições recusadas. Confira a hora do celular.'
  return null
}

/** Transmissão da viagem em andamento: destaque "Transmitindo", hora da última posição enviada e avisos. */
export function TransmissaoPainel({ viagem }: { viagem: ViagemMotorista }) {
  const queryClient = useQueryClient()
  const transmissao = useTransmissao(viagem.id, (mensagem) => {
    toast.error(mensagem)
    void queryClient.invalidateQueries({ queryKey: localizacaoKeys.minhasViagens })
  })
  const problema = aviso(transmissao)
  const transmitindo = transmissao.estado === 'transmitindo' && !transmissao.semConexao && !transmissao.falhaEnvio
  const ultima = transmissao.ultimaEnviadaEm ?? (viagem.ultimaPosicaoEm ? Date.parse(viagem.ultimaPosicaoEm) : null)

  return (
    <div role="status" aria-label="Transmissão da localização" className="grid gap-1 rounded-lg bg-muted/50 p-3">
      <p className={cn('flex items-center gap-2 text-lg font-semibold', transmitindo ? 'text-success' : 'text-muted-foreground')}>
        <RadioIcon aria-hidden className={cn('size-5', transmitindo && 'animate-pulse')} />
        {transmitindo ? 'Transmitindo' : transmissao.estado === 'aguardando' ? 'Obtendo localização…' : 'Sem transmissão'}
      </p>
      {problema && <p className="text-sm font-medium text-destructive">{problema}</p>}
      <p className="text-sm text-muted-foreground">
        {ultima ? `Última posição enviada às ${formatar.hora(new Date(ultima))}` : 'Nenhuma posição enviada ainda.'}
      </p>
    </div>
  )
}
