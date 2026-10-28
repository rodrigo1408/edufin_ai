import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { ApiError, errorMessage } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Checkbox, TextField } from '../components/form'
import { Logo } from '../components/Logo'
import { Alert, Button } from '../components/ui'
import { validateRegister, hasErrors, type Errors } from '../utils/validation'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', displayName: '', password: '', confirm: '', acceptTerms: false, ageConfirmation: false })
  const [errors, setErrors] = useState<Errors>({})
  const [serverError, setServerError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const set = <K extends keyof typeof form>(k: K, v: (typeof form)[K]) => setForm((f) => ({ ...f, [k]: v }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setServerError(null)
    const v = validateRegister(form)
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    try {
      await register({ email: form.email, password: form.password, displayName: form.displayName.trim(), acceptTerms: form.acceptTerms, ageConfirmation: form.ageConfirmation })
      navigate('/painel', { replace: true })
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors.length) {
        setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      }
      setServerError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card stack">
        <Logo to="/" />
        <div className="card">
          <h1>Criar conta</h1>
          <p className="muted small">Pedimos só o essencial. Seus registros financeiros ficam visíveis apenas para você.</p>
          <form className="stack" onSubmit={submit} noValidate>
            {serverError && <Alert tone="error">{serverError}</Alert>}
            <TextField label="E-mail" type="email" autoComplete="email" value={form.email} onChange={(e) => set('email', e.target.value)} error={errors.email} required />
            <TextField
              label="Apelido público"
              value={form.displayName}
              onChange={(e) => set('displayName', e.target.value)}
              error={errors.displayName}
              hint="É o nome que aparece na comunidade. Evite usar seu nome completo."
              required
            />
            <TextField label="Senha" type="password" autoComplete="new-password" value={form.password} onChange={(e) => set('password', e.target.value)} error={errors.password} hint="Mínimo de 8 caracteres, com letras e números." required />
            <TextField label="Confirme a senha" type="password" autoComplete="new-password" value={form.confirm} onChange={(e) => set('confirm', e.target.value)} error={errors.confirm} required />
            <Checkbox
              label="Li e aceito os termos de uso e a política de privacidade. Entendo que a plataforma é educativa e não movimenta dinheiro real."
              checked={form.acceptTerms}
              onChange={(e) => set('acceptTerms', e.target.checked)}
            />
            {errors.acceptTerms && <span className="error small" style={{ color: 'var(--danger)' }}>{errors.acceptTerms}</span>}
            <Checkbox
              label="Tenho 12 anos ou mais e, se for menor de 18 anos, meu/minha responsável está ciente do uso da plataforma."
              checked={form.ageConfirmation}
              onChange={(e) => set('ageConfirmation', e.target.checked)}
            />
            {errors.ageConfirmation && <span className="error small" style={{ color: 'var(--danger)' }}>{errors.ageConfirmation}</span>}
            <Button type="submit" loading={loading}>Criar conta</Button>
          </form>
        </div>
        <p className="small muted" style={{ textAlign: 'center' }}>
          Já tem conta? <Link to="/entrar">Entrar</Link>
        </p>
      </div>
    </div>
  )
}
