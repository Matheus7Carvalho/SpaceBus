import { expect, request, type APIRequestContext, type BrowserContext, type Page } from '@playwright/test'
import { contas, SENHA_DEMO, type Conta } from './auth'
import { PREFIXO_E2E, SEED, sufixoUnico } from './cadastros'

/**
 * Dados dos testes de localização (módulo 4). Cada teste cria a própria van e o próprio motorista (marca E2E): um
 * motorista e um veículo têm no máximo uma viagem em andamento, e desktop e mobile rodam em paralelo. A limpeza do
 * global-teardown apaga tudo pela marca. Viagem da seed nunca é iniciada.
 */

/** Mesmo fuso do backend (APP_FUSO_HORARIO): a partida é relógio de parede da operação. */
const FUSO = 'America/Araguaina'

/** "AAAA-MM-DDTHH:mm:00" daqui a {@code minutos}, no fuso da operação. */
export function partidaDaquiA(minutos: number) {
  const partes = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', { timeZone: FUSO, year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })
      .formatToParts(new Date(Date.now() + minutos * 60_000))
      .map((p) => [p.type, p.value]),
  )
  return `${partes.year}-${partes.month}-${partes.day}T${partes.hour}:${partes.minute}:00`
}

export async function contextoDe(baseURL: string, email: string) {
  const ctx = await request.newContext({ baseURL })
  expect((await ctx.post('/api/auth/login', { data: { email, senha: SENHA_DEMO } })).ok()).toBe(true)
  return ctx
}

export type Cenario = {
  viagemId: string
  motorista: Conta | null
  operador: APIRequestContext
  admin: APIRequestContext
  encerrar: () => Promise<void>
}

/** Placa válida e aleatória (padrão Mercosul), só para a van deste teste. */
function placaUnica() {
  const letra = () => String.fromCharCode(65 + Math.floor(Math.random() * 26))
  const digito = () => String(Math.floor(Math.random() * 10))
  return `X${letra()}${letra()}${digito()}${letra()}${digito()}${digito()}`
}

/**
 * Viagem publicada só deste teste, na rota da seed, com van e (se pedido) motorista próprios.
 * Partida daqui a {@code partidaEmMin} minutos: dentro da janela de início (30 min antes) e ainda reservável.
 */
export async function prepararViagem(baseURL: string, opcoes: { comMotorista?: boolean; partidaEmMin?: number } = {}): Promise<Cenario> {
  const { comMotorista = true, partidaEmMin = 15 } = opcoes
  const operador = await contextoDe(baseURL, contas.operador.email)
  const admin = await contextoDe(baseURL, contas.administrador.email)
  const sufixo = sufixoUnico()

  let motorista: Conta | null = null
  let motoristaId: string | null = null
  if (comMotorista) {
    const criado = await admin.post(`/api/dev/dados-teste/motorista?marca=${PREFIXO_E2E}`)
    expect(criado.ok()).toBe(true)
    const m = (await criado.json()) as { id: string; nome: string; email: string }
    motorista = { nome: m.nome, email: m.email, perfil: 'Motorista' }
    motoristaId = m.id
  }

  const van = await operador.post('/api/veiculo', {
    data: { placa: placaUnica(), descricao: `${PREFIXO_E2E} Van localização ${sufixo}`, tipo: 'VAN', capacidade: 15, ativo: true },
  })
  expect(van.ok()).toBe(true)
  const veiculoId = ((await van.json()) as { id: string }).id

  const viagem = await operador.post('/api/viagem', {
    data: { rotaId: SEED.rotaCentroAeroporto, veiculoId, motoristaId, partida: partidaDaquiA(partidaEmMin), preco: 15, capacidade: 4 },
  })
  expect(viagem.ok()).toBe(true)
  const viagemId = ((await viagem.json()) as { id: string }).id
  expect((await operador.post(`/api/viagem/${viagemId}/publicar`)).ok()).toBe(true)

  return {
    viagemId,
    motorista,
    operador,
    admin,
    encerrar: async () => {
      await operador.dispose()
      await admin.dispose()
    },
  }
}

/** Reserva confirmada (pagamento simulado aprovado) do passageiro da sessão {@code passageiro}. */
export async function reservaConfirmada(passageiro: APIRequestContext, viagemId: string) {
  const criada = await passageiro.post('/api/reserva', { data: { viagemId, embarqueOrdem: 1, desembarqueOrdem: 4, quantidade: 1 } })
  expect(criada.ok()).toBe(true)
  const { id } = (await criada.json()) as { id: string }
  const pagamento = await passageiro.post(`/api/reserva/${id}/pagamento`)
  expect(pagamento.ok()).toBe(true)
  const { provedorId } = (await pagamento.json()) as { provedorId: string }
  expect((await passageiro.post(`/api/provedor-simulado/cobrancas/${provedorId}/aprovar`)).ok()).toBe(true)
  return id
}

export async function iniciarPelaOperacao(operador: APIRequestContext, viagemId: string) {
  const r = await operador.post(`/api/viagem/${viagemId}/iniciar`)
  expect(r.ok(), await r.text()).toBe(true)
}

export async function encerrarPelaOperacao(operador: APIRequestContext, viagemId: string) {
  expect((await operador.post(`/api/viagem/${viagemId}/encerrar`)).ok()).toBe(true)
}

/** Posição simulada (H17) pelo endpoint de dev, com a hora de {@code segundosAtras} segundos atrás. */
export async function simularPosicao(admin: APIRequestContext, viagemId: string, opcoes: { segundosAtras?: number; precisaoM?: number } = {}) {
  const { segundosAtras = 0, precisaoM = 30 } = opcoes
  const r = await admin.post(`/api/dev/localizacao/${viagemId}`, {
    data: { latitude: -10.1843, longitude: -48.3336, precisaoM, registradaEm: new Date(Date.now() - segundosAtras * 1000).toISOString() },
  })
  expect(r.ok()).toBe(true)
  expect(((await r.json()) as { aceitas: number }).aceitas).toBe(1)
}

/** Os testes não dependem de internet: os tiles do OpenStreetMap são bloqueados (o mapa fica sem fundo). */
export async function bloquearTiles(alvo: Page | BrowserContext) {
  await alvo.route('https://tile.openstreetmap.org/**', (rota) => rota.abort())
}

export function respostaDe(page: Page, sufixo: string, metodo = 'POST') {
  return page.waitForResponse((r) => new URL(r.url()).pathname.endsWith(sufixo) && r.request().method() === metodo)
}
