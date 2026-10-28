import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Heart, MessageCircle, MessagesSquare, Plus, ShieldCheck } from 'lucide-react'
import { api, ApiError, errorMessage, qs } from '../api/client'
import type { ForumCategory, Page, PostDetail, PostSummary } from '../api/types'
import { SelectField, TextAreaField, TextField } from '../components/form'
import { hasErrors, type Errors } from '../utils/validation'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, Chips, EmptyState, ErrorState, PageHeader, Pagination, Spinner } from '../components/ui'
import { relativeTime } from '../utils/format'
import { forumCategoryLabel, options } from '../utils/labels'

export function CommunityPage() {
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState<ForumCategory | ''>('')
  const [mine, setMine] = useState(false)
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const qc = useQueryClient()
  const toast = useToast()
  const list = useQuery({
    queryKey: ['forum', search, category, mine, page],
    queryFn: () => api.get<Page<PostSummary>>('/api/forum/posts' + qs({ q: search, category, mine, page, size: 10 })),
  })

  return (
    <>
      <PageHeader title="Comunidade" subtitle="Troque dicas e dúvidas sobre educação financeira com outras pessoas da comunidade." actions={<Button onClick={() => setCreating(true)}><Plus size={16} aria-hidden /> Nova publicação</Button>} />
      <Alert tone="info">
        <ShieldCheck size={18} aria-hidden />
        <span>Regras: respeito sempre; nada de dados pessoais (telefone, e-mail, documentos); sem promessas de dinheiro fácil. Viu algo errado? Use “Denunciar”.</span>
      </Alert>
      <Card className="stack">
        <form className="row" onSubmit={(e) => { e.preventDefault(); setSearch(q); setPage(0) }} role="search">
          <label htmlFor="forum-search" className="sr-only">Pesquisar publicações</label>
          <input id="forum-search" className="input" style={{ flex: 1, minWidth: 180 }} type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Pesquisar publicações" maxLength={60} />
          <Button type="submit" variant="secondary">Buscar</Button>
          <button type="button" className="chip" aria-pressed={mine} onClick={() => { setMine(!mine); setPage(0) }}>Minhas publicações</button>
        </form>
        <Chips label="Categorias" value={category} onChange={(v) => { setCategory(v); setPage(0) }} items={[{ value: '', label: 'Todas' }, ...options(forumCategoryLabel)]} />
      </Card>
      <div className="stack" style={{ marginTop: 16 }}>
        {list.isLoading && <Spinner />}
        {list.error && <ErrorState error={list.error} onRetry={() => list.refetch()} />}
        {list.data?.content.length === 0 && (
          <Card><EmptyState icon={<MessagesSquare size={24} />} title="Nenhuma publicação encontrada" description="Que tal começar a conversa?" action={<Button onClick={() => setCreating(true)}>Publicar</Button>} /></Card>
        )}
        {list.data?.content.map((p) => (
          <article key={p.id} className="card stack" style={{ gap: 6 }}>
            <div className="row small muted">
              <Badge tone="primary">{forumCategoryLabel[p.category]}</Badge>
              <span>{p.author}</span>·<span>{relativeTime(p.createdAt)}</span>{p.edited && <span>· editado</span>}
            </div>
            <h3 style={{ margin: 0 }}><Link to={`/comunidade/${p.id}`} style={{ color: 'inherit' }}>{p.title}</Link></h3>
            <p className="small" style={{ margin: 0 }}>{p.excerpt}</p>
            <div className="row small muted">
              <span className="row" style={{ gap: 4 }}><Heart size={14} aria-hidden fill={p.likedByMe ? 'currentColor' : 'none'} /> {p.likes}</span>
              <span className="row" style={{ gap: 4 }}><MessageCircle size={14} aria-hidden /> {p.comments}</span>
            </div>
          </article>
        ))}
        {list.data && <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />}
      </div>
      <Modal open={creating} title="Nova publicação" onClose={() => setCreating(false)}>
        <PostForm
          onCancel={() => setCreating(false)}
          onSaved={() => {
            setCreating(false)
            toast.success('Publicação criada!')
            qc.invalidateQueries({ queryKey: ['forum'] })
          }}
        />
      </Modal>
    </>
  )
}

export function PostForm({ initial, onSaved, onCancel }: { initial?: PostDetail; onSaved: (p: PostDetail) => void; onCancel: () => void }) {
  const [category, setCategory] = useState<ForumCategory>(initial?.post.category ?? 'QUESTIONS')
  const [title, setTitle] = useState(initial?.post.title ?? '')
  const [body, setBody] = useState(initial?.body ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v: Errors = {
      title: title.trim().length < 5 ? 'O título deve ter pelo menos 5 caracteres.' : undefined,
      body: body.trim().length < 10 ? 'Escreva pelo menos 10 caracteres.' : undefined,
    }
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    setError(null)
    try {
      const payload = { category, title: title.trim(), body: body.trim() }
      const saved = initial ? await api.put<PostDetail>(`/api/forum/posts/${initial.post.id}`, payload) : await api.post<PostDetail>('/api/forum/posts', payload)
      onSaved(saved)
    } catch (err) {
      if (err instanceof ApiError) setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="stack" onSubmit={submit} noValidate>
      {error && <Alert tone="error">{error}</Alert>}
      <SelectField label="Categoria" value={category} onChange={(e) => setCategory(e.target.value as ForumCategory)} options={options(forumCategoryLabel)} />
      <TextField label="Título" value={title} onChange={(e) => setTitle(e.target.value)} error={errors.title} maxLength={140} required />
      <TextAreaField label="Texto" value={body} onChange={(e) => setBody(e.target.value)} error={errors.body} maxLength={5000} hint="Não compartilhe dados pessoais nem valores que você não queira expor." required />
      <div className="modal-actions">
        <Button variant="secondary" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" loading={loading}>{initial ? 'Salvar' : 'Publicar'}</Button>
      </div>
    </form>
  )
}
