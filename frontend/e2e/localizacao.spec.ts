import { expect, test, type Page } from '@playwright/test'
import { contas, entrar } from './support/auth'
import {
  bloquearTiles,
  contextoDe,
  encerrarPelaOperacao,
  iniciarPelaOperacao,
  prepararViagem,
  reservaConfirmada,
  respostaDe,
  simularPosicao,
  type Cenario,
} from './support/localizacao'

/**
 * Módulo 4 — localização da viagem ativa. Cada teste prepara a própria viagem (van e motorista E2E) pela API.
 * Tiles do mapa sempre bloqueados: nada depende de internet.
 */

const PALMAS = { latitude: -10.1843, longitude: -48.3336, accuracy: 15 }

let cenario: Cenario | null = null

test.beforeEach(async ({ context }) => {
  await bloquearTiles(context)
})

test.afterEach(async () => {
  await cenario?.encerrar()
  cenario = null
})

const base = (page: Page) => new URL(page.url()).origin

async function preparar(page: Page, opcoes?: Parameters<typeof prepararViagem>[1]) {
  cenario = await prepararViagem(base(page), opcoes)
  return cenario
}

const cartao = (page: Page) => page.getByRole('list', { name: 'Viagens de hoje' }).getByRole('listitem')
const transmissao = (page: Page) => page.getByRole('status', { name: 'Transmissão da localização' })

