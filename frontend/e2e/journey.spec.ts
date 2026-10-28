import { expect, test, type Page } from '@playwright/test'

/**
 * Jornada completa de um(a) estudante, da interface à persistência e de volta à interface.
 * Cada execução cria um usuário novo, então pode ser repetida sobre o mesmo banco.
 */
test.describe.serial('jornada principal do estudante', () => {
  const id = Date.now().toString(36)
  const email = `e2e_${id}@teste.com`
  const password = 'SenhaE2e2026'
  const nickname = `e2e_${id}`
  let page: Page

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage()
  })

  test.afterAll(async () => {
    await page.close()
  })

  test('1. cadastro, logout e login', async () => {
    await page.goto('/')
    await page.getByRole('link', { name: 'Criar conta' }).first().click()
    await page.getByLabel('E-mail').fill(email)
    await page.getByLabel('Apelido público').fill(nickname)
    await page.getByLabel('Senha', { exact: false }).first().fill(password)
    await page.getByLabel('Confirme a senha').fill(password)
    await page.getByLabel(/Li e aceito os termos/).check()
    await page.getByLabel(/Tenho 12 anos ou mais/).check()
    await page.getByRole('button', { name: 'Criar conta' }).click()
    await expect(page.getByRole('heading', { name: `Olá, ${nickname}!` })).toBeVisible()

    await page.getByRole('button', { name: /Sair/ }).click()
    await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible()
    await page.getByLabel('E-mail').fill(email)
    await page.getByLabel('Senha').fill('senhaErrada1')
    await page.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(page.getByText('E-mail ou senha inválidos.')).toBeVisible()
    await page.getByLabel('Senha').fill(password)
    await page.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(page.getByRole('heading', { name: `Olá, ${nickname}!` })).toBeVisible()
  })

  test('2. criação de uma meta', async () => {
    await page.getByRole('link', { name: 'Metas', exact: true }).click()
    await page.getByRole('button', { name: 'Nova meta' }).click()
    const dialog = page.getByRole('dialog', { name: 'Nova meta' })
    await dialog.getByLabel('Nome da meta').fill('Fone bluetooth')
    await dialog.getByLabel('Valor desejado (R$)').fill('200,00')
    await dialog.getByLabel('Categoria').selectOption('ELECTRONICS')
    await dialog.getByRole('button', { name: 'Criar meta' }).click()
    await expect(page.getByText('Meta criada!')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Fone bluetooth' })).toBeVisible()
  })

  test('3. registro de contribuição e conclusão automática', async () => {
    await page.getByRole('heading', { name: 'Fone bluetooth' }).click()
    await page.getByLabel('Valor (R$)').fill('150')
    await page.getByRole('button', { name: 'Adicionar à meta' }).click()
    await expect(page.getByText('Contribuição registrada!')).toBeVisible()
    await expect(page.getByText('75%')).toBeVisible()
    await page.getByLabel('Valor (R$)').fill('50')
    await page.getByRole('button', { name: 'Adicionar à meta' }).click()
    await expect(page.getByText(/Meta concluída em/)).toBeVisible()
    await expect(page.getByRole('list').getByText('Contribuição')).toHaveCount(2)
  })

  test('4. registro de transações', async () => {
    await page.getByRole('link', { name: 'Transações' }).click()
    for (const [type, desc, value, category] of [
      ['Receita', 'Mesada de outubro', '300', 'ALLOWANCE'],
      ['Despesa', 'Lanche na escola', '25,50', 'FOOD'],
    ]) {
      await page.getByRole('button', { name: 'Nova transação' }).click()
      const dialog = page.getByRole('dialog')
      await dialog.getByRole('radio', { name: type }).click()
      await dialog.getByLabel('Descrição').fill(desc)
      await dialog.getByLabel('Valor (R$)').fill(value)
      await dialog.getByLabel('Categoria').selectOption(category)
      await dialog.getByRole('button', { name: 'Registrar' }).click()
      await expect(page.getByRole('cell', { name: desc, exact: true })).toBeVisible()
    }
    await expect(page.getByText('R$ 274,50')).toBeVisible()
  })

  test('5. dashboard reflete os dados persistidos', async () => {
    await page.getByRole('link', { name: 'Painel' }).click()
    await expect(page.getByText('Guardado nas metas')).toBeVisible()
    await expect(page.getByText('R$ 200,00').first()).toBeVisible()
    await expect(page.getByText('R$ 274,50').first()).toBeVisible()
    await expect(page.getByText(/Meta concluída: Fone bluetooth|Movimentação registrada/).first()).toBeVisible()
  })

  test('6. execução de um quiz com nota calculada no servidor', async () => {
    await page.getByRole('link', { name: 'Quizzes' }).click()
    await page.getByRole('link', { name: /Começar/ }).first().click()
    const total = Number((await page.getByText(/Pergunta 1 de \d+/).textContent())!.match(/de (\d+)/)![1])
    for (let i = 0; i < total; i++) {
      await page.getByRole('radio').first().click()
      await page.getByRole('button', { name: 'Responder' }).click()
      await expect(page.getByText(/Você acertou!|Não foi dessa vez./)).toBeVisible()
      if (i < total - 1) await page.getByRole('button', { name: 'Próxima pergunta' }).click()
    }
    await page.getByRole('button', { name: 'Ver resultado' }).click()
    await expect(page.getByRole('heading', { name: /Resultado:/ })).toBeVisible()
    await expect(page.getByText('Revisão das respostas')).toBeVisible()
    await expect(page.getByText(/de \d+$/).first()).toBeVisible()
  })

  test('7. inscrição e progresso em um curso', async () => {
    await page.getByRole('link', { name: 'Cursos' }).click()
    await page.getByRole('link', { name: 'Primeiros passos com o dinheiro' }).click()
    await page.getByRole('button', { name: 'Inscrever-se gratuitamente' }).click()
    await expect(page.getByText('Inscrição feita! Bons estudos.')).toBeVisible()
    await page.getByRole('link', { name: 'Começar a primeira aula' }).click()
    await expect(page.getByRole('heading', { name: 'Para que serve o dinheiro' })).toBeVisible()
    await page.getByLabel('Anotações privadas').fill('Pague-se primeiro!')
    await page.getByRole('button', { name: 'Salvar anotação' }).click()
    await expect(page.getByText('Anotação salva.')).toBeVisible()
    await page.getByRole('button', { name: 'Marcar como concluída' }).click()
    await expect(page.getByText('Aula concluída!')).toBeVisible()
    await expect(page.getByText('+20 pontos!')).toBeVisible() // 10 da aula + 10 da conquista "Primeira aula"
    await expect(page.getByText(/Conquista desbloqueada: Primeira aula/)).toBeVisible()
    await page.getByRole('link', { name: /Primeiros passos com o dinheiro/ }).first().click()
    await expect(page.getByText('1/4 aulas')).toBeVisible()
  })

  test('8. relatórios calculados a partir dos registros', async () => {
    await page.getByRole('link', { name: 'Relatórios' }).click()
    await expect(page.getByRole('heading', { name: /Finanças/ })).toBeVisible()
    await expect(page.getByText('R$ 300,00').first()).toBeVisible()
    await expect(page.getByText('Despesas por categoria')).toBeVisible()
    await expect(page.getByText('Critérios de cálculo')).toBeVisible()
  })

  test('9. calculadora de juros', async () => {
    await page.getByRole('link', { name: 'Calculadora' }).click()
    await page.getByLabel('Valor inicial (R$)').fill('100')
    await page.getByLabel('Taxa de juros (%)').fill('10')
    await page.getByLabel('Período', { exact: true }).fill('2')
    await page.getByLabel('Aporte mensal (R$)').fill('0')
    await page.getByRole('button', { name: 'Calcular' }).click()
    await expect(page.getByText('R$ 121,00')).toBeVisible()
    await page.getByLabel('Tipo de juros').selectOption('SIMPLE')
    await page.getByRole('button', { name: 'Calcular' }).click()
    await expect(page.getByText('R$ 120,00')).toBeVisible()
  })

  test('10. criação de uma publicação na comunidade', async () => {
    await page.getByRole('link', { name: 'Comunidade' }).click()
    await page.getByRole('button', { name: 'Nova publicação' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Título').fill(`Dica ${id}: separe a poupança primeiro`)
    await dialog.getByLabel('Texto').fill('Quando recebo a mesada, separo 20% antes de gastar. Funcionou muito bem!')
    await dialog.getByRole('button', { name: 'Publicar' }).click()
    await expect(page.getByText('Publicação criada!')).toBeVisible()
    await page.getByRole('link', { name: `Dica ${id}: separe a poupança primeiro` }).click()
    await page.getByLabel('Seu comentário').fill('Vou testar também!')
    await page.getByRole('button', { name: 'Comentar' }).click()
    await expect(page.getByText('Vou testar também!')).toBeVisible()

    // Proteção: dados de contato são bloqueados
    await page.getByRole('link', { name: 'Comunidade' }).first().click()
    await page.getByRole('button', { name: 'Nova publicação' }).click()
    await page.getByRole('dialog').getByLabel('Título').fill('Me chama no zap')
    await page.getByRole('dialog').getByLabel('Texto').fill('meu número é 11 98765-4321, me chama')
    await page.getByRole('dialog').getByRole('button', { name: 'Publicar' }).click()
    await expect(page.getByText(/não publique e-mail, telefone ou documentos/)).toBeVisible()
    await page.getByRole('dialog').getByRole('button', { name: 'Cancelar' }).click()
  })

  test('11. consulta de notificações', async () => {
    await page.getByRole('link', { name: 'Notificações' }).first().click()
    await expect(page.getByRole('heading', { name: 'Notificações' })).toBeVisible()
    await expect(page.getByText('Meta alcançada! 🎉')).toBeVisible()
    await expect(page.getByText('Boas-vindas ao EduFin!')).toBeVisible()
    await page.getByRole('button', { name: 'Marcar todas como lidas' }).click()
    await expect(page.getByText('Todas marcadas como lidas.')).toBeVisible()
    await page.getByRole('tab', { name: 'Não lidas' }).click()
    await expect(page.getByText('Nenhuma notificação')).toBeVisible()
  })

  test('12. assistente educativo e carteira simulada', async () => {
    await page.getByRole('link', { name: 'Assistente' }).click()
    await page.getByLabel('Sua pergunta').fill('o que é inflação?')
    await page.getByRole('button', { name: 'Enviar pergunta' }).click()
    await expect(page.getByText(/aumento geral dos preços/)).toBeVisible()

    await page.getByRole('link', { name: 'Carteiras simuladas' }).click()
    await page.getByRole('button', { name: 'Nova carteira' }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByLabel('Nome').fill('Laboratório')
    await dialog.getByLabel('Saldo inicial simulado (R$)').fill('500')
    await dialog.getByRole('button', { name: 'Criar carteira' }).click()
    await expect(page.getByText('Carteira criada!')).toBeVisible()
    await page.getByRole('heading', { name: 'Laboratório' }).click()
    await expect(page.getByText(/Carteira SIMULADA para fins educativos/)).toBeVisible()
  })
})

test('acesso direto a página privada sem sessão leva ao login', async ({ page }) => {
  await page.goto('/relatorios')
  await expect(page.getByRole('heading', { name: 'Entrar' })).toBeVisible()
})
