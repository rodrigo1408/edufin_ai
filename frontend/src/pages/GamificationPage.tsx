import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  Award, BookOpen, Calculator, Calendar, Check, Flag, FlaskConical, Flame, GraduationCap, HelpCircle, Lock, Medal, Star, Target, Trophy, Zap,
} from 'lucide-react'
import type { ReactNode } from 'react'
import { api } from '../api/client'
import type { GamificationSummary, Page, PointEventView } from '../api/types'
import { Badge, Card, ErrorState, PageHeader, Pagination, ProgressBar, Spinner, Stat } from '../components/ui'
import { date, dateTime } from '../utils/format'

const icons: Record<string, ReactNode> = {
  book: <BookOpen size={20} />, graduation: <GraduationCap size={20} />, trophy: <Trophy size={20} />, help: <HelpCircle size={20} />,
  check: <Check size={20} />, star: <Star size={20} />, target: <Target size={20} />, flag: <Flag size={20} />, calendar: <Calendar size={20} />,
  calculator: <Calculator size={20} />, flask: <FlaskConical size={20} />, flame: <Flame size={20} />, zap: <Zap size={20} />, medal: <Medal size={20} />,
}

const rules = [
  ['Concluir uma aula', '10'],
  ['Concluir um curso', '50'],
  ['Ser aprovado(a) em um quiz pela 1ª vez (70%+)', '15'],
  ['Gabaritar um quiz (1ª vez)', '10'],
  ['Concluir uma meta (até 1x por dia)', '20'],
  ['Criar uma meta (até 1x por dia)', '5'],
  ['Registrar movimentações (até 1x por dia)', '2'],
  ['Salvar uma simulação (até 1x por dia)', '3'],
  ['Desafios semanais e conquistas', 'variável'],
]

export function GamificationPage() {
  const [page, setPage] = useState(0)
  const summary = useQuery({ queryKey: ['gamification'], queryFn: () => api.get<GamificationSummary>('/api/gamification/summary') })
  const history = useQuery({ queryKey: ['gamification', 'history', page], queryFn: () => api.get<Page<PointEventView>>(`/api/gamification/history?page=${page}&size=10`) })

  if (summary.isLoading) return <Spinner />
  if (summary.error) return <ErrorState error={summary.error} onRetry={() => summary.refetch()} />
  const s = summary.data!
  const toNext = s.nextLevelStart - s.totalPoints
  const levelPct = ((s.totalPoints - s.currentLevelStart) / Math.max(1, s.nextLevelStart - s.currentLevelStart)) * 100

  return (
    <>
      <PageHeader title="Conquistas e desafios" subtitle="Pontos por aprender e criar bons hábitos — nunca por quanto dinheiro você tem." />
      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <div className="card level-ring">
          <div className="level-number" aria-hidden>{s.level}</div>
          <div className="stack" style={{ gap: 6, flex: 1 }}>
            <strong>Nível {s.level}: {s.levelTitle}</strong>
            <ProgressBar value={levelPct} label="Progresso até o próximo nível" accent />
            <span className="small muted">{s.totalPoints} pontos · faltam {toNext} para o nível {s.level + 1}</span>
          </div>
        </div>
        <Stat icon={<Flame size={16} aria-hidden />} label="Sequência de estudos" value={`${s.currentStreak} dia(s)`} hint={`Recorde: ${s.bestStreak} dia(s)`} />
        <Stat icon={<Target size={16} aria-hidden />} label="Meta de aulas da semana" value={`${s.lessonsThisWeek} / ${s.weeklyLessonTarget}`} hint={<ProgressBar value={(s.lessonsThisWeek / s.weeklyLessonTarget) * 100} label="Meta semanal de aulas" />} />
      </div>

      <Card title="Desafios da semana">
        <div className="grid grid-3">
          {s.challenges.map((c) => (
            <div key={c.code} className="card stack" style={{ gap: 8, boxShadow: 'none' }}>
              <div className="row-between">
                <strong>{c.title}</strong>
                {c.completed ? <Badge tone="success">Concluído</Badge> : <Badge tone="accent">+{c.points} pts</Badge>}
              </div>
              <span className="small muted">{c.description}</span>
              <ProgressBar value={(c.progress / c.target) * 100} label={`Progresso no desafio ${c.title}`} accent />
              <span className="small">{c.progress} de {c.target}</span>
            </div>
          ))}
        </div>
      </Card>

      <Card title={`Conquistas (${s.achievementsUnlocked}/${s.achievementsTotal})`} className="stack">
        <div className="grid grid-3" style={{ marginTop: 4 }}>
          {s.achievements.map((a) => (
            <div key={a.code} className={`achievement ${a.unlocked ? '' : 'locked'}`}>
              <div className="achievement-icon" aria-hidden>{a.unlocked ? icons[a.icon] ?? <Award size={20} /> : <Lock size={20} />}</div>
              <div>
                <strong>{a.title}</strong> <span className="small muted">+{a.points}</span>
                <div className="small muted">{a.description}</div>
                <div className="small">{a.unlocked ? `Desbloqueada em ${date(a.unlockedAt)}` : <span className="sr-only">Bloqueada</span>}</div>
              </div>
            </div>
          ))}
        </div>
      </Card>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Card title="Histórico de pontos">
          {history.data?.content.length === 0 && <p className="muted small">Você ainda não ganhou pontos. Conclua uma aula para começar!</p>}
          <ul className="list">
            {history.data?.content.map((e, i) => (
              <li key={i} className="row-between">
                <span><span className="small">{e.description}</span><div className="small muted">{dateTime(e.createdAt)}</div></span>
                <Badge tone="accent">+{e.points}</Badge>
              </li>
            ))}
          </ul>
          {history.data && <Pagination page={page} totalPages={history.data.totalPages} onChange={setPage} />}
        </Card>
        <Card title="Como ganhar pontos">
          <table>
            <tbody>
              {rules.map(([r, p]) => <tr key={r}><td>{r}</td><td className="num">{p}</td></tr>)}
            </tbody>
          </table>
          <p className="small muted" style={{ marginTop: 10 }}>Ações repetidas não geram pontos extras, e não há ranking público.</p>
        </Card>
      </div>
    </>
  )
}
