import type { FullConfig } from '@playwright/test'
import { contas, SENHA_DEMO } from './support/auth'
import { PREFIXO_E2E } from './support/cadastros'

/**
 * Depois da suíte, apaga pela API os dados que os testes não conseguem excluir sozinhos (rotas E2E com viagens,
 * viagens canceladas das reservas). O endpoint só existe no perfil dev; em outro ambiente a limpeza é pulada.
 */
export default async function globalTeardown(config: FullConfig) {
  const base = config.projects[0]?.use.baseURL ?? 'http://localhost:5173'
  try {
    const login = await fetch(`${base}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: contas.administrador.email, senha: SENHA_DEMO }),
    })
    const sessao = login.headers.getSetCookie().map((c) => c.split(';')[0]).join('; ')
    const r = await fetch(`${base}/api/dev/dados-teste?marca=${PREFIXO_E2E}`, { method: 'DELETE', headers: { Cookie: sessao } })
    if (r.ok) console.log(`Limpeza E2E: ${JSON.stringify(await r.json())}`)
    else console.warn(`Limpeza E2E pulada: DELETE /api/dev/dados-teste respondeu ${r.status}.`)
  } catch (e) {
    console.warn(`Limpeza E2E pulada: ${(e as Error).message}`)
  }
}