test.describe('Motorista', () => {
  test('inicia, transmite, a posição chega ao passageiro com a hora e encerra', async ({ page, context }) => {
    await page.goto('/login')
    const c = await preparar(page)
    const passageiro = await contextoDe(base(page), contas.passageiro.email)
    const reservaId = await reservaConfirmada(passageiro, c.viagemId)
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation(PALMAS)

    await entrar(page, c.motorista!)
    await expect(page).toHaveURL(/\/motorista\/viagens$/)
    await expect(cartao(page)).toHaveCount(1)
    await expect(cartao(page)).toContainText('Publicada')

    const inicio = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/iniciar`)
    const primeiraPosicao = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await cartao(page).getByRole('button', { name: 'Iniciar' }).click()
    expect((await inicio).ok()).toBe(true)
    const envio = await primeiraPosicao
    expect(envio.ok()).toBe(true)
    expect(((await envio.json()) as { aceitas: number }).aceitas).toBe(1)
    await expect(transmissao(page)).toContainText('Transmitindo')
    await expect(transmissao(page)).toContainText(/Última posição enviada às \d{2}:\d{2}/)
    await expect(cartao(page)).toContainText('Em andamento')

    // O passageiro vê a posição com a hora, dada pelo servidor (a tela do passageiro tem testes próprios abaixo).
    const vista = (await (await passageiro.get(`/api/reserva/${reservaId}/localizacao`)).json()) as Record<string, unknown>
    expect(vista).toMatchObject({ situacaoViagem: 'EM_ANDAMENTO', disponivel: true, desatualizada: false, simulada: false, precisaoM: 15 })
    expect(vista.registradaEm).toEqual(expect.any(String))

    await cartao(page).getByRole('button', { name: 'Encerrar' }).click()
    const dialogo = page.getByRole('alertdialog', { name: 'Encerrar viagem?' })
    await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused()
    const fim = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/encerrar`)
    await dialogo.getByRole('button', { name: 'Encerrar' }).click()
    expect((await fim).ok()).toBe(true)
    await expect(cartao(page)).toContainText('Encerrada')
    await expect(transmissao(page)).toHaveCount(0)

    // Posição que chega depois do encerramento é recusada, e o passageiro não vê mais posição.
    const tardia = await page.request.post(`/api/motorista/viagem/${c.viagemId}/localizacao`, {
      data: { posicoes: [{ latitude: -10.18, longitude: -48.33, precisaoM: 10, registradaEm: new Date().toISOString() }] },
    })
    expect(tardia.status()).toBe(400)
    expect(((await tardia.json()) as { message: string }).message).toBe('A viagem não está em andamento.')
    expect(await (await passageiro.get(`/api/reserva/${reservaId}/localizacao`)).json()).toMatchObject({ situacaoViagem: 'ENCERRADA', disponivel: false })
    await passageiro.dispose()
  })

  test('recarregar com a viagem em andamento retoma a transmissão sem novo Iniciar', async ({ page, context }) => {
    await page.goto('/login')
    const c = await preparar(page)
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation(PALMAS)
    await entrar(page, c.motorista!)
    await expect(page).toHaveURL(/\/motorista\/viagens$/)
    expect((await page.request.post(`/api/motorista/viagem/${c.viagemId}/iniciar`)).ok()).toBe(true)

    const envio = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await page.reload()
    expect((await envio).ok()).toBe(true)
    await expect(transmissao(page)).toContainText('Transmitindo')
    await expect(cartao(page).getByRole('button', { name: 'Iniciar' })).toHaveCount(0)
  })

  test('permissão de localização negada avisa e a viagem continua em andamento', async ({ page, context }) => {
    await page.goto('/login')
    const c = await preparar(page)
    await context.clearPermissions()
    await entrar(page, c.motorista!)
    expect((await page.request.post(`/api/motorista/viagem/${c.viagemId}/iniciar`)).ok()).toBe(true)
    await page.reload()

    await expect(transmissao(page)).toContainText('Permita a localização no navegador para transmitir.')
    await expect(cartao(page)).toContainText('Em andamento')
    await expect(cartao(page).getByRole('button', { name: 'Encerrar' })).toBeEnabled()
  })

  test('sem internet guarda as posições na fila e envia em lote ao voltar', async ({ page, context }) => {
    await page.goto('/login')
    const c = await preparar(page)
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation(PALMAS)
    await entrar(page, c.motorista!)
    expect((await page.request.post(`/api/motorista/viagem/${c.viagemId}/iniciar`)).ok()).toBe(true)
    // Relógio controlado: o intervalo de 15 s entre envios passa sem esperar de verdade.
    await page.clock.install()
    const primeira = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await page.reload()
    expect((await primeira).ok()).toBe(true)
    await expect(transmissao(page)).toContainText('Transmitindo')

    await context.setOffline(true)
    await context.setGeolocation({ ...PALMAS, latitude: PALMAS.latitude + 0.001 })
    await page.clock.runFor(16_000)
    await expect(transmissao(page)).toContainText('Sem conexão · 1 na fila')
    await context.setGeolocation({ ...PALMAS, latitude: PALMAS.latitude + 0.002 })
    await page.clock.runFor(16_000)
    await expect(transmissao(page)).toContainText('Sem conexão · 2 na fila')

    // Sincroniza pela resposta (não pelo pedido): sob carga, o envio do segundo tique pode ter saído ainda offline e
    // ficar pendente até a rede voltar; o evento de pedido já passou, mas a resposta chega agora com o lote de 2.
    const resposta = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await context.setOffline(false)
    await page.clock.runFor(16_000)
    const lote = await resposta
    expect(lote.ok()).toBe(true)
    expect((lote.request().postDataJSON() as { posicoes: unknown[] }).posicoes).toHaveLength(2)
    await expect(transmissao(page)).toContainText('Transmitindo')
    await expect(transmissao(page)).not.toContainText('Sem conexão')
  })

  test('lote todo recusado avisa sem marcar como enviado; erro 500 mantém a fila e segue transmitindo', async ({ page, context }) => {
    await page.goto('/login')
    const c = await preparar(page)
    await context.grantPermissions(['geolocation'])
    await context.setGeolocation(PALMAS)
    await entrar(page, c.motorista!)
    expect((await page.request.post(`/api/motorista/viagem/${c.viagemId}/iniciar`)).ok()).toBe(true)
    await page.clock.install()
    const url = `**/api/motorista/viagem/${c.viagemId}/localizacao`

    // Relógio do celular errado: o servidor responde, mas rejeita tudo.
    await page.route(url, (rota) => rota.fulfill({ status: 200, json: { aceitas: 0, rejeitadas: 1, ignoradas: 0 } }))
    const recusada = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await page.reload()
    await recusada
    await expect(transmissao(page)).toContainText('Posições recusadas. Confira a hora do celular.')
    await expect(transmissao(page)).toContainText('Nenhuma posição enviada ainda.')

    // Erro do servidor (500) ou 4xx que não é "viagem fora de andamento": falha passageira, a fila espera.
    await page.unroute(url)
    await page.route(url, (rota) => rota.fulfill({ status: 500, json: { status: 500, message: 'Erro' } }))
    await context.setGeolocation({ ...PALMAS, latitude: PALMAS.latitude + 0.001 })
    await page.clock.runFor(16_000)
    await expect(transmissao(page)).toContainText('Falha ao enviar · 1 na fila')
    await page.unroute(url)
    await page.route(url, (rota) => rota.fulfill({ status: 400, json: { status: 400, message: 'Precisão deve estar entre 0 e 5000 m.' } }))
    await context.setGeolocation({ ...PALMAS, latitude: PALMAS.latitude + 0.002 })
    await page.clock.runFor(16_000)
    await expect(transmissao(page)).toContainText('Falha ao enviar · 2 na fila')
    await expect(cartao(page)).toContainText('Em andamento')

    await page.unroute(url)
    const real = respostaDe(page, `/api/motorista/viagem/${c.viagemId}/localizacao`)
    await page.clock.runFor(16_000)
    expect((await real).ok()).toBe(true)
    await expect(transmissao(page)).toContainText('Transmitindo')
    await expect(transmissao(page)).not.toContainText('Falha ao enviar')
  })

  test('tela do motorista tem estrutura HTML5 e lista vazia sem viagens hoje', async ({ page }) => {
    await page.goto('/login')
    const c = await preparar(page, { comMotorista: true, partidaEmMin: 60 * 24 * 3 })
    await entrar(page, c.motorista!)

    await expect(page.getByRole('main')).toHaveCount(1)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Minhas viagens')
    await expect(page.getByText('Nenhuma viagem para hoje.')).toBeVisible()
  })

  test('fora da janela o Iniciar fica desabilitado com o motivo em uma linha', async ({ page }) => {
    await page.goto('/login')
    // Hoje, mas a mais de 30 min da partida (quando ainda couber no dia); senão, só confere o motivo pela API.
    const c = await preparar(page, { partidaEmMin: 45 })
    await entrar(page, c.motorista!)
    const lista = (await (await page.request.get('/api/motorista/viagem')).json()) as { id: string }[]
    test.skip(lista.length === 0, 'Partida caiu no dia seguinte (perto da meia-noite).')

    await expect(cartao(page).getByRole('button', { name: 'Iniciar' })).toBeDisabled()
    await expect(cartao(page)).toContainText('A viagem pode ser iniciada a partir de 30 minutos antes da partida.')
  })
})

