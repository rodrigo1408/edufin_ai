import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, ArrowRight, CheckCircle2, Circle, HelpCircle, Sparkles } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { CompletionResult, CourseDetail, LessonView } from '../api/types'
import { TextAreaField } from '../components/form'
import { Markdown } from '../components/Markdown'
import { useToast } from '../components/Toast'
import { TutorChat } from '../components/TutorChat'
import { Alert, BackLink, Badge, Button, ButtonLink, Card, ErrorState, ProgressBar, Spinner } from '../components/ui'

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
  // A chave garante estado novo (anotação, conclusão, conversa com o tutor) a cada aula.
  return <Lesson key={lesson.data!.id} lesson={lesson.data!} />
}

function Lesson({ lesson: l }: { lesson: LessonView }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [note, setNote] = useState(l.note ?? '')
  const [completion, setCompletion] = useState<CompletionResult | null>(null)
  const course = useQuery({ queryKey: ['course', String(l.courseId)], queryFn: () => api.get<CourseDetail>(`/api/courses/${l.courseId}`) })

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
  const progress = completion?.progressPercent ?? course.data?.course.progressPercent ?? 0
  const modules = course.data?.modules.map((m, mi) => (
    <div key={m.id}>
      <h3>Módulo {mi + 1} · {m.title}</h3>
      {m.lessons.map((ls) => {
        const done = ls.completed || (ls.id === l.id && completed)
        return (
          <Link key={ls.id} to={`/aulas/${ls.id}`} aria-current={ls.id === l.id ? 'page' : undefined}>
            {done ? <CheckCircle2 size={16} color="var(--success)" aria-label="Concluída" /> : <Circle size={16} aria-label="Não concluída" />}
            <span>{ls.title}</span>
          </Link>
        )
      })}
    </div>
  ))

  return (
    <>
      <BackLink to={`/cursos/${l.courseId}`}>{l.courseTitle}</BackLink>
      <div className="lesson-layout">
        {/* Telas largas: coluna fixa. Notebooks, tablets e celulares: lista recolhível acima da aula. */}
        <nav className="card lesson-nav" aria-label="Módulos e aulas do curso">
          <strong className="small">{l.courseTitle}</strong>
          <div style={{ margin: '8px 0 4px' }}><ProgressBar value={progress} label="Progresso no curso" /></div>
          <span className="small muted">{Math.round(progress)}% concluído</span>
          {modules}
        </nav>

        <div className="stack" style={{ gap: 16, minWidth: 0 }}>
          <details className="card lesson-nav lesson-nav-compact">
            <summary>
              <span>Módulos e aulas · {Math.round(progress)}% concluído</span>
            </summary>
            <nav aria-label="Módulos e aulas do curso">{modules}</nav>
          </details>
          <article className="card">
            <div className="row small muted" style={{ marginBottom: 8, gap: 8 }}>
              <span>{l.moduleTitle}</span>·<span>Aula {l.position} de {l.totalLessons}</span>·<span>{l.estimatedMinutes} min</span>
              {completed && <Badge tone="success">Concluída</Badge>}
              <a href="#tutor-aula" className="tutor-jump"><Sparkles size={14} aria-hidden /> Perguntar ao tutor</a>
            </div>
            <h1>{l.title}</h1>
            <Markdown text={l.content} />
            {completion?.courseCompleted && <Alert tone="success"><span>🎓 Você concluiu o curso! Que tal deixar uma avaliação na página do curso?</span></Alert>}
            <div className="row" style={{ marginTop: 22, paddingTop: 16, borderTop: '1px solid var(--border)' }}>
              {l.previousLessonId && <ButtonLink to={`/aulas/${l.previousLessonId}`} variant="secondary"><ArrowLeft size={16} aria-hidden /> Anterior</ButtonLink>}
              <span className="spacer" />
              {!completed && (
                <Button onClick={() => complete.mutate()} loading={complete.isPending}><CheckCircle2 size={16} aria-hidden /> Marcar como concluída</Button>
              )}
              {nextId && <ButtonLink to={`/aulas/${nextId}`} variant={completed ? 'primary' : 'secondary'}>Próxima <ArrowRight size={16} aria-hidden /></ButtonLink>}
            </div>
          </article>

          {l.quizId && (
            <Card>
              <div className="row-between">
                <div>
                  <h2 style={{ marginBottom: 4 }}>Pratique o que aprendeu</h2>
                  <p className="small muted" style={{ margin: 0 }}>Esta aula tem um quiz. Se travar, peça uma dica ao tutor dentro do quiz.</p>
                </div>
                <ButtonLink to={`/quizzes/${l.quizId}`} variant="secondary"><HelpCircle size={16} aria-hidden /> Fazer o quiz</ButtonLink>
              </div>
            </Card>
          )}

          <Card title="Minhas anotações">
            <TextAreaField label="Anotações privadas" value={note} onChange={(e) => setNote(e.target.value)} maxLength={5000} hint="Só você vê. Deixe em branco e salve para apagar." />
            <Button variant="secondary" onClick={() => saveNote.mutate()} loading={saveNote.isPending} style={{ marginTop: 10 }}>Salvar anotação</Button>
          </Card>
        </div>

        <aside id="tutor-aula" className="card ai-surface tutor-panel" aria-label="Tutor da aula">
          <div className="tutor-head">
            <span className="ai-mark" aria-hidden><Sparkles size={18} /></span>
            <div>
              <strong>Tutor IA</strong>
              <span className="small muted">Tire dúvidas sobre esta aula</span>
            </div>
          </div>
          <TutorChat lessonId={l.id} compact />
        </aside>
      </div>
    </>
  )
}
