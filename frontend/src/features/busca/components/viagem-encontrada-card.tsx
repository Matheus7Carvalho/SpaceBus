import { ArrowDownIcon, UsersIcon } from 'lucide-react'
import { Link } from 'react-router'
import { formatar } from '@/lib/datas'
import { useOrigemAtual } from '@/lib/navegacao'
import { tipoLabel } from '@/features/veiculos/types'
import { formatarPreco } from '@/features/viagens/types'
import type { ViagemEncontrada } from '../types'

type Props = { viagem: ViagemEncontrada }

/**
 * Um resultado da busca. O card inteiro abre o detalhe (link principal com after:inset-0);
 * o trecho escolhido vai na URL para o detalhe destacar embarque e desembarque.
 */
export function ViagemEncontradaCard({ viagem }: Props) {
  const origem = useOrigemAtual()
  const { embarque, desembarque } = viagem
  const hora = formatar.hora(viagem.partida)
  const destinoLink = `/buscar/${viagem.id}?embarque=${embarque.ordem}&desembarque=${desembarque.ordem}`

  return (
    <li className="relative grid gap-3 rounded-lg border p-4 transition-colors hover:bg-muted/40 sm:grid-cols-[5rem_1fr_auto] sm:items-center">
      <p className="text-2xl font-semibold tabular-nums">{hora}</p>
      <div className="grid gap-1 text-sm">
        <h3 className="font-medium">
          {/* O nome acessível começa pelo texto visível (WCAG 2.5.3) e completa o trecho e o horário. */}
          <Link to={destinoLink} state={origem} className="after:absolute after:inset-0 focus-visible:outline-none">
            {embarque.nome}
            <span className="sr-only">
              {' '}
              até {desembarque.nome}, às {hora}: ver detalhes
            </span>
          </Link>
        </h3>
        {embarque.referencia && <p className="text-xs text-muted-foreground">{embarque.referencia}</p>}
        <ArrowDownIcon aria-hidden className="size-3.5 text-muted-foreground" />
        <p className="font-medium">{desembarque.nome}</p>
        <p className="text-xs text-muted-foreground">
          {viagem.transportadora} · {tipoLabel[viagem.tipoVeiculo]} · {viagem.rotaNome}
        </p>
      </div>
      <div className="flex items-center justify-between gap-4 sm:flex-col sm:items-end">
        <p className="text-lg font-semibold tabular-nums">{formatarPreco(viagem.preco)}</p>
        <p className="flex items-center gap-1 text-xs text-muted-foreground">
          <UsersIcon aria-hidden className="size-3.5" />
          {viagem.lugaresDisponiveis} {viagem.lugaresDisponiveis === 1 ? 'lugar' : 'lugares'}
        </p>
      </div>
    </li>
  )
}
