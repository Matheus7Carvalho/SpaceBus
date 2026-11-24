import { expect, test, type APIRequestContext, type Page } from '@playwright/test'
import { contas, entrar, type Conta } from './support/auth'
import { PREFIXO_E2E } from './support/cadastros'
import { contextoDe, prepararViagem, reservaConfirmada, type Cenario } from './support/localizacao'

/**
 * Módulo 5 — notificações. Cada teste usa um passageiro próprio (marca E2E), para a contagem do sino não misturar
 * notificações de testes paralelos, e uma viagem própria com partida daqui a 3 dias (longe do lembrete de 1 h).
 */

let cenario: Cenario | null = null

test.afterEach(async () => {
  await cenario?.encerrar()
  cenario = null
})

const base = (page: Page) => new URL(page.url()).origin
const TRES_DIAS = 60 * 24 * 3

async function passageiroDoTeste(page: Page) {
  await page.goto('/login')
  cenario = await prepararViagem(base(page), { comMotorista: false, partidaEmMin: TRES_DIAS })
  const criado = await cenario.admin.post(`/api/dev/dados-teste/passageiro?marca=${PREFIXO_E2E}`)
  expect(criado.ok()).toBe(true)
  const p = (await criado.json()) as { nome: string; email: string }
  const conta: Conta = { nome: p.nome, email: p.email, perfil: 'Passageiro' }
  await entrar(page, conta)
  return { c: cenario, conta }
}

const sino = (page: Page) => page.getByRole('banner').getByRole('link', { name: /^Notificações/ })
const itens = (page: Page) => page.getByRole('region', { name: 'Lista de notificações' }).getByRole('listitem').filter({ has: page.getByRole('heading') })

async function alterarPartida(operador: APIRequestContext, viagemId: string) {
  const viagem = (await (await operador.get(`/api/viagem/${viagemId}`)).json()) as Record<string, unknown> & { partida: string }
  const nova = new Date(new Date(viagem.partida).getTime() + 60 * 60_000)
  const pad = (n: number) => String(n).padStart(2, '0')
  const partida = `${nova.getFullYear()}-${pad(nova.getMonth() + 1)}-${pad(nova.getDate())}T${pad(nova.getHours())}:${pad(nova.getMinutes())}:00`
  const r = await operador.put(`/api/viagem/${viagemId}`, {
    data: { rotaId: viagem.rotaId, veiculoId: viagem.veiculoId, motoristaId: viagem.motoristaId, partida, preco: viagem.preco, capacidade: viagem.capacidade },
  })
  expect(r.ok(), await r.text()).toBe(true)
}

