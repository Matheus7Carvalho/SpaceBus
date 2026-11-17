import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { dialogoDescarte, escolher, excluirViaApi, linhaCom, PREFIXO_E2E, salvarCadastro, SEED, sufixoUnico, voltarParaLista } from './support/cadastros'

const LISTA = '/cadastros/veiculos'
const ROTULO = 'Veículo'

type Veiculo = { id: string; placa: string; descricao: string }

/** Placa válida e única por teste (padrão Mercosul). */
function placaUnica() {
  const s = sufixoUnico(4).replace(/[^A-Z0-9]/g, 'X')
  const letra = (c: string) => (/[A-Z]/.test(c) ? c : String.fromCharCode(65 + (Number(c) % 26)))
  const digito = (c: string) => (/[0-9]/.test(c) ? c : String(c.charCodeAt(0) % 10))
  return `Q${letra(s[0])}${letra(s[1])}${digito(s[2])}${s[3]}${digito(s[0])}${digito(s[1])}`
}

async function preencher(page: Page, placa: string, descricao: string, capacidade = '14') {
  await page.getByLabel('Placa').fill(placa)
  await page.getByLabel('Descrição').fill(descricao)
  await page.getByLabel('Capacidade (lugares)').fill(capacidade)
}

const salvar = (page: Page) => salvarCadastro(page, { rotulo: ROTULO, recurso: 'veiculo', lista: LISTA, mensagem: 'Veículo salvo.' })

/** Marca única por teste: o afterEach apaga só o que este teste criou (testes paralelos não se atrapalham). */
let marca = ''

test.beforeEach(async ({ page }) => {
  marca = `${PREFIXO_E2E}-${sufixoUnico()}`
  await entrar(page, contas.operador)
})
test.afterEach(async ({ page }) => excluirViaApi<Veiculo>(page, 'veiculo', (v) => v.descricao.startsWith(marca)))

