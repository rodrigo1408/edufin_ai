import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { api, errorMessage } from '../api/client'
import type { PublicConfig } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { TextField } from '../components/form'
import { AuthLayout } from '../components/AuthLayout'
import { Modal } from '../components/Modal'
import { Alert, Button } from '../components/ui'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [forgotOpen, setForgotOpen] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!email || !password) {
      setError('Informe e-mail e senha.')
      return
    }
    setLoading(true)
    try {
      await login(email, password)
      const from = (location.state as { from?: string } | null)?.from
      navigate(from && from !== '/entrar' ? from : '/painel', { replace: true })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
        <div className="card">
          <h1>Entrar</h1>
          <p className="muted">Que bom ver você de novo!</p>
          <form className="stack" onSubmit={submit} noValidate>
            {error && <Alert tone="error">{error}</Alert>}
            <TextField label="E-mail" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
            <TextField label="Senha" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
            <Button type="submit" loading={loading}>Entrar</Button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setForgotOpen(true)}>
              Esqueci minha senha
            </button>
          </form>
        </div>
        <p className="small muted" style={{ textAlign: 'center' }}>
          Ainda não tem conta? <Link to="/cadastro">Cadastre-se</Link>
        </p>
      <ForgotPassword open={forgotOpen} onClose={() => setForgotOpen(false)} />
    </AuthLayout>
  )
}

function ForgotPassword({ open, onClose }: { open: boolean; onClose: () => void }) {
  const config = useQuery({ queryKey: ['public-config'], queryFn: () => api.get<PublicConfig>('/api/public/config'), enabled: open })
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState<{ tone: 'success' | 'error'; text: string } | null>(null)
  const [loading, setLoading] = useState(false)

  const send = async () => {
    setLoading(true)
    setMessage(null)
    try {
      const r = await api.post<{ message: string }>('/api/auth/password-reset/request', { email })
      setMessage({ tone: 'success', text: r.message })
    } catch (e) {
      setMessage({ tone: 'error', text: errorMessage(e) })
    } finally {
      setLoading(false)
    }
  }

  const available = config.data?.passwordResetByEmail
  return (
    <Modal
      open={open}
      title="Recuperar senha"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Fechar</Button>
          {available && <Button onClick={send} loading={loading} disabled={!email}>Enviar link</Button>}
        </>
      }
    >
      {config.isLoading && <p className="muted">Verificando disponibilidade…</p>}
      {config.data && !available && (
        <Alert tone="warning">
          O envio de e-mails não está configurado nesta instalação, então não conseguimos mandar um link automaticamente.
          Peça a um(a) administrador(a) da plataforma um link de redefinição — ele é de uso único e expira em 30 minutos.
        </Alert>
      )}
      {available && (
        <div className="stack">
          <p className="muted small">Informe seu e-mail. Se ele estiver cadastrado, enviaremos um link válido por 30 minutos.</p>
          <TextField label="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
      )}
      {message && <div style={{ marginTop: 12 }}><Alert tone={message.tone}>{message.text}</Alert></div>}
    </Modal>
  )
}
