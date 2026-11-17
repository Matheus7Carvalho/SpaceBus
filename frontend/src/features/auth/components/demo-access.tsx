import { Button } from '@/components/ui/button'
import { FieldSeparator } from '@/components/ui/field'
import { perfilLabel, type Perfil } from '../types'

/** Contas da seed de desenvolvimento (backend/src/main/resources/db/changes/usuario). */
const contas: { perfil: Perfil; email: string }[] = [
  { perfil: 'ADMINISTRADOR', email: 'admin@spacebus.local' },
  { perfil: 'OPERADOR', email: 'operador@spacebus.local' },
  { perfil: 'MOTORISTA', email: 'motorista@spacebus.local' },
  { perfil: 'PASSAGEIRO', email: 'passageiro@spacebus.local' },
]
const SENHA_DEMO = 'demo123'

type DemoAccessProps = {
  disabled?: boolean
  onSelect: (email: string, senha: string) => void
}

export function DemoAccess({ disabled, onSelect }: DemoAccessProps) {
  return (
    <section aria-labelledby="demo-titulo" className="grid gap-4">
      <FieldSeparator>
        <span id="demo-titulo">Acesso de demonstração</span>
      </FieldSeparator>
      <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        {contas.map((conta) => (
          <Button
            key={conta.perfil}
            type="button"
            variant="outline"
            size="sm"
            disabled={disabled}
            onClick={() => onSelect(conta.email, SENHA_DEMO)}
          >
            {perfilLabel[conta.perfil]}
          </Button>
        ))}
      </div>
    </section>
  )
}
