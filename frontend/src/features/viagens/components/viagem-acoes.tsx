import { useState } from 'react'
import { toast } from 'sonner'
import { ConfirmDialog } from '@/components/shared/confirm-dialog'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { mensagemDeErro } from '@/lib/cadastro'
import { useTransicaoViagem } from '../hooks'
import { situacaoInfo, type AcaoViagem, type Viagem } from '../types'

type Acao = { acao: AcaoViagem; rotulo: string; titulo: string; aviso?: string; sucesso: string; destrutiva?: boolean }

const PUBLICAR: Acao = { acao: 'publicar', rotulo: 'Publicar', titulo: 'Publicar viagem?', aviso: 'A viagem fica disponível para reserva.', sucesso: 'Viagem publicada.' }
const SUSPENDER: Acao = { acao: 'suspender', rotulo: 'Suspender', titulo: 'Suspender viagem?', aviso: 'A viagem deixa de aparecer na busca até ser publicada de novo.', sucesso: 'Viagem suspensa.' }
const CANCELAR: Acao = {
  acao: 'cancelar',
  rotulo: 'Cancelar viagem',
  titulo: 'Cancelar viagem?',
  aviso: 'Não é possível desfazer.',
  sucesso: 'Viagem cancelada.',
  destrutiva: true,
}

const ACOES: Record<Viagem['status'], Acao[]> = {
  RASCUNHO: [PUBLICAR, CANCELAR],
  PUBLICADA: [SUSPENDER, CANCELAR],
  SUSPENSA: [PUBLICAR, CANCELAR],
  CANCELADA: [],
}

type ViagemAcoesProps = {
  viagem: Viagem
  /** Alterações não salvas: a ação valeria para a versão gravada, então fica bloqueada. */
  pendente: boolean
}

/** Situação da viagem e as transições permitidas a partir dela. */
export function ViagemAcoes({ viagem, pendente }: ViagemAcoesProps) {
  const transicao = useTransicaoViagem(viagem.id)
  const [confirmando, setConfirmando] = useState<Acao | null>(null)
  const situacao = situacaoInfo[viagem.status]

  const executar = (acao: Acao) =>
    transicao.mutate(acao.acao, {
      onSuccess: () => toast.success(acao.sucesso),
      onError: (e) => toast.error(mensagemDeErro(e, 'Não foi possível alterar a situação.')),
    })

  return (
    <section aria-label="Situação" className="flex flex-wrap items-center gap-2">
      <span className="text-sm text-muted-foreground">Situação</span>
      <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
      <div className="flex flex-wrap gap-2 sm:ml-2">
        {ACOES[viagem.status].map((a) => (
          <Button
            key={a.acao}
            type="button"
            size="sm"
            variant={a.destrutiva ? 'outline' : 'default'}
            className={a.destrutiva ? 'text-destructive' : undefined}
            disabled={pendente || transicao.isPending}
            title={pendente ? 'Salve as alterações antes.' : undefined}
            onClick={() => setConfirmando(a)}
          >
            {a.rotulo}
          </Button>
        ))}
      </div>
      {pendente && ACOES[viagem.status].length > 0 && <span className="text-xs text-muted-foreground">Salve as alterações antes.</span>}
      <ConfirmDialog
        open={!!confirmando}
        onOpenChange={(open) => !open && setConfirmando(null)}
        title={confirmando?.titulo ?? ''}
        description={confirmando?.aviso}
        confirmLabel={confirmando?.rotulo ?? ''}
        destructive={confirmando?.destrutiva}
        onConfirm={() => confirmando && executar(confirmando)}
      />
    </section>
  )
}
