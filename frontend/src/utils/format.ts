const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
const pct = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1, minimumFractionDigits: 0 })

export function money(value: number | null | undefined): string {
  return brl.format(value ?? 0)
}

export function percent(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : pct.format(value) + '%'
}

/** Datas ISO (yyyy-MM-dd) são exibidas sem conversão de fuso para não "voltar um dia". */
export function date(iso: string | null | undefined): string {
  if (!iso) return '—'
  if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
    const [y, m, d] = iso.split('-')
    return `${d}/${m}/${y}`
  }
  return new Date(iso).toLocaleDateString('pt-BR')
}

export function dateTime(iso: string | null | undefined): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })
}

export function monthLabel(yearMonth: string): string {
  const [y, m] = yearMonth.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString('pt-BR', { month: 'short', year: '2-digit' })
}

/** Data local de hoje no formato yyyy-MM-dd. */
export function todayIso(): string {
  const d = new Date()
  const off = d.getTimezoneOffset()
  return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10)
}

export function relativeTime(iso: string): string {
  const diff = (Date.now() - new Date(iso).getTime()) / 1000
  if (diff < 60) return 'agora'
  if (diff < 3600) return `há ${Math.floor(diff / 60)} min`
  if (diff < 86400) return `há ${Math.floor(diff / 3600)} h`
  if (diff < 86400 * 7) return `há ${Math.floor(diff / 86400)} d`
  return date(iso)
}

/** Converte texto digitado ("1.234,56" ou "1234.56") em número; retorna NaN se inválido. */
export function parseAmount(text: string): number {
  const t = text.trim()
  if (!t) return NaN
  const normalized = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) return NaN
  return Number(normalized)
}

export function hasMaxTwoDecimals(n: number): boolean {
  return Math.round(n * 100) === n * 100 || Math.abs(Math.round(n * 100) - n * 100) < 1e-6
}