test.describe('Passageiro', () => {
  const bloco = (page: Page) => page.getByRole('region', { name: 'Localização da van' })

  async function reservaDoTeste(page: Page, opcoes?: Parameters<typeof prepararViagem>[1]) {
    await entrar(page, contas.passageiro)
    const c = await preparar(page, opcoes)
    const reservaId = await reservaConfirmada(page.request, c.viagemId)
    return { c, reservaId }
  }

  test('indisponível sem posição, depois atualizada com hora, precisão e mapa sem tiles', async ({ page }) => {
    const { c, reservaId } = await reservaDoTeste(page)
    await iniciarPelaOperacao(c.operador, c.viagemId)

    await page.goto(`/reservas/${reservaId}`)
    await expect(bloco(page)).toContainText('Localização indisponível')
    await expect(bloco(page).getByRole('figure')).toHaveCount(0)

    await simularPosicao(c.admin, c.viagemId, { precisaoM: 30 })
    await page.reload()
    await expect(bloco(page)).toContainText(/Atualizada às \d{2}:\d{2}/)
    await expect(bloco(page)).toContainText('± 30 m')
    await expect(bloco(page).getByText('Simulado')).toBeVisible()
    await expect(bloco(page).getByRole('figure')).toBeVisible()
    // Tiles bloqueados: o aviso aparece e a hora continua fora do mapa.
    await expect(bloco(page)).toContainText('Mapa indisponível')
    await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1)
  })

  test('mais de 2 minutos sem posição: Desatualizada, com a hora e o marcador esmaecido', async ({ page }) => {
    const { c, reservaId } = await reservaDoTeste(page)
    await iniciarPelaOperacao(c.operador, c.viagemId)
    await simularPosicao(c.admin, c.viagemId, { segundosAtras: 180 })

    await page.goto(`/reservas/${reservaId}`)
    await expect(bloco(page).getByText('Desatualizada')).toBeVisible()
    await expect(bloco(page)).toContainText(/Última posição às \d{2}:\d{2}/)
    await expect(bloco(page).getByRole('figure')).toHaveAttribute('data-desatualizada', 'true')
  })

  test('antes do início avisa quando a localização aparece; encerrada informa sem posição', async ({ page }) => {
    const { c, reservaId } = await reservaDoTeste(page)

    await page.goto(`/reservas/${reservaId}`)
    await expect(bloco(page)).toContainText('A localização aparece quando a viagem começar.')

    await iniciarPelaOperacao(c.operador, c.viagemId)
    await simularPosicao(c.admin, c.viagemId)
    await encerrarPelaOperacao(c.operador, c.viagemId)
    await page.reload()
    await expect(bloco(page)).toContainText('Viagem encerrada')
    await expect(bloco(page).getByRole('figure')).toHaveCount(0)
  })

  test('Minhas reservas mostra Em andamento e Acompanhar abre o bloco de localização; e volta', async ({ page }) => {
    const { c, reservaId } = await reservaDoTeste(page)
    await iniciarPelaOperacao(c.operador, c.viagemId)
    await simularPosicao(c.admin, c.viagemId)

    await page.goto('/reservas')
    const item = page.getByRole('listitem').filter({ has: page.locator(`a[href="/reservas/${reservaId}"]`) })
    await expect(item).toContainText('Em andamento')
    await item.getByRole('link', { name: 'Acompanhar' }).click()
    await expect(page).toHaveURL(new RegExp(`/reservas/${reservaId}#localizacao$`))
    await expect(bloco(page)).toBeInViewport()

    await page.getByRole('main').getByRole('link', { name: 'Minhas reservas' }).click()
    await expect(page).toHaveURL(/\/reservas$/)
  })

  test('falha da API na atualização mantém a última leitura e avisa que tenta de novo', async ({ page }) => {
    const { c, reservaId } = await reservaDoTeste(page)
    await iniciarPelaOperacao(c.operador, c.viagemId)
    await simularPosicao(c.admin, c.viagemId)
    await page.clock.install()
    await page.goto(`/reservas/${reservaId}`)
    await expect(bloco(page)).toContainText(/Atualizada às/)

    await page.route(`**/api/reserva/${reservaId}/localizacao`, (rota) => rota.fulfill({ status: 500, json: { status: 500, message: 'Erro' } }))
    await page.clock.runFor(16_000)
    await page.clock.runFor(5_000) // nova tentativa automática da consulta
    await expect(bloco(page)).toContainText('Não foi possível atualizar. Tentando de novo.')
    await expect(bloco(page)).toContainText(/Atualizada às/)
  })

  test('reserva de outro perfil ou de outro passageiro não expõe a localização', async ({ page }) => {
    await entrar(page, contas.passageiro)
    expect((await page.request.get('/api/reserva/00000000-0000-0000-0000-000000000000/localizacao')).status()).toBe(404)
    expect((await page.request.get('/api/motorista/viagem')).status()).toBe(403)
    await page.goto('/motorista/viagens')
    await expect(page.getByText('Acesso não permitido para o seu perfil.')).toBeVisible()

    const motorista = await contextoDe(base(page), contas.motorista.email)
    expect((await motorista.get('/api/reserva/x/localizacao')).status()).toBe(403)
    expect((await motorista.get('/api/viagem/x/localizacao')).status()).toBe(403)
    expect((await motorista.post('/api/viagem/x/iniciar')).status()).toBe(403)
    await motorista.dispose()
  })
})

