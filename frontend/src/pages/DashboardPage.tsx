import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, Bell, BookOpen, Calculator, Flame, HelpCircle, Lightbulb, PiggyBank, Plus, Receipt, Target, Trophy, Wallet } from 'lucide-react'
import { api } from '../api/client'
import type { Dashboard } from '../api/types'
import { BalanceLineChart } from '../components/charts'
import { Badge, ButtonLink, Card, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Stat } from '../components/ui'
import { money, percent, relativeTime } from '../utils/format'

const shortcuts = [
  { to: '/transacoes?nova=1', label: 'Registrar gasto', icon: <Receipt size={18} /> },
  { to: '/metas?nova=1', label: 'Nova meta', icon: <Target size={18} /> },
  { to: '/cursos', label: 'Continuar estudando', icon: <BookOpen size={18} /> },
  { to: '/quizzes', label: 'Fazer um quiz', icon: <HelpCircle size={18} /> },
  { to: '/calculadora', label: 'Simular juros', icon: <Calculator size={18} /> },
]

export function DashboardPage() {
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => api.get<Dashboard>('/api/dashboard') })
  if (q.isLoading) return <Spinner label="Montando seu painel…" />
  if (q.error) return <ErrorState error={q.error} onRetry={() => q.refetch()} />
  const d = q.data!
  const g = d.gamification
  const levelProgress = ((g.points - g.currentLevelStart) / Math.max(1, g.nextLevelStart - g.currentLevelStart)) * 100

  return (
    <>
      <PageHeader title={`Olá, ${d.displayName}!`} subtitle="Aqui está um resumo do seu progresso." />

      <nav aria-label="Atalhos" className="row" style={{ marginBottom: 20 }}>
        {shortcuts.map((s) => (
          <Link key={s.to} to={s.to} className="btn btn-secondary btn-sm">
            <span aria-hidden>{s.icon}</span> {s.label}
          </Link>
        ))}
      </nav>

      <div className="grid grid-4" style={{ marginBottom: 16 }}>
        <Stat icon={<PiggyBank size={16} aria-hidden />} label="Guardado nas metas" value={money(d.goals.totalSaved)} hint={`${d.goals.active} ativa(s) · ${d.goals.completed} concluída(s)`} />
        <Stat icon={<Wallet size={16} aria-hidden />} label="Saldo do mês" value={<span className={d.monthFinances.balance < 0 ? 'negative' : ''}>{money(d.monthFinances.balance)}</span>} hint={`${money(d.monthFinances.income)} entraram · ${money(d.monthFinances.expense)} saíram`} />
        <Stat icon={<Trophy size={16} aria-hidden />} label={`Nível ${g.level} · ${g.levelTitle}`} value={`${g.points} pts`} hint={<ProgressBar value={levelProgress} label="Progresso até o próximo nível" accent />} />
        <Stat icon={<Flame size={16} aria-hidden />} label="Sequência de estudos" value={`${g.streak} dia(s)`} hint={`${g.unlocked} de ${g.total} conquistas`} />
      </div>

      <div className="grid grid-2" style={{ marginBottom: 16 }}>
        <Card title="Minhas metas" action={<ButtonLink to="/metas" variant="ghost" size="sm">Ver todas <ArrowRight size={14} /></ButtonLink>}>
          {d.goals.highlights.length === 0 ? (
            <EmptyState icon={<Target size={24} />} title="Nenhuma meta ativa" description="Metas transformam desejos em planos. Que tal criar a primeira?" action={<ButtonLink to="/metas?nova=1" size="sm"><Plus size={14} /> Criar meta</ButtonLink>} />
          ) : (
            <div className="stack">
              <div className="row-between small muted">
                <span>Progresso geral</span>
                <span>{money(d.goals.totalSaved)} de {money(d.goals.totalTarget)} ({percent(d.goals.overallProgress)})</span>
              </div>
              <ProgressBar value={d.goals.overallProgress} label="Progresso geral das metas" />
              <ul className="list">
                {d.goals.highlights.map((goal) => (
                  <li key={goal.id}>
                    <Link to={`/metas/${goal.id}`} className="row-between" style={{ textDecoration: 'none', color: 'inherit' }}>
                      <strong>{goal.title}</strong>
                      <span className="small muted">{money(goal.savedAmount)} / {money(goal.targetAmount)}</span>
                    </Link>
                    <div style={{ marginTop: 6 }}><ProgressBar value={goal.progressPercent} label={`Progresso de ${goal.title}`} /></div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </Card>

        <Card title="Evolução da economia (6 meses)" action={<ButtonLink to="/relatorios" variant="ghost" size="sm">Relatórios <ArrowRight size={14} /></ButtonLink>}>
          {d.hasTransactions ? (
            <>
              <BalanceLineChart data={d.savingsEvolution} title="Saldo registrado e resultado mensal dos últimos 6 meses" />
              <p className="legend-note">Baseado nas receitas e despesas que você registrou.</p>
            </>
          ) : (
            <EmptyState icon={<Receipt size={24} />} title="Sem movimentações ainda" description="Registre suas receitas e despesas para ver aqui a evolução da sua economia." action={<ButtonLink to="/transacoes?nova=1" size="sm">Registrar a primeira</ButtonLink>} />
          )}
        </Card>
      </div>

      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Card title="Aprendizado">
          {d.learning.inProgress.length === 0 ? (
            <EmptyState icon={<BookOpen size={24} />} title={d.learning.completed ? 'Tudo concluído por aqui!' : 'Nenhum curso em andamento'} description="Explore o catálogo de cursos." action={<ButtonLink to="/cursos" size="sm">Ver cursos</ButtonLink>} />
          ) : (
            <ul className="list">
              {d.learning.inProgress.map((c) => (
                <li key={c.courseId} className="stack" style={{ gap: 6 }}>
                  <div className="row-between">
                    <strong>{c.title}</strong>
                    <span className="small muted">{c.completedLessons}/{c.lessonCount}</span>
                  </div>
                  <ProgressBar value={c.progressPercent} label={`Progresso em ${c.title}`} />
                  {c.nextLessonId && <Link className="small" to={`/aulas/${c.nextLessonId}`}>Continuar de onde parei →</Link>}
                </li>
              ))}
            </ul>
          )}
          <p className="small muted" style={{ marginTop: 8 }}>{d.learning.completed} curso(s) concluído(s)</p>
        </Card>

        <Card title="Quizzes">
          {d.quizzes.attempts === 0 ? (
            <EmptyState icon={<HelpCircle size={24} />} title="Nenhum quiz feito" description="Teste o que você já sabe e descubra o que revisar." action={<ButtonLink to="/quizzes" size="sm">Fazer um quiz</ButtonLink>} />
          ) : (
            <>
              <p>Média de acertos: <strong>{percent(d.quizzes.averagePercent)}</strong> em {d.quizzes.attempts} tentativa(s)</p>
              <ul className="list">
                {d.quizzes.recent.map((a) => (
                  <li key={a.attemptId} className="row-between">
                    <Link to={`/quiz-tentativas/${a.attemptId}`}>{a.quizTitle}</Link>
                    <Badge tone={a.percent >= 70 ? 'success' : 'warning'}>{percent(a.percent)}</Badge>
                  </li>
                ))}
              </ul>
            </>
          )}
        </Card>

        <Card title="Conquistas recentes" action={<ButtonLink to="/gamificacao" variant="ghost" size="sm">Ver todas</ButtonLink>}>
          {g.recentAchievements.length === 0 ? (
            <EmptyState icon={<Trophy size={24} />} title="Ainda sem conquistas" description="Conclua uma aula ou crie uma meta para desbloquear a primeira." />
          ) : (
            <ul className="list">
              {g.recentAchievements.map((a) => (
                <li key={a.code}>
                  <strong>🏅 {a.title}</strong>
                  <div className="small muted">{a.description}</div>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid grid-3">
        <Card title="Recomendações para você">
          <ul className="list">
            {d.recommendations.map((r) => (
              <li key={r.title}>
                <Link to={r.link} style={{ textDecoration: 'none', color: 'inherit' }} className="row" aria-label={`${r.title}: ${r.description}`}>
                  <Lightbulb size={18} aria-hidden style={{ color: 'var(--accent)' }} />
                  <span>
                    <strong>{r.title}</strong>
                    <div className="small muted">{r.description}</div>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>

        <Card title="Notificações" action={<ButtonLink to="/notificacoes" variant="ghost" size="sm">{d.unreadNotifications} não lida(s)</ButtonLink>}>
          {d.notifications.length === 0 ? (
            <EmptyState icon={<Bell size={24} />} title="Tudo em dia" description="Você não tem notificações pendentes." />
          ) : (
            <ul className="list">
              {d.notifications.map((n) => (
                <li key={n.id}>
                  {n.link ? <Link to={n.link}><strong>{n.title}</strong></Link> : <strong>{n.title}</strong>}
                  <div className="small muted">{n.message}</div>
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Atividades recentes">
          {d.recentActivity.length === 0 ? (
            <p className="muted">Nada por aqui ainda.</p>
          ) : (
            <ul className="list">
              {d.recentActivity.map((a) => (
                <li key={a.id} className="row-between">
                  <span className="small">{a.description}</span>
                  <span className="small muted nowrap">{relativeTime(a.createdAt)}</span>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  )
}
