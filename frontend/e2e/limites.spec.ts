import { expect, test } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { SEED } from './support/cadastros'
import { camposSemLimite } from './support/limites'

/**
 * Guarda: nenhum campo de formulário sem limite de caracteres (ou min/max, para números).
 * Ao criar uma tela com formulário, inclua-a aqui.
 */
test.describe('Todo campo tem limite', () => {
  test('login', async ({ page }) => {
    await page.goto('/login')

    expect(await camposSemLimite(page)).toEqual([])
  })

  for (const tela of [
    '/cadastros/veiculos/novo',
    `/cadastros/veiculos/${SEED.vanSprinter}`,
    '/cadastros/rotas/nova',
    `/cadastros/rotas/${SEED.rotaCentroAeroporto}`,
    '/operacao/viagens/nova',
    `/operacao/viagens/${SEED.viagemPublicada}`,
    // Listagens: campos de filtro também têm limite.
    '/cadastros/veiculos',
    '/cadastros/rotas',
    '/operacao/viagens',
    '/buscar',
  ]) {
    test(`tela ${tela}`, async ({ page }) => {
      await entrar(page, contas.operador)
      await page.goto(tela)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
      await page.waitForLoadState('networkidle')

      expect(await camposSemLimite(page)).toEqual([])
    })
  }
})
