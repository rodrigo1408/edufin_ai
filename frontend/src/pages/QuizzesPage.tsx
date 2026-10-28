import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { HelpCircle } from 'lucide-react'
import { api, qs } from '../api/client'
import type { AttemptHistoryItem, Difficulty, Page, QuizCard, Topic } from '../api/types'
import { SimpleLine } from '../components/charts'
import { Badge, ButtonLink, Card, Chips, EmptyState, ErrorState, PageHeader, Spinner } from '../components/ui'
import { date, percent } from '../utils/format'
import { difficultyLabel, options, topicLabel } from '../utils/labels'

export function QuizzesPage() {
  const [topic, setTopic] = useState<Topic | ''>('')
  const [difficulty, setDifficulty] = useState<Difficulty | ''>('')
  const quizzes = useQuery({ queryKey: ['quizzes', topic, difficulty], queryFn: () => api.get<QuizCard[]>('/api/quizzes' + qs({ topic, difficulty })) })
  const history = useQuery({ queryKey: ['quiz-history'], queryFn: () => api.get<Page<AttemptHistoryItem>>('/api/quiz-attempts/history?size=20') })
  const evolution = [...(history.data?.content ?? [])].reverse().map((a, i) => ({ label: `#${i + 1}`, percent: a.percent }))

  return (
    <>
      <PageHeader title="Quizzes" subtitle="Teste seus conhecimentos e veja uma explicação a cada resposta." />
      <div className="stack" style={{ marginBottom: 16 }}>
        <Chips label="Filtrar por tema" value={topic} onChange={setTopic} items={[{ value: '', label: 'Todos os temas' }, ...options(topicLabel)]} />
        <Chips label="Filtrar por nível" value={difficulty} onChange={setDifficulty} items={[{ value: '', label: 'Todos os níveis' }, ...options(difficultyLabel)]} />
      </div>
      {quizzes.isLoading && <Spinner />}
      {quizzes.error && <ErrorState error={quizzes.error} onRetry={() => quizzes.refetch()} />}
      {quizzes.data?.length === 0 && <EmptyState icon={<HelpCircle size={24} />} title="Nenhum quiz com esses filtros" />}
      <div className="grid grid-3">
        {quizzes.data?.map((q) => (
          <div key={q.id} className="card stack" style={{ gap: 10 }}>
            <div className="row">
              <Badge tone="primary">{q.categoryLabel}</Badge>
              <Badge>{difficultyLabel[q.difficulty]}</Badge>
            </div>
            <h3 style={{ margin: 0 }}>{q.title}</h3>
            <p className="small muted" style={{ margin: 0 }}>{q.description}</p>
            <div className="row-between small">
              <span>{q.questionCount} perguntas</span>
              {q.bestPercent !== null ? <span>Melhor: <strong>{percent(q.bestPercent)}</strong></span> : <span className="muted">Ainda não feito</span>}
            </div>
            <ButtonLink to={`/quizzes/${q.id}`} size="sm">{q.attempts > 0 ? 'Refazer' : 'Começar'}</ButtonLink>
          </div>
        ))}
      </div>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Card title="Sua evolução">
          {evolution.length < 2 ? (
            <p className="muted small">Faça pelo menos dois quizzes para ver o gráfico de evolução.</p>
          ) : (
            <SimpleLine data={evolution} xKey="label" yKey="percent" name="Acertos" title="Percentual de acertos nas últimas tentativas" percent />
          )}
        </Card>
        <Card title="Histórico de tentativas">
          {history.data?.content.length === 0 && <p className="muted small">Nenhuma tentativa finalizada ainda.</p>}
          <ul className="list">
            {history.data?.content.map((a) => (
              <li key={a.attemptId} className="row-between">
                <Link to={`/quiz-tentativas/${a.attemptId}`}>{a.quizTitle}</Link>
                <span className="row small">
                  <span className="muted">{date(a.finishedAt)}</span>
                  <Badge tone={a.percent >= 70 ? 'success' : 'warning'}>{a.correct}/{a.total} · {percent(a.percent)}</Badge>
                </span>
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </>
  )
}
