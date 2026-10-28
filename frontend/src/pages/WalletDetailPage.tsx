import { useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Archive, Pencil, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Page, ScenarioResponse, WalletDetail, WalletMovement } from '../api/types'
import { ScenarioChart, SimpleLine } from '../components/charts'
import { SelectField, TextField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { BackLink, Alert, Badge, Button, Card, ErrorState, PageHeader, Pagination, Spinner, Stat } from '../components/ui'
import { date, money, parseAmount, percent, todayIso } from '../utils/format'
import { walletMovementLabel } from '../utils/labels'
import { WalletForm } from './WalletsPage'

function lastClosedMonths(n: number, createdAt: string): string[] {
  const created = createdAt.slice(0, 7)
  const out: string[] = []
  const d = new Date()
  for (let i = 1; i <= n; i++) {
    const m = new Date(d.getFullYear(), d.getMonth() - i, 1)
    const key = `${m.getFullYear()}-${String(m.getMonth() + 1).padStart(2, '0')}`
    if (key >= created) out.push(key)
  }
  return out
}

export function WalletDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const detail = useQuery({ queryKey: ['wallet', id], queryFn: () => api.get<WalletDetail>(`/api/wallets/${id}`) })
  const movements = useQuery({ queryKey: ['wallet', id, 'movements', page], queryFn: () => api.get<Page<WalletMovement>>(`/api/wallets/${id}/movements?page=${page}&size=10`) })
  const refresh = () => { qc.invalidateQueries({ queryKey: ['wallet', id] }); qc.invalidateQueries({ queryKey: ['wallets'] }) }

  const archive = useMutation({ mutationFn: (archived: boolean) => api.post(`/api/wallets/${id}/archive?archived=${archived}`), onSuccess: refresh })
  const remove = useMutation({ mutationFn: () => api.del(`/api/wallets/${id}`), onSuccess: () => { toast.success('Carteira excluída.'); qc.invalidateQueries({ queryKey: ['wallets'] }); navigate('/carteiras') } })
  const applyReturn = useMutation({
    mutationFn: (period: string) => api.post(`/api/wallets/${id}/returns`, { period }),
    onSuccess: () => { toast.success('Rendimento simulado aplicado.'); refresh() },
    onError: (e) => toast.error(errorMessage(e)),
  })
  const [period, setPeriod] = useState('')

  if (detail.isLoading) return <Spinner />
  if (detail.error) return <ErrorState error={detail.error} onRetry={() => detail.refetch()} />
  const d = detail.data!
  const w = d.wallet
  const months = lastClosedMonths(12, w.createdAt)

  return (
    <>
      <BackLink to="/carteiras">Carteiras</BackLink>
      <PageHeader
        title={w.name}
        subtitle={w.description ?? `Taxa hipotética: ${percent(w.assumedAnnualRate)} ao ano`}
        actions={
          <>
            <Button variant="secondary" onClick={() => setEditing(true)}><Pencil size={16} aria-hidden /> Editar</Button>
            <Button variant="ghost" onClick={() => archive.mutate(!w.archived)}><Archive size={16} aria-hidden /> {w.archived ? 'Reativar' : 'Arquivar'}</Button>
            <Button variant="ghost" onClick={() => setConfirmDelete(true)}><Trash2 size={16} aria-hidden /> Excluir</Button>
          </>
        }
      />
      <Alert tone="warning">{d.disclaimer}</Alert>
      <div className="grid grid-4" style={{ margin: '16px 0' }}>
        <Stat label="Saldo simulado" value={money(d.performance.balance)} />
        <Stat label="Aportes líquidos" value={money(d.performance.netContributed)} hint={`Inicial ${money(d.performance.initial)} + aportes ${money(d.performance.deposits)} − retiradas ${money(d.performance.withdrawals)}`} />
        <Stat label="Rendimentos simulados" value={<span className="positive">{money(d.performance.simulatedReturns)}</span>} />
        <Stat label="Rentabilidade simulada" value={percent(d.performance.returnPercent)} hint="Sobre os aportes líquidos" />
      </div>

      <div className="grid grid-2">
        <Card title="Evolução do saldo">
          {d.evolution.length < 2 ? <p className="muted small">Registre movimentações para ver a evolução.</p> : (
            <SimpleLine data={d.evolution.map((p) => ({ label: date(p.date), balance: p.balance }))} xKey="label" yKey="balance" name="Saldo simulado" title="Evolução do saldo simulado" />
          )}
        </Card>
        <div className="stack">
          {!w.archived && <MovementForm walletId={id!} onDone={refresh} />}
          {!w.archived && (
            <Card title="Aplicar rendimento simulado do mês">
              {months.length === 0 ? <p className="small muted">Disponível após o primeiro mês completo da carteira.</p> : (
                <form className="row" style={{ alignItems: 'flex-end' }} onSubmit={(e) => { e.preventDefault(); if (period) applyReturn.mutate(period) }}>
                  <SelectField label="Mês encerrado" value={period} onChange={(e) => setPeriod(e.target.value)} placeholder="Selecione" options={months.map((m) => ({ value: m, label: m }))} />
                  <Button type="submit" disabled={!period} loading={applyReturn.isPending}>Aplicar</Button>
                </form>
              )}
              <p className="small muted">Saldo ao fim do mês × taxa mensal equivalente à taxa hipotética. Cada mês só pode ser aplicado uma vez.</p>
            </Card>
          )}
        </div>
      </div>

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Scenarios walletId={id!} defaultRate={w.assumedAnnualRate} />
        <Card title="Histórico">
          <ul className="list">
            {movements.data?.content.map((m) => (
              <li key={m.id} className="row-between">
                <span><strong>{walletMovementLabel[m.type]}</strong><div className="small muted">{date(m.occurredOn)}{m.note ? ` · ${m.note}` : ''}</div></span>
                <span className={`mono ${m.type === 'WITHDRAWAL' ? 'negative' : 'positive'}`}>{m.type === 'WITHDRAWAL' ? '−' : '+'} {money(m.amount)}</span>
              </li>
            ))}
          </ul>
          {movements.data && <Pagination page={page} totalPages={movements.data.totalPages} onChange={setPage} />}
        </Card>
      </div>

      <Modal open={editing} title="Editar carteira" onClose={() => setEditing(false)}>
        <WalletForm initial={w} onCancel={() => setEditing(false)} onSaved={() => { setEditing(false); refresh(); toast.success('Carteira atualizada.') }} />
      </Modal>
      <ConfirmDialog open={confirmDelete} title="Excluir carteira simulada?" message="A carteira e todo o histórico simulado serão removidos." confirmLabel="Excluir" danger loading={remove.isPending} onConfirm={() => remove.mutate()} onCancel={() => setConfirmDelete(false)} />
    </>
  )
}

