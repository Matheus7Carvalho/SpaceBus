import { expect, type APIRequestContext, type Page } from '@playwright/test'

/**
 * Helpers genéricos das telas de cadastro (CadastroForm): salvar e permanecer, voltar à lista,
 * diálogo de descarte e limpeza via API.
 */

/** Ids fixos da seed de dev (backend/src/main/resources/db/changes). */
export const SEED = {
  rotaCentroAeroporto: '8b2c0000-0001-4d00-9000-000000000001',
  vanSprinter: '9a1b0000-0001-4c00-8000-000000000001',
  vanMaster: '9a1b0000-0001-4c00-8000-000000000002',
  viagemPublicada: '7d3e0000-0001-4e00-a000-000000000001',
  viagemPublicada12h30: '7d3e0000-0001-4e00-a000-000000000002',
  viagemCancelada: '7d3e0000-0001-4e00-a000-000000000004',
} as const

/** Prefixo dos registros criados pelos testes (limpeza no afterEach e no global-teardown). */
export const PREFIXO_E2E = 'E2E'

/**
 * Van só dos testes (criada no global-setup). Viagens criadas pela API usam esta van: com a descrição marcada,
 * a limpeza do teardown sabe que são de teste, inclusive as canceladas, que a regra não deixa excluir.
 */
export const VAN_E2E = { placa: 'TST9E99', descricao: `${PREFIXO_E2E} Van de testes`, tipo: 'VAN', capacidade: 15, ativo: true } as const

export async function idDaVanE2E(request: APIRequestContext) {
  const resposta = await request.get('/api/veiculo?size=500')
  expect(resposta.ok()).toBe(true)
  const { content } = (await resposta.json()) as { content: { id: string; placa: string }[] }
  const van = content.find((v) => v.placa === VAN_E2E.placa)
  if (!van) throw new Error(`Van ${VAN_E2E.placa} não encontrada: o global-setup deveria tê-la criado.`)
  return van.id
}

export function sufixoUnico(tamanho = 6) {
  return (Date.now().toString(36) + Math.random().toString(36).slice(2)).slice(-tamanho).toUpperCase()
}

/** Salva e confere que a tela permaneceu aberta em modo edição. Retorna o id do registro. */
export async function salvarCadastro(page: Page, opcoes: { rotulo: string; recurso: string; lista: string; mensagem: string }) {
  const resposta = page.waitForResponse(
    (r) => new RegExp(`/api/${opcoes.recurso}(/[^/?]+)?$`).test(new URL(r.url()).pathname) && ['POST', 'PUT'].includes(r.request().method()),
  )
  await page.getByRole('button', { name: 'Salvar' }).click()
  expect((await resposta).ok()).toBe(true)
  await expect(page.locator(`form[aria-label="${opcoes.rotulo}"]`)).toHaveAttribute('data-alterado', 'false')
  await expect(page.getByText(opcoes.mensagem).last()).toBeVisible()
  await expect(page).toHaveURL(new RegExp(`${opcoes.lista}/[0-9a-f-]{36}$`))
  return page.url().split('/').pop()!
}

export async function voltarParaLista(page: Page, lista: string) {
  await page.getByRole('button', { name: /^(Cancelar|Voltar)$/ }).click()
  await expect(page).toHaveURL(new RegExp(`${lista}$`))
}

export function dialogoDescarte(page: Page) {
  return page.getByRole('alertdialog', { name: 'Descartar alterações?' })
}

export function linhaCom(page: Page, texto: string) {
  return page.getByRole('row').filter({ hasText: texto })
}

/** Escolhe uma opção num Select (Radix) pelo rótulo do campo. */
export async function escolher(page: Page, rotulo: string, opcao: string | RegExp) {
  await page.getByLabel(rotulo, { exact: true }).click()
  await page.getByRole('option', { name: opcao }).click()
}

/** Apaga pela API os registros de um recurso que satisfazem o filtro (usa a sessão do navegador). */
export async function excluirViaApi<T extends { id: string }>(page: Page, recurso: string, filtro: (registro: T) => boolean) {
  const resposta = await page.request.get(`/api/${recurso}?size=500`)
  if (!resposta.ok()) return
  const { content } = (await resposta.json()) as { content: T[] }
  for (const registro of content.filter(filtro)) {
    await page.request.delete(`/api/${recurso}/${registro.id}`)
  }
}
