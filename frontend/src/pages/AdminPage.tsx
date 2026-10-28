import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, errorMessage, qs } from '../api/client'
import type { FeedbackStatus, FeedbackView, Page, Role } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { SelectField, TextAreaField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, Stars, EmptyState, ErrorState, PageHeader, Pagination, Spinner, Stat, Tabs } from '../components/ui'
import { dateTime } from '../utils/format'
import { feedbackStatusLabel, feedbackTypeLabel, options, reportReasonLabel, roleLabel } from '../utils/labels'
import { AdminCourses, AdminQuizzes } from './AdminContent'

type TabKey = 'overview' | 'users' | 'courses' | 'quizzes' | 'moderation' | 'feedback'

export function AdminPage() {
  const { can } = useAuth()
  const tabs: { value: TabKey; label: string }[] = [
    { value: 'overview', label: 'Visão geral' },
    ...(can('USER_MANAGE') ? [{ value: 'users' as const, label: 'Usuários' }] : []),
    ...(can('CONTENT_MANAGE') ? [{ value: 'courses' as const, label: 'Cursos' }, { value: 'quizzes' as const, label: 'Quizzes' }] : []),
    ...(can('FORUM_MODERATE') ? [{ value: 'moderation' as const, label: 'Moderação' }] : []),
    ...(can('FEEDBACK_MANAGE') ? [{ value: 'feedback' as const, label: 'Feedbacks' }] : []),
  ]
  const [tab, setTab] = useState<TabKey>('overview')
  return (
    <>
      <PageHeader title="Administração" subtitle="As opções exibidas dependem das suas permissões; todas são verificadas no servidor." />
      <Tabs label="Seções administrativas" value={tab} onChange={setTab} tabs={tabs} />
      {tab === 'overview' && <Overview />}
      {tab === 'users' && <Users />}
      {tab === 'courses' && <AdminCourses />}
      {tab === 'quizzes' && <AdminQuizzes />}
      {tab === 'moderation' && <Moderation />}
      {tab === 'feedback' && <FeedbackAdmin />}
    </>
  )
}

function Overview() {
  const stats = useQuery({ queryKey: ['admin-stats'], queryFn: () => api.get<Record<string, number>>('/api/admin/stats') })
  if (stats.isLoading) return <Spinner />
  if (stats.error) return <ErrorState error={stats.error} />
  const s = stats.data!
  const items: [string, string][] = [['users', 'Usuários'], ['courses', 'Cursos'], ['quizzes', 'Quizzes'], ['openReports', 'Denúncias abertas'], ['newFeedback', 'Feedbacks novos']]
  return (
    <>
      <div className="grid grid-4">
        {items.filter(([k]) => k in s).map(([k, label]) => <Stat key={k} label={label} value={s[k]} />)}
      </div>
      <p className="small muted" style={{ marginTop: 12 }}>Por privacidade, a área administrativa não dá acesso aos dados financeiros individuais dos usuários.</p>
    </>
  )
}

interface AdminUser {
  id: number
  email: string
  displayName: string
  role: Role
  enabled: boolean
  createdAt: string
  lastLoginAt: string | null
}

