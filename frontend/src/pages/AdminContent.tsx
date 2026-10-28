import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Difficulty, Topic } from '../api/types'
import { Checkbox, SelectField, TextAreaField, TextField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, EmptyState, ErrorState, Spinner } from '../components/ui'
import { difficultyLabel, options, topicLabel } from '../utils/labels'

interface LessonAdmin { id: number; position: number; title: string; content: string; estimatedMinutes: number; quizId: number | null }
interface ModuleAdmin { id: number; position: number; title: string; lessons: LessonAdmin[] }
interface CourseAdmin {
  id: number; slug: string; title: string; summary: string; description: string; objectives: string; category: Topic
  difficulty: Difficulty; estimatedMinutes: number; position: number; published: boolean; enrollments: number; modules: ModuleAdmin[]
}
interface OptionAdmin { id?: number; text: string; correct: boolean }
interface QuestionAdmin { id?: number; statement: string; explanation: string; topic: Topic; options: OptionAdmin[] }
interface QuizAdmin { id: number; title: string; description: string; category: Topic; difficulty: Difficulty; published: boolean; hasAttempts: boolean; questions: QuestionAdmin[] }

type CourseForm = Omit<CourseAdmin, 'id' | 'enrollments' | 'modules'>
const emptyCourse: CourseForm = { slug: '', title: '', summary: '', description: '', objectives: '', category: 'BUDGET', difficulty: 'BEGINNER', estimatedMinutes: 30, position: 10, published: false }

export function AdminCourses() {
  const qc = useQueryClient()
  const toast = useToast()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const courses = useQuery({ queryKey: ['admin-courses'], queryFn: () => api.get<CourseAdmin[]>('/api/admin/content/courses') })
  const selected = courses.data?.find((c) => c.id === selectedId) ?? null
  const refresh = () => { qc.invalidateQueries({ queryKey: ['admin-courses'] }); qc.invalidateQueries({ queryKey: ['courses'] }) }

  const create = useMutation({
    mutationFn: (c: CourseForm) => api.post<CourseAdmin>('/api/admin/content/courses', c),
    onSuccess: (c) => { toast.success('Curso criado.'); setCreating(false); refresh(); setSelectedId(c.id) },
  })

  if (courses.isLoading) return <Spinner />
  if (courses.error) return <ErrorState error={courses.error} />
  return (
    <div className="grid" style={{ gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 2fr)' }}>
      <Card title="Cursos" action={<Button size="sm" onClick={() => setCreating(true)}><Plus size={14} /> Novo</Button>}>
        <ul className="list">
          {courses.data?.map((c) => (
            <li key={c.id}>
              <button className="btn btn-ghost" style={{ width: '100%', justifyContent: 'space-between' }} aria-pressed={c.id === selectedId} onClick={() => setSelectedId(c.id)}>
                <span style={{ textAlign: 'left' }}>{c.title}</span>
                {c.published ? <Badge tone="success">Publicado</Badge> : <Badge>Rascunho</Badge>}
              </button>
            </li>
          ))}
        </ul>
      </Card>
      {selected ? <CourseEditor key={selected.id} course={selected} onChange={refresh} onDeleted={() => { setSelectedId(null); refresh() }} /> : <Card><EmptyState title="Selecione um curso" description="Escolha um curso para editar módulos e aulas." /></Card>}
      <Modal open={creating} title="Novo curso" onClose={() => setCreating(false)}>
        <CourseMetaForm initial={emptyCourse} isNew onSubmit={(c) => create.mutateAsync(c).then(() => undefined)} onCancel={() => setCreating(false)} />
      </Modal>
    </div>
  )
}

function CourseMetaForm({ initial, isNew, onSubmit, onCancel }: { initial: CourseForm; isNew?: boolean; onSubmit: (c: CourseForm) => Promise<void>; onCancel?: () => void }) {
  const [f, setF] = useState(initial)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const set = <K extends keyof CourseForm>(k: K, v: CourseForm[K]) => setF((x) => ({ ...x, [k]: v }))
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try { await onSubmit(f) } catch (err) { setError(errorMessage(err)) } finally { setLoading(false) }
  }
  return (
    <form className="stack" onSubmit={submit} noValidate>
      {error && <Alert tone="error">{error}</Alert>}
      <div className="form-grid">
        <TextField label="Identificador (slug)" value={f.slug} onChange={(e) => set('slug', e.target.value)} disabled={!isNew} hint="Minúsculas e hífens. Não pode ser alterado depois." />
        <TextField label="Título" value={f.title} onChange={(e) => set('title', e.target.value)} />
        <TextField className="full" label="Resumo" value={f.summary} onChange={(e) => set('summary', e.target.value)} maxLength={300} />
        <TextAreaField className="full" label="Descrição" value={f.description} onChange={(e) => set('description', e.target.value)} maxLength={2000} />
        <TextAreaField className="full" label="Objetivos (um por linha)" value={f.objectives} onChange={(e) => set('objectives', e.target.value)} maxLength={2000} />
        <SelectField label="Tema" value={f.category} onChange={(e) => set('category', e.target.value as Topic)} options={options(topicLabel)} />
        <SelectField label="Nível" value={f.difficulty} onChange={(e) => set('difficulty', e.target.value as Difficulty)} options={options(difficultyLabel)} />
        <TextField label="Duração estimada (min)" type="number" value={f.estimatedMinutes} onChange={(e) => set('estimatedMinutes', Number(e.target.value))} />
        <TextField label="Ordem na trilha" type="number" value={f.position} onChange={(e) => set('position', Number(e.target.value))} />
        <Checkbox label="Publicado (visível para os usuários)" checked={f.published} onChange={(e) => set('published', e.target.checked)} />
      </div>
      <div className="modal-actions">
        {onCancel && <Button variant="secondary" onClick={onCancel}>Cancelar</Button>}
        <Button type="submit" loading={loading}>Salvar curso</Button>
      </div>
    </form>
  )
}

