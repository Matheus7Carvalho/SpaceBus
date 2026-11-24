import { useState } from 'react'
import { toast } from 'sonner'
import { ConfirmDialog } from '@/components/shared/confirm-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { formatarPlaca } from '@/features/veiculos/types'
import { situacaoInfo } from '@/features/viagens/types'
import { mensagemDeErro } from '@/lib/cadastro'
import { formatar } from '@/lib/datas'
import { useTransicaoMotorista } from '../hooks'
import type { ViagemMotorista } from '../types'
import { TransmissaoPainel } from './transmissao-painel'

/** Viagem de hoje do motorista: Iniciar (na janela) ou, em andamento, a transmissão e Encerrar. */
export function ViagemMotoristaCard({ viagem: v }: { viagem: ViagemMotorista }) {
  const transicao = useTransicaoMotorista(v.id)
  const [confirmandoEncerrar, setConfirmandoEncerrar] = useState(false)
  const situacao = situacaoInfo[v.status]
  const titulo = `titulo-${v.id}`

  const executar = (acao: 'iniciar' | 'encerrar', sucesso: string) =>
    transicao.mutate(acao, {
      onSuccess: () => toast.success(sucesso),
      onError: (e) => toast.error(mensagemDeErro(e, 'Não foi possível alterar a viagem.')),
    })

  return (
    <li>
      <article aria-labelledby={titulo} className="grid gap-3 rounded-lg border p-4">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div className="grid gap-0.5">
            <h2 id={titulo} className="text-base font-semibold">
              {formatar.hora(v.partida)} · {v.rotaNome}
            </h2>
            <p className="text-sm text-muted-foreground">
              {v.origem} → {v.destino}
            </p>
            <p className="text-sm">
              <span className="font-mono">{formatarPlaca(v.veiculoPlaca)}</span> · {v.veiculoDescricao}
            </p>
          </div>
          <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
        </div>

        {v.status === 'PUBLICADA' && (
          <div className="grid gap-1">
            <Button size="lg" className="w-full sm:w-auto sm:justify-self-start" disabled={!v.podeIniciar || transicao.isPending} onClick={() => executar('iniciar', 'Viagem iniciada.')}>
              Iniciar
            </Button>
            {v.motivo && <p className="text-sm text-muted-foreground">{v.motivo}</p>}
          </div>
        )}

        {v.status === 'EM_ANDAMENTO' && (
          <>
            <TransmissaoPainel viagem={v} />
            <Button size="lg" variant="outline" className="w-full text-destructive sm:w-auto sm:justify-self-start" disabled={transicao.isPending} onClick={() => setConfirmandoEncerrar(true)}>
              Encerrar
            </Button>
            <ConfirmDialog
              open={confirmandoEncerrar}
              onOpenChange={setConfirmandoEncerrar}
              title="Encerrar viagem?"
              description="A localização deixa de ser transmitida. Não é possível desfazer."
              confirmLabel="Encerrar"
              destructive
              onConfirm={() => executar('encerrar', 'Viagem encerrada.')}
            />
          </>
        )}
      </article>
    </li>
  )
}
