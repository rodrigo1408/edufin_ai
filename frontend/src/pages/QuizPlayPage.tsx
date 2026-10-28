import { useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Sparkles, XCircle } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { AnswerFeedback, AttemptResult, AttemptView, ChatResponse } from '../api/types'
import { useToast } from '../components/Toast'
import { Alert, BackLink, Button, Card, ErrorState, PageHeader, ProgressBar, Spinner } from '../components/ui'

/**
 * Execução do quiz. As perguntas chegam SEM a alternativa correta; a correção de cada resposta e a
 * nota final vêm do servidor. Iniciar é idempotente: retoma a tentativa em andamento, se houver.
 */
export function QuizPlayPage() {
  const { id } = useParams()
  const start = useQuery({ queryKey: ['quiz-attempt-start', id], queryFn: () => api.post<AttemptView>(`/api/quizzes/${id}/attempts`), staleTime: Infinity, gcTime: 0 })
  if (start.isLoading) return <Spinner label="Preparando o quiz…" />
  if (start.error) return <ErrorState error={start.error} onRetry={() => start.refetch()} />
  return <QuizRunner key={start.data!.attemptId} attempt={start.data!} />
}

export function QuizRunner({ attempt }: { attempt: AttemptView }) {
  const navigate = useNavigate()
  const toast = useToast()
  const qc = useQueryClient()
  const questions = attempt.questions
  const [feedback, setFeedback] = useState<Record<number, AnswerFeedback>>(() => Object.fromEntries(attempt.answered.map((a) => [a.questionId, a])))
  const [index, setIndex] = useState(() => {
    const firstOpen = questions.findIndex((q) => !attempt.answered.some((a) => a.questionId === q.id))
    return firstOpen === -1 ? questions.length - 1 : firstOpen
  })
  const [selected, setSelected] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [hint, setHint] = useState<{ questionId: number | null; data: string | null; loading: boolean }>({ questionId: null, data: null, loading: false })

  const answer = useMutation({
    mutationFn: (body: { questionId: number; optionId: number }) => api.post<AnswerFeedback>(`/api/quiz-attempts/${attempt.attemptId}/answers`, body),
    onSuccess: (f) => setFeedback((m) => ({ ...m, [f.questionId]: f })),
    onError: (e) => setError(errorMessage(e)),
  })
  const finish = useMutation({
    mutationFn: () => api.post<AttemptResult>(`/api/quiz-attempts/${attempt.attemptId}/finish`),
    onSuccess: (r) => {
      toast.reward(r.reward)
      qc.invalidateQueries({ queryKey: ['quizzes'] })
      qc.invalidateQueries({ queryKey: ['quiz-history'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.setQueryData(['quiz-result', String(r.attemptId)], r)
      navigate(`/quiz-tentativas/${r.attemptId}`, { replace: true })
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const q = questions[index]
  const fb = q ? feedback[q.id] : undefined
  const answeredCount = Object.keys(feedback).length
  const allAnswered = answeredCount === questions.length

  const confirm = () => {
    if (selected === null || !q) return
    setError(null)
    answer.mutate({ questionId: q.id, optionId: selected })
  }
  const go = (i: number) => {
    setSelected(null)
    setIndex(i)
  }
  // Dica conceitual do tutor: o servidor nunca devolve a alternativa correta por esta rota.
  const requestHint = async (questionId: number) => {
    setHint({ questionId, data: null, loading: true })
    try {
      const r = await api.post<ChatResponse>(`/api/chatbot/hint/question/${questionId}`)
      setHint({ questionId, data: r.answer, loading: false })
    } catch (e) {
      setHint({ questionId: null, data: null, loading: false })
      setError(errorMessage(e))
    }
  }

  return (
    <>
      <BackLink to="/quizzes">Quizzes</BackLink>
      <PageHeader title={attempt.quizTitle} subtitle={`Pergunta ${index + 1} de ${questions.length}`} />
      <ProgressBar value={(answeredCount / questions.length) * 100} label="Perguntas respondidas" />
      {q && (
        <Card className="stack">
          <h2 id="pergunta">{q.statement}</h2>
          <div className="stack" role="radiogroup" aria-labelledby="pergunta">
            {q.options.map((o) => {
              const isSelected = (fb?.selectedOptionId ?? selected) === o.id
              const state = fb ? (o.id === fb.correctOptionId ? 'correct' : isSelected ? 'wrong' : '') : isSelected ? 'selected' : ''
              return (
                <button key={o.id} type="button" role="radio" aria-checked={isSelected} className={`option ${state}`} disabled={!!fb} onClick={() => setSelected(o.id)}>
                  {state === 'correct' && <CheckCircle2 size={18} aria-hidden />}
                  {state === 'wrong' && <XCircle size={18} aria-hidden />}
                  <span>{o.text}</span>
                  {state === 'correct' && <span className="sr-only">(alternativa correta)</span>}
                  {state === 'wrong' && <span className="sr-only">(sua resposta, incorreta)</span>}
                </button>
              )
            })}
          </div>
          {fb && (
            <div aria-live="polite">
              <Alert tone={fb.correct ? 'success' : 'warning'}>
                <div>
                  <strong>{fb.correct ? 'Você acertou!' : 'Não foi dessa vez.'}</strong>
                  <div>{fb.explanation}</div>
                </div>
              </Alert>
            </div>
          )}
          {!fb && hint.data && hint.questionId === q.id && (
            <div className="ai-surface" style={{ padding: 14 }} aria-live="polite">
              <div className="tutor-head" style={{ marginBottom: 8 }}>
                <span className="ai-mark" style={{ width: 28, height: 28 }} aria-hidden><Sparkles size={15} /></span>
                <strong>Dica do tutor</strong>
              </div>
              <p className="small" style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{hint.data}</p>
            </div>
          )}
          {error && <Alert tone="error">{error}</Alert>}
          <div className="row">
            {index > 0 && <Button variant="secondary" onClick={() => go(index - 1)}>Anterior</Button>}
            {!fb && !(hint.questionId === q.id && hint.data) && (
              <Button variant="ghost" onClick={() => requestHint(q.id)} loading={hint.loading}>
                <Sparkles size={16} aria-hidden /> Pedir dica ao tutor
              </Button>
            )}
            <span className="spacer" />
            {!fb && <Button onClick={confirm} disabled={selected === null} loading={answer.isPending}>Responder</Button>}
            {fb && index < questions.length - 1 && <Button onClick={() => go(index + 1)}>Próxima pergunta</Button>}
            {fb && allAnswered && <Button onClick={() => finish.mutate()} loading={finish.isPending}>Ver resultado</Button>}
          </div>
        </Card>
      )}
    </>
  )
}