function CourseEditor({ course, onChange, onDeleted }: { course: CourseAdmin; onChange: () => void; onDeleted: () => void }) {
  const toast = useToast()
  const [lessonEdit, setLessonEdit] = useState<{ moduleId: number; lesson?: LessonAdmin } | null>(null)
  const [newModule, setNewModule] = useState('')
  const [confirmDelete, setConfirmDelete] = useState(false)
  const quizzes = useQuery({ queryKey: ['admin-quizzes'], queryFn: () => api.get<QuizAdmin[]>('/api/admin/content/quizzes') })
  const ok = (msg: string) => () => { toast.success(msg); onChange() }
  const fail = (e: unknown) => toast.error(errorMessage(e))
  const update = useMutation({ mutationFn: (c: CourseForm) => api.put(`/api/admin/content/courses/${course.id}`, c), onSuccess: ok('Curso salvo.'), onError: fail })
  const addModule = useMutation({ mutationFn: () => api.post(`/api/admin/content/courses/${course.id}/modules`, { title: newModule }), onSuccess: () => { setNewModule(''); ok('Módulo adicionado.')() }, onError: fail })
  const delModule = useMutation({ mutationFn: (id: number) => api.del(`/api/admin/content/modules/${id}`), onSuccess: ok('Módulo removido.'), onError: fail })
  const delLesson = useMutation({ mutationFn: (id: number) => api.del(`/api/admin/content/lessons/${id}`), onSuccess: ok('Aula removida.'), onError: fail })
  const delCourse = useMutation({ mutationFn: () => api.del(`/api/admin/content/courses/${course.id}`), onSuccess: () => { toast.success('Curso excluído.'); onDeleted() }, onError: (e) => { setConfirmDelete(false); fail(e) } })

  return (
    <div className="stack">
      <Card title={course.title} action={<Button size="sm" variant="ghost" onClick={() => setConfirmDelete(true)}><Trash2 size={14} /> Excluir</Button>}>
        <p className="small muted">{course.enrollments} inscrição(ões). Cursos com inscrições não podem ser excluídos — despublique-os.</p>
        <CourseMetaForm initial={{ ...course }} onSubmit={(c) => update.mutateAsync(c).then(() => undefined)} />
      </Card>
      <Card title="Módulos e aulas">
        {course.modules.map((m, mi) => (
          <div key={m.id} style={{ marginBottom: 14 }}>
            <div className="row-between">
              <h3 style={{ margin: 0 }}>Módulo {mi + 1}: {m.title}</h3>
              <div className="row">
                <Button size="sm" variant="secondary" onClick={() => setLessonEdit({ moduleId: m.id })}><Plus size={14} /> Aula</Button>
                <Button size="sm" variant="ghost" onClick={() => delModule.mutate(m.id)} aria-label={`Excluir módulo ${m.title}`}><Trash2 size={14} /></Button>
              </div>
            </div>
            <ul className="list">
              {m.lessons.map((l) => (
                <li key={l.id} className="row-between">
                  <span>{l.position}. {l.title} <span className="small muted">({l.estimatedMinutes} min{l.quizId ? ', com quiz' : ''})</span></span>
                  <span className="row" style={{ gap: 2 }}>
                    <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Editar aula ${l.title}`} onClick={() => setLessonEdit({ moduleId: m.id, lesson: l })}><Pencil size={14} /></button>
                    <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Excluir aula ${l.title}`} onClick={() => delLesson.mutate(l.id)}><Trash2 size={14} /></button>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        ))}
        <form className="row" onSubmit={(e) => { e.preventDefault(); if (newModule.trim()) addModule.mutate() }}>
          <label className="sr-only" htmlFor="new-module">Título do novo módulo</label>
          <input id="new-module" className="input" style={{ flex: 1 }} placeholder="Título do novo módulo" value={newModule} onChange={(e) => setNewModule(e.target.value)} maxLength={120} />
          <Button type="submit" variant="secondary" loading={addModule.isPending}>Adicionar módulo</Button>
        </form>
      </Card>
      <Modal open={!!lessonEdit} title={lessonEdit?.lesson ? 'Editar aula' : 'Nova aula'} onClose={() => setLessonEdit(null)}>
        {lessonEdit && <LessonForm key={lessonEdit.lesson?.id ?? 'new'} moduleId={lessonEdit.moduleId} lesson={lessonEdit.lesson} quizzes={quizzes.data ?? []} onDone={() => { setLessonEdit(null); onChange() }} onCancel={() => setLessonEdit(null)} />}
      </Modal>
      <ConfirmDialog open={confirmDelete} title="Excluir curso?" message="Esta ação remove o curso, módulos e aulas." confirmLabel="Excluir" danger loading={delCourse.isPending} onConfirm={() => delCourse.mutate()} onCancel={() => setConfirmDelete(false)} />
    </div>
  )
}

function LessonForm({ moduleId, lesson, quizzes, onDone, onCancel }: { moduleId: number; lesson?: LessonAdmin; quizzes: QuizAdmin[]; onDone: () => void; onCancel: () => void }) {
  const [title, setTitle] = useState(lesson?.title ?? '')
  const [content, setContent] = useState(lesson?.content ?? '')
  const [minutes, setMinutes] = useState(lesson?.estimatedMinutes ?? 10)
  const [quizId, setQuizId] = useState(lesson?.quizId ? String(lesson.quizId) : '')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    const body = { title, content, estimatedMinutes: minutes, quizId: quizId ? Number(quizId) : null }
    try {
      if (lesson) await api.put(`/api/admin/content/lessons/${lesson.id}`, body)
      else await api.post(`/api/admin/content/modules/${moduleId}/lessons`, body)
      onDone()
    } catch (err) { setError(errorMessage(err)) } finally { setLoading(false) }
  }
  return (
    <form className="stack" onSubmit={submit} noValidate>
      {error && <Alert tone="error">{error}</Alert>}
      <TextField label="Título" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={120} />
      <TextAreaField label="Conteúdo" value={content} onChange={(e) => setContent(e.target.value)} maxLength={20000} style={{ minHeight: 220 }} hint="Use ## para títulos, - para listas, > para destaques e **negrito**." />
      <div className="form-grid">
        <TextField label="Duração (min)" type="number" min={1} value={minutes} onChange={(e) => setMinutes(Number(e.target.value))} />
        <SelectField label="Quiz associado" value={quizId} onChange={(e) => setQuizId(e.target.value)} placeholder="Nenhum" options={quizzes.map((q) => ({ value: String(q.id), label: q.title }))} />
      </div>
      <div className="modal-actions"><Button variant="secondary" onClick={onCancel}>Cancelar</Button><Button type="submit" loading={loading}>Salvar aula</Button></div>
    </form>
  )
}

const emptyQuestion = (): QuestionAdmin => ({ statement: '', explanation: '', topic: 'BUDGET', options: [{ text: '', correct: true }, { text: '', correct: false }] })

export function AdminQuizzes() {
  const qc = useQueryClient()
  const toast = useToast()
  const quizzes = useQuery({ queryKey: ['admin-quizzes'], queryFn: () => api.get<QuizAdmin[]>('/api/admin/content/quizzes') })
  const [editing, setEditing] = useState<QuizAdmin | 'new' | null>(null)
  const [deleting, setDeleting] = useState<QuizAdmin | null>(null)
  const refresh = () => { qc.invalidateQueries({ queryKey: ['admin-quizzes'] }); qc.invalidateQueries({ queryKey: ['quizzes'] }) }
  const remove = useMutation({ mutationFn: (id: number) => api.del(`/api/admin/content/quizzes/${id}`), onSuccess: () => { setDeleting(null); toast.success('Quiz excluído.'); refresh() }, onError: (e) => { setDeleting(null); toast.error(errorMessage(e)) } })

  if (quizzes.isLoading) return <Spinner />
  if (quizzes.error) return <ErrorState error={quizzes.error} />
  return (
    <>
      <Card title="Quizzes" action={<Button size="sm" onClick={() => setEditing('new')}><Plus size={14} /> Novo quiz</Button>}>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Título</th><th>Tema</th><th>Perguntas</th><th>Status</th><th><span className="sr-only">Ações</span></th></tr></thead>
            <tbody>
              {quizzes.data?.map((q) => (
                <tr key={q.id}>
                  <td>{q.title}</td>
                  <td>{topicLabel[q.category]}</td>
                  <td>{q.questions.length}</td>
                  <td>{q.published ? <Badge tone="success">Publicado</Badge> : <Badge>Rascunho</Badge>} {q.hasAttempts && <Badge tone="info">Com tentativas</Badge>}</td>
                  <td className="nowrap">
                    <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Editar ${q.title}`} onClick={() => setEditing(q)}><Pencil size={14} /></button>
                    <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Excluir ${q.title}`} onClick={() => setDeleting(q)}><Trash2 size={14} /></button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
      <Modal open={!!editing} title={editing === 'new' ? 'Novo quiz' : 'Editar quiz'} onClose={() => setEditing(null)}>
        {editing && <QuizEditor key={editing === 'new' ? 'new' : editing.id} quiz={editing === 'new' ? null : editing} onDone={() => { setEditing(null); refresh() }} onCancel={() => setEditing(null)} />}
      </Modal>
      <ConfirmDialog open={!!deleting} title="Excluir quiz?" message="Quizzes já respondidos não podem ser excluídos (despublique-os para preservar o histórico)." confirmLabel="Excluir" danger loading={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)} onCancel={() => setDeleting(null)} />
    </>
  )
}

function QuizEditor({ quiz, onDone, onCancel }: { quiz: QuizAdmin | null; onDone: () => void; onCancel: () => void }) {
  const toast = useToast()
  const [meta, setMeta] = useState({ title: quiz?.title ?? '', description: quiz?.description ?? '', category: quiz?.category ?? ('BUDGET' as Topic), difficulty: quiz?.difficulty ?? ('BEGINNER' as Difficulty), published: quiz?.published ?? false })
  const [questions, setQuestions] = useState<QuestionAdmin[]>(quiz?.questions.map((q) => ({ ...q, options: q.options.map((o) => ({ ...o })) })) ?? [emptyQuestion()])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const locked = !!quiz?.hasAttempts

  const setQ = (i: number, patch: Partial<QuestionAdmin>) => setQuestions((list) => list.map((q, j) => (j === i ? { ...q, ...patch } : q)))
  const setOpt = (qi: number, oi: number, patch: Partial<OptionAdmin>) =>
    setQuestions((list) => list.map((q, j) => (j !== qi ? q : { ...q, options: q.options.map((o, k) => (k === oi ? { ...o, ...patch } : patch.correct ? { ...o, correct: false } : o)) })))

  const payloadQuestions = () => questions.map((q) => ({ statement: q.statement, explanation: q.explanation, topic: q.topic, options: q.options.map((o) => ({ text: o.text, correct: o.correct })) }))

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (!quiz) {
        await api.post('/api/admin/content/quizzes', { quiz: meta, questions: payloadQuestions() })
      } else {
        await api.put(`/api/admin/content/quizzes/${quiz.id}`, meta)
        if (!locked) await api.put(`/api/admin/content/quizzes/${quiz.id}/questions`, { questions: payloadQuestions() })
      }
      toast.success('Quiz salvo.')
      onDone()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="stack" onSubmit={save} noValidate>
      {error && <Alert tone="error">{error}</Alert>}
      <div className="form-grid">
        <TextField className="full" label="Título" value={meta.title} onChange={(e) => setMeta({ ...meta, title: e.target.value })} maxLength={120} />
        <TextAreaField className="full" label="Descrição" value={meta.description} onChange={(e) => setMeta({ ...meta, description: e.target.value })} maxLength={500} />
        <SelectField label="Tema" value={meta.category} onChange={(e) => setMeta({ ...meta, category: e.target.value as Topic })} options={options(topicLabel)} />
        <SelectField label="Nível" value={meta.difficulty} onChange={(e) => setMeta({ ...meta, difficulty: e.target.value as Difficulty })} options={options(difficultyLabel)} />
        <Checkbox label="Publicado" checked={meta.published} onChange={(e) => setMeta({ ...meta, published: e.target.checked })} />
      </div>
      {locked && <Alert tone="info">Este quiz já foi respondido: as perguntas ficam bloqueadas para preservar o histórico. Você ainda pode editar os dados gerais ou despublicá-lo.</Alert>}
      {questions.map((q, qi) => (
        <fieldset key={qi} className="card stack" style={{ boxShadow: 'none' }} disabled={locked}>
          <legend className="small muted">Pergunta {qi + 1}</legend>
          <TextAreaField label="Enunciado" value={q.statement} onChange={(e) => setQ(qi, { statement: e.target.value })} maxLength={500} style={{ minHeight: 60 }} />
          <SelectField label="Tema da pergunta" value={q.topic} onChange={(e) => setQ(qi, { topic: e.target.value as Topic })} options={options(topicLabel)} />
          {q.options.map((o, oi) => (
            <div key={oi} className="row">
              <input type="radio" name={`correct-${qi}`} checked={o.correct} onChange={() => setOpt(qi, oi, { correct: true })} aria-label={`Marcar alternativa ${oi + 1} como correta`} />
              <input className="input" style={{ flex: 1 }} value={o.text} onChange={(e) => setOpt(qi, oi, { text: e.target.value })} placeholder={`Alternativa ${oi + 1}`} maxLength={300} aria-label={`Texto da alternativa ${oi + 1}`} />
              {q.options.length > 2 && <button type="button" className="btn btn-ghost btn-icon btn-sm" aria-label={`Remover alternativa ${oi + 1}`} onClick={() => setQ(qi, { options: q.options.filter((_, k) => k !== oi) })}><Trash2 size={14} /></button>}
            </div>
          ))}
          {q.options.length < 6 && <Button size="sm" variant="ghost" onClick={() => setQ(qi, { options: [...q.options, { text: '', correct: false }] })}><Plus size={14} /> Alternativa</Button>}
          <TextAreaField label="Explicação (mostrada após responder)" value={q.explanation} onChange={(e) => setQ(qi, { explanation: e.target.value })} maxLength={1000} style={{ minHeight: 60 }} />
          {questions.length > 1 && <Button size="sm" variant="ghost" onClick={() => setQuestions((list) => list.filter((_, j) => j !== qi))}><Trash2 size={14} /> Remover pergunta</Button>}
        </fieldset>
      ))}
      {!locked && <Button variant="secondary" onClick={() => setQuestions((l) => [...l, emptyQuestion()])}><Plus size={14} /> Adicionar pergunta</Button>}
      <div className="modal-actions"><Button variant="secondary" onClick={onCancel}>Cancelar</Button><Button type="submit" loading={loading}>Salvar quiz</Button></div>
    </form>
  )
}
