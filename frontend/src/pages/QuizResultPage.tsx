import { Link, useParams } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, XCircle } from 'lucide-react'
import { api } from '../api/client'
import type { AttemptResult, AttemptView } from '../api/types'
import { Alert, Badge, ButtonLink, Card, ErrorState, PageHeader, Spinner, Stat } from '../components/ui'
import { percent } from '../utils/format'

export function QuizResultPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const result = useQuery({
    queryKey: ['quiz-result', id],
    queryFn: () => api.get<AttemptResult>(`/api/quiz-attempts/${id}/result`),
    initialData: () => qc.getQueryData<AttemptResult>(['quiz-result', id]),
  })
  const attempt = useQuery({ queryKey: ['quiz-attempt', id], queryFn: () => api.get<AttemptView>(`/api/quiz-attempts/${id}`) })

  if (result.isLoading) return <Spinner />
  if (result.error) return <ErrorState error={result.error} onRetry={() => result.refetch()} />
  const r = result.data!
  const questionText = (qid: number) => attempt.data?.questions.find((q) => q.id === qid)
  const optionText = (qid: number, oid: number | null) => questionText(qid)?.options.find((o) => o.id === oid)?.text

  return (
    <>
      <PageHeader title={`Resultado: ${r.quizTitle}`} actions={<ButtonLink to={`/quizzes/${r.quizId}`} variant="secondary">Refazer quiz</ButtonLink>} />
      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Stat label="Acertos" value={`${r.correct} de ${r.total}`} />
        <Stat label="Percentual" value={percent(r.percent)} hint={r.passed ? 'Aprovado(a) — 70% ou mais' : 'Abaixo de 70%: vale revisar'} />
        <Stat label="Pontuação do quiz" value={`${r.score} pts`} hint="10 pontos por acerto" />
      </div>
      {r.passed ? <Alert tone="success">Mandou bem! 🎉 Continue praticando para fixar.</Alert> : <Alert tone="info">Errar faz parte do aprendizado. Veja as explicações abaixo e os conteúdos recomendados.</Alert>}

      {r.recommendedCourses.length > 0 && (
        <Card title={`Recomendado para revisar: ${r.weakTopics.join(', ')}`} className="stack">
          <ul className="list">
            {r.recommendedCourses.map((c) => (
              <li key={c.id} className="row-between">
                <span><strong>{c.title}</strong><div className="small muted">{c.summary}</div></span>
                <ButtonLink to={`/cursos/${c.id}`} size="sm" variant="secondary">Ver curso</ButtonLink>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Card title="Revisão das respostas" className="stack">
        <ol className="list" style={{ paddingLeft: 0 }}>
          {r.review.map((f, i) => (
            <li key={f.questionId} className="stack" style={{ gap: 6 }}>
              <div className="row">
                {f.correct ? <CheckCircle2 size={18} className="positive" aria-hidden /> : <XCircle size={18} className="negative" aria-hidden />}
                <strong>{i + 1}. {questionText(f.questionId)?.statement ?? 'Pergunta'}</strong>
                <Badge tone={f.correct ? 'success' : 'danger'}>{f.correct ? 'Certa' : f.selectedOptionId ? 'Errada' : 'Sem resposta'}</Badge>
              </div>
              {!f.correct && f.selectedOptionId && <span className="small">Sua resposta: {optionText(f.questionId, f.selectedOptionId)}</span>}
              <span className="small">Resposta correta: <strong>{optionText(f.questionId, f.correctOptionId)}</strong></span>
              <span className="small muted">{f.explanation}</span>
            </li>
          ))}
        </ol>
      </Card>
      <p style={{ marginTop: 12 }}><Link to="/quizzes">← Voltar para os quizzes</Link></p>
    </>
  )
}
