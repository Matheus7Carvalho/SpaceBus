import { expect, test, type Page } from '@playwright/test'
import { contas, entrar, menuDoUsuario, pedirSaida, preencherLogin, sair, SENHA_DEMO } from './support/auth'

/** Itens do menu lateral. No celular o menu fica numa gaveta aberta pelo botão "Alternar menu". */
async function navegacao(page: Page, isMobile: boolean) {
  if (isMobile) await page.getByRole('button', { name: 'Alternar menu' }).click()
  return page.getByRole('navigation', { name: 'Principal' })
}

test.describe('Tela de login', () => {
  test('rota interna sem sessão leva ao login', async ({ page }) => {
    await page.goto('/operacao/viagens')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Entrar' })).toBeVisible()
  })

  test('formulário usa campos HTML5 com rótulo e validação nativa', async ({ page }) => {
    await page.goto('/login')

    const email = page.getByLabel('E-mail')
    const senha = page.getByLabel('Senha')
    await expect(email).toHaveAttribute('type', 'email')
    await expect(email).toHaveAttribute('required', '')
    await expect(senha).toHaveAttribute('type', 'password')
    await expect(senha).toHaveAttribute('required', '')
    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-BR')

    await page.getByRole('button', { name: 'Entrar', exact: true }).click()

    expect(await email.evaluate((el: HTMLInputElement) => el.validity.valueMissing)).toBe(true)
    await expect(page).toHaveURL(/\/login$/)
  })

  test('e-mail em formato inválido é bloqueado pelo navegador', async ({ page }) => {
    await page.goto('/login')

    await preencherLogin(page, 'admin', SENHA_DEMO)

    expect(await page.getByLabel('E-mail').evaluate((el: HTMLInputElement) => el.validity.typeMismatch)).toBe(true)
    await expect(page).toHaveURL(/\/login$/)
  })

  test('senha errada mostra erro e mantém no login', async ({ page }) => {
    await page.goto('/login')

    await preencherLogin(page, contas.operador.email, 'senha-errada')

    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos.')
    await expect(page).toHaveURL(/\/login$/)
  })

  test('e-mail inexistente mostra a mesma mensagem de senha errada', async ({ page }) => {
    await page.goto('/login')

    await preencherLogin(page, 'ninguem@spacebus.local', SENHA_DEMO)

    await expect(page.getByRole('alert')).toHaveText('E-mail ou senha inválidos.')
  })
})

test.describe('Login por perfil', () => {
  for (const conta of Object.values(contas)) {
    test(`${conta.perfil} entra pelo formulário e vê nome e perfil`, async ({ page, isMobile }) => {
      await entrar(page, conta)

      // Antes de abrir o menu: com ele aberto, o resto da página fica aria-hidden.
      if (!isMobile) await expect(page.getByRole('banner')).toContainText(`${conta.nome}${conta.perfil}`)

      await menuDoUsuario(page, conta).click()
      const menu = page.getByRole('menu')
      await expect(menu).toContainText(conta.nome)
      await expect(menu).toContainText(conta.email)
    })

    test(`${conta.perfil} entra pelo acesso de demonstração`, async ({ page }) => {
      await page.goto('/login')

      await page.getByRole('button', { name: conta.perfil, exact: true }).click()

      await expect(menuDoUsuario(page, conta)).toBeVisible()
      await expect(page).not.toHaveURL(/\/login$/)
    })
  }
})

test.describe('Permissões por perfil', () => {
  test('Administrador e Operador acessam Viagens, Rotas e Veículos', async ({ page, isMobile }) => {
    for (const conta of [contas.administrador, contas.operador]) {
      await entrar(page, conta)

      await expect(page).toHaveURL(/\/operacao\/viagens$/)
      await expect(page.getByRole('heading', { level: 1, name: 'Viagens' })).toBeVisible()
      const menu = await navegacao(page, isMobile)
      for (const item of ['Viagens', 'Rotas', 'Veículos']) {
        await expect(menu.getByRole('link', { name: item, exact: true })).toBeVisible()
      }

      await page.keyboard.press('Escape')
      await sair(page, conta)
    }
  })

  for (const conta of [contas.motorista, contas.passageiro]) {
    test(`${conta.perfil} entra na busca, não vê a operação no menu e é bloqueado pela URL`, async ({ page, isMobile }) => {
      await entrar(page, conta)

      await expect(page).toHaveURL(/\/buscar$/)
      await expect(page.getByRole('heading', { level: 1, name: 'Buscar viagens' })).toBeVisible()
      const menu = await navegacao(page, isMobile)
      await expect(menu.getByRole('link', { name: 'Buscar viagens' })).toBeVisible()
      for (const restrito of ['Viagens', 'Rotas', 'Veículos']) {
        await expect(menu.getByRole('link', { name: restrito, exact: true })).toHaveCount(0)
      }
      await page.keyboard.press('Escape')

      for (const rota of ['/operacao/viagens', '/cadastros/rotas/nova', '/cadastros/veiculos']) {
        await page.goto(rota)
        await expect(page.getByText('Acesso não permitido para o seu perfil.')).toBeVisible()
      }
      expect((await page.request.get('/api/viagem')).status()).toBe(403)
    })
  }
})

