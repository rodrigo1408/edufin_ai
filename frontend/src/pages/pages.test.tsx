import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { AttemptView, Goal } from '../api/types'
import { renderApp } from '../test/render'
import { emptyPage, ME, server } from '../test/server'

const goal: Goal = {
  id: 7, title: 'Fone novo', description: null, category: 'ELECTRONICS', targetAmount: 300, savedAmount: 75, remainingAmount: 225,
  progressPercent: 25, deadline: null, daysRemaining: null, suggestedMonthlySaving: null, status: 'ACTIVE', createdAt: '2026-10-01T00:00:00Z', completedAt: null,
}
const noReward = { points: 0, achievements: [], challenges: [] }

describe('navegação e autenticação', () => {
  it('redireciona visitante não autenticado para o login', async () => {
    server.use(http.get('/api/me', () => HttpResponse.json({ code: 'UNAUTHENTICATED' }, { status: 401 })))
    renderApp('/painel')
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('faz login e abre o painel', async () => {
    let authenticated = false
    server.use(
      http.get('/api/me', () => (authenticated ? HttpResponse.json(ME) : HttpResponse.json({ code: 'UNAUTHENTICATED' }, { status: 401 }))),
      http.post('/api/auth/login', async ({ request }) => {
        const body = (await request.json()) as { email: string; password: string }
        if (body.password !== 'Senha123') return HttpResponse.json({ code: 'BAD_CREDENTIALS', message: 'E-mail ou senha inválidos.' }, { status: 401 })
        authenticated = true
        return HttpResponse.json(ME)
      }),
      http.get('/api/dashboard', () => HttpResponse.json(emptyDashboard())),
    )
    const user = userEvent.setup()
    renderApp('/entrar')
    await screen.findByRole('heading', { name: 'Entrar' })

    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('Informe e-mail e senha.')).toBeInTheDocument()

    await user.type(screen.getByLabelText(/E-mail/), 'ana@teste.com')
    await user.type(screen.getByLabelText(/Senha/), 'errada1')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(await screen.findByText('E-mail ou senha inválidos.')).toBeInTheDocument()

    await user.clear(screen.getByLabelText(/Senha/))
    await user.type(screen.getByLabelText(/Senha/), 'Senha123')
    await user.click(screen.getByRole('button', { name: 'Entrar' }))
    // Primeira carga do chunk (lazy) do painel: pode levar mais que o timeout padrão de 1 s.
    expect(await screen.findByRole('heading', { name: 'Olá, ana!' }, { timeout: 8000 })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Atalhos' })).toBeInTheDocument()
  })

  it('valida o formulário de cadastro antes de enviar', async () => {
    server.use(http.get('/api/me', () => HttpResponse.json({}, { status: 401 })))
    const user = userEvent.setup()
    renderApp('/cadastro')
    await user.click(await screen.findByRole('button', { name: 'Criar conta' }))
    expect(screen.getByText('Informe um e-mail válido.')).toBeInTheDocument()
    expect(screen.getByText('Use de 8 a 72 caracteres, com letras e números.')).toBeInTheDocument()
    expect(screen.getByText('É necessário aceitar os termos.')).toBeInTheDocument()
  })
})

describe('painel', () => {
  it('mostra estados vazios com orientações para novos usuários', async () => {
    server.use(http.get('/api/dashboard', () => HttpResponse.json(emptyDashboard())))
    renderApp('/painel')
    expect(await screen.findByText('Nenhuma meta ativa')).toBeInTheDocument()
    expect(screen.getByText('Sem movimentações ainda')).toBeInTheDocument()
    expect(screen.getByText('Nenhum quiz feito')).toBeInTheDocument()
  })

  it('exibe erro de comunicação com opção de tentar novamente', async () => {
    let calls = 0
    server.use(
      http.get('/api/dashboard', () => {
        calls++
        return calls === 1 ? HttpResponse.json({ code: 'INTERNAL_ERROR', message: 'Ocorreu um erro inesperado.' }, { status: 500 }) : HttpResponse.json(emptyDashboard())
      }),
    )
    const user = userEvent.setup()
    renderApp('/painel')
    expect(await screen.findByRole('alert')).toHaveTextContent('Ocorreu um erro inesperado.')
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))
    expect(await screen.findByText('Nenhuma meta ativa')).toBeInTheDocument()
  })
})

