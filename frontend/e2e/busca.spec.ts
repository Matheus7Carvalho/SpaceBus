import { expect, request, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import { SEED } from './support/cadastros'

/**
 * Busca do passageiro. Lê a seed (viagens publicadas amanhã às 07:00 e 12:30 na rota Centro – Aeroporto)
 * e só altera viagens que o próprio teste cria.
 */

const pad = (n: number) => String(n).padStart(2, '0')
const iso = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
function diaMais(dias: number) {
  const d = new Date()
  d.setDate(d.getDate() + dias)
  return iso(d)
}

const resultados = (page: Page) => page.getByRole('region', { name: /viage(m|ns) em/ }).getByRole('listitem')

async function buscar(page: Page, origem: string, destino: string, data: string) {
  const busca = page.getByRole('search', { name: 'Buscar viagens' })
  await busca.getByLabel('Origem', { exact: true }).fill(origem)
  await busca.getByLabel('Destino', { exact: true }).fill(destino)
  await busca.getByLabel('Data').fill(data)
  const resposta = page.waitForResponse((r) => r.url().includes('/api/busca/viagens?'))
  await busca.getByRole('button', { name: 'Buscar' }).click()
  await resposta
}

test.beforeEach(async ({ page }) => entrar(page, contas.passageiro))

test.describe('Busca de viagens', () => {
  test('encontra as viagens da seed com embarque e desembarque, ignorando acento', async ({ page }) => {
    await expect(page).toHaveURL(/\/buscar$/)

    await buscar(page, 'praca', 'AEROPORTO', diaMais(1))

    await expect(page).toHaveURL(/origem=praca&destino=AEROPORTO&data=/)
    const seed = resultados(page).filter({ has: page.locator(`a[href*="${SEED.viagemPublicada}"]`) })
    await expect(seed).toHaveCount(1)
    await expect(seed).toContainText('07:00')
    await expect(seed).toContainText('Praça Principal')
    await expect(seed).toContainText('Aeroporto')
    await expect(seed).toContainText('R$ 18,50')
    await expect(seed).toContainText('Van')
    await expect(seed).toContainText('15 lugares')

    // Ordem por horário: a das 07:00 vem antes da das 12:30 (as duas da seed).
    const links = await resultados(page).locator('a').evaluateAll((as) => as.map((a) => a.getAttribute('href') ?? ''))
    const posicao = (id: string) => links.findIndex((h) => h.includes(id))
    expect(posicao(SEED.viagemPublicada)).toBeGreaterThanOrEqual(0)
    expect(posicao(SEED.viagemPublicada)).toBeLessThan(posicao(SEED.viagemPublicada12h30))
  })

  test('sentido contrário ao percurso não encontra viagem e mostra orientação', async ({ page }) => {
    await buscar(page, 'aeroporto', 'centro', diaMais(1))

    await expect(page.getByText('Nenhuma viagem encontrada. Tente outra data ou confira os locais de embarque e desembarque.')).toBeVisible()
    await expect(page.getByRole('heading', { level: 2, name: /^0 viagens em/ })).toBeVisible()
  })

  test('campos obrigatórios, data mínima e sugestões de locais', async ({ page }) => {
    const busca = page.getByRole('search', { name: 'Buscar viagens' })
    await expect(busca.getByLabel('Data')).toHaveAttribute('min', diaMais(0))
    await expect(busca.getByLabel('Origem', { exact: true })).toHaveAttribute('required', '')
    await expect(page.locator('#busca-locais option[value="Terminal Central"]')).toHaveCount(1)

    await busca.getByRole('button', { name: 'Buscar' }).click()
    expect(await busca.getByLabel('Origem', { exact: true }).evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true)
    await expect(page).toHaveURL(/\/buscar$/)
  })

  test('data passada pela URL mostra a orientação do servidor', async ({ page }) => {
    await page.goto(`/buscar?origem=centro&destino=aeroporto&data=${diaMais(-1)}`)

    await expect(page.getByRole('alert')).toHaveText('Escolha hoje ou uma data futura.')
  })

  test('inverter troca origem e destino', async ({ page }) => {
    const busca = page.getByRole('search', { name: 'Buscar viagens' })
    await busca.getByLabel('Origem', { exact: true }).fill('Shopping')
    await busca.getByLabel('Destino', { exact: true }).fill('Terminal')

    await busca.getByRole('button', { name: 'Inverter origem e destino' }).click()

    await expect(busca.getByLabel('Origem', { exact: true })).toHaveValue('Terminal')
    await expect(busca.getByLabel('Destino', { exact: true })).toHaveValue('Shopping')
  })
})

test.describe('Detalhe da viagem', () => {
  test('abre pelo resultado, destaca o trecho e volta para a mesma busca', async ({ page }) => {
    const url = `/buscar?origem=praca&destino=aeroporto&data=${diaMais(1)}`
    await page.goto(url)
    const seed = resultados(page).filter({ has: page.locator(`a[href*="${SEED.viagemPublicada}"]`) })
    await seed.click({ position: { x: 10, y: 10 } })

    await expect(page.getByRole('heading', { level: 1, name: 'Centro – Aeroporto' })).toBeVisible()
    const percurso = page.getByRole('list', { name: 'Pontos de parada' }).getByRole('listitem')
    await expect(percurso).toHaveCount(4)
    await expect(percurso.nth(1)).toContainText('Praça PrincipalEmbarque')
    await expect(percurso.nth(3)).toContainText('AeroportoDesembarque')
    await expect(percurso.nth(0)).not.toContainText('Embarque')
    await expect(page.getByText(/Atualizada pela transportadora em/)).toBeVisible()

    await page.getByRole('link', { name: 'Voltar à busca' }).click()
    await expect(page).toHaveURL(new RegExp(`${url.replace(/[?]/g, '\\?')}$`))
    await expect(page.getByRole('search', { name: 'Buscar viagens' }).getByLabel('Origem', { exact: true })).toHaveValue('praca')
  })

  test('referência ausente aparece como não informada', async ({ page }) => {
    // Rota própria do teste, com um ponto sem referência; criada e removida via API do operador.
    const operador = await request.newContext({ baseURL: new URL(page.url()).origin })
    expect((await operador.post('/api/auth/login', { data: { email: contas.operador.email, senha: 'demo123' } })).ok()).toBe(true)
    // Nomes de ponto únicos: a busca abaixo só pode encontrar a viagem deste teste.
    const sufixo = Date.now().toString(36)
    const [pontoA, pontoB] = [`E2E Ponto A ${sufixo}`, `E2E Ponto B ${sufixo}`]
    const rotaCriada = await operador.post('/api/rota', {
      data: { nome: `E2E-Busca ${sufixo}`, origem: 'A', destino: 'B', pontos: [{ nome: pontoA, referencia: null }, { nome: pontoB, referencia: 'Portão 1' }] },
    })
    expect(rotaCriada.ok()).toBe(true)
    const rota = await rotaCriada.json()
    const partida = new Date()
    partida.setDate(partida.getDate() + 40 + Math.floor(Math.random() * 300))
    partida.setHours(5 + Math.floor(Math.random() * 17), Math.floor(Math.random() * 60), 0, 0)
    const viagemCriada = await operador.post('/api/viagem', {
      data: { rotaId: rota.id, veiculoId: SEED.vanMaster, motoristaId: null, partida: `${iso(partida)}T${pad(partida.getHours())}:${pad(partida.getMinutes())}:00`, preco: 10, capacidade: 5 },
    })
    expect(viagemCriada.ok()).toBe(true)
    const viagem = await viagemCriada.json()
    expect((await operador.post(`/api/viagem/${viagem.id}/publicar`)).ok()).toBe(true)
    const buscarNaApi = async () =>
      (await page.request.get('/api/busca/viagens', { params: { origem: pontoA, destino: pontoB, data: iso(partida) } }).then((r) => r.json())) as { id: string }[]

    try {
      await page.goto(`/buscar/${viagem.id}`)
      const percurso = page.getByRole('list', { name: 'Pontos de parada' }).getByRole('listitem')
      await expect(percurso.nth(0)).toContainText('Referência não informada')
      await expect(percurso.nth(1)).toContainText('Portão 1')

      // Publicada: a busca encontra. Suspensa pelo operador: some da busca e o detalhe avisa.
      expect((await buscarNaApi()).map((v) => v.id)).toEqual([viagem.id])
      expect((await operador.post(`/api/viagem/${viagem.id}/suspender`)).ok()).toBe(true)
      expect(await buscarNaApi()).toEqual([])
      await page.reload()
      await expect(page.getByText('Esta viagem não está mais disponível.')).toBeVisible()
    } finally {
      await operador.post(`/api/viagem/${viagem.id}/cancelar`)
      await operador.dispose()
    }
  })

  test('viagem inexistente avisa que não está disponível', async ({ page }) => {
    await page.goto('/buscar/00000000-0000-0000-0000-000000000000')

    await expect(page.getByText('Esta viagem não está mais disponível.')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Voltar à busca' })).toHaveAttribute('href', '/buscar')
  })
})