function Users() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const toast = useToast()
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [page, setPage] = useState(0)
  const [link, setLink] = useState<string | null>(null)
  const [toggle, setToggle] = useState<AdminUser | null>(null)
  const list = useQuery({ queryKey: ['admin-users', search, role, page], queryFn: () => api.get<Page<AdminUser>>('/api/admin/users' + qs({ q: search, role, page, size: 15 })) })
  const refresh = () => qc.invalidateQueries({ queryKey: ['admin-users'] })
  const changeRole = useMutation({ mutationFn: (p: { id: number; role: Role }) => api.put(`/api/admin/users/${p.id}/role`, { role: p.role }), onSuccess: () => { toast.success('Papel alterado. A pessoa precisará entrar novamente.'); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  const setEnabled = useMutation({ mutationFn: (u: AdminUser) => api.put(`/api/admin/users/${u.id}/status`, { enabled: !u.enabled }), onSuccess: () => { setToggle(null); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  const resetLink = useMutation({ mutationFn: (id: number) => api.post<{ resetUrl: string }>(`/api/admin/users/${id}/reset-link`), onSuccess: (r) => setLink(r.resetUrl), onError: (e) => toast.error(errorMessage(e)) })

  return (
    <Card>
      <form className="row" onSubmit={(e) => { e.preventDefault(); setSearch(q); setPage(0) }} style={{ marginBottom: 12, alignItems: 'flex-end' }}>
        <div className="field" style={{ flex: 1, minWidth: 200 }}>
          <label htmlFor="user-search">Buscar por e-mail ou apelido</label>
          <input id="user-search" className="input" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <SelectField label="Papel" value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }} placeholder="Todos" options={options(roleLabel)} />
        <Button type="submit" variant="secondary">Buscar</Button>
      </form>
      {list.isLoading && <Spinner />}
      {list.error && <ErrorState error={list.error} />}
      <div className="table-wrap">
        <table>
          <thead><tr><th>Usuário</th><th>Papel</th><th>Status</th><th>Último acesso</th><th><span className="sr-only">Ações</span></th></tr></thead>
          <tbody>
            {list.data?.content.map((u) => (
              <tr key={u.id}>
                <td><strong>{u.displayName}</strong><div className="small muted">{u.email}</div></td>
                <td>
                  <label className="sr-only" htmlFor={`role-${u.id}`}>Papel de {u.displayName}</label>
                  <select id={`role-${u.id}`} className="select" value={u.role} disabled={u.id === user?.id} onChange={(e) => changeRole.mutate({ id: u.id, role: e.target.value as Role })}>
                    {options(roleLabel).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                </td>
                <td>{u.enabled ? <Badge tone="success">Ativa</Badge> : <Badge tone="danger">Desativada</Badge>}</td>
                <td className="small">{dateTime(u.lastLoginAt)}</td>
                <td className="nowrap">
                  {u.id !== user?.id && (
                    <>
                      <Button size="sm" variant="ghost" onClick={() => setToggle(u)}>{u.enabled ? 'Desativar' : 'Ativar'}</Button>
                      <Button size="sm" variant="ghost" onClick={() => resetLink.mutate(u.id)}>Link de senha</Button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {list.data && <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />}
      <ConfirmDialog open={!!toggle} title={toggle?.enabled ? 'Desativar conta?' : 'Reativar conta?'} message={toggle?.enabled ? 'A pessoa será desconectada e não conseguirá entrar até a conta ser reativada.' : 'A pessoa poderá entrar novamente.'} confirmLabel={toggle?.enabled ? 'Desativar' : 'Reativar'} danger={toggle?.enabled} loading={setEnabled.isPending} onConfirm={() => toggle && setEnabled.mutate(toggle)} onCancel={() => setToggle(null)} />
      <Modal open={!!link} title="Link de redefinição de senha" onClose={() => setLink(null)} footer={<Button onClick={() => setLink(null)}>Fechar</Button>}>
        <Alert tone="warning">Link de uso único, válido por 30 minutos. Entregue somente à própria pessoa, por um canal confiável.</Alert>
        <input className="input" style={{ marginTop: 12 }} readOnly value={link ?? ''} aria-label="Link de redefinição" onFocus={(e) => e.target.select()} />
      </Modal>
    </Card>
  )
}

interface ReportView {
  id: number
  targetType: 'POST' | 'COMMENT'
  targetId: number
  postId: number | null
  targetTitle: string | null
  targetExcerpt: string | null
  targetAuthor: string | null
  targetStatus: string | null
  reason: string
  details: string | null
  status: string
  openReportsOnTarget: number
  createdAt: string
}

interface ModeratedItem {
  id: number
  type: 'POST' | 'COMMENT'
  postId: number
  title: string | null
  excerpt: string
  author: string
  status: string
  updatedAt: string
}

interface ReviewModeration {
  id: number
  courseId: number
  author: string
  rating: number
  comment: string | null
  hidden: boolean
  createdAt: string
}

function Moderation() {
  const qc = useQueryClient()
  const toast = useToast()
  const [section, setSection] = useState<'reports' | 'hidden' | 'reviews'>('reports')
  const reports = useQuery({ queryKey: ['mod-reports'], queryFn: () => api.get<Page<ReportView>>('/api/admin/moderation/reports?size=50'), enabled: section === 'reports' })
  const hidden = useQuery({ queryKey: ['mod-hidden'], queryFn: async () => {
    const [posts, comments] = await Promise.all([
      api.get<Page<ModeratedItem>>('/api/admin/moderation/posts?status=HIDDEN&size=50'),
      api.get<Page<ModeratedItem>>('/api/admin/moderation/comments?status=HIDDEN&size=50'),
    ])
    return [...posts.content, ...comments.content]
  }, enabled: section === 'hidden' })
  const reviews = useQuery({ queryKey: ['mod-reviews'], queryFn: () => api.get<Page<ReviewModeration>>('/api/admin/moderation/reviews?size=50'), enabled: section === 'reviews' })
  const refresh = () => { qc.invalidateQueries({ queryKey: ['mod-reports'] }); qc.invalidateQueries({ queryKey: ['mod-hidden'] }); qc.invalidateQueries({ queryKey: ['mod-reviews'] }); qc.invalidateQueries({ queryKey: ['admin-stats'] }) }
  const resolve = useMutation({ mutationFn: (p: { id: number; action: string }) => api.post(`/api/admin/moderation/reports/${p.id}/resolve`, { action: p.action }), onSuccess: () => { toast.success('Denúncia tratada.'); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  const moderate = useMutation({ mutationFn: (p: { item: ModeratedItem; action: string }) => api.post(`/api/admin/moderation/${p.item.type === 'POST' ? 'posts' : 'comments'}/${p.item.id}`, { action: p.action }), onSuccess: () => { toast.success('Conteúdo atualizado.'); refresh() }, onError: (e) => toast.error(errorMessage(e)) })
  const reviewHidden = useMutation({ mutationFn: (p: { id: number; hidden: boolean }) => api.post(`/api/admin/moderation/reviews/${p.id}`, { hidden: p.hidden }), onSuccess: refresh })

  return (
    <>
      <div className="chip-group" role="group" aria-label="Seção de moderação" style={{ marginBottom: 12 }}>
        <button className="chip" aria-pressed={section === 'reports'} onClick={() => setSection('reports')}>Denúncias abertas</button>
        <button className="chip" aria-pressed={section === 'hidden'} onClick={() => setSection('hidden')}>Conteúdo ocultado</button>
        <button className="chip" aria-pressed={section === 'reviews'} onClick={() => setSection('reviews')}>Avaliações de cursos</button>
      </div>
      {section === 'reports' && (
        <div className="stack">
          {reports.isLoading && <Spinner />}
          {reports.data?.content.length === 0 && <Card><EmptyState title="Nenhuma denúncia aberta" description="Tudo tranquilo na comunidade." /></Card>}
          {reports.data?.content.map((r) => (
            <Card key={r.id}>
              <div className="row small muted" style={{ marginBottom: 6 }}>
                <Badge tone="danger">{reportReasonLabel[r.reason] ?? r.reason}</Badge>
                <span>{r.targetType === 'POST' ? 'Publicação' : 'Comentário'} de {r.targetAuthor ?? '—'}</span>·<span>{r.openReportsOnTarget} denúncia(s) aberta(s)</span>·<span>{dateTime(r.createdAt)}</span>
                {r.targetStatus && <Badge>{r.targetStatus === 'HIDDEN' ? 'Oculto' : r.targetStatus === 'REMOVED' ? 'Removido' : 'Visível'}</Badge>}
              </div>
              {r.targetTitle && <strong>{r.targetTitle}</strong>}
              <p className="small" style={{ whiteSpace: 'pre-wrap' }}>{r.targetExcerpt}</p>
              {r.details && <p className="small muted">Detalhes: {r.details}</p>}
              <div className="row">
                <Button size="sm" variant="secondary" onClick={() => resolve.mutate({ id: r.id, action: 'HIDE' })}>Ocultar</Button>
                <Button size="sm" variant="danger" onClick={() => resolve.mutate({ id: r.id, action: 'REMOVE' })}>Remover</Button>
                <Button size="sm" variant="ghost" onClick={() => resolve.mutate({ id: r.id, action: 'DISMISS' })}>Descartar denúncia</Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      {section === 'hidden' && (
        <div className="stack">
          {hidden.isLoading && <Spinner />}
          {hidden.data?.length === 0 && <Card><EmptyState title="Nenhum conteúdo ocultado" /></Card>}
          {hidden.data?.map((i) => (
            <Card key={i.type + i.id}>
              <div className="small muted">{i.type === 'POST' ? 'Publicação' : 'Comentário'} de {i.author} · {dateTime(i.updatedAt)}</div>
              {i.title && <strong>{i.title}</strong>}
              <p className="small">{i.excerpt}</p>
              <div className="row">
                <Button size="sm" variant="secondary" onClick={() => moderate.mutate({ item: i, action: 'RESTORE' })}>Restaurar</Button>
                <Button size="sm" variant="danger" onClick={() => moderate.mutate({ item: i, action: 'REMOVE' })}>Remover</Button>
              </div>
            </Card>
          ))}
        </div>
      )}
      {section === 'reviews' && (
        <Card>
          {reviews.data?.content.length === 0 && <EmptyState title="Nenhuma avaliação" />}
          <ul className="list">
            {reviews.data?.content.map((r) => (
              <li key={r.id} className="row-between">
                <span><strong>{r.author}</strong> · <Stars value={r.rating} size={12} /> · curso #{r.courseId}<div className="small">{r.comment ?? <span className="muted">Sem comentário</span>}</div></span>
                <Button size="sm" variant={r.hidden ? 'secondary' : 'ghost'} onClick={() => reviewHidden.mutate({ id: r.id, hidden: !r.hidden })}>{r.hidden ? 'Mostrar' : 'Ocultar'}</Button>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  )
}

function FeedbackAdmin() {
  const qc = useQueryClient()
  const toast = useToast()
  const [status, setStatus] = useState('NEW')
  const [type, setType] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<FeedbackView | null>(null)
  const [response, setResponse] = useState('')
  const [newStatus, setNewStatus] = useState<FeedbackStatus>('RESOLVED')
  const list = useQuery({ queryKey: ['admin-feedback', status, type, page], queryFn: () => api.get<Page<FeedbackView>>('/api/admin/feedback' + qs({ status, type, page, size: 15 })) })
  const save = useMutation({
    mutationFn: () => api.put(`/api/admin/feedback/${selected!.id}`, { status: newStatus, response: response || null }),
    onSuccess: () => { toast.success('Feedback atualizado.'); setSelected(null); qc.invalidateQueries({ queryKey: ['admin-feedback'] }) },
    onError: (e) => toast.error(errorMessage(e)),
  })
  return (
    <Card>
      <div className="row" style={{ marginBottom: 12 }}>
        <SelectField label="Status" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} placeholder="Todos" options={options(feedbackStatusLabel)} />
        <SelectField label="Tipo" value={type} onChange={(e) => { setType(e.target.value); setPage(0) }} placeholder="Todos" options={options(feedbackTypeLabel)} />
      </div>
      {list.data?.content.length === 0 && <EmptyState title="Nenhum feedback com esses filtros" />}
      <ul className="list">
        {list.data?.content.map((f) => (
          <li key={f.id} className="row-between" style={{ alignItems: 'flex-start' }}>
            <span style={{ flex: 1 }}>
              <strong>{f.subject}</strong> <Badge>{feedbackTypeLabel[f.type]}</Badge> <Badge tone="info">{feedbackStatusLabel[f.status]}</Badge>
              <div className="small" style={{ whiteSpace: 'pre-wrap' }}>{f.message}</div>
              <div className="small muted">{f.author ?? '—'} · {dateTime(f.createdAt)}</div>
            </span>
            <Button size="sm" variant="secondary" onClick={() => { setSelected(f); setResponse(f.adminResponse ?? ''); setNewStatus(f.status === 'NEW' ? 'RESOLVED' : f.status) }}>Tratar</Button>
          </li>
        ))}
      </ul>
      {list.data && <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />}
      <Modal open={!!selected} title={`Tratar: ${selected?.subject ?? ''}`} onClose={() => setSelected(null)} footer={<><Button variant="secondary" onClick={() => setSelected(null)}>Cancelar</Button><Button onClick={() => save.mutate()} loading={save.isPending}>Salvar</Button></>}>
        <div className="stack">
          <SelectField label="Novo status" value={newStatus} onChange={(e) => setNewStatus(e.target.value as FeedbackStatus)} options={options(feedbackStatusLabel)} />
          <TextAreaField label="Resposta para a pessoa (opcional)" value={response} onChange={(e) => setResponse(e.target.value)} maxLength={1000} hint="Ao responder, a pessoa recebe uma notificação interna." />
        </div>
      </Modal>
    </Card>
  )
}
