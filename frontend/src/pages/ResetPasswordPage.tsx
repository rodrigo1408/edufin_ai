import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import { api, errorMessage } from '../api/client'
import { TextField } from '../components/form'
import { AuthLayout } from '../components/AuthLayout'
import { Alert, Button } from '../components/ui'
import { PASSWORD_RULE } from '../utils/validation'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!PASSWORD_RULE.test(password)) return setError('Use de 8 a 72 caracteres, com letras e números.')
    if (password !== confirm) return setError('As senhas não conferem.')
    setLoading(true)
    try {
      await api.post('/api/auth/password-reset/confirm', { token, newPassword: password })
      setDone(true)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthLayout>
        <div className="card">
          <h1>Nova senha</h1>
          {!token && <Alert tone="error">Link inválido. Solicite um novo link de redefinição.</Alert>}
          {done ? (
            <div className="stack">
              <Alert tone="success">Senha alterada com sucesso.</Alert>
              <Link className="btn btn-primary" to="/entrar">Ir para o login</Link>
            </div>
          ) : (
            token && (
              <form className="stack" onSubmit={submit} noValidate>
                {error && <Alert tone="error">{error}</Alert>}
                <TextField label="Nova senha" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                <TextField label="Confirme a nova senha" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} required />
                <Button type="submit" loading={loading}>Salvar nova senha</Button>
              </form>
            )
          )}
        </div>
    </AuthLayout>
  )
}
