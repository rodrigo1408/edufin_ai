import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { AlertTriangle, ArrowLeft, ChevronLeft, ChevronRight, Inbox, Star } from 'lucide-react'
import { Link } from 'react-router'
import { errorMessage } from '../api/client'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'ai'

export function Button({
  variant = 'primary',
  size,
  loading,
  children,
  className = '',
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: 'sm'; loading?: boolean }) {
  return (
    <button
      type="button"
      className={`btn btn-${variant} ${size === 'sm' ? 'btn-sm' : ''} ${className}`}
      disabled={loading || rest.disabled}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }} aria-hidden />}
      {children}
    </button>
  )
}

export function ButtonLink({ to, variant = 'primary', size, children }: { to: string; variant?: Variant; size?: 'sm'; children: ReactNode }) {
  return (
    <Link to={to} className={`btn btn-${variant} ${size === 'sm' ? 'btn-sm' : ''}`}>
      {children}
    </Link>
  )
}

export function Card({ children, className = '', title, action }: { children: ReactNode; className?: string; title?: ReactNode; action?: ReactNode }) {
  return (
    <section className={`card ${className}`}>
      {(title || action) && (
        <div className="card-header">
          {title && <h2>{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </section>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="row">{actions}</div>}
    </header>
  )
}

export function Badge({ children, tone }: { children: ReactNode; tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'accent' | 'ai' }) {
  return <span className={`badge ${tone ? 'badge-' + tone : ''}`}>{children}</span>
}

export function ProgressBar({ value, label, accent }: { value: number; label: string; accent?: boolean }) {
  const v = Math.max(0, Math.min(100, value))
  return (
    <div className={`progress ${accent ? 'accent' : ''}`} role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(v)} aria-label={label}>
      <span style={{ width: `${v}%` }} />
    </div>
  )
}

export function Spinner({ label = 'Carregando…' }: { label?: string }) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden />
      <span>{label}</span>
    </div>
  )
}

export function EmptyState({ icon, title, description, action }: { icon?: ReactNode; title: string; description?: ReactNode; action?: ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-icon" aria-hidden>{icon ?? <Inbox size={24} />}</div>
      <h3>{title}</h3>
      {description && <p>{description}</p>}
      {action}
    </div>
  )
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  return (
    <div className="alert alert-error" role="alert">
      <AlertTriangle size={18} aria-hidden />
      <div className="stack" style={{ gap: 8 }}>
        <span>{errorMessage(error)}</span>
        {onRetry && (
          <div>
            <Button variant="secondary" size="sm" onClick={onRetry}>
              Tentar novamente
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}

export function Alert({ tone = 'info', children }: { tone?: 'info' | 'warning' | 'error' | 'success'; children: ReactNode }) {
  return (
    <div className={`alert alert-${tone}`} role={tone === 'error' ? 'alert' : 'note'}>
      {children}
    </div>
  )
}

export function Stat({ label, value, hint, icon }: { label: string; value: ReactNode; hint?: ReactNode; icon?: ReactNode }) {
  return (
    <div className="card stat">
      <span className="stat-label">
        {icon}
        {label}
      </span>
      <span className="stat-value">{value}</span>
      {hint && <span className="stat-hint">{hint}</span>}
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <nav className="pagination" aria-label="Paginação">
      <Button variant="secondary" size="sm" onClick={() => onChange(page - 1)} disabled={page <= 0} aria-label="Página anterior">
        <ChevronLeft size={16} aria-hidden /> Anterior
      </Button>
      <span className="small muted">
        Página {page + 1} de {totalPages}
      </span>
      <Button variant="secondary" size="sm" onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1} aria-label="Próxima página">
        Próxima <ChevronRight size={16} aria-hidden />
      </Button>
    </nav>
  )
}

export function Tabs<T extends string>({ tabs, value, onChange, label }: { tabs: { value: T; label: string }[]; value: T; onChange: (v: T) => void; label: string }) {
  return (
    <div className="tabs" role="tablist" aria-label={label}>
      {tabs.map((t) => (
        <button key={t.value} role="tab" type="button" className="tab" aria-selected={t.value === value} onClick={() => onChange(t.value)}>
          {t.label}
        </button>
      ))}
    </div>
  )
}

export function Chips<T extends string>({ items, value, onChange, label }: { items: { value: T | ''; label: string }[]; value: T | ''; onChange: (v: T | '') => void; label: string }) {
  return (
    <div className="chip-group" role="group" aria-label={label}>
      {items.map((i) => (
        <button key={i.value || 'all'} type="button" className="chip" aria-pressed={i.value === value} onClick={() => onChange(i.value)}>
          {i.label}
        </button>
      ))}
    </div>
  )
}

/** Link de retorno para a página anterior na hierarquia (ex.: aula → curso). */
export function BackLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link to={to} className="back-link">
      <ArrowLeft size={14} aria-hidden /> {children}
    </Link>
  )
}

/** Avaliação de 1 a 5 em estrelas, com texto acessível. */
export function Stars({ value, size = 14 }: { value: number; size?: number }) {
  const v = Math.round(value)
  return (
    <span className="row" style={{ gap: 2, display: 'inline-flex' }} role="img" aria-label={`${v} de 5 estrelas`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} size={size} aria-hidden fill={n <= v ? 'var(--accent)' : 'none'} color="var(--accent)" />
      ))}
    </span>
  )
}
