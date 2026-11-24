import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { escolher, excluirViaApi, linhaCom, PREFIXO_E2E, salvarCadastro, SEED, sufixoUnico } from './support/cadastros'

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

test.describe('Rota Fixa', () => {
  const pad = (n: number) => String(n).padStart(2, '0')
  const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

  /** Rascunhos gerados somem ao virar Rota Única; aí a rota fica sem viagens e pode ser excluída no afterEach. */
  async function desfazerRotina(page: Page, id: string, nome: string) {
    await page.request.put(`/api/rota/${id}`, {
      data: { nome, origem: 'Rodoviária', destino: 'Universidade', tipo: 'UNICA', rotina: null, pontos: [{ nome: 'Rodoviária' }, { nome: 'Universidade' }] },
    })
  }

  test('rotina diária gera as viagens da vigência e leva às viagens da rota', async ({ page }) => {
    const nome = `${marca} Fixa`
    // Horário aleatório: não colide com viagens de outros testes no mesmo veículo.
    const horario = `${pad(5 + Math.floor(Math.random() * 17))}:${pad(Math.floor(Math.random() * 60))}`
    const fim = new Date()
    fim.setDate(fim.getDate() + 2)
    await page.goto(`${LISTA}/nova`)

    await page.getByLabel('Nome', { exact: true }).fill(nome)
    await page.getByLabel('Origem').fill('Rodoviária')
    await page.getByLabel('Destino').fill('Universidade')
    await page.getByRole('radio', { name: /Rota Fixa/ }).check()
    await expect(page.getByRole('heading', { level: 2, name: 'Rotina' })).toBeVisible()
    await page.getByLabel('Horário 1', { exact: true }).fill(horario)
    await escolher(page, 'Veículo', /SBV-2B34/)
    await page.getByLabel('Preço (R$)').fill('11.50')
    await page.getByLabel('Capacidade (lugares)').fill('8')
    await page.getByLabel('Fim (opcional)').fill(iso(fim))
    await nomeDoPonto(page, 1).fill('Rodoviária')
    await nomeDoPonto(page, 2).fill('Universidade')

    const id = await salvar(page)
    try {
      const resultado = page.getByRole('status').filter({ hasText: 'Última geração' })
      await expect(resultado).toBeVisible()
      const geradas = Number((await resultado.locator('strong').first().textContent()) ?? 'NaN')
      // Hoje (se o horário ainda não passou), amanhã e depois de amanhã.
      expect(geradas).toBeGreaterThanOrEqual(2)
      expect(geradas).toBeLessThanOrEqual(3)

      const agenda = await (await page.request.get('/api/viagem/agenda', { params: { rotaId: id } })).json()
      expect(agenda.page.totalElements).toBe(geradas)
      for (const v of agenda.content) {
        expect(v.status).toBe('RASCUNHO')
        expect(v.capacidade).toBe(8)
        expect(v.partida.slice(11, 16)).toBe(horario)
        expect(v.rotinaId).toBeTruthy()
      }

      await page.getByRole('link', { name: 'Ver viagens da rota' }).click()
      await expect(page).toHaveURL(new RegExp(`/operacao/viagens\\?rota=${id}$`))
      await expect(page.locator('tbody tr')).toHaveCount(geradas)
    } finally {
      await desfazerRotina(page, id, nome)
    }
  })

  test('virar Rota Única remove a rotina e os rascunhos gerados', async ({ page }) => {
    const nome = `${marca} Fixa`
    const hoje = new Date()
    const criada = await page.request.post('/api/rota', {
      data: {
        nome,
        origem: 'Rodoviária',
        destino: 'Universidade',
        tipo: 'FIXA',
        pontos: [{ nome: 'Rodoviária' }, { nome: 'Universidade' }],
        rotina: {
          frequencia: 'DIARIA',
          dias: [],
          horarios: [`${pad(5 + Math.floor(Math.random() * 17))}:${pad(Math.floor(Math.random() * 60))}`],
          veiculoId: SEED.vanMaster,
          preco: 10,
          capacidade: 5,
          inicio: iso(hoje),
          fim: iso(new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + 1)),
          publicar: false,
        },
      },
    })
    expect(criada.ok()).toBe(true)
    const rota = await criada.json()
    expect(rota.rotina.geradas).toBeGreaterThan(0)
    await page.goto(`${LISTA}/${rota.id}`)

    await page.getByRole('radio', { name: /Rota Única/ }).check()
    await expect(page.getByRole('heading', { level: 2, name: 'Rotina' })).toHaveCount(0)
    await salvar(page)

    const agenda = await (await page.request.get('/api/viagem/agenda', { params: { rotaId: rota.id } })).json()
    expect(agenda.page.totalElements).toBe(0)
    await page.goto(LISTA)
    await expect(linhaCom(page, nome)).toContainText('Única')
  })

  test('rotina semanal exige ao menos um dia', async ({ page }) => {
    await page.goto(`${LISTA}/nova`)
    await page.getByLabel('Nome', { exact: true }).fill(`${marca} Semanal`)
    await page.getByLabel('Origem').fill('A')
    await page.getByLabel('Destino').fill('B')
    await nomeDoPonto(page, 1).fill('A')
    await nomeDoPonto(page, 2).fill('B')
    await page.getByRole('radio', { name: /Rota Fixa/ }).check()
    await escolher(page, 'Frequência', 'Semanal')
    await page.getByLabel('Horário 1', { exact: true }).fill('07:00')
    await escolher(page, 'Veículo', /SBV-2B34/)
    await page.getByLabel('Preço (R$)').fill('10')
    await page.getByLabel('Capacidade (lugares)').fill('5')

    await page.getByRole('button', { name: 'Salvar' }).click()

    await expect(page.getByText('Escolha ao menos um dia da semana.')).toBeVisible()
    await page.getByLabel('Quarta').check()
    await expect(page.getByText('Escolha ao menos um dia da semana.')).toHaveCount(0)
    await expect(page).toHaveURL(new RegExp(`${LISTA}/nova$`))
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
