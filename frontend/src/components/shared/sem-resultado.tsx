import { SearchXIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { EmptyState } from './empty-state'

/** Estado vazio de uma listagem filtrada (diferente de "nenhum cadastro"). */
export function SemResultado({ onLimpar, titulo = 'Nenhum resultado para os filtros.' }: { onLimpar: () => void; titulo?: string }) {
  return (
    <EmptyState
      title={titulo}
      icon={SearchXIcon}
      action={
        <Button variant="outline" size="sm" onClick={onLimpar}>
          Limpar filtros
        </Button>
      }
    />
  )
}
