import { PencilIcon, Trash2Icon } from 'lucide-react'
import { Link } from 'react-router'
import { LinhaClicavel } from '@/components/shared/linha-clicavel'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatarDataHora } from '@/lib/format'
import { useOrigemAtual } from '@/lib/navegacao'
import { formatarPlaca } from '@/features/veiculos/types'
import { formatarPreco, situacaoInfo, type Viagem } from '../types'

type ViagemTableProps = {
  viagens: Viagem[]
  onExcluir: (viagem: Viagem) => void
}

export function ViagemTable({ viagens, onExcluir }: ViagemTableProps) {
  const origem = useOrigemAtual()
  return (
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-36">Partida</TableHead>
            <TableHead>Rota</TableHead>
            <TableHead className="hidden md:table-cell">Veículo</TableHead>
            <TableHead className="hidden text-right sm:table-cell">Preço</TableHead>
            <TableHead className="hidden w-20 text-right sm:table-cell">Lugares</TableHead>
            <TableHead className="w-28">Situação</TableHead>
            <TableHead className="w-24">
              <span className="sr-only">Ações</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {viagens.map((v) => {
            const situacao = situacaoInfo[v.status]
            const descricao = `${v.rotaNome} ${formatarDataHora(v.partida)}`
            return (
              <LinhaClicavel key={v.id} para={v.id}>
                <TableCell className="tabular-nums">{formatarDataHora(v.partida)}</TableCell>
                <TableCell className="whitespace-normal">
                  <span className="font-medium">{v.rotaNome}</span>
                  <span className="block text-xs text-muted-foreground">
                    {v.origem} → {v.destino}
                  </span>
                </TableCell>
                <TableCell className="hidden font-mono text-xs md:table-cell">{formatarPlaca(v.veiculoPlaca)}</TableCell>
                <TableCell className="hidden text-right tabular-nums sm:table-cell">{formatarPreco(v.preco)}</TableCell>
                <TableCell className="hidden text-right tabular-nums sm:table-cell">{v.capacidade}</TableCell>
                <TableCell>
                  <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button asChild variant="ghost" size="icon-sm">
                      <Link to={v.id} state={origem} aria-label={`Editar viagem ${descricao}`}>
                        <PencilIcon aria-hidden />
                      </Link>
                    </Button>
                    {v.status === 'RASCUNHO' && (
                      <Button variant="ghost" size="icon-sm" aria-label={`Excluir viagem ${descricao}`} onClick={() => onExcluir(v)}>
                        <Trash2Icon aria-hidden />
                      </Button>
                    )}
                  </div>
                </TableCell>
              </LinhaClicavel>
            )
          })}
        </TableBody>
      </Table>
    </div>
  )
}
