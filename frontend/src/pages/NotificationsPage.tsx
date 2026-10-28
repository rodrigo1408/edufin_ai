import { useState } from 'react'
import { Link } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell, Check, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { NotificationView, Page } from '../api/types'
import { useToast } from '../components/Toast'
import { Button, Card, EmptyState, ErrorState, PageHeader, Pagination, Spinner, Tabs } from '../components/ui'
import { relativeTime } from '../utils/format'

export function NotificationsPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [unreadOnly, setUnreadOnly] = useState<'all' | 'unread'>('all')
  const [page, setPage] = useState(0)
  const list = useQuery({
    queryKey: ['notifications', unreadOnly, page],
    queryFn: () => api.get<Page<NotificationView>>(`/api/notifications?unreadOnly=${unreadOnly === 'unread'}&page=${page}&size=15`),
  })
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['notifications'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }
  const read = useMutation({ mutationFn: (id: number) => api.post(`/api/notifications/${id}/read`), onSuccess: refresh })
  const readAll = useMutation({ mutationFn: () => api.post('/api/notifications/read-all'), onSuccess: () => { refresh(); toast.success('Todas marcadas como lidas.') } })
  const remove = useMutation({ mutationFn: (id: number) => api.del(`/api/notifications/${id}`), onSuccess: refresh, onError: (e) => toast.error(errorMessage(e)) })

  return (
    <>
      <PageHeader
        title="Notificações"
        subtitle="Avisos sobre suas metas, estudos e conquistas. Preferências em Perfil."
        actions={<Button variant="secondary" onClick={() => readAll.mutate()} loading={readAll.isPending}><Check size={16} aria-hidden /> Marcar todas como lidas</Button>}
      />
      <Tabs label="Filtro" value={unreadOnly} onChange={(v) => { setUnreadOnly(v); setPage(0) }} tabs={[{ value: 'all', label: 'Todas' }, { value: 'unread', label: 'Não lidas' }]} />
      <Card>
        {list.isLoading && <Spinner />}
        {list.error && <ErrorState error={list.error} onRetry={() => list.refetch()} />}
        {list.data?.content.length === 0 && <EmptyState icon={<Bell size={24} />} title="Nenhuma notificação" description="Quando algo importante acontecer, avisaremos aqui." />}
        <ul className="list">
          {list.data?.content.map((n) => (
            <li key={n.id} className="row-between" style={{ alignItems: 'flex-start' }}>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div className="row" style={{ gap: 8 }}>
                  {!n.read && <span className="badge badge-primary">Nova</span>}
                  {n.link ? <Link to={n.link} onClick={() => !n.read && read.mutate(n.id)}><strong>{n.title}</strong></Link> : <strong>{n.title}</strong>}
                </div>
                <div className="small">{n.message}</div>
                <div className="small muted">{relativeTime(n.createdAt)}</div>
              </div>
              <div className="row" style={{ gap: 4 }}>
                {!n.read && <button className="btn btn-ghost btn-icon btn-sm" onClick={() => read.mutate(n.id)} aria-label={`Marcar "${n.title}" como lida`}><Check size={15} /></button>}
                <button className="btn btn-ghost btn-icon btn-sm" onClick={() => remove.mutate(n.id)} aria-label={`Excluir notificação "${n.title}"`}><Trash2 size={15} /></button>
              </div>
            </li>
          ))}
        </ul>
        {list.data && <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />}
      </Card>
      <p className="small muted" style={{ marginTop: 10 }}>As notificações são internas à plataforma. Envio por e-mail ou push ainda não está configurado.</p>
    </>
  )
}
