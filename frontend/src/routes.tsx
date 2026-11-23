import type { ReactNode } from 'react'
import { createBrowserRouter, type RouteObject } from 'react-router'
import { AppShell } from '@/components/layout/app-shell'
import { RequireAuth } from '@/features/auth/components/require-auth'
import { RequirePerfil } from '@/features/auth/components/require-perfil'
import { BuscaDetalhePage } from '@/pages/busca-detalhe-page'
import { BuscaPage } from '@/pages/busca-page'
import { HomeRedirect } from '@/pages/home-redirect'
import { LoginPage } from '@/pages/login-page'
import { NotFoundPage } from '@/pages/not-found-page'
import { PagamentoSimuladoPage } from '@/pages/pagamento-simulado-page'
import { ReservaPage } from '@/pages/reserva-page'
import { ReservasPage } from '@/pages/reservas-page'
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
      {
        path: 'buscar',
        children: [
          { index: true, element: protegida('/buscar', <BuscaPage />) },
          { path: ':id', element: protegida('/buscar', <BuscaDetalhePage />) },
        ],
      },
      {
        path: 'reservas',
        children: [
          { index: true, element: protegida('/reservas', <ReservasPage />) },
          { path: 'pagamento/:provedorId', element: protegida('/reservas', <PagamentoSimuladoPage />) },
          { path: ':id', element: protegida('/reservas', <ReservaPage />) },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
