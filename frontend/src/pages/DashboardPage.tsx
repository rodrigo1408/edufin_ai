import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { ArrowRight, BookOpen, Flame, GraduationCap, HelpCircle, Lightbulb, PiggyBank, Plus, Receipt, Sparkles, Target, Trophy, Wallet } from 'lucide-react'
import type { ReactNode } from 'react'
import { api } from '../api/client'
import type { Dashboard } from '../api/types'
import { BalanceLineChart } from '../components/charts'
import { Badge, ButtonLink, Card, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Stat } from '../components/ui'
import { money, percent, relativeTime } from '../utils/format'

const kindIcon: Record<string, ReactNode> = {
  COURSE: <BookOpen size={16} />,
  QUIZ: <HelpCircle size={16} />,
  GOAL: <Target size={16} />,
  HABIT: <Receipt size={16} />,
}

/** Início da área do aluno: aprendizado primeiro, depois progresso e, por fim, as finanças. */
export function DashboardPage() {
  const q = useQuery({ queryKey: ['dashboard'], queryFn: () => api.get<Dashboard>('/api/dashboard') })
  if (q.isLoading) return <Spinner label="Montando seu painel…" />
  if (q.error) return <ErrorState error={q.error} onRetry={() => q.refetch()} />
  const d = q.data!
  const g = d.gamification
  const levelProgress = ((g.points - g.currentLevelStart) / Math.max(1, g.nextLevelStart - g.currentLevelStart)) * 100
  const current = d.learning.inProgress[0]

  return (
    <>
      <PageHeader title={`Olá, ${d.displayName}!`} subtitle="Continue de onde parou e veja o que vem a seguir." />

      <section className="hero-grid" aria-label="Continuar aprendendo">
        <Card className="continue-card">
          {current ? (
            <>
              <div>
                <span className="eyebrow">Continue aprendendo</span>
                <h2>{current.title}</h2>
                <p className="small muted" style={{ margin: '6px 0 0' }}>{current.completedLessons} de {current.lessonCount} aulas concluídas</p>
              </div>
              <ProgressBar value={current.progressPercent} label={`Progresso em ${current.title}`} />
              <div className="row">
                {current.nextLessonId ? (
                  <ButtonLink to={`/aulas/${current.nextLessonId}`}>Continuar aula <ArrowRight size={16} aria-hidden /></ButtonLink>
                ) : (
                  <ButtonLink to={`/cursos/${current.courseId}`}>Ver curso</ButtonLink>
                )}
                <ButtonLink to="/cursos" variant="ghost">Ver trilha completa</ButtonLink>
              </div>
            </>
          ) : (
            <>
              <div>
                <span className="eyebrow">{d.learning.completed > 0 ? 'Próximo passo' : 'Comece por aqui'}</span>
                <h2>{d.learning.completed > 0 ? 'Escolha o próximo curso da trilha' : 'Sua primeira aula leva menos de 10 minutos'}</h2>
                <p className="small muted" style={{ margin: '6px 0 0' }}>
                  {d.learning.completed > 0 ? `Você já concluiu ${d.learning.completed} curso(s). Continue avançando na trilha.` : 'Comece pelo curso "Primeiros passos com o dinheiro".'}
                </p>
              </div>
              <div className="row"><ButtonLink to="/cursos">Ver cursos <ArrowRight size={16} aria-hidden /></ButtonLink></div>
            </>
          )}
        </Card>

        <div className="card ai-surface stack" style={{ justifyContent: 'space-between' }}>
          <div className="tutor-head">
            <span className="ai-mark" aria-hidden><Sparkles size={18} /></span>
            <div>
              <strong>Tutor IA</strong>
              <span className="small muted">Explicações, exemplos e dicas para os quizzes</span>
            </div>
          </div>
          <p className="small muted" style={{ margin: 0 }}>Travou em algum conceito? Pergunte ao tutor — dentro das aulas ele já sabe o que você está estudando.</p>
          <ButtonLink to="/assistente" variant="ai">Perguntar ao tutor</ButtonLink>
        </div>
      </section>

      <section className="stat-strip" style={{ marginTop: 16 }} aria-label="Seu progresso">
        <Stat icon={<Trophy size={16} aria-hidden />} label={`Nível ${g.level} · ${g.levelTitle}`} value={`${g.points} pts`} hint={<ProgressBar value={levelProgress} label="Progresso até o próximo nível" accent />} />
        <Stat icon={<Flame size={16} aria-hidden />} label="Sequência de estudos" value={`${g.streak} dia(s)`} hint={`${g.unlocked} de ${g.total} conquistas`} />
        <Stat icon={<GraduationCap size={16} aria-hidden />} label="Cursos" value={`${d.learning.completed} concluído(s)`} hint={`${d.learning.enrolled} inscrição(ões)`} />
        <Stat icon={<HelpCircle size={16} aria-hidden />} label="Quizzes" value={d.quizzes.attempts === 0 ? '—' : percent(d.quizzes.averagePercent)} hint={d.quizzes.attempts === 0 ? 'Nenhum quiz feito' : `média em ${d.quizzes.attempts} tentativa(s)`} />
      </section>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Card title="Próximas atividades">
          <ul className="list next-list" style={{ margin: '-6px -10px' }}>
            {d.recommendations.map((r) => (
              <li key={r.title} style={{ border: 0, padding: 0 }}>
                <Link to={r.link}>
                  <span className="next-icon" aria-hidden>{kindIcon[r.kind] ?? <Lightbulb size={16} />}</span>
                  <span>
                    <strong>{r.title}</strong>
                    <span className="small muted" style={{ display: 'block' }}>{r.description}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </Card>
        <Card title="Conquistas recentes" action={<ButtonLink to="/gamificacao" variant="ghost" size="sm">Ver todas</ButtonLink>}>
          {g.recentAchievements.length === 0 ? (
            <EmptyState icon={<Trophy size={22} />} title="Ainda sem conquistas" description="Conclua uma aula ou crie uma meta para desbloquear a primeira." />
          ) : (
            <ul className="list">
              {g.recentAchievements.map((a) => (
                <li key={a.code} className="row" style={{ gap: 10 }}>
                  <span className="achievement-icon" style={{ width: 32, height: 32 }} aria-hidden><Trophy size={16} /></span>
                  <span><strong>{a.title}</strong><span className="small muted" style={{ display: 'block' }}>{a.description}</span></span>
                </li>
              ))}
            </ul>
          )}
          {d.quizzes.recent.length > 0 && (
            <>
              <h3 style={{ marginTop: 16 }}>Últimos quizzes</h3>
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
      </div>

      <div className="section-title">
        <h2>Suas finanças</h2>
        <nav aria-label="Atalhos" className="row">
          <Link to="/transacoes?nova=1" className="btn btn-secondary btn-sm"><Receipt size={15} aria-hidden /> Registrar gasto</Link>
          <Link to="/metas?nova=1" className="btn btn-secondary btn-sm"><Plus size={15} aria-hidden /> Nova meta</Link>
        </nav>
      </div>
      <div className="grid grid-3">
        <Stat icon={<PiggyBank size={16} aria-hidden />} label="Guardado nas metas" value={money(d.goals.totalSaved)} hint={`${d.goals.active} ativa(s) · ${d.goals.completed} concluída(s)`} />
        <Stat icon={<Wallet size={16} aria-hidden />} label="Saldo do mês" value={<span className={d.monthFinances.balance < 0 ? 'negative' : ''}>{money(d.monthFinances.balance)}</span>} hint={`${money(d.monthFinances.income)} entraram · ${money(d.monthFinances.expense)} saíram`} />
        <Card>
          <div className="row-between" style={{ marginBottom: 8 }}>
            <span className="stat-label"><Target size={16} aria-hidden /> Metas ativas</span>
            <Link className="small" to="/metas">Ver todas</Link>
          </div>
          {d.goals.highlights.length === 0 ? (
            <p className="small muted" style={{ margin: 0 }}>Nenhuma meta ativa. <Link to="/metas?nova=1">Criar a primeira</Link></p>
          ) : (
            <ul className="list">
              {d.goals.highlights.map((goal) => (
                <li key={goal.id} className="stack" style={{ gap: 6, padding: '6px 0' }}>
                  <Link to={`/metas/${goal.id}`} className="row-between" style={{ textDecoration: 'none', color: 'inherit' }}>
                    <span className="small"><strong>{goal.title}</strong></span>
                    <span className="small muted">{percent(goal.progressPercent)}</span>
                  </Link>
                  <ProgressBar value={goal.progressPercent} label={`Progresso de ${goal.title}`} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Card title="Evolução da economia (6 meses)" action={<ButtonLink to="/relatorios" variant="ghost" size="sm">Relatórios <ArrowRight size={14} aria-hidden /></ButtonLink>}>
          {d.hasTransactions ? (
            <>
              <BalanceLineChart data={d.savingsEvolution} title="Saldo registrado e resultado mensal dos últimos 6 meses" />
              <p className="legend-note">Baseado nas receitas e despesas que você registrou.</p>
            </>
          ) : (
            <EmptyState icon={<Receipt size={22} />} title="Sem movimentações ainda" description="Registre receitas e despesas para ver aqui a evolução da sua economia." action={<ButtonLink to="/transacoes?nova=1" size="sm">Registrar a primeira</ButtonLink>} />
          )}
        </Card>
        <Card title="Notificações e atividades" action={<ButtonLink to="/notificacoes" variant="ghost" size="sm">{d.unreadNotifications} não lida(s)</ButtonLink>}>
          <ul className="list">
            {d.notifications.slice(0, 3).map((n) => (
              <li key={n.id}>
                {n.link ? <Link to={n.link}><strong>{n.title}</strong></Link> : <strong>{n.title}</strong>}
                <div className="small muted">{n.message}</div>
              </li>
            ))}
            {d.recentActivity.slice(0, 4).map((a) => (
              <li key={'a' + a.id} className="row-between">
                <span className="small">{a.description}</span>
                <span className="small muted nowrap">{relativeTime(a.createdAt)}</span>
              </li>
            ))}
            {d.notifications.length === 0 && d.recentActivity.length === 0 && <li className="muted small">Nada por aqui ainda.</li>}
          </ul>
        </Card>
      </div>
    </>
  )
}
