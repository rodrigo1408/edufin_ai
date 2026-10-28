import { expect, test, type Page } from '@playwright/test'

const pages = ['/painel', '/cursos', '/assistente', '/quizzes', '/metas', '/transacoes', '/calculadora', '/relatorios', '/comunidade', '/perfil']

async function expectNoHorizontalScroll(page: Page, path: string) {
  await page.goto(path)
  await page.waitForLoadState('networkidle')
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow, `rolagem horizontal em ${path} (${page.viewportSize()?.width}px)`).toBeLessThanOrEqual(1)
}

/** Layout em celular e tablet: menu recolhível e nenhuma rolagem horizontal nas páginas principais. */
test('layout responsivo em celular e tablet', async ({ page }) => {
  const id = Date.now().toString(36)
  await page.goto('/')
  const landingOverflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(landingOverflow, 'rolagem horizontal na página inicial').toBeLessThanOrEqual(1)

  await page.goto('/cadastro')
  await page.getByLabel('E-mail').fill(`mob_${id}@teste.com`)
  await page.getByLabel('Apelido público').fill(`mob_${id}`)
  await page.getByLabel('Senha', { exact: false }).first().fill('SenhaMobile1')
  await page.getByLabel('Confirme a senha').fill('SenhaMobile1')
  await page.getByLabel(/Li e aceito os termos/).check()
  await page.getByLabel(/Tenho 12 anos ou mais/).check()
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page.getByRole('heading', { name: `Olá, mob_${id}!` })).toBeVisible()

  for (const path of pages) await expectNoHorizontalScroll(page, path)

  await page.getByRole('button', { name: 'Abrir menu' }).click()
  await page.getByRole('link', { name: 'Metas', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Metas financeiras' })).toBeVisible()

  await page.getByRole('link', { name: 'Abrir o Tutor IA' }).click()
  await expect(page.getByRole('heading', { name: 'Tutor IA' })).toBeVisible()

  await page.setViewportSize({ width: 820, height: 1180 })
  for (const path of pages) await expectNoHorizontalScroll(page, path)
})