function MovementForm({ walletId, onDone }: { walletId: string; onDone: () => void }) {
  const toast = useToast()
  const [type, setType] = useState<'DEPOSIT' | 'WITHDRAWAL'>('DEPOSIT')
  const [amount, setAmount] = useState('')
  const [occurredOn, setOccurredOn] = useState(todayIso())
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const value = parseAmount(amount)
    if (isNaN(value) || value <= 0) return setError('Informe um valor maior que zero.')
    setError(null)
    setLoading(true)
    try {
      await api.post(`/api/wallets/${walletId}/movements`, { type, amount: value, occurredOn })
      toast.success(type === 'DEPOSIT' ? 'Aporte simulado registrado.' : 'Retirada simulada registrada.')
      setAmount('')
      onDone()
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Card title="Aporte ou retirada simulada">
      <form className="form-grid" onSubmit={submit} noValidate>
        <SelectField label="Tipo" value={type} onChange={(e) => setType(e.target.value as 'DEPOSIT' | 'WITHDRAWAL')} options={[{ value: 'DEPOSIT', label: 'Aporte' }, { value: 'WITHDRAWAL', label: 'Retirada' }]} />
        <TextField label="Valor (R$)" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
        <TextField label="Data" type="date" max={todayIso()} value={occurredOn} onChange={(e) => setOccurredOn(e.target.value)} />
        <div className="field" style={{ justifyContent: 'flex-end' }}><Button type="submit" loading={loading}>Registrar</Button></div>
        {error && <div className="full"><Alert tone="error">{error}</Alert></div>}
      </form>
    </Card>
  )
}

function Scenarios({ walletId, defaultRate }: { walletId: string; defaultRate: number }) {
  const [months, setMonths] = useState('24')
  const [contribution, setContribution] = useState('50')
  const [rates, setRates] = useState(`0, ${defaultRate}, ${Math.min(100, defaultRate + 4)}`)
  const [result, setResult] = useState<ScenarioResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const run = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    const list = rates.split(/[;,]\s*/).map((r) => parseAmount(r)).filter((r) => !isNaN(r))
    if (list.length < 1 || list.length > 4) return setError('Informe de 1 a 4 taxas separadas por vírgula.')
    try {
      setResult(await api.post<ScenarioResponse>(`/api/wallets/${walletId}/scenarios`, { months: Number(months), monthlyContribution: parseAmount(contribution), annualRates: list }))
    } catch (err) {
      setError(errorMessage(err))
    }
  }
  return (
    <Card title="Comparar cenários educativos">
      <form className="form-grid" onSubmit={run} noValidate>
        <TextField label="Meses" type="number" min={1} max={360} value={months} onChange={(e) => setMonths(e.target.value)} />
        <TextField label="Aporte mensal (R$)" inputMode="decimal" value={contribution} onChange={(e) => setContribution(e.target.value)} />
        <TextField className="full" label="Taxas anuais hipotéticas (%)" value={rates} onChange={(e) => setRates(e.target.value)} hint="Separe por vírgula. Ex.: 0, 8, 12" />
        <div className="full"><Button type="submit" variant="secondary">Comparar</Button></div>
      </form>
      {error && <Alert tone="error">{error}</Alert>}
      {result && (
        <>
          <ScenarioChart title="Projeção do saldo simulado por taxa" scenarios={result.scenarios.map((s) => ({ name: `${s.annualRate}% a.a.`, series: s.result.series }))} />
          <ul className="list">
            {result.scenarios.map((s) => (
              <li key={s.annualRate} className="row-between"><Badge>{s.annualRate}% a.a.</Badge><span>Final: <strong>{money(s.result.finalAmount)}</strong> (juros {money(s.result.totalInterest)})</span></li>
            ))}
          </ul>
          <p className="small muted">{result.disclaimer}</p>
        </>
      )}
    </Card>
  )
}
