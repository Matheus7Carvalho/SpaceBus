import { useEffect, useMemo, useState } from 'react'
import { useSessao } from '@/features/auth/hooks'
import { noFuso } from './datas'

/**
 * "Agora" estável durante a renderização, atualizado a cada minuto (atrasada, futuras, hoje), no fuso da operação
 * informado pela sessão, e não no do navegador.
 */
export function useAgora(intervaloMs = 60_000) {
  const fuso = useSessao().data?.fusoHorario
  const [agora, setAgora] = useState(() => new Date())
  useEffect(() => {
    const timer = setInterval(() => setAgora(new Date()), intervaloMs)
    return () => clearInterval(timer)
  }, [intervaloMs])
  return useMemo(() => noFuso(agora, fuso), [agora, fuso])
}
