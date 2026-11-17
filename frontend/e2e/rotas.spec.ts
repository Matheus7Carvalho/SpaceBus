import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { excluirViaApi, linhaCom, PREFIXO_E2E, salvarCadastro, SEED, sufixoUnico } from './support/cadastros'

const LISTA = '/cadastros/rotas'

type Rota = { id: string; nome: string; pontos: { nome: string; ordem: number }[] }

const salvar = (page: Page) => salvarCadastro(page, { rotulo: 'Rota', recurso: 'rota', lista: LISTA, mensagem: 'Rota salva.' })
const nomeDoPonto = (page: Page, n: number) => page.getByLabel(`Nome do ponto ${n}`, { exact: true })

/** Marca única por teste: o afterEach apaga só o que este teste criou (testes paralelos não se atrapalham). */
let marca = ''

test.beforeEach(async ({ page }) => {
  marca = `${PREFIXO_E2E}-${sufixoUnico()}`
  await entrar(page, contas.operador)
})
test.afterEach(async ({ page }) => excluirViaApi<Rota>(page, 'rota', (r) => r.nome.startsWith(marca)))

test.describe('Cadastro de rota', () => {
  test('cria com pontos, reordena e salva na ordem da tela', async ({ page }) => {
    const nome = `${marca} Rota`
    await page.goto(LISTA)
    await page.getByRole('link', { name: 'Nova rota' }).click()
    // Espera a tela nova: a listagem também tem um campo "Nome" (filtro).
    await expect(page.getByRole('heading', { level: 1, name: 'Nova rota' })).toBeVisible()

    await page.getByLabel('Nome', { exact: true }).fill(nome)
    await page.getByLabel('Origem').fill('Rodoviária')
    await page.getByLabel('Destino').fill('Universidade')
    await nomeDoPonto(page, 1).fill('Rodoviária')
    await nomeDoPonto(page, 2).fill('Universidade')
    // Enter no último ponto cria um novo e leva o foco para ele.
    await nomeDoPonto(page, 2).press('Enter')
    await expect(nomeDoPonto(page, 3)).toBeFocused()
    await nomeDoPonto(page, 3).fill('Hospital')
    await page.getByRole('button', { name: 'Mover ponto 3 para cima' }).click()
    await expect(nomeDoPonto(page, 2)).toHaveValue('Hospital')

    const resposta = page.waitForResponse((r) => r.url().endsWith('/api/rota') && r.request().method() === 'POST')
    const id = await salvar(page)
    const salva = (await (await resposta).json()) as Rota
    expect(salva.pontos.map((p) => p.nome)).toEqual(['Rodoviária', 'Hospital', 'Universidade'])
    expect(salva.id).toBe(id)
    await expect(page.getByRole('heading', { level: 1, name: 'Editar rota' })).toBeVisible()
  })

  test('exige dois pontos e o nome de cada ponto', async ({ page }) => {
    await page.goto(`${LISTA}/nova`)

    await expect(page.getByRole('button', { name: 'Remover ponto 1' })).toBeDisabled()
    await page.getByLabel('Nome', { exact: true }).fill(`${marca} Incompleta`)
    await page.getByLabel('Origem').fill('A')
    await page.getByLabel('Destino').fill('B')
    await page.getByRole('button', { name: 'Salvar' }).click()

    // Campo obrigatório vazio: o navegador barra o envio (required) antes da validação Zod.
    expect(await nomeDoPonto(page, 1).evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true)
    await expect(page).toHaveURL(new RegExp(`${LISTA}/nova$`))

    // Só com espaços passa pelo navegador e cai na validação da tela.
    await nomeDoPonto(page, 1).fill('   ')
    await nomeDoPonto(page, 2).fill('   ')
    await page.getByRole('button', { name: 'Salvar' }).click()
    await expect(page.getByText('Informe o nome do ponto.')).toHaveCount(2)
    await expect(page).toHaveURL(new RegExp(`${LISTA}/nova$`))
  })

  test('nome repetido mostra o erro do servidor e mantém os dados', async ({ page }) => {
    await page.goto(`${LISTA}/nova`)

    await page.getByLabel('Nome', { exact: true }).fill('Centro – Aeroporto')
    await page.getByLabel('Origem').fill('Centro')
    await page.getByLabel('Destino').fill('Aeroporto')
    await nomeDoPonto(page, 1).fill('A')
    await nomeDoPonto(page, 2).fill('B')
    await page.getByRole('button', { name: 'Salvar' }).click()

    await expect(page.getByRole('alert')).toContainText('Já existe uma rota com o nome Centro – Aeroporto.')
    await expect(nomeDoPonto(page, 2)).toHaveValue('B')
  })
})

test.describe('Listagem de rotas', () => {
  test('filtra por ponto de parada e abre pela linha', async ({ page }) => {
    await page.goto(LISTA)

    await page.getByRole('search', { name: 'Filtros' }).getByLabel('Local ou ponto').fill('shopping')
    await linhaCom(page, 'Centro – Aeroporto').getByRole('cell', { name: 'Centro – Aeroporto', exact: true }).click()

    await expect(page).toHaveURL(new RegExp(`${LISTA}/${SEED.rotaCentroAeroporto}$`))
    await expect(nomeDoPonto(page, 3)).toHaveValue('Shopping')
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click()
    await expect(page).toHaveURL(/\/cadastros\/rotas\?local=shopping$/)
  })

  test('rota com viagens não pode ser excluída', async ({ page }) => {
    await page.goto(LISTA)

    await page.getByRole('button', { name: 'Excluir Centro – Aeroporto' }).click()
    await page.getByRole('alertdialog', { name: 'Excluir rota?' }).getByRole('button', { name: 'Excluir' }).click()

    await expect(page.getByText('Rota com viagens não pode ser excluída.')).toBeVisible()
  })
})
