import { PencilIcon, Trash2Icon } from 'lucide-react'
import { Link } from 'react-router'
import { LinhaClicavel } from '@/components/shared/linha-clicavel'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useOrigemAtual } from '@/lib/navegacao'
import { formatarPlaca, tipoLabel, type Veiculo } from '../types'

type VeiculoTableProps = {
  veiculos: Veiculo[]
  onExcluir: (veiculo: Veiculo) => void
}

export function VeiculoTable({ veiculos, onExcluir }: VeiculoTableProps) {
  const origem = useOrigemAtual()
  return (
    <div className="rounded-lg border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-28">Placa</TableHead>
            <TableHead>Descrição</TableHead>
            <TableHead className="hidden sm:table-cell">Tipo</TableHead>
            <TableHead className="w-24 text-right">Lugares</TableHead>
            <TableHead className="w-24">Situação</TableHead>
            <TableHead className="w-24">
              <span className="sr-only">Ações</span>
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {veiculos.map((v) => {
            const placa = formatarPlaca(v.placa)
            return (
              <LinhaClicavel key={v.id} para={v.id}>
                <TableCell className="font-mono text-xs">{placa}</TableCell>
                <TableCell className="font-medium whitespace-normal">{v.descricao}</TableCell>
                <TableCell className="hidden sm:table-cell">{tipoLabel[v.tipo]}</TableCell>
                <TableCell className="text-right tabular-nums">{v.capacidade}</TableCell>
                <TableCell>
                  <StatusBadge tom={v.ativo ? 'sucesso' : 'neutro'}>{v.ativo ? 'Ativo' : 'Inativo'}</StatusBadge>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button asChild variant="ghost" size="icon-sm">
                      <Link to={v.id} state={origem} aria-label={`Editar ${placa}`}>
                        <PencilIcon aria-hidden />
                      </Link>
                    </Button>
                    <Button variant="ghost" size="icon-sm" aria-label={`Excluir ${placa}`} onClick={() => onExcluir(v)}>
                      <Trash2Icon aria-hidden />
                    </Button>
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
