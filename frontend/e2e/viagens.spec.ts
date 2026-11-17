import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { escolher, linhaCom, salvarCadastro, SEED } from './support/cadastros'

const LISTA = '/operacao/viagens'

type Viagem = { id: string; status: string; capacidade: number }

const salvar = (page: Page) => salvarCadastro(page, { rotulo: 'Viagem', recurso: 'viagem', lista: LISTA, mensagem: 'Viagem salva.' })
const situacao = (page: Page) => page.getByRole('region', { name: 'Situação' })

/**
 * Partida única por teste (dia e minuto aleatórios no futuro): evita o conflito de "mesmo veículo,
 * mesmo horário" entre testes paralelos e rodadas anteriores (viagens canceladas não são excluídas).
 */
function partidaUnica() {
  const d = new Date()
  d.setDate(d.getDate() + 30 + Math.floor(Math.random() * 300))
  d.setHours(5 + Math.floor(Math.random() * 17), Math.floor(Math.random() * 60), 0, 0)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** Rascunho criado pela API, para o teste não depender de outro. */
async function criarRascunho(page: Page) {
  const r = await page.request.post('/api/viagem', {
    data: { rotaId: SEED.rotaCentroAeroporto, veiculoId: SEED.vanMaster, motoristaId: null, partida: `${partidaUnica()}:00`, preco: 15, capacidade: 12 },
  })
  expect(r.ok()).toBe(true)
  return (await r.json()) as Viagem
}

async function executar(page: Page, botao: string, confirmar = botao) {
  await situacao(page).getByRole('button', { name: botao }).click()
  const resposta = page.waitForResponse((r) => /\/api\/viagem\/[^/]+\/(publicar|suspender|cancelar)$/.test(r.url()))
  await page.getByRole('alertdialog').getByRole('button', { name: confirmar }).click()
  expect((await resposta).ok()).toBe(true)
}

test.beforeEach(async ({ page }) => entrar(page, contas.operador))

test.describe('Cadastro de viagem', () => {
  test('cria rascunho com a capacidade do veículo e o segundo Salvar não duplica', async ({ page }) => {
    await page.goto(LISTA)
    await page.getByRole('link', { name: 'Nova viagem' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Nova viagem' })).toBeVisible()

    await escolher(page, 'Rota', /Centro – Aeroporto/)
    await escolher(page, 'Veículo', /SBV-2B34/)
    await expect(page.getByLabel('Capacidade (lugares)')).toHaveValue('12')
    await page.getByLabel('Partida').fill(partidaUnica())
    await page.getByLabel('Preço (R$)').fill('19.90')
    await escolher(page, 'Motorista', 'João Costa')

    const id = await salvar(page)
    await expect(situacao(page)).toContainText('Rascunho')

    await page.getByLabel('Preço (R$)').fill('21.50')
    expect(await salvar(page)).toBe(id)
    const salva = (await (await page.request.get(`/api/viagem/${id}`)).json()) as Viagem & { preco: number }
    expect(salva.preco).toBe(21.5)

    // Limpeza: rascunho pode ser excluído.
    expect((await page.request.delete(`/api/viagem/${id}`)).status()).toBe(204)
  })

  test('capacidade acima do veículo é barrada na tela e no servidor', async ({ page }) => {
    await page.goto(`${LISTA}/nova`)

    await escolher(page, 'Rota', /Centro – Aeroporto/)
    await escolher(page, 'Veículo', /SBV-2B34/)
    const capacidade = page.getByLabel('Capacidade (lugares)')
    // O máximo do campo acompanha o veículo escolhido.
    await expect(capacidade).toHaveAttribute('max', '12')
    await page.getByLabel('Partida').fill(partidaUnica())
    await page.getByLabel('Preço (R$)').fill('19.90')
    await capacidade.fill('13')
    await page.getByRole('button', { name: 'Salvar' }).click()

    expect(await capacidade.evaluate((el: HTMLInputElement) => el.validity.rangeOverflow)).toBe(true)
    await expect(page).toHaveURL(new RegExp(`${LISTA}/nova$`))

    const resposta = await page.request.post('/api/viagem', {
      data: { rotaId: SEED.rotaCentroAeroporto, veiculoId: SEED.vanMaster, motoristaId: null, partida: `${partidaUnica()}:00`, preco: 15, capacidade: 13 },
    })
    expect(resposta.status()).toBe(400)
    expect((await resposta.json()).message).toBe('A capacidade não pode passar dos 12 lugares do veículo SBV2B34.')
  })

  test('partida no passado é recusada', async ({ page }) => {
    await page.goto(`${LISTA}/nova`)

    await escolher(page, 'Rota', /Centro – Aeroporto/)
    await escolher(page, 'Veículo', /SBV-2B34/)
    await page.getByLabel('Partida').fill('2020-01-01T08:00')
    await page.getByLabel('Preço (R$)').fill('19.90')
    await page.getByRole('button', { name: 'Salvar' }).click()

    await expect(page.getByRole('alert')).toContainText('A partida deve ser depois do horário atual.')
  })
})

test.describe('Situação da viagem', () => {
  test('publica, suspende, publica de novo e cancela; cancelada fica só leitura', async ({ page }) => {
    const viagem = await criarRascunho(page)
    await page.goto(`${LISTA}/${viagem.id}`)

    await executar(page, 'Publicar')
    await expect(situacao(page)).toContainText('Publicada')
    await executar(page, 'Suspender')
    await expect(situacao(page)).toContainText('Suspensa')
    await executar(page, 'Publicar')
    await expect(situacao(page)).toContainText('Publicada')
    await executar(page, 'Cancelar viagem')

    await expect(situacao(page)).toContainText('Cancelada')
    await expect(situacao(page).getByRole('button')).toHaveCount(0)
    await expect(page.getByLabel('Preço (R$)')).toBeDisabled()
    await expect(page.getByRole('button', { name: 'Salvar' })).toHaveCount(0)
    await expect(page.getByRole('button', { name: 'Voltar' })).toBeVisible()
  })

  test('ações ficam bloqueadas enquanto há alterações não salvas', async ({ page }) => {
    const viagem = await criarRascunho(page)
    await page.goto(`${LISTA}/${viagem.id}`)

    await page.getByLabel('Preço (R$)').fill('17')
    await expect(situacao(page).getByRole('button', { name: 'Publicar' })).toBeDisabled()
    await expect(situacao(page)).toContainText('Salve as alterações antes.')

    await page.getByLabel('Preço (R$)').fill('15')
    await expect(situacao(page).getByRole('button', { name: 'Publicar' })).toBeEnabled()
    expect((await page.request.delete(`/api/viagem/${viagem.id}`)).status()).toBe(204)
  })
})

test.describe('Listagem de viagens', () => {
  test('filtra por situação e exclui só rascunho', async ({ page }) => {
    const viagem = await criarRascunho(page)
    await page.goto(`${LISTA}?situacao=CANCELADA`)

    await expect(page.locator(`tbody a[href$="${SEED.viagemCancelada}"]`)).toHaveCount(1)
    await expect(page.locator('tbody').getByRole('button', { name: /^Excluir/ })).toHaveCount(0)

    await escolher(page, 'Situação', 'Rascunho')
    await expect(page).toHaveURL(/situacao=RASCUNHO/)
    const link = page.locator(`tbody a[href$="${viagem.id}"]`)
    await expect(link).toHaveCount(1)
    const linha = page.getByRole('row').filter({ has: page.locator(`a[href$="${viagem.id}"]`) })
    await linha.getByRole('button', { name: /^Excluir/ }).click()
    const resposta = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().endsWith(`/api/viagem/${viagem.id}`))
    await page.getByRole('alertdialog', { name: 'Excluir viagem?' }).getByRole('button', { name: 'Excluir' }).click()

    expect((await resposta).status()).toBe(204)
    await expect(link).toHaveCount(0)
  })

  test('linha abre a viagem e Cancelar volta com os filtros', async ({ page }) => {
    await page.goto(`${LISTA}?situacao=PUBLICADA`)

    await linhaCom(page, 'SBV-1A23').filter({ has: page.locator(`a[href$="${SEED.viagemPublicada}"]`) }).getByRole('cell').first().click()

    await expect(page.getByRole('heading', { level: 1, name: 'Editar viagem' })).toBeVisible()
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await expect(page).toHaveURL(/\/operacao\/viagens\?situacao=PUBLICADA$/)
  })
})
