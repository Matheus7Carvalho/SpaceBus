import { SimulatedBadge } from '@/components/shared/simulated-badge'
import { StatusBadge } from '@/components/shared/status-badge'
import { formatar } from '@/lib/datas'
import type { Localizacao } from '../types'

type SituacaoLocalizacaoProps = {
  localizacao: Localizacao
  /** A última atualização falhou: a leitura anterior continua na tela. */
  falhaAoAtualizar: boolean
}

/** Situação da posição fora do mapa: continua visível mesmo sem os tiles. Nunca esconde dado desatualizado. */
export function SituacaoLocalizacao({ localizacao: l, falhaAoAtualizar }: SituacaoLocalizacaoProps) {
  return (
    <div aria-live="polite" className="grid gap-1 text-sm">
      <Linha localizacao={l} />
      {falhaAoAtualizar && <p className="text-destructive">Não foi possível atualizar. Tentando de novo.</p>}
    </div>
  )
}

function Linha({ localizacao: l }: { localizacao: Localizacao }) {
  if (l.situacaoViagem === 'ENCERRADA') return <p className="font-medium">Viagem encerrada</p>
  if (l.situacaoViagem !== 'EM_ANDAMENTO') return <p className="text-muted-foreground">A localização aparece quando a viagem começar.</p>
  if (!l.disponivel || !l.registradaEm) return <p className="font-medium">Localização indisponível</p>

  const hora = formatar.hora(l.registradaEm)
  return (
    <p className="flex flex-wrap items-center gap-2">
      {l.desatualizada && <StatusBadge tom="atencao">Desatualizada</StatusBadge>}
      <span className="font-medium">{l.desatualizada ? `Última posição às ${hora}` : `Atualizada às ${hora}`}</span>
      {l.precisaoM != null && <span className="text-muted-foreground">± {Math.round(l.precisaoM)} m</span>}
      {l.simulada && <SimulatedBadge />}
    </p>
  )
}
