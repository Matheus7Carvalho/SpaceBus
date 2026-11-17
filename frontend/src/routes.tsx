import type { ReactNode } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { AppShell } from '@/components/layout/app-shell'
import { RequireAuth } from '@/features/auth/components/require-auth'
import { RequirePerfil } from '@/features/auth/components/require-perfil'
import { HomeRedirect } from '@/pages/home-redirect'
import { LoginPage } from '@/pages/login-page'
import { NotFoundPage } from '@/pages/not-found-page'
import { RotaFormPage } from '@/pages/rota-form-page'
import { RotasPage } from '@/pages/rotas-page'
import { VeiculoFormPage } from '@/pages/veiculo-form-page'
import { VeiculosPage } from '@/pages/veiculos-page'
import { ViagemFormPage } from '@/pages/viagem-form-page'
import { ViagensPage } from '@/pages/viagens-page'

/** Aplica os perfis do item de menu correspondente à rota. */
function protegida(rota: string, pagina: ReactNode) {
  return <RequirePerfil rota={rota}>{pagina}</RequirePerfil>
}

/** Listagem + cadastro novo + edição, todos com os perfis do item de menu. */
function cadastro(rota: string, lista: ReactNode, formulario: ReactNode, novo = 'novo'): RouteObject {
  return {
    path: rota.slice(1),
    children: [
      { index: true, element: protegida(rota, lista) },
      { path: novo, element: protegida(rota, formulario) },
      { path: ':id', element: protegida(rota, formulario) },
    ],
  }
}

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <HomeRedirect /> },
      cadastro('/operacao/viagens', <ViagensPage />, <ViagemFormPage />, 'nova'),
      cadastro('/cadastros/rotas', <RotasPage />, <RotaFormPage />, 'nova'),
      cadastro('/cadastros/veiculos', <VeiculosPage />, <VeiculoFormPage />),
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
