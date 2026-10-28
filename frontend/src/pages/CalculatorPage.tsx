import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Calculator, Plus, Save, Trash2, X } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { CalcInput, CalcResponse, Reward, SimulationView } from '../api/types'
import { GrowthChart, ScenarioChart } from '../components/charts'
import { SelectField, TextField } from '../components/form'
import { ConfirmDialog, Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Button, Card, EmptyState, PageHeader, Stat } from '../components/ui'
import { dateTime, money, parseAmount } from '../utils/format'
import { describeCalc, validateCalc, hasErrors, type Errors } from '../utils/validation'

const DEFAULT: CalcInput = { interestType: 'COMPOUND', principal: 100, ratePercent: 1, rateUnit: 'MONTHLY', period: 12, periodUnit: 'MONTHS', monthlyContribution: 50 }

export function CalculatorPage() {
  const toast = useToast()
  const qc = useQueryClient()
  const [form, setForm] = useState<Record<keyof CalcInput, string>>(Object.fromEntries(Object.entries(DEFAULT).map(([k, v]) => [k, String(v)])) as Record<keyof CalcInput, string>)
  const [errors, setErrors] = useState<Errors>({})
  const [result, setResult] = useState<CalcResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [scenarios, setScenarios] = useState<CalcInput[]>([])
  const [comparison, setComparison] = useState<CalcResponse[] | null>(null)
  const [saveOpen, setSaveOpen] = useState(false)
  const [deleting, setDeleting] = useState<SimulationView | null>(null)

  const saved = useQuery({ queryKey: ['simulations'], queryFn: () => api.get<SimulationView[]>('/api/calculator/simulations') })
  const calc = useMutation({ mutationFn: (i: CalcInput) => api.post<CalcResponse>('/api/calculator/calculate', i) })
  const compare = useMutation({ mutationFn: (list: CalcInput[]) => api.post<CalcResponse[]>('/api/calculator/compare', { scenarios: list }) })
  const remove = useMutation({
    mutationFn: (id: number) => api.del(`/api/calculator/simulations/${id}`),
    onSuccess: () => { setDeleting(null); qc.invalidateQueries({ queryKey: ['simulations'] }); toast.success('Simulação excluída.') },
  })

  const set = (k: keyof CalcInput, v: string) => setForm((f) => ({ ...f, [k]: v }))
  const toInput = (): CalcInput => ({
    interestType: form.interestType as CalcInput['interestType'],
    principal: parseAmount(form.principal),
    ratePercent: parseAmount(form.ratePercent),
    rateUnit: form.rateUnit as CalcInput['rateUnit'],
    period: Number(form.period),
    periodUnit: form.periodUnit as CalcInput['periodUnit'],
    monthlyContribution: parseAmount(form.monthlyContribution),
  })

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v = validateCalc(form)
    setErrors(v)
    if (hasErrors(v)) return
    setError(null)
    try {
      setResult(await calc.mutateAsync(toInput()))
    } catch (err) {
      setError(errorMessage(err))
    }
  }

  const addScenario = () => {
    const v = validateCalc(form)
    setErrors(v)
    if (hasErrors(v)) return
    if (scenarios.length >= 4) return toast.error('Compare no máximo 4 cenários.')
    setScenarios((s) => [...s, toInput()])
    setComparison(null)
  }

  const runCompare = async () => {
    try {
      setComparison(await compare.mutateAsync(scenarios))
    } catch (err) {
      toast.error(errorMessage(err))
    }
  }

  const load = (input: CalcInput) => {
    setForm(Object.fromEntries(Object.entries(input).map(([k, v]) => [k, String(v)])) as Record<keyof CalcInput, string>)
    calc.mutateAsync(input).then(setResult)
  }

  return (
    <>
      <PageHeader title="Calculadora de juros" subtitle="Veja como o dinheiro cresce com juros simples e compostos." />
      <div className="grid grid-2">
        <Card title="Parâmetros">
          <form className="stack" onSubmit={submit} noValidate>
            <div className="form-grid">
              <SelectField label="Tipo de juros" value={form.interestType} onChange={(e) => set('interestType', e.target.value)} options={[{ value: 'COMPOUND', label: 'Compostos' }, { value: 'SIMPLE', label: 'Simples' }]} />
              <TextField label="Valor inicial (R$)" inputMode="decimal" value={form.principal} onChange={(e) => set('principal', e.target.value)} error={errors.principal} />
              <TextField label="Taxa de juros (%)" inputMode="decimal" value={form.ratePercent} onChange={(e) => set('ratePercent', e.target.value)} error={errors.ratePercent} />
              <SelectField label="Unidade da taxa" value={form.rateUnit} onChange={(e) => set('rateUnit', e.target.value)} options={[{ value: 'MONTHLY', label: 'ao mês' }, { value: 'YEARLY', label: 'ao ano' }]} />
              <TextField label="Período" type="number" min={1} value={form.period} onChange={(e) => set('period', e.target.value)} error={errors.period} />
              <SelectField label="Unidade do período" value={form.periodUnit} onChange={(e) => set('periodUnit', e.target.value)} options={[{ value: 'MONTHS', label: 'meses' }, { value: 'YEARS', label: 'anos' }]} />
              <TextField className="full" label="Aporte mensal (R$)" inputMode="decimal" value={form.monthlyContribution} onChange={(e) => set('monthlyContribution', e.target.value)} error={errors.monthlyContribution} hint="Valor que você adicionaria todo mês (0 se nenhum)." />
            </div>
            {error && <Alert tone="error">{error}</Alert>}
            <div className="row">
              <Button type="submit" loading={calc.isPending}><Calculator size={16} aria-hidden /> Calcular</Button>
              <Button variant="secondary" onClick={addScenario}><Plus size={16} aria-hidden /> Adicionar à comparação</Button>
              {result && <Button variant="ghost" onClick={() => setSaveOpen(true)}><Save size={16} aria-hidden /> Salvar</Button>}
            </div>
          </form>
        </Card>

        <Card title="Como o cálculo funciona">
          <div className="stack small">
            <p><strong>Juros simples:</strong> os juros incidem só sobre o valor aplicado. <br /><code>Montante = Capital × (1 + taxa × tempo)</code></p>
            <p><strong>Juros compostos:</strong> os juros de cada mês também passam a render. <br /><code>Montante = Capital × (1 + taxa)<sup>tempo</sup> + aportes corrigidos</code></p>
            <Alert tone="warning"><span>Esta é uma <strong>simulação matemática</strong> com taxa constante. Não é previsão nem garantia de rendimento: investimentos reais variam e podem ter impostos e taxas.</span></Alert>
          </div>
        </Card>
      </div>

      {result && (
        <section style={{ marginTop: 16 }} aria-live="polite">
          <div className="grid grid-3" style={{ marginBottom: 16 }}>
            <Stat label="Total investido" value={money(result.result.totalInvested)} hint="Valor inicial + aportes" />
            <Stat label="Juros acumulados" value={<span className="positive">{money(result.result.totalInterest)}</span>} />
            <Stat label="Valor final estimado" value={money(result.result.finalAmount)} hint={`Taxa mensal usada: ${result.result.monthlyRatePercent.toLocaleString('pt-BR', { maximumFractionDigits: 4 })}%`} />
          </div>
          <Card title="Evolução estimada">
            <GrowthChart series={result.result.series} title="Evolução do valor investido e dos juros ao longo do tempo" />
            <ul className="small muted" style={{ marginTop: 12 }}>
              {result.assumptions.map((a) => <li key={a}>{a}</li>)}
            </ul>
          </Card>
        </section>
      )}

      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <Card title={`Comparar cenários (${scenarios.length}/4)`} action={scenarios.length >= 2 && <Button size="sm" onClick={runCompare} loading={compare.isPending}>Comparar</Button>}>
          {scenarios.length === 0 ? (
            <p className="muted small">Ajuste os parâmetros e clique em “Adicionar à comparação”. Compare de 2 a 4 cenários.</p>
          ) : (
            <ul className="list">
              {scenarios.map((s, i) => (
                <li key={i} className="row-between">
                  <span className="small"><strong>Cenário {i + 1}:</strong> {describeCalc(s)} · início {money(s.principal)} · aporte {money(s.monthlyContribution)}</span>
                  <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Remover cenário ${i + 1}`} onClick={() => { setScenarios((list) => list.filter((_, j) => j !== i)); setComparison(null) }}><X size={14} /></button>
                </li>
              ))}
            </ul>
          )}
          {comparison && (
            <>
              <ScenarioChart title="Comparação de cenários" scenarios={comparison.map((c, i) => ({ name: `Cenário ${i + 1}`, series: c.result.series }))} />
              <div className="table-wrap">
                <table>
                  <thead><tr><th>Cenário</th><th className="num">Investido</th><th className="num">Juros</th><th className="num">Final</th></tr></thead>
                  <tbody>
                    {comparison.map((c, i) => (
                      <tr key={i}><td>{i + 1}</td><td className="num">{money(c.result.totalInvested)}</td><td className="num">{money(c.result.totalInterest)}</td><td className="num"><strong>{money(c.result.finalAmount)}</strong></td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </Card>

        <Card title="Simulações salvas">
          {saved.data?.length === 0 && <EmptyState title="Nenhuma simulação salva" description="Calcule e clique em “Salvar” para consultar depois." />}
          <ul className="list">
            {saved.data?.map((s) => (
              <li key={s.id} className="row-between">
                <button className="btn btn-ghost" style={{ textAlign: 'left', justifyContent: 'flex-start', padding: 0, minHeight: 0 }} onClick={() => load(s.input)}>
                  <span>
                    <strong>{s.name}</strong>
                    <div className="small muted">{describeCalc(s.input)} · final {money(s.finalAmount)} · {dateTime(s.createdAt)}</div>
                  </span>
                </button>
                <button className="btn btn-ghost btn-icon btn-sm" aria-label={`Excluir simulação ${s.name}`} onClick={() => setDeleting(s)}><Trash2 size={15} /></button>
              </li>
            ))}
          </ul>
        </Card>
      </div>

      <SaveSimulation open={saveOpen} input={result?.input ?? null} onClose={() => setSaveOpen(false)} onSaved={(r) => { toast.success('Simulação salva!'); toast.reward(r); qc.invalidateQueries({ queryKey: ['simulations'] }) }} />
      <ConfirmDialog open={!!deleting} title="Excluir simulação?" message={`"${deleting?.name}" será removida.`} confirmLabel="Excluir" danger loading={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)} onCancel={() => setDeleting(null)} />
    </>
  )
}

function SaveSimulation({ open, input, onClose, onSaved }: { open: boolean; input: CalcInput | null; onClose: () => void; onSaved: (r: Reward) => void }) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const save = async () => {
    if (!name.trim()) return setError('Dê um nome para a simulação.')
    setLoading(true)
    try {
      const r = await api.post<{ reward: Reward }>('/api/calculator/simulations', { name: name.trim(), input })
      onSaved(r.reward)
      setName('')
      onClose()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Modal open={open} title="Salvar simulação" onClose={onClose} footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button onClick={save} loading={loading}>Salvar</Button></>}>
      <TextField label="Nome" value={name} onChange={(e) => { setName(e.target.value); setError(null) }} error={error ?? undefined} maxLength={80} placeholder="Ex.: Guardar para a viagem" />
    </Modal>
  )
}
