import { expect, request, test, type APIRequestContext, type Page } from '@playwright/test'
import { contas, entrar, SENHA_DEMO } from './support/auth'
import { idDaVanE2E, SEED } from './support/cadastros'
import { camposSemLimite } from './support/limites'

/**
 * Reserva e pagamento simulado. Cada teste publica a própria viagem (capacidade pequena, partida aleatória)
 * pela API do operador e, no fim, cancela essa viagem: a seed nunca é reservada.
 */

const pad = (n: number) => String(n).padStart(2, '0')

async function contextoOperador(page: Page) {
  const operador = await request.newContext({ baseURL: new URL(page.url()).origin })
  expect((await operador.post('/api/auth/login', { data: { email: contas.operador.email, senha: SENHA_DEMO } })).ok()).toBe(true)
  return operador
}

/** Viagem publicada só deste teste, na rota da seed (4 pontos) e na van de testes, preço R$ 15,00. */
async function viagemPublicada(operador: APIRequestContext, capacidade: number) {
  const d = new Date()
  d.setDate(d.getDate() + 30 + Math.floor(Math.random() * 300))
  d.setHours(5 + Math.floor(Math.random() * 17), Math.floor(Math.random() * 60), 0, 0)
  const partida = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`
  const criada = await operador.post('/api/viagem', {
    data: { rotaId: SEED.rotaCentroAeroporto, veiculoId: await idDaVanE2E(operador), motoristaId: null, partida, preco: 15, capacidade },
  })
  expect(criada.ok()).toBe(true)
  const { id } = (await criada.json()) as { id: string }
  expect((await operador.post(`/api/viagem/${id}/publicar`)).ok()).toBe(true)
  return id
}

const situacao = (page: Page) => page.getByRole('region', { name: 'Situação da reserva' })

/** Detalhe da viagem com o trecho Terminal Central (1) → Aeroporto (4), como vindo da busca. */
async function reservarPelaTela(page: Page, viagemId: string, lugares: number) {
  await page.goto(`/buscar/${viagemId}?embarque=1&desembarque=4`)
  const form = page.getByRole('form', { name: 'Reservar lugares' })
  await form.getByLabel('Lugares').fill(String(lugares))
  const resposta = page.waitForResponse((r) => r.url().endsWith('/api/reserva') && r.request().method() === 'POST')
  await form.getByRole('button', { name: 'Reservar' }).click()
  expect((await resposta).ok()).toBe(true)
  await expect(page).toHaveURL(/\/reservas\/[0-9a-f-]{36}$/)
  return page.url().split('/').pop()!
}

async function pagarNoProvedor(page: Page, acao: 'Aprovar pagamento' | 'Recusar pagamento', botao = 'Pagar') {
  await situacao(page).getByRole('button', { name: botao }).click()
  await expect(page).toHaveURL(/\/reservas\/pagamento\/sim_/)
  await expect(page.getByText('Simulado')).toBeVisible()
  const resposta = page.waitForResponse((r) => /\/api\/provedor-simulado\/cobrancas\/[^/]+\/(aprovar|recusar)$/.test(r.url()))
  await page.getByRole('button', { name: acao }).click()
  expect((await resposta).ok()).toBe(true)
  await expect(page).toHaveURL(/\/reservas\/[0-9a-f-]{36}$/)
}

let operador: APIRequestContext
let viagens: string[] = []

test.beforeEach(async ({ page }) => {
  await entrar(page, contas.passageiro)
  operador = await contextoOperador(page)
  viagens = []
})

test.afterEach(async () => {
  for (const id of viagens) await operador.post(`/api/viagem/${id}/cancelar`)
  await operador.dispose()
})

test.describe('Reserva do passageiro', () => {
  test('reserva, paga no provedor, fica confirmada, aparece em Minhas reservas e cancela com reembolso', async ({ page }) => {
    const viagem = await viagemPublicada(operador, 4)
    viagens.push(viagem)

    await page.goto(`/buscar/${viagem}?embarque=1&desembarque=4`)
    const form = page.getByRole('form', { name: 'Reservar lugares' })
    await expect(form.getByLabel('Lugares')).toHaveAttribute('max', '4')
    await form.getByLabel('Lugares').fill('2')
    await expect(form).toContainText('R$ 30,00')
    await expect(form).toContainText('Cancelamento gratuito até 2 horas antes da partida')
    const id = await reservarPelaTela(page, viagem, 2)

    await expect(situacao(page)).toContainText('Aguardando pagamento')
    await expect(situacao(page)).toContainText('Pague até')
    const codigo = (await page.getByRole('heading', { level: 1 }).textContent())!.replace('Reserva ', '')
    expect(codigo).toMatch(/^SB-[A-Z0-9]{6}$/)

    await pagarNoProvedor(page, 'Aprovar pagamento')
    await expect(situacao(page)).toContainText('Confirmada')
    await expect(situacao(page)).toContainText('Aprovado')

    // Sai e volta: a reserva continua lá (requisito de aceite 4).
    await page.goto('/reservas')
    const item = page.getByRole('listitem').filter({ has: page.locator(`a[href="/reservas/${id}"]`) })
    await expect(item).toContainText(codigo)
    await expect(item).toContainText('Confirmada')
    await item.click({ position: { x: 10, y: 10 } })

    await situacao(page).getByRole('button', { name: 'Cancelar reserva' }).click()
    const dialogo = page.getByRole('alertdialog', { name: 'Cancelar reserva?' })
    await expect(dialogo).toContainText('reembolsado integralmente')
    const cancelamento = page.waitForResponse((r) => r.url().endsWith(`/api/reserva/${id}/cancelar`))
    await dialogo.getByRole('button', { name: 'Cancelar reserva' }).click()
    expect((await cancelamento).ok()).toBe(true)
    await expect(situacao(page)).toContainText('Cancelada')
    await expect(situacao(page)).toContainText('Reembolsado')
    await expect(situacao(page).getByRole('button')).toHaveCount(0)

    // Histórico com cada passo, na ordem (CLAUDE.md: mudanças de reserva e pagamento gravam histórico).
    const historico = page.getByRole('list', { name: 'Histórico da reserva' }).getByRole('listitem')
    await expect(historico).toHaveCount(6)
    await expect(historico.nth(0)).toContainText('Reserva feita por você')
    await expect(historico.nth(2)).toContainText('Pagamento aprovado pelo provedor de pagamento')
    await expect(historico.nth(3)).toContainText('Reserva confirmada')
    await expect(historico.nth(4)).toContainText('Reserva cancelada por você')
    await expect(historico.nth(5)).toContainText('Valor reembolsado')
  })

  test('pagamento recusado mantém a reserva e permite tentar de novo', async ({ page }) => {
    const viagem = await viagemPublicada(operador, 3)
    viagens.push(viagem)
    await reservarPelaTela(page, viagem, 1)

    await pagarNoProvedor(page, 'Recusar pagamento')
    await expect(situacao(page)).toContainText('Aguardando pagamento')
    await expect(situacao(page)).toContainText('Pagamento recusado')

    await pagarNoProvedor(page, 'Aprovar pagamento', 'Tentar pagar de novo')
    await expect(situacao(page)).toContainText('Confirmada')
  })

  test('duas reservas simultâneas da última vaga: uma passa, a outra é recusada', async ({ page }) => {
    const viagem = await viagemPublicada(operador, 1)
    viagens.push(viagem)
    const pedido = { viagemId: viagem, embarqueOrdem: 1, desembarqueOrdem: 4, quantidade: 1 }

    const respostas = await Promise.all([page.request.post('/api/reserva', { data: pedido }), page.request.post('/api/reserva', { data: pedido })])

    expect(respostas.map((r) => r.status()).sort()).toEqual([200, 400])
    const recusada = respostas.find((r) => r.status() === 400)!
    expect((await recusada.json()).message).toBe('Não há lugares disponíveis nesta viagem.')
    await page.goto(`/buscar/${viagem}?embarque=1&desembarque=4`)
    await expect(page.getByText('Viagem lotada. Não há lugares disponíveis.')).toBeVisible()
  })

  test('formulário de reserva tem limites e o passageiro não vê a operação', async ({ page }) => {
    const viagem = await viagemPublicada(operador, 2)
    viagens.push(viagem)
    await page.goto(`/buscar/${viagem}?embarque=1&desembarque=4`)
    await expect(page.getByRole('form', { name: 'Reservar lugares' })).toBeVisible()

    expect(await camposSemLimite(page)).toEqual([])
    expect((await page.request.get(`/api/viagem/${viagem}/reservas`)).status()).toBe(403)
  })
})

test.describe('Reservas na operação', () => {
  test('operador vê os passageiros e, ao cancelar a viagem, sabe quem avisar', async ({ page }) => {
    const viagem = await viagemPublicada(operador, 3)
    viagens.push(viagem)
    // Reserva paga preparada pela API com a sessão do passageiro.
    const reserva = await (await page.request.post('/api/reserva', { data: { viagemId: viagem, embarqueOrdem: 2, desembarqueOrdem: 4, quantidade: 2 } })).json()
    const pagamento = await (await page.request.post(`/api/reserva/${reserva.id}/pagamento`)).json()
    expect((await page.request.post(`/api/provedor-simulado/cobrancas/${pagamento.provedorId}/aprovar`)).ok()).toBe(true)

    // Troca de perfil: encerra a sessão do passageiro antes de entrar como operador.
    await page.request.post('/api/auth/logout')
    await entrar(page, contas.operador)
    await page.goto(`/operacao/viagens/${viagem}`)
    const passageiros = page.getByRole('region', { name: 'Passageiros' })
    const linha = passageiros.getByRole('row').filter({ hasText: reserva.codigo })
    await expect(linha).toContainText(contas.passageiro.nome)
    await expect(linha).toContainText('Confirmada')
    await expect(passageiros).toContainText('2 confirmados · 0 aguardando pagamento · 1 livres de 3')

    // Não dá para reduzir a capacidade abaixo do vendido.
    await page.getByLabel('Capacidade (lugares)').fill('1')
    await page.getByRole('button', { name: 'Salvar' }).click()
    await expect(page.getByRole('alert')).toContainText('Já há 2 lugares reservados nesta viagem.')
    await page.getByLabel('Capacidade (lugares)').fill('3')

    await page.getByRole('region', { name: 'Situação' }).getByRole('button', { name: 'Cancelar viagem' }).click()
    const dialogo = page.getByRole('alertdialog', { name: 'Cancelar viagem?' })
    await expect(dialogo).toContainText('As reservas serão canceladas')
    await dialogo.getByRole('button', { name: 'Cancelar viagem' }).click()

    await expect(linha).toContainText('Cancelada')
    await expect(linha).toContainText('A avisar')
    await expect(passageiros).toContainText('1 passageiro precisa ser avisado')
    viagens = [] // já cancelada
  })
})
