import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, CheckCircle2, HelpCircle } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { CompletionResult, LessonView } from '../api/types'
import { TextAreaField } from '../components/form'
import { Markdown } from '../components/Markdown'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, ButtonLink, Card, ErrorState, Spinner } from '../components/ui'

export function LessonPage() {
  const { id } = useParams()
  const lesson = useQuery({ queryKey: ['lesson', id], queryFn: () => api.get<LessonView>(`/api/lessons/${id}`) })

  if (lesson.isLoading) return <Spinner />
  if (lesson.error) {
    return (
      <div className="stack">
        <ErrorState error={lesson.error} />
        <ButtonLink to="/cursos" variant="secondary">Voltar aos cursos</ButtonLink>
      </div>
    )
  }
  // A chave garante estado novo (anotação, conclusão) a cada aula.
  return <Lesson key={lesson.data!.id} lesson={lesson.data!} />
}

function Lesson({ lesson: l }: { lesson: LessonView }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [note, setNote] = useState(l.note ?? '')
  const [completion, setCompletion] = useState<CompletionResult | null>(null)

  useEffect(() => {
    // Registra no servidor a última aula aberta ("continuar de onde parou").
    api.post(`/api/lessons/${l.id}/visit`).catch(() => undefined)
  }, [l.id])

  const complete = useMutation({
    mutationFn: () => api.post<CompletionResult>(`/api/lessons/${l.id}/complete`),
    onSuccess: (r) => {
      setCompletion(r)
      toast.success(r.courseCompleted ? '🎓 Curso concluído! Parabéns!' : 'Aula concluída!')
      toast.reward(r.reward)
      qc.invalidateQueries({ queryKey: ['lesson', String(l.id)] })
      qc.invalidateQueries({ queryKey: ['course'] })
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })
  const saveNote = useMutation({
    mutationFn: () => api.put(`/api/lessons/${l.id}/note`, { content: note }),
    onSuccess: () => {
      toast.success('Anotação salva.')
      qc.invalidateQueries({ queryKey: ['notes'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const completed = l.completed || !!completion
  const nextId = completion?.nextLessonId ?? l.nextLessonId

  return (
    <>
      <Link to={`/cursos/${l.courseId}`} className="btn btn-ghost btn-sm" style={{ marginBottom: 8 }}><ArrowLeft size={14} aria-hidden /> {l.courseTitle}</Link>
      <div className="grid lesson-layout">
        <article className="card">
          <div className="row small muted" style={{ marginBottom: 6 }}>
            <span>{l.moduleTitle}</span>·<span>Aula {l.position} de {l.totalLessons}</span>·<span>{l.estimatedMinutes} min</span>
            {completed && <Badge tone="success">Concluída</Badge>}
          </div>
          <h1>{l.title}</h1>
          <Markdown text={l.content} />
          {completion?.courseCompleted && <Alert tone="success">🎓 Você concluiu o curso! Que tal deixar uma avaliação?</Alert>}
          <div className="row" style={{ marginTop: 20 }}>
            {l.previousLessonId && <ButtonLink to={`/aulas/${l.previousLessonId}`} variant="secondary"><ArrowLeft size={16} aria-hidden /> Anterior</ButtonLink>}
            <span className="spacer" />
            {!completed && (
              <Button onClick={() => complete.mutate()} loading={complete.isPending}><CheckCircle2 size={16} aria-hidden /> Marcar como concluída</Button>
            )}
            {nextId && <ButtonLink to={`/aulas/${nextId}`} variant={completed ? 'primary' : 'secondary'}>Próxima <ArrowRight size={16} aria-hidden /></ButtonLink>}
          </div>
        </article>
        <aside className="stack">
          {l.quizId && (
            <Card title="Pratique">
              <p className="small muted">Esta aula tem um quiz para fixar o conteúdo.</p>
              <ButtonLink to={`/quizzes/${l.quizId}`} variant="secondary"><HelpCircle size={16} aria-hidden /> Fazer o quiz</ButtonLink>
            </Card>
          )}
          <Card title="Minhas anotações">
            <TextAreaField label="Anotações privadas" value={note} onChange={(e) => setNote(e.target.value)} maxLength={5000} hint="Só você vê. Deixe em branco e salve para apagar." />
            <Button variant="secondary" onClick={() => saveNote.mutate()} loading={saveNote.isPending} style={{ marginTop: 10 }}>Salvar anotação</Button>
          </Card>
        </aside>
      </div>
    </>
  )
}
