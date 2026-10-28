import { Link } from 'react-router'
import { TrendingUp } from 'lucide-react'

export function Logo({ to = '/painel' }: { to?: string }) {
  return (
    <Link to={to} className="brand" aria-label="EduFin — página inicial">
      <span className="brand-logo" aria-hidden>
        <TrendingUp size={18} />
      </span>
      EduFin
    </Link>
  )
}
