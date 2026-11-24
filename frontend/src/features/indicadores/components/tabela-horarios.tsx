import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import type { ResumoIndicadores } from '../types'
import { BarraOcupacao } from './barra-ocupacao'

/** Comparação entre os horários de partida (canceladas fora). */
export function TabelaHorarios({ horarios }: { horarios: ResumoIndicadores['horarios'] }) {
  return (
    <section aria-labelledby="horarios-titulo" className="grid min-w-0 gap-2">
      <h2 id="horarios-titulo" className="text-base font-semibold">
        Ocupação por horário
      </h2>
      <div className="rounded-lg border">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Horário</TableHead>
              <TableHead className="text-right">Viagens</TableHead>
              <TableHead className="text-right">Oferecidos</TableHead>
              <TableHead className="text-right">Vendidos</TableHead>
              <TableHead className="text-right">Ocupação</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {horarios.map((h) => (
              <TableRow key={h.horario}>
                <TableCell className="font-medium tabular-nums">{h.horario}</TableCell>
                <TableCell className="text-right tabular-nums">{h.viagens}</TableCell>
                <TableCell className="text-right tabular-nums">{h.oferecidos}</TableCell>
                <TableCell className="text-right tabular-nums">{h.vendidos}</TableCell>
                <TableCell>
                  <BarraOcupacao valor={h.ocupacao} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </section>
  )
}