test.describe('Notificações do passageiro', () => {
  test('reserva paga gera notificação no sino e na lista; abrir marca como lida e leva à reserva', async ({ page }) => {
    const { c } = await passageiroDoTeste(page)
    await expect(sino(page)).toHaveAccessibleName('Notificações')

    const reservaId = await reservaConfirmada(page.request, c.viagemId)
    await page.goto('/notificacoes')
    await expect(sino(page)).toHaveAccessibleName('Notificações, 1 não lida')
    await expect(itens(page)).toHaveCount(1)
    const item = itens(page)
    await expect(item).toContainText(/Reserva SB-[A-Z0-9]{6} confirmada/)
    await expect(item).toContainText('(não lida)')
    await expect(item.getByRole('list', { name: 'Envios' })).toContainText('E-mail · Enviado')
    await expect(item.getByText('Simulado')).toBeVisible()

    const leitura = page.waitForResponse((r) => /\/api\/notificacao\/[^/]+\/ler$/.test(r.url()))
    await item.getByRole('link').click()
    expect((await leitura).ok()).toBe(true)
    await expect(page).toHaveURL(new RegExp(`/reservas/${reservaId}$`))
    await expect(sino(page)).toHaveAccessibleName('Notificações')

    await page.goBack()
    await expect(page).toHaveURL(/\/notificacoes$/)
    await expect(itens(page)).toHaveAttribute('data-lida', 'true')
  })

  test('alteração de partida e cancelamento da viagem chegam ao passageiro; marcar todas como lidas', async ({ page }) => {
    const { c } = await passageiroDoTeste(page)
    await reservaConfirmada(page.request, c.viagemId)
    await alterarPartida(c.operador, c.viagemId)
    expect((await c.operador.post(`/api/viagem/${c.viagemId}/cancelar`)).ok()).toBe(true)

    await page.goto('/notificacoes')
    await expect(itens(page)).toHaveCount(3)
    await expect(itens(page).filter({ hasText: 'Viagem cancelada' })).toContainText('O valor será reembolsado.')
    await expect(itens(page).filter({ hasText: 'Horário alterado' })).toContainText(/Nova partida: .* \(antes .*\)\./)
    await expect(sino(page)).toHaveAccessibleName('Notificações, 3 não lidas')

    const todas = page.waitForResponse((r) => r.url().endsWith('/api/notificacao/ler-todas'))
    await page.getByRole('button', { name: 'Marcar todas como lidas' }).click()
    expect((await todas).ok()).toBe(true)
    await expect(sino(page)).toHaveAccessibleName('Notificações')
    await expect(page.getByRole('button', { name: 'Marcar todas como lidas' })).toHaveCount(0)
  })

  test('WhatsApp ligado: a próxima notificação também sai por WhatsApp (simulado)', async ({ page }) => {
    const { c } = await passageiroDoTeste(page)
    await page.goto('/notificacoes')
    await expect(page.getByText('Nenhuma notificação')).toBeVisible()
    const opcao = page.getByRole('switch', { name: 'Receber por WhatsApp' })
    await expect(opcao).not.toBeChecked()

    const salvar = page.waitForResponse((r) => r.url().endsWith('/api/notificacao/preferencias') && r.request().method() === 'PUT')
    await opcao.click()
    expect((await salvar).ok()).toBe(true)
    await expect(opcao).toBeChecked()

    await reservaConfirmada(page.request, c.viagemId)
    await page.reload()
    await expect(page.getByRole('switch', { name: 'Receber por WhatsApp' })).toBeChecked()
    await expect(itens(page).getByRole('list', { name: 'Envios' })).toContainText('WhatsApp · Enviado')
  })

  test('erro ao carregar mostra Tentar novamente; estrutura HTML5', async ({ page }) => {
    await passageiroDoTeste(page)
    await page.route((url) => url.pathname === '/api/notificacao', (rota) => rota.fulfill({ status: 500, json: { status: 500, message: 'Erro' } }))
    await page.goto('/notificacoes')

    await expect(page.getByRole('alert')).toContainText('Não foi possível carregar.')
    await expect(page.getByRole('button', { name: 'Tentar novamente' })).toBeVisible()
    await expect(page.getByRole('main')).toHaveCount(1)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Notificações')
  })
})

test.describe('Permissões', () => {
  for (const conta of [contas.operador, contas.motorista]) {
    test(`${conta.perfil} não vê o sino nem a tela de notificações`, async ({ page }) => {
      await entrar(page, conta)
      await expect(page.getByRole('banner').getByRole('link', { name: /^Notificações/ })).toHaveCount(0)
      await page.goto('/notificacoes')
      await expect(page.getByText('Acesso não permitido para o seu perfil.')).toBeVisible()
      expect((await page.request.get('/api/notificacao')).status()).toBe(403)
    })
  }

  test('notificação de outro passageiro responde 404', async ({ page }) => {
    await page.goto('/login')
    const passageiro = await contextoDe(base(page), contas.passageiro.email)
    const r = await passageiro.post('/api/notificacao/00000000-0000-0000-0000-000000000000/ler')
    expect(r.status()).toBe(404)
    expect(((await r.json()) as { message: string }).message).toBe('Notificação não encontrada.')
    await passageiro.dispose()
  })
})
