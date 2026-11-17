import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import type { PontoParada } from '@/features/rotas/types'

type PercursoProps = {
  pontos: PontoParada[]
  /** Ordem dos pontos escolhidos na busca (ausente: sem destaque). */
  embarque?: number
  desembarque?: number
}

/** Pontos de parada na ordem do percurso, com o trecho do passageiro destacado. */
export function Percurso({ pontos, embarque, desembarque }: PercursoProps) {
  const noTrecho = (ordem: number) => embarque !== undefined && desembarque !== undefined && ordem >= embarque && ordem <= desembarque

  return (
    <ol aria-label="Pontos de parada" className="grid">
      {pontos.map((p, i) => {
        const marca = p.ordem === embarque ? 'Embarque' : p.ordem === desembarque ? 'Desembarque' : null
        return (
          <li key={p.id} className="relative grid grid-cols-[1.5rem_1fr] gap-3 pb-4 last:pb-0">
            {i < pontos.length - 1 && (
              <span aria-hidden className={cn('absolute top-5 bottom-0 left-[0.6875rem] w-0.5 bg-border', noTrecho(p.ordem) && p.ordem !== desembarque && 'bg-primary')} />
            )}
            <span
              aria-hidden
              className={cn('relative mt-1 size-3.5 justify-self-center rounded-full border-2 border-border bg-background', noTrecho(p.ordem) && 'border-primary', marca && 'bg-primary')}
            />
            <div className="grid gap-0.5">
              <p className={cn('text-sm', noTrecho(p.ordem) ? 'font-medium' : 'text-muted-foreground')}>
                {p.nome}
                {marca && (
                  <Badge variant="outline" className="ml-2 border-primary/30 bg-primary/10 text-primary">
                    {marca}
                  </Badge>
                )}
              </p>
              <p className="text-xs text-muted-foreground">{p.referencia ?? <span className="italic">Referência não informada</span>}</p>
            </div>
          </li>
        )
      })}
    </ol>
  )
}
