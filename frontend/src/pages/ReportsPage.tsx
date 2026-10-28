import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, qs } from '../api/client'
import type { FullReport } from '../api/types'
import { BalanceLineChart, CategoryBars, MonthlyFlowChart, SimpleLine } from '../components/charts'
import { TextField } from '../components/form'
import { Badge, Button, Card, EmptyState, ErrorState, PageHeader, ProgressBar, Spinner, Stat } from '../components/ui'
import { date, money, monthLabel, percent, todayIso } from '../utils/format'
import { goalStatusLabel } from '../utils/labels'

function monthsAgo(n: number): string {
  const d = new Date()
  d.setMonth(d.getMonth() - n, 1)
  return d.toISOString().slice(0, 10)
}

export function ReportsPage() {
  const [range, setRange] = useState({ from: monthsAgo(5), to: todayIso() })
  const [draft, setDraft] = useState(range)
  const invalid = draft.from > draft.to
  const report = useQuery({ queryKey: ['report', range], queryFn: () => api.get<FullReport>('/api/reports' + qs(range)) })

  const presets = [
    { label: 'Este mês', from: todayIso().slice(0, 8) + '01' },
    { label: '3 meses', from: monthsAgo(2) },
    { label: '6 meses', from: monthsAgo(5) },
    { label: '12 meses', from: monthsAgo(11) },
  ]

  return (
    <>
      <PageHeader title="Relatórios" subtitle="Análises calculadas a partir dos seus registros." />
      <Card>
        <form className="row" style={{ alignItems: 'flex-end' }} onSubmit={(e) => { e.preventDefault(); if (!invalid) setRange(draft) }}>
          <TextField label="De" type="date" value={draft.from} onChange={(e) => setDraft({ ...draft, from: e.target.value })} error={invalid ? 'Data inicial maior que a final' : undefined} />
          <TextField label="Até" type="date" value={draft.to} max={todayIso()} onChange={(e) => setDraft({ ...draft, to: e.target.value })} />
          <Button type="submit" disabled={invalid}>Aplicar</Button>
          <div className="chip-group" role="group" aria-label="Períodos rápidos">
            {presets.map((p) => (
              <button key={p.label} type="button" className="chip" aria-pressed={range.from === p.from && range.to === todayIso()} onClick={() => { const r = { from: p.from, to: todayIso() }; setDraft(r); setRange(r) }}>{p.label}</button>
            ))}
          </div>
        </form>
      </Card>

      {report.isLoading && <Spinner label="Calculando relatórios…" />}
      {report.error && <ErrorState error={report.error} onRetry={() => report.refetch()} />}
      {report.data && <ReportBody r={report.data} />}
    </>
  )
}

