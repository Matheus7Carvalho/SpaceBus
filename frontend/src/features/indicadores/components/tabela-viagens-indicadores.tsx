import { Link } from 'react-router'
import { LinhaClicavel } from '@/components/shared/linha-clicavel'
import { StatusBadge } from '@/components/shared/status-badge'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatarPlaca } from '@/features/veiculos/types'
import { situacaoInfo } from '@/features/viagens/types'
import { formatarDataHora } from '@/lib/format'
import { useOrigemAtual } from '@/lib/navegacao'
import type { IndicadorViagem } from '../types'
import { BarraOcupacao } from './barra-ocupacao'

type Props = { viagens: IndicadorViagem[]; total: number; pagina: number; paginas: number; onPagina: (pagina: number) => void }

/** Ocupação por viagem; a linha abre o cadastro da viagem e o Voltar retorna com os filtros. */
export function TabelaViagensIndicadores({ viagens, total, pagina, paginas, onPagina }: Props) {
  const origem = useOrigemAtual()
  return (
    <section aria-labelledby="por-viagem-titulo" className="grid min-w-0 gap-2">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 id="por-viagem-titulo" className="text-base font-semibold">
          Por viagem
        </h2>
        <p className="text-sm text-muted-foreground">
          {total} {total === 1 ? 'viagem' : 'viagens'}
        </p>
      </div>
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-36">Partida</TableHead>
              <TableHead className="hidden sm:table-cell">Rota</TableHead>
              <TableHead className="hidden md:table-cell">Veículo</TableHead>
              <TableHead className="w-28">Situação</TableHead>
              <TableHead className="hidden text-right sm:table-cell">Capacidade</TableHead>
              <TableHead className="text-right">Vendidos</TableHead>
              <TableHead className="hidden text-right sm:table-cell">Disponíveis</TableHead>
              <TableHead className="text-right">Ocupação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {viagens.map((v) => {
              const situacao = situacaoInfo[v.status]
              return (
                <LinhaClicavel key={v.id} para={`/operacao/viagens/${v.id}`}>
                  <TableCell className="tabular-nums">
                    <Link to={`/operacao/viagens/${v.id}`} state={origem} className="hover:underline" aria-label={`Abrir viagem ${v.rotaNome} ${formatarDataHora(v.partida)}`}>
                      {formatarDataHora(v.partida)}
                    </Link>
                    <span className="block text-xs text-muted-foreground sm:hidden">{v.rotaNome}</span>
                  </TableCell>
                  <TableCell className="hidden whitespace-normal sm:table-cell">{v.rotaNome}</TableCell>
                  <TableCell className="hidden font-mono text-xs md:table-cell">{formatarPlaca(v.veiculoPlaca)}</TableCell>
                  <TableCell>
                    <StatusBadge tom={situacao.tom}>{situacao.rotulo}</StatusBadge>
                  </TableCell>
                  <TableCell className="hidden text-right tabular-nums sm:table-cell">{v.capacidade}</TableCell>
                  <TableCell className="text-right tabular-nums">{v.vendidos}</TableCell>
                  <TableCell className="hidden text-right tabular-nums sm:table-cell">{v.disponiveis ?? '—'}</TableCell>
                  <TableCell>
                    <BarraOcupacao valor={v.ocupacao} />
                  </TableCell>
                </LinhaClicavel>
              )
            })}
          </TableBody>
        </Table>
      </div>
      {paginas > 1 && (
        <nav aria-label="Páginas" className="flex items-center justify-center gap-2 text-sm">
          <Button variant="outline" size="sm" disabled={pagina === 0} onClick={() => onPagina(pagina - 1)}>
            Anterior
          </Button>
          <span className="tabular-nums">
            {pagina + 1} de {paginas}
          </span>
          <Button variant="outline" size="sm" disabled={pagina + 1 >= paginas} onClick={() => onPagina(pagina + 1)}>
            Próxima
          </Button>
        </nav>
      )}
    </section>
  )
}