test.describe('Operação', () => {
  const situacao = (page: Page) => page.getByRole('region', { name: 'Situação' })
  const localizacao = (page: Page) => page.getByRole('region', { name: 'Localização', exact: true })

  async function confirmar(page: Page, botao: string, titulo: string, acao: string) {
    await situacao(page).getByRole('button', { name: botao }).click()
    const dialogo = page.getByRole('alertdialog', { name: titulo })
    const resposta = page.waitForResponse((r) => r.url().endsWith(`/${acao}`) && r.request().method() === 'POST')
    await dialogo.getByRole('button', { name: botao }).click()
    expect((await resposta).ok()).toBe(true)
  }

  test('inicia e encerra com confirmação, vê o diagnóstico e o selo Sem sinal na listagem', async ({ page }) => {
    await entrar(page, contas.operador)
    const c = await preparar(page)

    await page.goto(`/operacao/viagens/${c.viagemId}`)
    await confirmar(page, 'Iniciar', 'Iniciar viagem?', 'iniciar')
    await expect(situacao(page)).toContainText('Em andamento')
    await expect(localizacao(page)).toContainText('Localização indisponível')
    await expect(page.getByLabel('Diagnóstico do sinal')).toContainText(/Aceitas\s*0/)
    await expect(page.getByLabel('Diagnóstico do sinal')).toContainText(/Início\s*\d{2}\/\d{2}\/\d{4}/)

    await page.goto('/operacao/viagens?situacao=EM_ANDAMENTO')
    const linha = page.getByRole('row').filter({ has: page.locator(`a[href$="${c.viagemId}"]`) })
    await expect(linha).toContainText('Em andamento')
    await expect(linha).toContainText('Sem sinal')

    await simularPosicao(c.admin, c.viagemId)
    await page.reload()
    await expect(linha).not.toContainText('Sem sinal')
    await linha.getByRole('link', { name: /Editar viagem/ }).click()
    await expect(localizacao(page)).toContainText(/Atualizada às/)
    await expect(page.getByLabel('Diagnóstico do sinal')).toContainText(/Aceitas\s*1/)
    await expect(localizacao(page).getByText('Simulado')).toBeVisible()

    await confirmar(page, 'Encerrar', 'Encerrar viagem?', 'encerrar')
    await expect(situacao(page)).toContainText('Encerrada')
    await expect(localizacao(page)).toContainText('Viagem encerrada')
    await expect(page.getByRole('button', { name: 'Salvar' })).toHaveCount(0)
  })

  test('viagem em andamento sem motorista avisa que a localização está indisponível', async ({ page }) => {
    await entrar(page, contas.operador)
    const c = await preparar(page, { comMotorista: false })
    await iniciarPelaOperacao(c.operador, c.viagemId)

    await page.goto(`/operacao/viagens/${c.viagemId}`)
    await expect(localizacao(page)).toContainText('Sem motorista: localização indisponível')
  })
})