function ReportBody({ r }: { r: FullReport }) {
  const f = r.finances
  const hasFinance = f.income > 0 || f.expense > 0
  return (
    <div className="stack" style={{ marginTop: 16, gap: 16 }}>
      <h2>Finanças · {date(r.from)} a {date(r.to)}</h2>
      <div className="grid grid-4">
        <Stat label="Receitas" value={<span className="positive">{money(f.income)}</span>} />
        <Stat label="Despesas" value={<span className="negative">{money(f.expense)}</span>} />
        <Stat label="Resultado do período" value={money(f.balance)} />
        <Stat label="Saldo registrado no fim" value={money(f.monthly.at(-1)?.cumulativeBalance ?? f.openingBalance)} hint={`Saldo anterior: ${money(f.openingBalance)}`} />
      </div>
      {!hasFinance ? (
        <Card><EmptyState title="Sem receitas ou despesas no período" description="Registre movimentações em Transações para ver os gráficos." /></Card>
      ) : (
        <>
          <div className="grid grid-2">
            <Card title="Receitas x despesas por mês"><MonthlyFlowChart data={f.monthly} title="Receitas e despesas por mês" /></Card>
            <Card title="Evolução do saldo registrado"><BalanceLineChart data={f.monthly} title="Saldo registrado acumulado por mês" /></Card>
          </div>
          <div className="grid grid-2">
            <Card title="Despesas por categoria">
              {f.expensesByCategory.length ? <CategoryBars data={f.expensesByCategory} title="Despesas por categoria" /> : <p className="muted small">Sem despesas.</p>}
            </Card>
            <Card title="Receitas por categoria">
              {f.incomeByCategory.length ? <CategoryBars data={f.incomeByCategory} title="Receitas por categoria" color="var(--chart-1)" /> : <p className="muted small">Sem receitas.</p>}
            </Card>
          </div>
        </>
      )}

      <h2>Metas</h2>
      <div className="grid grid-4">
        <Stat label="Ativas" value={r.goals.active} />
        <Stat label="Concluídas" value={r.goals.completed} hint={`Taxa de conclusão: ${percent(r.goals.completionRate)}`} />
        <Stat label="Contribuições no período" value={money(r.goals.contributedInPeriod)} />
        <Stat label="Retiradas no período" value={money(r.goals.withdrawnInPeriod)} />
      </div>
      <div className="grid grid-2">
        <Card title="Contribuições para metas por mês">
          <div className="table-wrap">
            <table>
              <thead><tr><th>Mês</th><th className="num">Contribuições</th><th className="num">Retiradas</th></tr></thead>
              <tbody>{r.goals.monthly.map((m) => <tr key={m.month}><td>{monthLabel(m.month)}</td><td className="num">{money(m.contributions)}</td><td className="num">{money(m.withdrawals)}</td></tr>)}</tbody>
            </table>
          </div>
        </Card>
        <Card title="Conclusão de cada meta">
          {r.goals.goals.length === 0 ? <p className="muted small">Nenhuma meta criada.</p> : (
            <ul className="list">
              {r.goals.goals.map((g) => (
                <li key={g.id} className="stack" style={{ gap: 4 }}>
                  <div className="row-between"><span>{g.title} <Badge>{goalStatusLabel[g.status]}</Badge></span><strong>{percent(g.progressPercent)}</strong></div>
                  <ProgressBar value={g.progressPercent} label={`Conclusão de ${g.title}`} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <h2>Aprendizado</h2>
      <div className="grid grid-4">
        <Stat label="Quizzes no período" value={r.quizzes.attempts} hint={`Média: ${percent(r.quizzes.averagePercent)} · Melhor: ${percent(r.quizzes.bestPercent)}`} />
        <Stat label="Cursos inscritos" value={r.courses.enrolled} hint={`${r.courses.completedTotal} concluído(s), ${r.courses.completedInPeriod} no período`} />
        <Stat label="Pontos no período" value={r.achievements.pointsInPeriod} hint={`Total: ${r.achievements.totalPoints}`} />
        <Stat label="Conquistas" value={`${r.achievements.unlocked}/${r.achievements.total}`} hint={`${r.achievements.unlockedInPeriod.length} no período`} />
      </div>
      <div className="grid grid-2">
        <Card title="Desempenho nos quizzes">
          {r.quizzes.evolution.length === 0 ? <p className="muted small">Nenhum quiz finalizado no período.</p> : (
            <SimpleLine data={r.quizzes.evolution.map((e) => ({ label: date(e.finishedAt), percent: e.percent }))} xKey="label" yKey="percent" name="Acertos" title="Percentual de acertos por tentativa" percent />
          )}
        </Card>
        <Card title="Progresso nos cursos">
          {r.courses.courses.length === 0 ? <p className="muted small">Nenhuma inscrição.</p> : (
            <ul className="list">
              {r.courses.courses.map((c) => (
                <li key={c.courseId} className="stack" style={{ gap: 4 }}>
                  <div className="row-between"><span>{c.title}</span><span className="small">{c.completedLessons}/{c.lessonCount}</span></div>
                  <ProgressBar value={c.progressPercent} label={`Progresso em ${c.title}`} />
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
      <Card title="Critérios de cálculo">
        <ul className="small muted">{r.criteria.map((c) => <li key={c}>{c}</li>)}</ul>
      </Card>
    </div>
  )
}
