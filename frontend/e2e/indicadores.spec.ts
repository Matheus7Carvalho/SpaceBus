import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { contextoDe, prepararViagem, reservaConfirmada, type Cenario } from './support/localizacao'

/**
 * Módulo 6 — indicadores. Os cartões somam tudo o que há no período (inclusive dados de testes paralelos); por isso
 * os números exatos são conferidos na linha da viagem criada por este teste, localizada pelo id.
 */

let cenario: Cenario | null = null

test.afterEach(async () => {
  await cenario?.encerrar()
  cenario = null
})

const base = (page: Page) => new URL(page.url()).origin
const linhaDa = (page: Page, viagemId: string) => page.getByRole('row').filter({ has: page.locator(`a[href="/operacao/viagens/${viagemId}"]`) })

test.describe('Indicadores', () => {
  test('a viagem de teste aparece com vendidos, disponíveis e ocupação; filtros sobrevivem ao recarregar e à volta do cadastro', async ({ page }) => {
    await entrar(page, contas.operador)
    cenario = await prepararViagem(base(page), { comMotorista: false, partidaEmMin: 60 * 24 * 4 })
    const passageiro = await contextoDe(base(page), contas.passageiro.email)
    await reservaConfirmada(passageiro, cenario.viagemId)
    await reservaConfirmada(passageiro, cenario.viagemId)
    await passageiro.dispose()
    const viagem = (await (await cenario.operador.get(`/api/viagem/${cenario.viagemId}`)).json()) as { partida: string }
    const dia = viagem.partida.slice(0, 10)

    await page.goto(`/operacao/indicadores?de=${dia}&ate=${dia}`)
    const linha = linhaDa(page, cenario.viagemId)
    await expect(linha).toContainText('Publicada')
    await expect(linha).toContainText('50%') // 2 vendidos de 4 lugares
    await expect(linha.locator('td').nth(5)).toHaveText('2')
    await expect(page.getByRole('region', { name: 'Ocupação por horário' })).toBeVisible()
    // O total da tabela bate com o cartão Viagens.
    const total = (await page.getByRole('region', { name: 'Viagens', exact: true }).locator('[data-valor]').textContent())!.trim()
    await expect(page.getByRole('region', { name: 'Por viagem' })).toContainText(`${total} ${total === '1' ? 'viagem' : 'viagens'}`)

    await page.reload()
    await expect(page.getByLabel('De', { exact: true })).toHaveValue(dia)
    await expect(page.getByLabel('Até', { exact: true })).toHaveValue(dia)
    await expect(linhaDa(page, cenario.viagemId)).toContainText('50%')

    await linhaDa(page, cenario.viagemId).getByRole('link').click()
    await expect(page).toHaveURL(new RegExp(`/operacao/viagens/${cenario.viagemId}$`))
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await expect(page).toHaveURL(new RegExp(`/operacao/indicadores\\?de=${dia}&ate=${dia}$`))
  })

  test('período sem viagens: cartões zerados, ocupação "—" e Limpar filtros', async ({ page }) => {
    await entrar(page, contas.operador)
    await page.goto('/operacao/indicadores?de=2020-01-01&ate=2020-01-02')

    await expect(page.getByText('Nenhuma viagem no período.')).toBeVisible()
    await expect(page.getByRole('region', { name: 'Ocupação', exact: true })).toContainText('—')
    await expect(page.getByRole('region', { name: 'Lugares vendidos' })).toContainText('0')
    await page.getByRole('search').getByRole('button', { name: 'Limpar filtros' }).click()
    await expect(page).toHaveURL(/\/operacao\/indicadores$/)
  })

  test('só a data inicial: o fim fecha a janela padrão e os cartões continuam na tela', async ({ page }) => {
    await entrar(page, contas.operador)
    await page.goto('/operacao/indicadores?de=2030-01-01')

    await expect(page.getByText('Partidas de 01/01/2030 a 07/02/2030')).toBeVisible()
    await expect(page.getByRole('region', { name: 'Viagens', exact: true })).toBeVisible()
  })

  test('período inválido mostra a mensagem do servidor', async ({ page }) => {
    await entrar(page, contas.operador)
    await page.goto('/operacao/indicadores?de=2026-11-21&ate=2026-11-20')

    await expect(page.getByRole('alert')).toContainText('A data inicial deve ser anterior ou igual à final.')
  })

  test('erro 500 mostra Tentar novamente; estrutura HTML5', async ({ page }) => {
    await entrar(page, contas.operador)
    await page.route((url) => url.pathname === '/api/indicador', (rota) => rota.fulfill({ status: 500, json: { status: 500, message: 'Erro' } }))
    await page.goto('/operacao/indicadores')

    await expect(page.getByRole('button', { name: 'Tentar novamente' })).toBeVisible()
    await expect(page.getByRole('main')).toHaveCount(1)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Indicadores')
    await expect(page.getByRole('search')).toBeVisible()
    await expect(page.getByLabel('De', { exact: true })).toBeVisible()
    await expect(page.getByLabel('Até', { exact: true })).toBeVisible()
  })

  for (const conta of [contas.motorista, contas.passageiro]) {
    test(`${conta.perfil} não acessa os indicadores`, async ({ page }) => {
      await entrar(page, conta)
      await page.goto('/operacao/indicadores')
      await expect(page.getByText('Acesso não permitido para o seu perfil.')).toBeVisible()
      expect((await page.request.get('/api/indicador')).status()).toBe(403)
      expect((await page.request.get('/api/indicador/viagem')).status()).toBe(403)
    })
  }
})
