import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle2, Circle, HelpCircle, Star } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { CourseDetail, Page, ReviewView } from '../api/types'
import { TextAreaField } from '../components/form'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, ButtonLink, Card, ErrorState, PageHeader, ProgressBar, Spinner } from '../components/ui'
import { date } from '../utils/format'
import { difficultyLabel } from '../utils/labels'

export function CourseDetailPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const toast = useToast()
  const detail = useQuery({ queryKey: ['course', id], queryFn: () => api.get<CourseDetail>(`/api/courses/${id}`) })
  const reviews = useQuery({ queryKey: ['course', id, 'reviews'], queryFn: () => api.get<Page<ReviewView>>(`/api/courses/${id}/reviews?size=10`) })
  const enroll = useMutation({
    mutationFn: () => api.post<CourseDetail>(`/api/courses/${id}/enroll`),
    onSuccess: (d) => {
      qc.setQueryData(['course', id], d)
      qc.invalidateQueries({ queryKey: ['courses'] })
      toast.success('Inscrição feita! Bons estudos.')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (detail.isLoading) return <Spinner />
  if (detail.error) return <ErrorState error={detail.error} onRetry={() => detail.refetch()} />
  const d = detail.data!
  const c = d.course
  const next = d.enrollment?.nextLessonId

  return (
    <>
      <Link to="/cursos" className="btn btn-ghost btn-sm" style={{ marginBottom: 8 }}><ArrowLeft size={14} aria-hidden /> Cursos</Link>
      <PageHeader
        title={c.title}
        subtitle={c.summary}
        actions={
          !d.enrollment ? (
            <Button onClick={() => enroll.mutate()} loading={enroll.isPending}>Inscrever-se gratuitamente</Button>
          ) : next ? (
            <ButtonLink to={`/aulas/${next}`}>{c.completedLessons > 0 ? 'Continuar de onde parei' : 'Começar a primeira aula'}</ButtonLink>
          ) : (
            <Badge tone="success">Curso concluído</Badge>
          )
        }
      />
      <div className="grid grid-2">
        <Card title="Sobre o curso">
          <div className="row" style={{ marginBottom: 10 }}>
            <Badge tone="primary">{c.categoryLabel}</Badge>
            <Badge>{difficultyLabel[c.difficulty]}</Badge>
            <Badge>{c.estimatedMinutes} min</Badge>
          </div>
          <p>{d.description}</p>
          <h3>Você vai aprender a</h3>
          <ul>{d.objectives.map((o) => <li key={o}>{o}</li>)}</ul>
          {d.enrollment && (
            <div className="stack" style={{ gap: 6 }}>
              <div className="row-between small"><span>Seu progresso</span><strong>{c.completedLessons}/{c.lessonCount} aulas</strong></div>
              <ProgressBar value={c.progressPercent} label="Progresso no curso" />
              {d.enrollment.completedAt && <span className="small muted">Concluído em {date(d.enrollment.completedAt)}</span>}
            </div>
          )}
        </Card>
        <Card title="Conteúdo">
          {!d.enrollment && <Alert tone="info">Inscreva-se para acessar as aulas e registrar seu progresso.</Alert>}
          {d.modules.map((m, mi) => (
            <div key={m.id} style={{ marginTop: 10 }}>
              <h3>Módulo {mi + 1}: {m.title}</h3>
              <ul className="list">
                {m.lessons.map((l) => (
                  <li key={l.id} className="row-between">
                    <span className="row" style={{ gap: 8 }}>
                      {l.completed ? <CheckCircle2 size={18} className="positive" aria-label="Concluída" /> : <Circle size={18} className="muted" aria-label="Não concluída" />}
                      {d.enrollment ? <Link to={`/aulas/${l.id}`}>{l.title}</Link> : <span>{l.title}</span>}
                      {l.hasQuiz && <HelpCircle size={14} aria-label="Possui quiz" className="muted" />}
                    </span>
                    <span className="small muted nowrap">{l.estimatedMinutes} min</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </Card>
      </div>
      <div className="grid grid-2" style={{ marginTop: 16 }}>
        {d.enrollment && <ReviewForm courseId={Number(id)} initial={d.myReview} />}
        <Card title={`Avaliações${c.averageRating !== null ? ` · ${c.averageRating} ★` : ''}`}>
          {reviews.data?.content.length === 0 && <p className="muted small">Ainda não há avaliações.</p>}
          <ul className="list">
            {reviews.data?.content.map((r) => (
              <li key={r.id}>
                <div className="row-between"><strong>{r.author}</strong><span aria-label={`${r.rating} de 5 estrelas`}>{'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}</span></div>
                {r.comment && <p className="small" style={{ margin: '4px 0 0' }}>{r.comment}</p>}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </>
  )
}

function ReviewForm({ courseId, initial }: { courseId: number; initial: CourseDetail['myReview'] }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [rating, setRating] = useState(initial?.rating ?? 0)
  const [comment, setComment] = useState(initial?.comment ?? '')
  const [error, setError] = useState<string | null>(null)
  const save = useMutation({
    mutationFn: () => api.put(`/api/courses/${courseId}/review`, { rating, comment: comment || null }),
    onSuccess: () => {
      toast.success('Obrigado pela avaliação!')
      qc.invalidateQueries({ queryKey: ['course', String(courseId)] })
    },
    onError: (e) => setError(errorMessage(e)),
  })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (rating < 1) return setError('Escolha de 1 a 5 estrelas.')
    save.mutate()
  }
  return (
    <Card title={initial ? 'Sua avaliação' : 'Avalie este curso'}>
      <form className="stack" onSubmit={submit} noValidate>
        <div className="row" role="radiogroup" aria-label="Nota de 1 a 5">
          {[1, 2, 3, 4, 5].map((n) => (
            <button key={n} type="button" role="radio" aria-checked={rating === n} aria-label={`${n} estrela${n > 1 ? 's' : ''}`} className="btn btn-ghost btn-icon" onClick={() => setRating(n)}>
              <Star size={22} fill={n <= rating ? 'var(--chart-2)' : 'none'} color="var(--chart-2)" />
            </button>
          ))}
        </div>
        <TextAreaField label="Comentário (opcional)" value={comment} onChange={(e) => setComment(e.target.value)} maxLength={1000} hint="Comentários são públicos (apenas com seu apelido). Não inclua dados pessoais." />
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={save.isPending}>Enviar avaliação</Button>
      </form>
    </Card>
  )
}