describe('metas', () => {
  it('cria uma meta pelo formulário e atualiza a lista', async () => {
    const goals: Goal[] = []
    let posted: unknown = null
    server.use(
      http.get('/api/goals', () => HttpResponse.json(goals)),
      http.post('/api/goals', async ({ request }) => {
        posted = await request.json()
        goals.push({ ...goal, savedAmount: 0, progressPercent: 0 })
        return HttpResponse.json({ goal, reward: { points: 5, achievements: [], challenges: [] } }, { status: 201 })
      }),
      http.get('/api/dashboard', () => HttpResponse.json(emptyDashboard())),
    )
    const user = userEvent.setup()
    renderApp('/metas')
    expect(await screen.findByText('Nenhuma meta ativa')).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: /Nova meta/ })[0])
    const dialog = await screen.findByRole('dialog', { name: 'Nova meta' })
    await user.click(within(dialog).getByRole('button', { name: 'Criar meta' }))
    expect(within(dialog).getByText('Dê um nome para a meta.')).toBeInTheDocument()

    await user.type(within(dialog).getByLabelText(/Nome da meta/), 'Fone novo')
    await user.type(within(dialog).getByLabelText(/Valor desejado/), '300,00')
    await user.click(within(dialog).getByRole('button', { name: 'Criar meta' }))

    expect(await screen.findByText('Meta criada!')).toBeInTheDocument()
    expect(await screen.findByText('+5 pontos!')).toBeInTheDocument()
    expect(posted).toMatchObject({ title: 'Fone novo', targetAmount: 300, category: 'OTHER' })
    expect(await screen.findByRole('heading', { name: 'Fone novo' })).toBeInTheDocument()
  })

  it('pede confirmação antes de cancelar uma meta', async () => {
    let cancelled = false
    server.use(
      http.get('/api/goals/7', () => HttpResponse.json(cancelled ? { ...goal, status: 'CANCELLED' } : goal)),
      http.get('/api/goals/7/movements', () => HttpResponse.json(emptyPage)),
      http.post('/api/goals/7/cancel', () => { cancelled = true; return HttpResponse.json({ ...goal, status: 'CANCELLED' }) }),
    )
    const user = userEvent.setup()
    renderApp('/metas/7')
    await user.click(await screen.findByRole('button', { name: /Cancelar meta/ }))
    const dialog = await screen.findByRole('dialog', { name: 'Cancelar meta?' })
    expect(cancelled).toBe(false)
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar meta' }))
    expect(await screen.findByText(/Esta meta foi cancelada/)).toBeInTheDocument()
    expect(cancelled).toBe(true)
  })

  it('impede retirada maior que o saldo antes de chamar a API', async () => {
    server.use(
      http.get('/api/goals/7', () => HttpResponse.json(goal)),
      http.get('/api/goals/7/movements', () => HttpResponse.json(emptyPage)),
    )
    const user = userEvent.setup()
    renderApp('/metas/7')
    await user.click(await screen.findByRole('radio', { name: 'Retirada' }))
    await user.type(screen.getByLabelText(/Valor \(R\$\)/), '100')
    await user.click(screen.getByRole('button', { name: 'Registrar retirada' }))
    expect(await screen.findByText(/Você só pode retirar até/)).toBeInTheDocument()
  })
})

describe('quiz', () => {
  const attempt: AttemptView = {
    attemptId: 50, quizId: 3, quizTitle: 'Juros', status: 'IN_PROGRESS', answered: [],
    questions: [
      { id: 1, position: 1, statement: 'O que são juros?', options: [{ id: 11, text: 'Preço do dinheiro no tempo' }, { id: 12, text: 'Um imposto' }] },
      { id: 2, position: 2, statement: 'Juros compostos são…', options: [{ id: 21, text: 'Juros sobre juros' }, { id: 22, text: 'Sempre menores' }] },
    ],
  }

  it('mostra a correção vinda do servidor só depois de responder', async () => {
    let answerBody: unknown = null
    server.use(
      http.post('/api/quizzes/3/attempts', () => HttpResponse.json(attempt)),
      http.post('/api/quiz-attempts/50/answers', async ({ request }) => {
        answerBody = await request.json()
        return HttpResponse.json({ questionId: 1, selectedOptionId: 12, correct: false, correctOptionId: 11, explanation: 'Juros remuneram o tempo do dinheiro.' })
      }),
    )
    const user = userEvent.setup()
    renderApp('/quizzes/3')
    expect(await screen.findByRole('heading', { name: 'O que são juros?' })).toBeInTheDocument()
    expect(screen.queryByText(/alternativa correta/)).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Responder' })).toBeDisabled()

    await user.click(screen.getByRole('radio', { name: 'Um imposto' }))
    await user.click(screen.getByRole('button', { name: 'Responder' }))
    expect(await screen.findByText('Não foi dessa vez.')).toBeInTheDocument()
    expect(screen.getByText('Juros remuneram o tempo do dinheiro.')).toBeInTheDocument()
    expect(screen.getByText('(alternativa correta)')).toBeInTheDocument()
    expect(answerBody).toEqual({ questionId: 1, optionId: 12 })
    expect(screen.getByRole('button', { name: 'Próxima pergunta' })).toBeInTheDocument()
  })
})

