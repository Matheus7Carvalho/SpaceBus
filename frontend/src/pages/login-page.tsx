import { Navigate, useLocation, useNavigate } from 'react-router'
import { LoginForm } from '@/features/auth/components/login-form'
import { podeAcessar } from '@/components/layout/navigation'
import { useSessao } from '@/features/auth/hooks'
import type { Perfil } from '@/features/auth/types'

type LocationState = { from?: string } | null

export function LoginPage() {
  const { data: usuario } = useSessao()
  const navigate = useNavigate()
  const destino = (useLocation().state as LocationState)?.from ?? '/'

  // Volta à rota de origem só se o perfil puder acessá-la.
  const destinoPara = (perfil: Perfil) => (podeAcessar(perfil, destino) ? destino : '/')

  if (usuario) return <Navigate to={destinoPara(usuario.perfil)} replace />

  return (
    <div className="grid min-h-svh lg:grid-cols-2">
      <aside className="hidden flex-col justify-between relative isolate overflow-hidden bg-sidebar p-10 xl:p-16 text-sidebar-foreground lg:flex">
        <Marca />
        <img src="/favicon.svg" alt="" className="my-10 size-48 self-center -rotate-6 xl:size-64" />
        <p className="max-w-md text-balance text-3xl font-semibold leading-tight tracking-tight xl:text-5xl">Encontre, acompanhe e reserve sua viagem de van.</p>
      </aside>
      <main className="flex items-center justify-center p-6">
        <div className="grid w-full max-w-sm gap-6">
          <div className="lg:hidden">
            <Marca />
          </div>
          <h1 id="login-titulo" className="text-3xl font-semibold tracking-tight">
            Entrar
          </h1>
          <LoginForm onSuccess={(logado) => navigate(destinoPara(logado.perfil), { replace: true })} />
        </div>
      </main>
    </div>
  )
}

function Marca() {
  return (
    <div className="flex items-center gap-3 text-xl font-bold tracking-tight">
      <img src="/favicon.svg" alt="" className="size-8" />
      SpaceBus
    </div>
  )
}
