import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDownCircle, ArrowLeft, ArrowUpCircle, Pencil, XCircle } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Goal, GoalMovement, Page, Reward } from '../api/types'
import { TextField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, EmptyState, ErrorState, PageHeader, Pagination, ProgressBar, Spinner, Stat } from '../components/ui'
import { date, dateTime, hasMaxTwoDecimals, money, parseAmount, percent } from '../utils/format'
import { goalCategoryLabel, goalStatusLabel } from '../utils/labels'
import { GoalForm, type GoalPayload } from './GoalForm'

export function GoalDetailPage() {
  const { id } = useParams()
  const qc = useQueryClient()
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(false)
  const [confirmCancel, setConfirmCancel] = useState(false)
  const goal = useQuery({ queryKey: ['goal', id], queryFn: () => api.get<Goal>(`/api/goals/${id}`) })
  const movements = useQuery({
    queryKey: ['goal', id, 'movements', page],
    queryFn: () => api.get<Page<GoalMovement>>(`/api/goals/${id}/movements?page=${page}&size=10`),
  })

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['goal', id] })
    qc.invalidateQueries({ queryKey: ['goals'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const update = useMutation({
    mutationFn: (p: GoalPayload) => api.put<Goal>(`/api/goals/${id}`, p),
    onSuccess: () => {
      toast.success('Meta atualizada.')
      setEditing(false)
      refresh()
    },
  })
  const cancel = useMutation({
    mutationFn: () => api.post<Goal>(`/api/goals/${id}/cancel`),
    onSuccess: () => {
      toast.success('Meta cancelada.')
      setConfirmCancel(false)
      refresh()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (goal.isLoading) return <Spinner />
  if (goal.error) return <ErrorState error={goal.error} onRetry={() => goal.refetch()} />
  const g = goal.data!

  return (
    <>
      <Link to="/metas" className="btn btn-ghost btn-sm" style={{ marginBottom: 8 }}><ArrowLeft size={14} aria-hidden /> Metas</Link>
      <PageHeader
        title={g.title}
        subtitle={g.description ?? goalCategoryLabel[g.category]}
        actions={
          g.status !== 'CANCELLED' && (
            <>
              <Button variant="secondary" onClick={() => setEditing(true)}><Pencil size={16} aria-hidden /> Editar</Button>
              <Button variant="ghost" onClick={() => setConfirmCancel(true)}><XCircle size={16} aria-hidden /> Cancelar meta</Button>
            </>
          )
        }
      />
      {g.status === 'COMPLETED' && <Alert tone="success">🎉 Meta concluída em {dateTime(g.completedAt)}. Parabéns pela disciplina!</Alert>}
      {g.status === 'CANCELLED' && <Alert tone="warning">Esta meta foi cancelada. O histórico continua disponível para consulta.</Alert>}

      <div className="grid grid-4" style={{ margin: '16px 0' }}>
        <Stat label="Acumulado" value={money(g.savedAmount)} />
        <Stat label="Objetivo" value={money(g.targetAmount)} />
        <Stat label="Falta" value={money(g.remainingAmount)} />
        <Stat label="Prazo" value={g.deadline ? date(g.deadline) : 'Sem prazo'} hint={g.suggestedMonthlySaving ? `Guarde cerca de ${money(g.suggestedMonthlySaving)}/mês` : undefined} />
      </div>

      <Card>
        <div className="row-between" style={{ marginBottom: 8 }}>
          <Badge tone={g.status === 'COMPLETED' ? 'success' : g.status === 'CANCELLED' ? 'danger' : 'primary'}>{goalStatusLabel[g.status]}</Badge>
          <strong>{percent(g.progressPercent)}</strong>
        </div>
        <ProgressBar value={g.progressPercent} label="Progresso da meta" />
      </Card>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        {g.status !== 'CANCELLED' && <MovementForm goal={g} onDone={refresh} />}
        <Card title="Histórico de movimentações">
          {movements.isLoading && <Spinner />}
          {movements.error && <ErrorState error={movements.error} />}
          {movements.data?.content.length === 0 && <EmptyState title="Nenhuma movimentação" description="Registre sua primeira contribuição para começar." />}
          <ul className="list">
            {movements.data?.content.map((m) => (
              <li key={m.id} className="row-between">
                <span className="row" style={{ gap: 8 }}>
                  {m.type === 'CONTRIBUTION' ? <ArrowUpCircle size={18} className="positive" aria-hidden /> : <ArrowDownCircle size={18} className="negative" aria-hidden />}
                  <span>
                    <strong>{m.type === 'CONTRIBUTION' ? 'Contribuição' : 'Retirada'}</strong>
                    <div className="small muted">{dateTime(m.createdAt)}{m.note ? ` · ${m.note}` : ''}</div>
                  </span>
                </span>
                <span className={`mono ${m.type === 'CONTRIBUTION' ? 'positive' : 'negative'}`}>
                  {m.type === 'CONTRIBUTION' ? '+' : '−'} {money(m.amount)}
                </span>
              </li>
            ))}
          </ul>
          {movements.data && <Pagination page={page} totalPages={movements.data.totalPages} onChange={setPage} />}
        </Card>
      </div>

      <Modal open={editing} title="Editar meta" onClose={() => setEditing(false)}>
        <GoalForm initial={g} submitLabel="Salvar" onCancel={() => setEditing(false)} onSubmit={(p) => update.mutateAsync(p).then(() => undefined)} />
      </Modal>
      <ConfirmDialog
        open={confirmCancel}
        title="Cancelar meta?"
        message="A meta deixará de aceitar contribuições e não poderá ser reativada. O histórico será mantido."
        confirmLabel="Cancelar meta"
        danger
        loading={cancel.isPending}
        onConfirm={() => cancel.mutate()}
        onCancel={() => setConfirmCancel(false)}
      />
    </>
  )
}

function MovementForm({ goal, onDone }: { goal: Goal; onDone: () => void }) {
  const toast = useToast()
  const [type, setType] = useState<'CONTRIBUTION' | 'WITHDRAWAL'>('CONTRIBUTION')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const value = parseAmount(amount)
    if (isNaN(value) || value <= 0) return setError('Informe um valor maior que zero.')
    if (!hasMaxTwoDecimals(value)) return setError('Use no máximo 2 casas decimais.')
    if (type === 'WITHDRAWAL' && value > goal.savedAmount) return setError(`Você só pode retirar até ${money(goal.savedAmount)}.`)
    setLoading(true)
    try {
      const r = await api.post<{ goalCompleted: boolean; reward: Reward }>(`/api/goals/${goal.id}/movements`, { type, amount: value, note: note || null })
      toast.success(type === 'CONTRIBUTION' ? 'Contribuição registrada!' : 'Retirada registrada.')
      if (r.goalCompleted) toast.success('🎉 Meta alcançada!')
      toast.reward(r.reward)
      setAmount('')
      setNote('')
      onDone()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="Registrar movimentação">
      <form className="stack" onSubmit={submit} noValidate>
        <div className="chip-group" role="radiogroup" aria-label="Tipo de movimentação">
          <button type="button" role="radio" aria-checked={type === 'CONTRIBUTION'} aria-pressed={type === 'CONTRIBUTION'} className="chip" onClick={() => setType('CONTRIBUTION')}>Contribuição</button>
          <button type="button" role="radio" aria-checked={type === 'WITHDRAWAL'} aria-pressed={type === 'WITHDRAWAL'} className="chip" onClick={() => setType('WITHDRAWAL')} disabled={goal.savedAmount <= 0}>Retirada</button>
        </div>
        {error && <Alert tone="error">{error}</Alert>}
        <TextField label="Valor (R$)" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="Ex.: 25,00" required />
        <TextField label="Observação (opcional)" value={note} onChange={(e) => setNote(e.target.value)} maxLength={200} />
        <Button type="submit" loading={loading}>{type === 'CONTRIBUTION' ? 'Adicionar à meta' : 'Registrar retirada'}</Button>
        <p className="small muted">Registro educativo: a plataforma não movimenta dinheiro real.</p>
      </form>
    </Card>
  )
}
