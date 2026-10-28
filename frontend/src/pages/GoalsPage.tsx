import { useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarClock, Plus, Target } from 'lucide-react'
import { api } from '../api/client'
import type { Goal, Reward } from '../api/types'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Badge, Button, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Tabs } from '../components/ui'
import { date, money, percent } from '../utils/format'
import { goalCategoryLabel, goalStatusLabel } from '../utils/labels'
import { GoalForm, type GoalPayload } from './GoalForm'

type Filter = 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'ALL'

export function GoalsPage() {
  const [params, setParams] = useSearchParams()
  const [filter, setFilter] = useState<Filter>('ACTIVE')
  const creating = params.get('nova') === '1'
  const qc = useQueryClient()
  const toast = useToast()
  const goals = useQuery({ queryKey: ['goals', filter], queryFn: () => api.get<Goal[]>(`/api/goals?status=${filter}`) })

  const create = useMutation({
    mutationFn: (p: GoalPayload) => api.post<{ goal: Goal; reward: Reward }>('/api/goals', p),
    onSuccess: (r) => {
      toast.success('Meta criada!')
      toast.reward(r.reward)
      qc.invalidateQueries({ queryKey: ['goals'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      setParams({})
    },
  })

  return (
    <>
      <PageHeader
        title="Metas financeiras"
        subtitle="Transforme desejos em planos: defina valor, prazo e acompanhe cada contribuição."
        actions={<Button onClick={() => setParams({ nova: '1' })}><Plus size={16} aria-hidden /> Nova meta</Button>}
      />
      <Tabs
        label="Filtrar metas"
        value={filter}
        onChange={setFilter}
        tabs={[
          { value: 'ACTIVE', label: 'Ativas' },
          { value: 'COMPLETED', label: 'Concluídas' },
          { value: 'CANCELLED', label: 'Canceladas' },
          { value: 'ALL', label: 'Todas' },
        ]}
      />
      {goals.isLoading && <Spinner />}
      {goals.error && <ErrorState error={goals.error} onRetry={() => goals.refetch()} />}
      {goals.data && goals.data.length === 0 && (
        <div className="card">
          <EmptyState
            icon={<Target size={24} />}
            title={filter === 'ACTIVE' ? 'Nenhuma meta ativa' : 'Nada por aqui'}
            description="Uma boa meta diz o quê, quanto e até quando. Ex.: juntar R$ 600 para um fone até dezembro."
            action={<Button onClick={() => setParams({ nova: '1' })}>Criar meta</Button>}
          />
        </div>
      )}
      <div className="grid grid-3">
        {goals.data?.map((g) => (
          <Link key={g.id} to={`/metas/${g.id}`} className="card stack" style={{ textDecoration: 'none', color: 'inherit', gap: 10 }}>
            <div className="row-between">
              <Badge>{goalCategoryLabel[g.category]}</Badge>
              <Badge tone={g.status === 'COMPLETED' ? 'success' : g.status === 'CANCELLED' ? 'danger' : 'primary'}>{goalStatusLabel[g.status]}</Badge>
            </div>
            <h3 style={{ margin: 0 }}>{g.title}</h3>
            <div className="row-between small">
              <span className="mono">{money(g.savedAmount)} de {money(g.targetAmount)}</span>
              <strong>{percent(g.progressPercent)}</strong>
            </div>
            <ProgressBar value={g.progressPercent} label={`Progresso de ${g.title}`} />
            {g.deadline && (
              <span className="small muted row" style={{ gap: 6 }}>
                <CalendarClock size={14} aria-hidden /> Prazo: {date(g.deadline)}
                {g.status === 'ACTIVE' && g.daysRemaining !== null && g.daysRemaining >= 0 && ` (${g.daysRemaining} dias)`}
              </span>
            )}
          </Link>
        ))}
      </div>
      <Modal open={creating} title="Nova meta" onClose={() => setParams({})}>
        <GoalForm submitLabel="Criar meta" onCancel={() => setParams({})} onSubmit={(p) => create.mutateAsync(p).then(() => undefined)} />
      </Modal>
    </>
  )
}