test.describe('Cadastro de veículo', () => {
  test('salvar novo vira edição e o segundo Salvar não duplica', async ({ page }) => {
    const descricao = `${marca} Van`
    const placa = placaUnica()
    await page.goto(LISTA)
    await page.getByRole('link', { name: 'Novo veículo' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Novo veículo' })).toBeVisible()

    await preencher(page, `${placa.slice(0, 3)}-${placa.slice(3).toLowerCase()}`, descricao)
    const id = await salvar(page)
    await expect(page.getByRole('heading', { level: 1, name: 'Editar veículo' })).toBeVisible()
    await expect(page.getByLabel('Placa')).toHaveValue(placa)

    await page.getByLabel('Capacidade (lugares)').fill('10')
    expect(await salvar(page)).toBe(id)

    const todos = (await (await page.request.get('/api/veiculo?size=500')).json()).content as Veiculo[]
    expect(todos.filter((v) => v.descricao === descricao)).toHaveLength(1)
  })

  test('placa inválida e duplicada mostram o motivo', async ({ page }) => {
    await page.goto(`${LISTA}/novo`)

    await preencher(page, 'AB12', `${marca} Inválida`)
    await page.getByRole('button', { name: 'Salvar' }).click()
    await expect(page.getByText('Informe uma placa válida (ex.: ABC1D23).')).toBeVisible()

    await page.getByLabel('Placa').fill('SBV-1A23')
    await page.getByRole('button', { name: 'Salvar' }).click()
    await expect(page.getByRole('alert')).toContainText('Já existe um veículo com a placa SBV1A23.')
    await expect(page.locator(`form[aria-label="${ROTULO}"]`)).toHaveAttribute('data-alterado', 'true')
  })

  test('edição com placa de outro veículo mostra o motivo e não altera', async ({ page }) => {
    const descricao = `${marca} Editar`
    const placa = placaUnica()
    const criado = (await (await page.request.post('/api/veiculo', { data: { placa, descricao, tipo: 'VAN', capacidade: 10, ativo: true } })).json()) as Veiculo
    await page.goto(`${LISTA}/${criado.id}`)

    await page.getByLabel('Placa').fill('sbv-1a23')
    await page.getByRole('button', { name: 'Salvar' }).click()

    await expect(page.getByRole('alert')).toContainText('Já existe um veículo com a placa SBV1A23.')
    const atual = (await (await page.request.get(`/api/veiculo/${criado.id}`)).json()) as Veiculo
    expect(atual.placa).toBe(placa)
  })

  test('veículo com viagens futuras não pode ser inativado', async ({ page }) => {
    await page.goto(`${LISTA}/${SEED.vanSprinter}`)

    await page.getByLabel('Ativo').click()
    await page.getByRole('button', { name: 'Salvar' }).click()

    await expect(page.getByRole('alert')).toContainText('Veículo com viagens futuras não pode ser inativado.')
    await expect(page.locator(`form[aria-label="${ROTULO}"]`)).toHaveAttribute('data-alterado', 'true')
  })

  test('Cancelar: direto sem alterações, com confirmação quando há alterações', async ({ page }) => {
    await page.goto(LISTA)
    await linhaCom(page, 'SBV-1A23').getByRole('cell', { name: 'Van Sprinter 416' }).click()
    await expect(page.getByLabel('Placa')).toHaveValue('SBV1A23')
    await voltarParaLista(page, LISTA)

    await page.goto(`${LISTA}/${SEED.vanSprinter}`)
    await page.getByLabel('Descrição').fill('Alterada sem salvar')
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    const dialogo = dialogoDescarte(page)
    await expect(dialogo).toBeVisible()
    await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused()
    await dialogo.getByRole('button', { name: 'Cancelar' }).click()
    await expect(page.getByLabel('Descrição')).toHaveValue('Alterada sem salvar')

    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await dialogoDescarte(page).getByRole('button', { name: 'Descartar' }).click()
    await expect(page).toHaveURL(new RegExp(`${LISTA}$`))
    await expect(linhaCom(page, 'SBV-1A23')).toContainText('Van Sprinter 416')
  })
})

test.describe('Listagem de veículos', () => {
  test('filtra por texto, tipo e situação, com estado na URL', async ({ page }) => {
    await page.goto(LISTA)
    const filtros = page.getByRole('search', { name: 'Filtros' })

    await filtros.getByLabel('Placa ou descrição').fill('sprinter')
    await expect(linhaCom(page, 'SBV-1A23')).toBeVisible()
    await expect(linhaCom(page, 'SBV-2B34')).toHaveCount(0)
    await expect(page).toHaveURL(/busca=sprinter/)

    await filtros.getByRole('button', { name: 'Limpar filtros' }).click()
    await escolher(page, 'Situação', 'Inativo')
    await expect(linhaCom(page, 'SBO-3C45')).toContainText('Inativo')
    await expect(linhaCom(page, 'SBV-1A23')).toHaveCount(0)

    await escolher(page, 'Tipo', 'Van')
    await expect(page.getByText('Nenhum resultado para os filtros.')).toBeVisible()
  })

  test('veículo com viagens não pode ser excluído', async ({ page }) => {
    await page.goto(LISTA)

    await page.getByRole('button', { name: 'Excluir SBV-1A23' }).click()
    await page.getByRole('alertdialog', { name: 'Excluir veículo?' }).getByRole('button', { name: 'Excluir' }).click()

    await expect(page.getByText('Veículo com viagens não pode ser excluído. Inative o veículo.')).toBeVisible()
    await expect(linhaCom(page, 'SBV-1A23')).toBeVisible()
  })

  test('exclui veículo sem viagens', async ({ page }) => {
    const descricao = `${marca} Excluir`
    const placa = placaUnica()
    const criado = await page.request.post('/api/veiculo', { data: { placa, descricao, tipo: 'VAN', capacidade: 10, ativo: true } })
    expect(criado.ok()).toBe(true)
    await page.goto(LISTA)
    const rotuloPlaca = `${placa.slice(0, 3)}-${placa.slice(3)}`

    await page.getByRole('button', { name: `Excluir ${rotuloPlaca}` }).click()
    const resposta = page.waitForResponse((r) => r.request().method() === 'DELETE' && r.url().includes('/api/veiculo/'))
    await page.getByRole('alertdialog', { name: 'Excluir veículo?' }).getByRole('button', { name: 'Excluir' }).click()

    expect((await resposta).status()).toBe(204)
    await expect(linhaCom(page, rotuloPlaca)).toHaveCount(0)
  })
})
