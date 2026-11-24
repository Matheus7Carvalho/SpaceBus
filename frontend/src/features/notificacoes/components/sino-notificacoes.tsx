import { BellIcon } from 'lucide-react'
import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { useSessao } from '@/features/auth/hooks'
import { useNaoLidas } from '../hooks'

/** Sino do cabeçalho, só para o Passageiro, com a contagem de não lidas (oculta quando zero). */
export function SinoNotificacoes() {
  const passageiro = useSessao().data?.perfil === 'PASSAGEIRO'
  const naoLidas = useNaoLidas(passageiro)
  if (!passageiro) return null

  const quantidade = naoLidas.data?.quantidade ?? 0
  const rotulo = quantidade === 0 ? 'Notificações' : `Notificações, ${quantidade} ${quantidade === 1 ? 'não lida' : 'não lidas'}`
  return (
    <Button asChild variant="ghost" size="icon" className="relative">
      <Link to="/notificacoes" aria-label={rotulo}>
        <BellIcon aria-hidden />
        {quantidade > 0 && (
          <span aria-hidden className="absolute -top-0.5 -right-0.5 min-w-4 rounded-full bg-destructive px-1 text-center text-[10px] leading-4 font-semibold text-white">
            {quantidade > 9 ? '9+' : quantidade}
          </span>
        )}
      </Link>
    </Button>
  )
}
