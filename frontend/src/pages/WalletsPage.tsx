import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { FlaskConical, Plus } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { Reward, WalletSummary } from '../api/types'
import { TextAreaField, TextField } from '../components/form'
import { hasErrors, type Errors } from '../utils/validation'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, EmptyState, ErrorState, PageHeader, Spinner } from '../components/ui'
import { money, parseAmount, percent } from '../utils/format'

export function WalletsPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const [creating, setCreating] = useState(false)
  const wallets = useQuery({ queryKey: ['wallets'], queryFn: () => api.get<WalletSummary[]>('/api/wallets') })

  return (
    <>
      <PageHeader title="Carteiras simuladas" subtitle="Um laboratório para praticar investimentos sem dinheiro real." actions={<Button onClick={() => setCreating(true)}><Plus size={16} aria-hidden /> Nova carteira</Button>} />
      <Alert tone="warning">
        <FlaskConical size={18} aria-hidden />
        <span><strong>Tudo aqui é simulação.</strong> Não há dinheiro real, conexão com bancos ou corretoras, nem cotações de mercado. Os rendimentos usam uma taxa hipotética escolhida por você.</span>
      </Alert>
      <div style={{ marginTop: 16 }}>
        {wallets.isLoading && <Spinner />}
        {wallets.error && <ErrorState error={wallets.error} onRetry={() => wallets.refetch()} />}
        {wallets.data?.length === 0 && (
          <div className="card"><EmptyState icon={<FlaskConical size={24} />} title="Nenhuma carteira simulada" description="Crie uma carteira com um saldo fictício e veja como aportes e juros funcionam ao longo do tempo." action={<Button onClick={() => setCreating(true)}>Criar carteira</Button>} /></div>
        )}
        <div className="grid grid-3">
          {wallets.data?.map((w) => (
            <Link key={w.id} to={`/carteiras/${w.id}`} className="card stack" style={{ textDecoration: 'none', color: 'inherit', gap: 8 }}>
              <div className="row-between"><Badge tone="accent">Simulada</Badge>{w.archived && <Badge>Arquivada</Badge>}</div>
              <h3 style={{ margin: 0 }}>{w.name}</h3>
              <span className="stat-value">{money(w.balance)}</span>
              <span className="small muted">Taxa hipotética: {percent(w.assumedAnnualRate)} ao ano</span>
            </Link>
          ))}
        </div>
      </div>
      <Modal open={creating} title="Nova carteira simulada" onClose={() => setCreating(false)}>
        <WalletForm
          onCancel={() => setCreating(false)}
          onSaved={(r) => { setCreating(false); toast.success('Carteira criada!'); toast.reward(r); qc.invalidateQueries({ queryKey: ['wallets'] }) }}
        />
      </Modal>
    </>
  )
}

export function WalletForm({ initial, onSaved, onCancel }: { initial?: WalletSummary; onSaved: (r?: Reward) => void; onCancel: () => void }) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [rate, setRate] = useState(initial ? String(initial.assumedAnnualRate) : '10')
  const [initialBalance, setInitialBalance] = useState('0')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const r = parseAmount(rate)
    const b = parseAmount(initialBalance)
    const v: Errors = {
      name: name.trim() ? undefined : 'Dê um nome para a carteira.',
      rate: isNaN(r) || r < 0 || r > 100 ? 'A taxa deve estar entre 0% e 100% ao ano.' : undefined,
      initialBalance: !initial && (isNaN(b) || b < 0) ? 'Informe um valor igual ou maior que zero.' : undefined,
    }
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    try {
      const body = { name: name.trim(), description: description || null, assumedAnnualRate: r, initialBalance: initial ? undefined : b }
      if (initial) {
        await api.put(`/api/wallets/${initial.id}`, body)
        onSaved()
      } else {
        const res = await api.post<{ reward: Reward }>('/api/wallets', body)
        onSaved(res.reward)
      }
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form className="stack" onSubmit={submit} noValidate>
      {error && <Alert tone="error">{error}</Alert>}
      <TextField label="Nome" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} maxLength={60} placeholder="Ex.: Laboratório de renda fixa" required />
      <TextField label="Taxa anual hipotética (%)" inputMode="decimal" value={rate} onChange={(e) => setRate(e.target.value)} error={errors.rate} hint="Usada apenas para calcular rendimentos simulados. Não é uma previsão." required />
      {!initial && <TextField label="Saldo inicial simulado (R$)" inputMode="decimal" value={initialBalance} onChange={(e) => setInitialBalance(e.target.value)} error={errors.initialBalance} />}
      <TextAreaField label="Descrição (opcional)" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={300} />
      <div className="modal-actions">
        <Button variant="secondary" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" loading={loading}>{initial ? 'Salvar' : 'Criar carteira'}</Button>
      </div>
    </form>
  )
}
