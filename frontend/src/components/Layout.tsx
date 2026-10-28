import { useState, type ReactNode } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import {
  Bell, BookOpen, Calculator, ChartColumn, FlaskConical, HelpCircle, LayoutDashboard, LogOut, Menu,
  MessageSquareHeart, MessagesSquare, Receipt, Shield, Sparkles, Target, Trophy, UserRound, X,
} from 'lucide-react'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Logo } from './Logo'

interface Item {
  to: string
  label: string
  icon: ReactNode
  badge?: number
  ai?: boolean
}

/**
 * Estrutura da área do aluno. A navegação prioriza o aprendizado (início, cursos, tutor, quizzes);
 * as ferramentas financeiras ficam agrupadas em seguida.
 */
export function Layout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const unread = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () => api.get<{ count: number }>('/api/notifications/unread-count'),
    refetchInterval: 60_000,
  })

  const sections: { title: string; items: Item[] }[] = [
    {
      title: 'Aprender',
      items: [
        { to: '/painel', label: 'Início', icon: <LayoutDashboard size={18} /> },
        { to: '/cursos', label: 'Cursos e trilha', icon: <BookOpen size={18} /> },
        { to: '/assistente', label: 'Tutor IA', icon: <Sparkles size={18} />, ai: true },
        { to: '/quizzes', label: 'Quizzes', icon: <HelpCircle size={18} /> },
        { to: '/gamificacao', label: 'Conquistas', icon: <Trophy size={18} /> },
      ],
    },
    {
      title: 'Praticar com dinheiro',
      items: [
        { to: '/metas', label: 'Metas', icon: <Target size={18} /> },
        { to: '/transacoes', label: 'Transações', icon: <Receipt size={18} /> },
        { to: '/relatorios', label: 'Relatórios', icon: <ChartColumn size={18} /> },
        { to: '/calculadora', label: 'Calculadora', icon: <Calculator size={18} /> },
        { to: '/carteiras', label: 'Carteiras simuladas', icon: <FlaskConical size={18} /> },
      ],
    },
    {
      title: 'Comunidade e conta',
      items: [
        { to: '/comunidade', label: 'Comunidade', icon: <MessagesSquare size={18} /> },
        { to: '/notificacoes', label: 'Notificações', icon: <Bell size={18} />, badge: unread.data?.count },
        { to: '/perfil', label: 'Perfil', icon: <UserRound size={18} /> },
        { to: '/feedback', label: 'Feedback', icon: <MessageSquareHeart size={18} /> },
        ...(user?.permissions.includes('ADMIN_AREA') ? [{ to: '/admin', label: 'Administração', icon: <Shield size={18} /> }] : []),
      ],
    },
  ]

  const doLogout = async () => {
    await logout()
    navigate('/entrar')
  }

  return (
    <div className="app-shell">
      <a href="#conteudo" className="skip-link">Pular para o conteúdo</a>
      {open && <div className="backdrop" onClick={() => setOpen(false)} aria-hidden />}
      <aside className={`sidebar ${open ? 'open' : ''}`} aria-label="Menu principal">
        <div className="row-between">
          <Logo />
          {open && (
            <button className="btn btn-ghost btn-icon" onClick={() => setOpen(false)} aria-label="Fechar menu">
              <X size={18} />
            </button>
          )}
        </div>
        <nav>
          {sections.map((s) => (
            <div key={s.title}>
              <div className="nav-section">{s.title}</div>
              {s.items.map((i) => (
                <NavLink key={i.to} to={i.to} onClick={() => setOpen(false)} className={({ isActive }) => `nav-link ${i.ai ? 'ai' : ''} ${isActive ? 'active' : ''}`}>
                  <span className="nav-icon" aria-hidden>{i.icon}</span>
                  {i.label}
                  {!!i.badge && <span className="count" aria-label={`${i.badge} não lidas`}>{i.badge}</span>}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="small muted" style={{ padding: '0 10px 6px' }}>Conectado como <strong style={{ color: 'var(--text)' }}>{user?.displayName}</strong></div>
          <button className="nav-link" style={{ width: '100%', border: 0, background: 'none', cursor: 'pointer', font: 'inherit' }} onClick={doLogout}>
            <LogOut size={18} aria-hidden /> Sair
          </button>
        </div>
      </aside>
      <div className="main">
        <header className="topbar">
          <button className="btn btn-ghost btn-icon" onClick={() => setOpen(true)} aria-label="Abrir menu" aria-expanded={open}>
            <Menu size={20} />
          </button>
          <Logo />
          <span className="spacer" />
          <NavLink to="/assistente" className="btn btn-ghost btn-icon" aria-label="Abrir o Tutor IA">
            <Sparkles size={20} color="#b8a6ff" />
          </NavLink>
          <NavLink to="/notificacoes" className="btn btn-ghost btn-icon" aria-label={`Notificações${unread.data?.count ? `: ${unread.data.count} não lidas` : ''}`}>
            <Bell size={20} />
            {!!unread.data?.count && <span className="badge badge-danger">{unread.data.count}</span>}
          </NavLink>
        </header>
        <main id="conteudo" className="content" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
    </div>
  )
}
