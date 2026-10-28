import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Flag, Heart, Pencil, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { CommentView, Page, PostDetail } from '../api/types'
import { SelectField, TextAreaField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { BackLink, Alert, Badge, Button, Card, ErrorState, Spinner } from '../components/ui'
import { relativeTime } from '../utils/format'
import { forumCategoryLabel, options, reportReasonLabel } from '../utils/labels'
import { PostForm } from './CommunityPage'

type ReportTarget = { kind: 'post' | 'comment'; id: number } | null

export function PostPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const toast = useToast()
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const [reportTarget, setReportTarget] = useState<ReportTarget>(null)
  const post = useQuery({ queryKey: ['post', id], queryFn: () => api.get<PostDetail>(`/api/forum/posts/${id}`) })
  const comments = useQuery({ queryKey: ['post', id, 'comments'], queryFn: () => api.get<Page<CommentView>>(`/api/forum/posts/${id}/comments?size=100`) })

  const like = useMutation({
    mutationFn: (liked: boolean) => (liked ? api.del<{ liked: boolean; likes: number }>(`/api/forum/posts/${id}/like`) : api.post<{ liked: boolean; likes: number }>(`/api/forum/posts/${id}/like`)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['post', id] }),
    onError: (e) => toast.error(errorMessage(e)),
  })
  const remove = useMutation({
    mutationFn: () => api.del(`/api/forum/posts/${id}`),
    onSuccess: () => {
      toast.success('Publicação excluída.')
      qc.invalidateQueries({ queryKey: ['forum'] })
      navigate('/comunidade')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (post.isLoading) return <Spinner />
  if (post.error) return <><ErrorState error={post.error} /><p style={{ marginTop: 12 }}><Link to="/comunidade">← Voltar à comunidade</Link></p></>
  const p = post.data!

  return (
    <>
      <BackLink to="/comunidade">Comunidade</BackLink>
      <article className="card stack">
        <div className="row small muted">
          <Badge tone="primary">{forumCategoryLabel[p.post.category]}</Badge>
          <span>{p.post.author}</span>·<span>{relativeTime(p.post.createdAt)}</span>{p.post.edited && <span>· editado</span>}
        </div>
        <h1>{p.post.title}</h1>
        <p style={{ whiteSpace: 'pre-wrap' }}>{p.body}</p>
        <div className="row">
          <Button variant={p.post.likedByMe ? 'primary' : 'secondary'} size="sm" onClick={() => like.mutate(p.post.likedByMe)} aria-pressed={p.post.likedByMe} loading={like.isPending}>
            <Heart size={15} aria-hidden fill={p.post.likedByMe ? 'currentColor' : 'none'} /> {p.post.likes} {p.post.likes === 1 ? 'curtida' : 'curtidas'}
          </Button>
          <span className="spacer" />
          {p.post.mine ? (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing(true)}><Pencil size={15} aria-hidden /> Editar</Button>
              <Button variant="ghost" size="sm" onClick={() => setConfirmDelete(true)}><Trash2 size={15} aria-hidden /> Excluir</Button>
            </>
          ) : (
            <Button variant="ghost" size="sm" onClick={() => setReportTarget({ kind: 'post', id: p.post.id })}><Flag size={15} aria-hidden /> Denunciar</Button>
          )}
        </div>
      </article>

      <Card title={`Comentários (${comments.data?.totalElements ?? 0})`} className="stack">
        {comments.data?.content.length === 0 && <p className="muted small">Seja a primeira pessoa a comentar.</p>}
        <ul className="list">
          {comments.data?.content.map((c) => (
            <CommentItem key={c.id} comment={c} postId={id!} onReport={() => setReportTarget({ kind: 'comment', id: c.id })} />
          ))}
        </ul>
        <CommentForm postId={id!} />
      </Card>

      <Modal open={editing} title="Editar publicação" onClose={() => setEditing(false)}>
        <PostForm initial={p} onCancel={() => setEditing(false)} onSaved={(saved) => { qc.setQueryData(['post', id], saved); setEditing(false); toast.success('Publicação atualizada.') }} />
      </Modal>
      <ConfirmDialog open={confirmDelete} title="Excluir publicação?" message="A publicação e seus comentários serão removidos permanentemente." confirmLabel="Excluir" danger loading={remove.isPending} onConfirm={() => remove.mutate()} onCancel={() => setConfirmDelete(false)} />
      <ReportDialog target={reportTarget} onClose={() => setReportTarget(null)} />
    </>
  )
}

function CommentItem({ comment, postId, onReport }: { comment: CommentView; postId: string; onReport: () => void }) {
  const qc = useQueryClient()
  const toast = useToast()
  const [editing, setEditing] = useState(false)
  const [body, setBody] = useState(comment.body)
  const [confirm, setConfirm] = useState(false)
  const refresh = () => qc.invalidateQueries({ queryKey: ['post', postId] })
  const save = useMutation({ mutationFn: () => api.put(`/api/forum/comments/${comment.id}`, { body }), onSuccess: () => { setEditing(false); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  const remove = useMutation({ mutationFn: () => api.del(`/api/forum/comments/${comment.id}`), onSuccess: () => { setConfirm(false); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  return (
    <li className="stack" style={{ gap: 4 }}>
      <div className="row-between small">
        <span><strong>{comment.author}</strong> <span className="muted">· {relativeTime(comment.createdAt)}{comment.edited ? ' · editado' : ''}</span></span>
        <span className="row" style={{ gap: 2 }}>
          {comment.mine ? (
            <>
              <button className="btn btn-ghost btn-icon btn-sm" aria-label="Editar comentário" onClick={() => setEditing(!editing)}><Pencil size={14} /></button>
              <button className="btn btn-ghost btn-icon btn-sm" aria-label="Excluir comentário" onClick={() => setConfirm(true)}><Trash2 size={14} /></button>
            </>
          ) : (
            <button className="btn btn-ghost btn-icon btn-sm" aria-label="Denunciar comentário" onClick={onReport}><Flag size={14} /></button>
          )}
        </span>
      </div>
      {editing ? (
        <div className="stack">
          <TextAreaField label="Editar comentário" value={body} onChange={(e) => setBody(e.target.value)} maxLength={2000} />
          <div className="row"><Button size="sm" onClick={() => save.mutate()} loading={save.isPending}>Salvar</Button><Button size="sm" variant="secondary" onClick={() => setEditing(false)}>Cancelar</Button></div>
        </div>
      ) : (
        <p className="small" style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{comment.body}</p>
      )}
      <ConfirmDialog open={confirm} title="Excluir comentário?" message="Esta ação não pode ser desfeita." confirmLabel="Excluir" danger loading={remove.isPending} onConfirm={() => remove.mutate()} onCancel={() => setConfirm(false)} />
    </li>
  )
}

function CommentForm({ postId }: { postId: string }) {
  const qc = useQueryClient()
  const [body, setBody] = useState('')
  const [error, setError] = useState<string | null>(null)
  const send = useMutation({
    mutationFn: () => api.post(`/api/forum/posts/${postId}/comments`, { body: body.trim() }),
    onSuccess: () => { setBody(''); qc.invalidateQueries({ queryKey: ['post', postId] }) },
    onError: (e) => setError(errorMessage(e)),
  })
  const submit = (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (body.trim().length < 2) return setError('Escreva um comentário.')
    send.mutate()
  }
  return (
    <form className="stack" onSubmit={submit} noValidate>
      <TextAreaField label="Seu comentário" value={body} onChange={(e) => setBody(e.target.value)} maxLength={2000} />
      {error && <Alert tone="error">{error}</Alert>}
      <div><Button type="submit" loading={send.isPending}>Comentar</Button></div>
    </form>
  )
}

function ReportDialog({ target, onClose }: { target: ReportTarget; onClose: () => void }) {
  const toast = useToast()
  const [reason, setReason] = useState('SPAM')
  const [details, setDetails] = useState('')
  const [error, setError] = useState<string | null>(null)
  const send = useMutation({
    mutationFn: () => api.post(`/api/forum/${target!.kind === 'post' ? 'posts' : 'comments'}/${target!.id}/report`, { reason, details: details || null }),
    onSuccess: () => { toast.success('Denúncia enviada. Obrigado por ajudar a manter a comunidade segura!'); setDetails(''); onClose() },
    onError: (e) => setError(errorMessage(e)),
  })
  return (
    <Modal open={!!target} title="Denunciar conteúdo" onClose={onClose} footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button variant="danger" onClick={() => send.mutate()} loading={send.isPending}>Enviar denúncia</Button></>}>
      <div className="stack">
        <SelectField label="Motivo" value={reason} onChange={(e) => setReason(e.target.value)} options={options(reportReasonLabel)} />
        <TextAreaField label="Detalhes (opcional)" value={details} onChange={(e) => setDetails(e.target.value)} maxLength={500} />
        {error && <Alert tone="error">{error}</Alert>}
        <p className="small muted">Conteúdos com várias denúncias ficam ocultos até a análise da moderação.</p>
      </div>
    </Modal>
  )
}
