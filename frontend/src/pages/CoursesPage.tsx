import { useMemo, useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, CheckCircle2, Clock, NotebookPen, Search } from 'lucide-react'
import { api, qs } from '../api/client'
import type { CourseCard, Difficulty, NoteView, Topic } from '../api/types'
import { Badge, Button, ButtonLink, Chips, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Stars, Tabs } from '../components/ui'
import { dateTime } from '../utils/format'
import { difficultyLabel, options, topicLabel } from '../utils/labels'

function normalize(s: string): string {
  return s.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
}

export function CoursesPage() {
  const [tab, setTab] = useState<'catalog' | 'notes'>('catalog')
  const [topic, setTopic] = useState<Topic | ''>('')
  const [difficulty, setDifficulty] = useState<Difficulty | ''>('')
  const [search, setSearch] = useState('')
  const courses = useQuery({ queryKey: ['courses', topic, difficulty], queryFn: () => api.get<CourseCard[]>('/api/courses' + qs({ topic, difficulty })) })
  const notes = useQuery({ queryKey: ['notes'], queryFn: () => api.get<NoteView[]>('/api/courses/notes'), enabled: tab === 'notes' })

  const visible = useMemo(() => {
    const term = normalize(search.trim())
    return (courses.data ?? []).filter((c) => !term || normalize(`${c.title} ${c.summary} ${c.categoryLabel}`).includes(term))
  }, [courses.data, search])
  const inProgress = (courses.data ?? []).filter((c) => c.enrolled && !c.completed)
  const clearFilters = () => { setSearch(''); setTopic(''); setDifficulty('') }

  return (
    <>
      <PageHeader title="Cursos e trilha" subtitle="Uma trilha progressiva: comece pelo básico e avance no seu ritmo. Em cada aula, o tutor está disponível." />
      <Tabs label="Seções de cursos" value={tab} onChange={setTab} tabs={[{ value: 'catalog', label: 'Catálogo' }, { value: 'notes', label: 'Minhas anotações' }]} />
      {tab === 'catalog' && (
        <>
          {inProgress.length > 0 && !search && !topic && !difficulty && (
            <section aria-label="Em andamento" className="grid grid-2" style={{ marginBottom: 20 }}>
              {inProgress.slice(0, 2).map((c) => (
                <Link key={c.id} to={`/cursos/${c.id}`} className="card card-link stack" style={{ gap: 8 }}>
                  <span className="eyebrow">Em andamento</span>
                  <strong>{c.title}</strong>
                  <ProgressBar value={c.progressPercent} label={`Progresso em ${c.title}`} />
                  <span className="small muted">{c.completedLessons} de {c.lessonCount} aulas</span>
                </Link>
              ))}
            </section>
          )}
          <div className="stack" style={{ marginBottom: 18 }}>
            <div className="search">
              <Search size={16} aria-hidden />
              <label htmlFor="course-search" className="sr-only">Pesquisar cursos</label>
              <input id="course-search" className="input" type="search" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Pesquisar por título ou tema" maxLength={60} />
            </div>
            <Chips label="Tema" value={topic} onChange={setTopic} items={[{ value: '', label: 'Todos os temas' }, ...options(topicLabel)]} />
            <Chips label="Nível" value={difficulty} onChange={setDifficulty} items={[{ value: '', label: 'Todos os níveis' }, ...options(difficultyLabel)]} />
          </div>
          {courses.isLoading && <Spinner />}
          {courses.error && <ErrorState error={courses.error} onRetry={() => courses.refetch()} />}
          {courses.data && visible.length === 0 && (
            <div className="card">
              <EmptyState
                icon={<BookOpen size={22} />}
                title={search ? `Nenhum curso encontrado para "${search}"` : 'Nenhum curso com esses filtros'}
                description="Tente outro termo ou remova os filtros."
                action={<Button variant="secondary" onClick={clearFilters}>Limpar filtros</Button>}
              />
            </div>
          )}
          <div className="grid grid-3">
            {visible.map((c) => (
              <article key={c.id} className="card stack" style={{ gap: 10 }}>
                <div className="row" style={{ gap: 6 }}>
                  <Badge tone="primary">Etapa {c.position}</Badge>
                  <Badge>{difficultyLabel[c.difficulty]}</Badge>
                  {c.completed && <Badge tone="success"><CheckCircle2 size={12} aria-hidden /> Concluído</Badge>}
                </div>
                <h3 style={{ margin: 0 }}><Link to={`/cursos/${c.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>{c.title}</Link></h3>
                <p className="small muted" style={{ margin: 0, flex: 1 }}>{c.summary}</p>
                <div className="row small muted" style={{ gap: 12 }}>
                  <span>{c.categoryLabel}</span>
                  <span className="row" style={{ gap: 4 }}><Clock size={14} aria-hidden /> {c.estimatedMinutes} min</span>
                  <span>{c.lessonCount} aulas</span>
                  {c.averageRating !== null && <span className="row" style={{ gap: 4 }}><Stars value={c.averageRating} size={12} /> ({c.reviewCount})</span>}
                </div>
                {c.enrolled && <ProgressBar value={c.progressPercent} label={`Progresso em ${c.title}`} />}
                <ButtonLink to={`/cursos/${c.id}`} size="sm" variant={c.enrolled && !c.completed ? 'primary' : 'secondary'}>
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
          {notes.error && <ErrorState error={notes.error} onRetry={() => notes.refetch()} />}
          {notes.data?.length === 0 && <EmptyState icon={<NotebookPen size={22} />} title="Nenhuma anotação" description="Durante as aulas, use o campo “Minhas anotações” para registrar o que aprendeu." />}
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
