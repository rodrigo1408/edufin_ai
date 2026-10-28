import type { ReactNode } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { Logo } from './Logo'

/** Moldura das telas de acesso: painel da marca (desktop) + formulário centralizado. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-page">
      <aside className="auth-aside" aria-label="Sobre o EduFin AI">
        <Logo to="/" />
        <div>
          <p className="eyebrow">Educação com Inteligência Artificial</p>
          <h2 style={{ fontSize: '1.7rem', lineHeight: 1.2, margin: '10px 0 22px' }}>Aprenda finanças no seu ritmo, com um tutor sempre por perto.</h2>
          <ul>
            <li><CheckCircle2 size={18} aria-hidden /> Trilha de cursos curtos, do orçamento aos investimentos</li>
            <li><CheckCircle2 size={18} aria-hidden /> Tutor que conhece a aula e explica de outro jeito</li>
            <li><CheckCircle2 size={18} aria-hidden /> Metas, quizzes e simuladores sem dinheiro real</li>
          </ul>
        </div>
        <p className="small muted" style={{ margin: 0 }}>Seus dados financeiros são privados. Nunca pedimos senhas de banco.</p>
      </aside>
      <div className="auth-main">
        <div className="auth-card stack">
          <div className="auth-mobile-logo"><Logo to="/" /></div>
          {children}
        </div>
      </div>
    </div>
  )
}