describe('calculadora', () => {
  it('envia os parâmetros ao servidor e exibe o resultado', async () => {
    server.use(
      http.get('/api/calculator/simulations', () => HttpResponse.json([])),
      http.post('/api/calculator/calculate', async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        expect(body).toMatchObject({ interestType: 'COMPOUND', principal: 100, ratePercent: 1, period: 12 })
        return HttpResponse.json({
          input: body,
          result: { finalAmount: 746.9, totalInvested: 700, totalInterest: 46.9, monthlyRatePercent: 1, months: 12, series: [{ month: 0, invested: 100, interest: 0, balance: 100 }, { month: 12, invested: 700, interest: 46.9, balance: 746.9 }] },
          assumptions: ['Simulação matemática: não é previsão nem garantia de rentabilidade.'],
        })
      }),
    )
    const user = userEvent.setup()
    renderApp('/calculadora')
    await user.click(await screen.findByRole('button', { name: /Calcular/ }))
    expect(await screen.findByText('Valor final estimado')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s746,90/)).toBeInTheDocument()
    expect(screen.getByText('Simulação matemática: não é previsão nem garantia de rentabilidade.')).toBeInTheDocument()
  })

  it('valida a taxa antes de calcular', async () => {
    server.use(http.get('/api/calculator/simulations', () => HttpResponse.json([])))
    const user = userEvent.setup()
    renderApp('/calculadora')
    const rate = await screen.findByLabelText(/Taxa de juros/)
    await user.clear(rate)
    await user.type(rate, '150')
    await user.click(screen.getByRole('button', { name: /Calcular/ }))
    expect(screen.getByText('A taxa deve estar entre 0% e 100%.')).toBeInTheDocument()
  })
})

describe('área administrativa', () => {
  it('não é exibida para estudantes', async () => {
    server.use(http.get('/api/dashboard', () => HttpResponse.json(emptyDashboard())))
    renderApp('/admin')
    expect(await screen.findByRole('heading', { name: 'Olá, ana!' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Administração/ })).not.toBeInTheDocument()
  })
})

describe('transações', () => {
  it('lista com paginação e mostra o resumo do período', async () => {
    server.use(
      http.get('/api/transactions/categories', () => HttpResponse.json([{ value: 'FOOD', label: 'Alimentação', type: 'EXPENSE' }])),
      http.get('/api/transactions/summary', () => HttpResponse.json({ from: '2026-10-01', to: '2026-10-28', income: 200, expense: 50, balance: 150, expensesByCategory: [], incomeByCategory: [] })),
      http.get('/api/transactions', ({ request }) => {
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json({
          content: [{ id: page + 1, description: `Lanche ${page + 1}`, amount: 10, type: 'EXPENSE', category: 'FOOD', categoryLabel: 'Alimentação', occurredOn: '2026-10-10', notes: null, origin: 'MANUAL', goalId: null, createdAt: '2026-10-10T00:00:00Z' }],
          page, size: 15, totalElements: 2, totalPages: 2,
        })
      }),
    )
    const user = userEvent.setup()
    renderApp('/transacoes')
    expect(await screen.findByText('Lanche 1')).toBeInTheDocument()
    expect(screen.getByText(/R\$\s150,00/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Próxima página' }))
    await waitFor(() => expect(screen.getByText('Lanche 2')).toBeInTheDocument())
  })
})

function emptyDashboard() {
  return {
    displayName: 'ana',
    goals: { active: 0, completed: 0, totalSaved: 0, totalTarget: 0, overallProgress: 0, highlights: [] },
    monthFinances: { from: '2026-10-01', to: '2026-10-28', income: 0, expense: 0, balance: 0, expensesByCategory: [], incomeByCategory: [] },
    savingsEvolution: [],
    recentActivity: [],
    unreadNotifications: 0,
    notifications: [],
    learning: { enrolled: 0, completed: 0, inProgress: [] },
    quizzes: { attempts: 0, averagePercent: null, recent: [] },
    gamification: { points: 0, level: 1, levelTitle: 'Iniciante', currentLevelStart: 0, nextLevelStart: 100, streak: 0, unlocked: 0, total: 14, recentAchievements: [] },
    recommendations: [{ title: 'Crie sua primeira meta', description: 'x', link: '/metas', kind: 'GOAL' }],
    hasTransactions: false,
    reward: noReward,
  }
}
