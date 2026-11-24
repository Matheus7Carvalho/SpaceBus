import { PencilIcon, Trash2Icon } from 'lucide-react'
import { Link } from 'react-router'
import { LinhaClicavel } from '@/components/shared/linha-clicavel'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useOrigemAtual } from '@/lib/navegacao'
import type { Rota } from '../types'

type RotaTableProps = {
  rotas: Rota[]
  onExcluir: (rota: Rota) => void
}

export function RotaTable({ rotas, onExcluir }: RotaTableProps) {
  const origem = useOrigemAtual()
  return (
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Nome</TableHead>
            <TableHead className="hidden md:table-cell">Origem → destino</TableHead>
            <TableHead className="w-28">Tipo</TableHead>
            <TableHead className="hidden w-24 text-right sm:table-cell">Pontos</TableHead>
            <TableHead className="w-24">
              <span className="sr-only">Ações</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {rotas.map((r) => (
            <LinhaClicavel key={r.id} para={r.id}>
              <TableCell className="font-medium whitespace-normal">{r.nome}</TableCell>
              <TableCell className="hidden whitespace-normal text-muted-foreground md:table-cell">
                {r.origem} → {r.destino}
              </TableCell>
              <TableCell>
                <StatusBadge tom={r.tipo === 'FIXA' ? 'info' : 'neutro'}>{r.tipo === 'FIXA' ? 'Fixa' : 'Única'}</StatusBadge>
              </TableCell>
              <TableCell className="hidden text-right tabular-nums sm:table-cell">{r.pontos.length}</TableCell>
              <TableCell>
                <div className="flex justify-end gap-1">
                  <Button asChild variant="ghost" size="icon-sm">
                    <Link to={r.id} state={origem} aria-label={`Editar ${r.nome}`}>
                      <PencilIcon aria-hidden />
                    </Link>
                  </Button>
                  <Button variant="ghost" size="icon-sm" aria-label={`Excluir ${r.nome}`} onClick={() => onExcluir(r)}>
                    <Trash2Icon aria-hidden />
                  </Button>
                </div>
              </TableCell>
            </LinhaClicavel>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