test.describe('Sessão', () => {
  test('volta à rota de origem depois do login', async ({ page }) => {
    await page.goto('/operacao/viagens')
    await expect(page).toHaveURL(/\/login$/)

    await preencherLogin(page, contas.operador.email, SENHA_DEMO)

    await expect(page).toHaveURL(/\/operacao\/viagens$/)
  })

  test('ignora rota de origem não permitida ao perfil', async ({ page }) => {
    await page.goto('/operacao/viagens')

    await preencherLogin(page, contas.passageiro.email, SENHA_DEMO)

    // Vai para o primeiro módulo permitido (busca), não para a rota de origem.
    await expect(page).toHaveURL(/\/buscar$/)
  })

  test('sessão continua após recarregar a página', async ({ page }) => {
    await entrar(page, contas.operador)

    await page.reload()

    await expect(menuDoUsuario(page, contas.operador)).toBeVisible()
  })

  test('cookie de sessão é HttpOnly e SameSite=Strict', async ({ page, context }) => {
    await entrar(page, contas.operador)

    const cookie = (await context.cookies()).find((c) => c.name === 'JSESSIONID')
    expect(cookie?.httpOnly).toBe(true)
    expect(cookie?.sameSite).toBe('Strict')
    expect(await page.evaluate(() => document.cookie)).not.toContain('JSESSIONID')
  })

  test('usuário logado que abre /login vai para o sistema', async ({ page }) => {
    await entrar(page, contas.administrador)

    await page.goto('/login')

    await expect(page).toHaveURL(/\/operacao\/viagens$/)
  })

  test('sair encerra a sessão e protege as rotas novamente', async ({ page }) => {
    await entrar(page, contas.operador)

    await sair(page, contas.operador)
    await page.goto('/operacao/viagens')

    await expect(page).toHaveURL(/\/login$/)
    expect((await page.request.get('/api/auth/sessao')).status()).toBe(401)
  })

  test('sair pede confirmação e cancelar mantém a sessão', async ({ page }) => {
    await entrar(page, contas.operador)
    const url = page.url()

    const dialogo = await pedirSaida(page, contas.operador)
    await expect(dialogo.getByRole('button', { name: 'Cancelar' })).toBeFocused()
    await dialogo.getByRole('button', { name: 'Cancelar' }).click()

    await expect(dialogo).toBeHidden()
    await expect(page).toHaveURL(url)
    expect((await page.request.get('/api/auth/sessao')).status()).toBe(200)
  })

  test('Esc fecha a confirmação sem sair', async ({ page }) => {
    await entrar(page, contas.administrador)

    const dialogo = await pedirSaida(page, contas.administrador)
    await page.keyboard.press('Escape')

    await expect(dialogo).toBeHidden()
    await expect(menuDoUsuario(page, contas.administrador)).toBeVisible()
    expect((await page.request.get('/api/auth/sessao')).status()).toBe(200)
  })

  test('menu continua utilizável depois de cancelar a saída', async ({ page }) => {
    await entrar(page, contas.operador)

    const dialogo = await pedirSaida(page, contas.operador)
    await dialogo.getByRole('button', { name: 'Cancelar' }).click()

    await sair(page, contas.operador)
  })

  test('sessão expirada no servidor leva ao login', async ({ page, context }) => {
    await entrar(page, contas.operador)

    await context.clearCookies()
    await page.reload()

    await expect(page).toHaveURL(/\/login$/)
  })

  test('401 numa chamada da tela encerra a sessão no cliente', async ({ page }) => {
    await page.route('**/api/viagem**', (route) =>
      route.fulfill({ status: 401, json: { status: 401, message: 'Sessão expirada. Entre novamente.' } }),
    )
    await page.goto('/login')

    await preencherLogin(page, contas.operador.email, SENHA_DEMO)

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('heading', { level: 1, name: 'Entrar' })).toBeVisible()
  })

  test('API sem sessão responde 401', async ({ page }) => {
    expect((await page.request.get('/api/viagem')).status()).toBe(401)
  })
})
