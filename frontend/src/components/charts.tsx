import {
  Area, AreaChart, Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import type { CalcPoint, CategoryTotal, MonthPoint } from '../api/types'
import { money, monthLabel } from '../utils/format'

const axis = { stroke: 'var(--text-muted)', fontSize: 12 }
const tooltipStyle = { background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, color: 'var(--text)' }
const compact = (v: number) => (Math.abs(v) >= 1000 ? `${(v / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil` : String(v))

/** Receitas x despesas por mês, com o saldo registrado acumulado. */
export function MonthlyFlowChart({ data, title }: { data: MonthPoint[]; title: string }) {
  const rows = data.map((d) => ({ ...d, label: monthLabel(d.month) }))
  return (
    <figure className="chart-box" aria-label={title} style={{ margin: 0 }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="label" tick={axis} />
          <YAxis tick={axis} tickFormatter={compact} width={56} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(Number(v))} />
          <Legend />
          <Bar dataKey="income" name="Receitas" fill="var(--chart-1)" radius={[6, 6, 0, 0]} />
          <Bar dataKey="expense" name="Despesas" fill="var(--chart-2)" radius={[6, 6, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
      <figcaption className="sr-only">{title}</figcaption>
    </figure>
  )
}

export function BalanceLineChart({ data, title }: { data: MonthPoint[]; title: string }) {
  const rows = data.map((d) => ({ ...d, label: monthLabel(d.month) }))
  return (
    <figure className="chart-box" aria-label={title} style={{ margin: 0 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="label" tick={axis} />
          <YAxis tick={axis} tickFormatter={compact} width={56} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(Number(v))} />
          <Legend />
          <Line type="monotone" dataKey="cumulativeBalance" name="Saldo registrado" stroke="var(--chart-1)" strokeWidth={3} dot={{ r: 3 }} />
          <Line type="monotone" dataKey="net" name="Resultado do mês" stroke="var(--chart-3)" strokeWidth={2} strokeDasharray="5 4" />
        </LineChart>
      </ResponsiveContainer>
    </figure>
  )
}

export function CategoryBars({ data, title, color = 'var(--chart-2)' }: { data: CategoryTotal[]; title: string; color?: string }) {
  const height = Math.max(160, data.length * 38 + 30)
  return (
    <figure style={{ margin: 0, width: '100%', height }} aria-label={title}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 4, right: 16, left: 8, bottom: 4 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" horizontal={false} />
          <XAxis type="number" tick={axis} tickFormatter={compact} />
          <YAxis type="category" dataKey="label" tick={axis} width={130} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(Number(v))} />
          <Bar dataKey="total" name="Total" fill={color} radius={[0, 6, 6, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </figure>
  )
}

/** Evolução de uma simulação: valor investido x juros acumulados. */
export function GrowthChart({ series, title }: { series: CalcPoint[]; title: string }) {
  const rows = series.map((p) => ({ ...p, label: p.month % 12 === 0 && p.month >= 12 ? `${p.month / 12}a` : `${p.month}m` }))
  return (
    <figure className="chart-box" aria-label={title} style={{ margin: 0, height: 300 }}>
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="label" tick={axis} minTickGap={16} />
          <YAxis tick={axis} tickFormatter={compact} width={60} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(Number(v))} />
          <Legend />
          <Area type="monotone" dataKey="invested" stackId="1" name="Valor investido" stroke="var(--chart-3)" fill="var(--chart-3)" fillOpacity={0.35} />
          <Area type="monotone" dataKey="interest" stackId="1" name="Juros" stroke="var(--chart-1)" fill="var(--chart-1)" fillOpacity={0.45} />
        </AreaChart>
      </ResponsiveContainer>
    </figure>
  )
}

export function SimpleLine({ data, xKey, yKey, name, title, percent }: { data: Record<string, unknown>[]; xKey: string; yKey: string; name: string; title: string; percent?: boolean }) {
  return (
    <figure className="chart-box" aria-label={title} style={{ margin: 0 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey={xKey} tick={axis} minTickGap={16} />
          <YAxis tick={axis} width={56} domain={percent ? [0, 100] : ['auto', 'auto']} tickFormatter={percent ? (v) => `${v}%` : compact} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => (percent ? `${v}%` : money(Number(v)))} />
          <Line type="monotone" dataKey={yKey} name={name} stroke="var(--chart-1)" strokeWidth={3} dot={{ r: 3 }} />
        </LineChart>
      </ResponsiveContainer>
    </figure>
  )
}

const SERIES_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)']

/** Várias séries de saldo (comparação de cenários) no mesmo gráfico. */
export function ScenarioChart({ scenarios, title }: { scenarios: { name: string; series: CalcPoint[] }[]; title: string }) {
  const months = Array.from(new Set(scenarios.flatMap((s) => s.series.map((p) => p.month)))).sort((a, b) => a - b)
  const rows = months.map((m) => {
    const row: Record<string, number | string> = { label: m % 12 === 0 && m >= 12 ? `${m / 12}a` : `${m}m` }
    scenarios.forEach((s, i) => {
      const p = s.series.find((x) => x.month === m)
      if (p) row['s' + i] = p.balance
    })
    return row
  })
  return (
    <figure className="chart-box" aria-label={title} style={{ margin: 0, height: 300 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={rows} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" vertical={false} />
          <XAxis dataKey="label" tick={axis} minTickGap={16} />
          <YAxis tick={axis} tickFormatter={compact} width={60} />
          <Tooltip contentStyle={tooltipStyle} formatter={(v) => money(Number(v))} />
          <Legend />
          {scenarios.map((s, i) => (
            <Line key={i} type="monotone" dataKey={'s' + i} name={s.name} stroke={SERIES_COLORS[i % 4]} strokeWidth={2.5} dot={false} connectNulls />
          ))}
        </LineChart>
      </ResponsiveContainer>
    </figure>
  )
}
