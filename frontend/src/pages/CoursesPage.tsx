import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, Clock, NotebookPen, Star } from 'lucide-react'
import { api, qs } from '../api/client'
import type { CourseCard, Difficulty, NoteView, Topic } from '../api/types'
import { Badge, ButtonLink, Chips, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Tabs } from '../components/ui'
import { dateTime } from '../utils/format'
import { difficultyLabel, options, topicLabel } from '../utils/labels'

export function CoursesPage() {
  const [tab, setTab] = useState<'catalog' | 'notes'>('catalog')
  const [topic, setTopic] = useState<Topic | ''>('')
  const [difficulty, setDifficulty] = useState<Difficulty | ''>('')
  const courses = useQuery({ queryKey: ['courses', topic, difficulty], queryFn: () => api.get<CourseCard[]>('/api/courses' + qs({ topic, difficulty })) })
  const notes = useQuery({ queryKey: ['notes'], queryFn: () => api.get<NoteView[]>('/api/courses/notes'), enabled: tab === 'notes' })

  return (
    <>
      <PageHeader title="Cursos" subtitle="Uma trilha progressiva: comece pelo básico e avance no seu ritmo." />
      <Tabs label="Seções de cursos" value={tab} onChange={setTab} tabs={[{ value: 'catalog', label: 'Catálogo' }, { value: 'notes', label: 'Minhas anotações' }]} />
      {tab === 'catalog' && (
        <>
          <div className="stack" style={{ marginBottom: 16 }}>
            <Chips label="Tema" value={topic} onChange={setTopic} items={[{ value: '', label: 'Todos os temas' }, ...options(topicLabel)]} />
            <Chips label="Nível" value={difficulty} onChange={setDifficulty} items={[{ value: '', label: 'Todos os níveis' }, ...options(difficultyLabel)]} />
          </div>
          {courses.isLoading && <Spinner />}
          {courses.error && <ErrorState error={courses.error} onRetry={() => courses.refetch()} />}
          {courses.data?.length === 0 && <EmptyState icon={<BookOpen size={24} />} title="Nenhum curso com esses filtros" />}
          <div className="grid grid-3">
            {courses.data?.map((c, i) => (
              <article key={c.id} className="card stack" style={{ gap: 10 }}>
                <div className="row">
                  <Badge tone="primary">Etapa {i + 1}</Badge>
                  <Badge>{difficultyLabel[c.difficulty]}</Badge>
                  {c.completed && <Badge tone="success">Concluído</Badge>}
                </div>
                <h3 style={{ margin: 0 }}><Link to={`/cursos/${c.id}`} style={{ color: 'inherit' }}>{c.title}</Link></h3>
                <p className="small muted" style={{ margin: 0 }}>{c.summary}</p>
                <div className="row small muted">
                  <span className="row" style={{ gap: 4 }}><Clock size={14} aria-hidden /> {c.estimatedMinutes} min</span>
                  <span>{c.lessonCount} aulas</span>
                  {c.averageRating !== null && <span className="row" style={{ gap: 4 }}><Star size={14} aria-hidden /> {c.averageRating} ({c.reviewCount})</span>}
                </div>
                {c.enrolled && <ProgressBar value={c.progressPercent} label={`Progresso em ${c.title}`} />}
                <ButtonLink to={`/cursos/${c.id}`} size="sm" variant={c.enrolled ? 'primary' : 'secondary'}>
                  {c.completed ? 'Revisar' : c.enrolled ? 'Continuar' : 'Ver curso'}
                </ButtonLink>
              </article>
            ))}
          </div>
        </>
      )}
      {tab === 'notes' && (
        <>
          {notes.isLoading && <Spinner />}
          {notes.data?.length === 0 && <EmptyState icon={<NotebookPen size={24} />} title="Nenhuma anotação" description="Durante as aulas, use o campo “Minhas anotações” para registrar o que aprendeu." />}
          <div className="grid grid-2">
            {notes.data?.map((n) => (
              <div key={n.lessonId} className="card stack" style={{ gap: 6 }}>
                <span className="small muted">{n.courseTitle}</span>
                <Link to={`/aulas/${n.lessonId}`}><strong>{n.lessonTitle}</strong></Link>
                <p style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{n.content}</p>
                <span className="small muted">Atualizada em {dateTime(n.updatedAt)}</span>
              </div>
            ))}
          </div>
        </>
      )}
    </>
  )
}
