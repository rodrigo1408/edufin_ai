import { Link } from 'react-router'

/** Marca do EduFin AI: símbolo (livro aberto + faísca de IA) e nome. */
export function Logo({ to = '/painel' }: { to?: string }) {
  return (
    <Link to={to} className="brand" aria-label="EduFin AI — página inicial">
      <span className="brand-logo" aria-hidden>
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 6.5c3-1.5 6-1.5 9 .5 3-2 6-2 9-.5V19c-3-1.5-6-1.5-9 .5-3-2-6-2-9-.5z" />
          <path d="M12 7v12.5" />
          <path d="M18.5 2.5l.6 1.4 1.4.6-1.4.6-.6 1.4-.6-1.4-1.4-.6 1.4-.6z" fill="currentColor" stroke="none" />
        </svg>
      </span>
      EduFin <span className="brand-ai">AI</span>
    </Link>
  )
}
