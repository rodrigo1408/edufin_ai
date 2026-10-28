import { expect, test } from '@playwright/test'

/** Layout em celular: menu recolhível e nenhuma rolagem horizontal nas páginas principais. */
test('layout responsivo em celular', async ({ page }) => {
  const id = Date.now().toString(36)
  await page.goto('/cadastro')
  await page.getByLabel('E-mail').fill(`mob_${id}@teste.com`)
  await page.getByLabel('Apelido público').fill(`mob_${id}`)
  await page.getByLabel('Senha', { exact: false }).first().fill('SenhaMobile1')
  await page.getByLabel('Confirme a senha').fill('SenhaMobile1')
  await page.getByLabel(/Li e aceito os termos/).check()
  await page.getByLabel(/Tenho 12 anos ou mais/).check()
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page.getByRole('heading', { name: `Olá, mob_${id}!` })).toBeVisible()

  for (const path of ['/painel', '/metas', '/transacoes', '/calculadora', '/cursos', '/quizzes', '/relatorios', '/comunidade', '/perfil']) {
    await page.goto(path)
    await page.waitForLoadState('networkidle')
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
    expect(overflow, `rolagem horizontal em ${path}`).toBeLessThanOrEqual(1)
  }

  await page.getByRole('button', { name: 'Abrir menu' }).click()
  await page.getByRole('link', { name: 'Metas', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Metas financeiras' })).toBeVisible()
})
