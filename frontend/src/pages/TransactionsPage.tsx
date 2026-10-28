import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Plus, Receipt, Trash2 } from 'lucide-react'
import { api, ApiError, errorMessage, qs } from '../api/client'
import type { CategoryOption, Goal, Page, Reward, Transaction, TxOrigin, TxSummary, TxType } from '../api/types'
import { SelectField, TextAreaField, TextField } from '../components/form'
import { hasErrors, type Errors } from '../utils/validation'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, EmptyState, ErrorState, PageHeader, Pagination, Spinner, Stat } from '../components/ui'
import { date, hasMaxTwoDecimals, money, parseAmount, todayIso } from '../utils/format'
import { originLabel } from '../utils/labels'

interface Filters {
  from: string
  to: string
  type: '' | TxType
  category: string
  q: string
  sort: 'DATE' | 'AMOUNT' | 'DESCRIPTION'
  direction: 'DESC' | 'ASC'
}

const firstOfMonth = () => todayIso().slice(0, 8) + '01'

export function TransactionsPage() {
  const [params, setParams] = useSearchParams()
  const qc = useQueryClient()
  const toast = useToast()
  const [filters, setFilters] = useState<Filters>({ from: firstOfMonth(), to: todayIso(), type: '', category: '', q: '', sort: 'DATE', direction: 'DESC' })
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Transaction | null>(null)
  const [deleting, setDeleting] = useState<Transaction | null>(null)
  const creating = params.get('nova') === '1'

  const categories = useQuery({ queryKey: ['tx-categories'], queryFn: () => api.get<CategoryOption[]>('/api/transactions/categories'), staleTime: Infinity })
  const list = useQuery({
    queryKey: ['transactions', filters, page],
    queryFn: () => api.get<Page<Transaction>>('/api/transactions' + qs({ ...filters, page, size: 15 })),
  })
  const summary = useQuery({
    queryKey: ['transactions', 'summary', filters.from, filters.to],
    queryFn: () => api.get<TxSummary>('/api/transactions/summary' + qs({ from: filters.from, to: filters.to })),
    enabled: !!filters.from && !!filters.to && filters.from <= filters.to,
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['transactions'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }
  const remove = useMutation({
    mutationFn: (id: number) => api.del(`/api/transactions/${id}`),
    onSuccess: () => {
      toast.success('Movimentação excluída.')
      setDeleting(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const setFilter = <K extends keyof Filters>(k: K, v: Filters[K]) => {
    setPage(0)
    setFilters((f) => ({ ...f, [k]: v, ...(k === 'type' ? { category: '' } : {}) }))
  }
  const categoryOptions = (categories.data ?? []).filter((c) => !filters.type || c.type === filters.type)
  const periodInvalid = filters.from > filters.to

  return (
    <>
      <PageHeader
        title="Transações"
        subtitle="Registre o que entra e o que sai para entender seus hábitos."
        actions={<Button onClick={() => setParams({ nova: '1' })}><Plus size={16} aria-hidden /> Nova transação</Button>}
      />

      <div className="grid grid-3" style={{ marginBottom: 16 }}>
        <Stat label="Receitas no período" value={<span className="positive">{money(summary.data?.income)}</span>} />
        <Stat label="Despesas no período" value={<span className="negative">{money(summary.data?.expense)}</span>} />
        <Stat label="Saldo do período" value={money(summary.data?.balance)} hint="Receitas − despesas registradas" />
      </div>

      <Card>
        <form className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', marginBottom: 12 }} onSubmit={(e) => e.preventDefault()} aria-label="Filtros">
          <TextField label="De" type="date" value={filters.from} onChange={(e) => setFilter('from', e.target.value)} error={periodInvalid ? 'Data inicial maior que a final' : undefined} />
          <TextField label="Até" type="date" value={filters.to} onChange={(e) => setFilter('to', e.target.value)} />
          <SelectField label="Tipo" value={filters.type} onChange={(e) => setFilter('type', e.target.value as Filters['type'])} placeholder="Todos" options={[{ value: 'INCOME', label: 'Receitas' }, { value: 'EXPENSE', label: 'Despesas' }]} />
          <SelectField label="Categoria" value={filters.category} onChange={(e) => setFilter('category', e.target.value)} placeholder="Todas" options={categoryOptions} />
          <TextField label="Buscar" type="search" value={filters.q} onChange={(e) => setFilter('q', e.target.value)} placeholder="Descrição" maxLength={60} />
          <SelectField
            label="Ordenar por"
            value={`${filters.sort}:${filters.direction}`}
            onChange={(e) => {
              const [sort, direction] = e.target.value.split(':') as [Filters['sort'], Filters['direction']]
              setFilters((f) => ({ ...f, sort, direction }))
            }}
            options={[
              { value: 'DATE:DESC', label: 'Mais recentes' },
              { value: 'DATE:ASC', label: 'Mais antigas' },
              { value: 'AMOUNT:DESC', label: 'Maior valor' },
              { value: 'AMOUNT:ASC', label: 'Menor valor' },
              { value: 'DESCRIPTION:ASC', label: 'Descrição (A-Z)' },
            ]}
          />
        </form>

        {list.isLoading && <Spinner />}
        {list.error && <ErrorState error={list.error} onRetry={() => list.refetch()} />}
        {list.data && list.data.content.length === 0 && (
          <EmptyState icon={<Receipt size={24} />} title="Nenhuma movimentação encontrada" description="Ajuste os filtros ou registre uma nova transação." action={<Button onClick={() => setParams({ nova: '1' })}>Registrar transação</Button>} />
        )}
        {list.data && list.data.content.length > 0 && (
          <div className="table-wrap">
            <table>
              <caption className="sr-only">Lista de transações</caption>
              <thead>
                <tr>
                  <th scope="col">Data</th>
                  <th scope="col">Descrição</th>
                  <th scope="col">Categoria</th>
                  <th scope="col" className="num">Valor</th>
                  <th scope="col"><span className="sr-only">Ações</span></th>
                </tr>
              </thead>
              <tbody>
                {list.data.content.map((t) => (
                  <tr key={t.id}>
                    <td className="nowrap">{date(t.occurredOn)}</td>
                    <td>
                      {t.description} {t.origin !== 'MANUAL' && <Badge tone="info">{originLabel[t.origin]}</Badge>}
                      {t.notes && <div className="small muted">{t.notes}</div>}
                    </td>
                    <td>{t.categoryLabel}</td>
                    <td className={`num nowrap ${t.type === 'INCOME' ? 'positive' : 'negative'}`}>
                      {t.type === 'INCOME' ? '+' : '−'} {money(t.amount)}
                    </td>
                    <td className="nowrap">
                      {t.origin !== 'EXTERNAL' && (
                        <>
                          <button className="btn btn-ghost btn-icon btn-sm" onClick={() => setEditing(t)} aria-label={`Editar ${t.description}`}><Pencil size={15} /></button>
                          <button className="btn btn-ghost btn-icon btn-sm" onClick={() => setDeleting(t)} aria-label={`Excluir ${t.description}`}><Trash2 size={15} /></button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {list.data && <Pagination page={page} totalPages={list.data.totalPages} onChange={setPage} />}
      </Card>

      <Modal open={creating || !!editing} title={editing ? 'Editar transação' : 'Nova transação'} onClose={() => { setEditing(null); setParams({}) }}>
        <TransactionForm
          key={editing?.id ?? 'new'}
          initial={editing}
          categories={categories.data ?? []}
          onDone={(reward) => {
            toast.success(editing ? 'Transação atualizada.' : 'Transação registrada!')
            toast.reward(reward)
            setEditing(null)
            setParams({})
            invalidate()
          }}
          onCancel={() => { setEditing(null); setParams({}) }}
        />
      </Modal>
      <ConfirmDialog
        open={!!deleting}
        title="Excluir transação?"
        message={`"${deleting?.description}" será removida permanentemente.`}
        confirmLabel="Excluir"
        danger
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
        onCancel={() => setDeleting(null)}
      />
    </>
  )
}

export function TransactionForm({ initial, categories, onDone, onCancel }: { initial: Transaction | null; categories: CategoryOption[]; onDone: (r?: Reward) => void; onCancel: () => void }) {
  const goals = useQuery({ queryKey: ['goals', 'ACTIVE'], queryFn: () => api.get<Goal[]>('/api/goals?status=ACTIVE') })
  const [type, setType] = useState<TxType>(initial?.type ?? 'EXPENSE')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [amount, setAmount] = useState(initial ? String(initial.amount) : '')
  const [category, setCategory] = useState(initial?.category ?? '')
  const [occurredOn, setOccurredOn] = useState(initial?.occurredOn ?? todayIso())
  const [notes, setNotes] = useState(initial?.notes ?? '')
  const [origin, setOrigin] = useState<TxOrigin>(initial?.origin ?? 'MANUAL')
  const [goalId, setGoalId] = useState(initial?.goalId ? String(initial.goalId) : '')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const value = parseAmount(amount)
    const v: Errors = {
      description: description.trim() ? undefined : 'Descreva a movimentação.',
      amount: isNaN(value) || value <= 0 ? 'Informe um valor maior que zero.' : !hasMaxTwoDecimals(value) ? 'Use no máximo 2 casas decimais.' : undefined,
      category: category ? undefined : 'Escolha uma categoria.',
      occurredOn: !occurredOn ? 'Informe a data.' : occurredOn > todayIso() ? 'A data não pode estar no futuro.' : undefined,
    }
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    setError(null)
    const body = { description: description.trim(), amount: value, type, category, occurredOn, notes: notes || null, origin, goalId: goalId ? Number(goalId) : null }
    try {
      if (initial) {
        await api.put(`/api/transactions/${initial.id}`, body)
        onDone()
      } else {
        const r = await api.post<{ reward: Reward }>('/api/transactions', body)
        onDone(r.reward)
      }
    } catch (err) {
      if (err instanceof ApiError) setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="stack">
      {error && <Alert tone="error">{error}</Alert>}
      <div className="chip-group" role="radiogroup" aria-label="Tipo">
        {(['EXPENSE', 'INCOME'] as TxType[]).map((t) => (
          <button key={t} type="button" role="radio" aria-checked={type === t} aria-pressed={type === t} className="chip" onClick={() => { setType(t); setCategory('') }}>
            {t === 'EXPENSE' ? 'Despesa' : 'Receita'}
          </button>
        ))}
      </div>
      <div className="form-grid">
        <TextField className="full" label="Descrição" value={description} onChange={(e) => setDescription(e.target.value)} error={errors.description} maxLength={120} required />
        <TextField label="Valor (R$)" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} error={errors.amount} required />
        <TextField label="Data" type="date" value={occurredOn} max={todayIso()} onChange={(e) => setOccurredOn(e.target.value)} error={errors.occurredOn} required />
        <SelectField label="Categoria" value={category} onChange={(e) => setCategory(e.target.value)} error={errors.category} placeholder="Selecione" options={categories.filter((c) => c.type === type)} required />
        <SelectField
          label="Natureza do registro"
          value={origin}
          onChange={(e) => setOrigin(e.target.value as TxOrigin)}
          options={[{ value: 'MANUAL', label: 'Registro da minha rotina' }, { value: 'SIMULATED', label: 'Exercício / simulação' }]}
          hint="Use “simulação” para praticar sem misturar com seus registros reais."
        />
        <SelectField className="full" label="Relacionar a uma meta (opcional)" value={goalId} onChange={(e) => setGoalId(e.target.value)} placeholder="Nenhuma" options={(goals.data ?? []).map((g) => ({ value: String(g.id), label: g.title }))} />
        <TextAreaField className="full" label="Observações (opcional)" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={500} />
      </div>
      <div className="modal-actions">
        <Button variant="secondary" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" loading={loading}>{initial ? 'Salvar' : 'Registrar'}</Button>
      </div>
    </form>
  )
}
