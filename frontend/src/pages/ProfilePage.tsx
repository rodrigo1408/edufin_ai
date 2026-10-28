import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, Trash2 } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { ActivityView, Page, Preferences, Theme } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Checkbox, SelectField, TextField } from '../components/form'
import { Modal } from '../components/Modal'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, PageHeader, Pagination } from '../components/ui'
import { date, dateTime } from '../utils/format'
import { roleLabel } from '../utils/labels'
import { DISPLAY_NAME_RULE, PASSWORD_RULE } from '../utils/validation'

export function ProfilePage() {
  const { user } = useAuth()
  if (!user) return null
  return (
    <>
      <PageHeader title="Perfil" subtitle={<>Conta criada em {date(user.createdAt)} · <Badge>{roleLabel[user.role]}</Badge></>} />
      <div className="grid grid-2">
        <ProfileForm />
        <PreferencesForm prefs={user.preferences} />
        <PasswordForm />
        <EmailForm />
      </div>
      <div className="grid grid-2" style={{ marginTop: 16 }}>
        <ActivityHistory />
        <PrivacyCard />
      </div>
    </>
  )
}

function ProfileForm() {
  const { user, refresh } = useAuth()
  const toast = useToast()
  const [name, setName] = useState(user!.displayName)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!DISPLAY_NAME_RULE.test(name.trim())) return setError('De 3 a 30 caracteres: letras, números, espaço, ponto, hífen ou _.')
    setLoading(true)
    setError(null)
    try {
      await api.put('/api/me', { displayName: name.trim() })
      await refresh()
      toast.success('Apelido atualizado.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Card title="Informações públicas">
      <form className="stack" onSubmit={submit} noValidate>
        <TextField label="Apelido" value={name} onChange={(e) => setName(e.target.value)} error={error ?? undefined} hint="Aparece na comunidade. Evite seu nome completo." />
        <Button type="submit" loading={loading}>Salvar</Button>
      </form>
    </Card>
  )
}

function PreferencesForm({ prefs }: { prefs: Preferences }) {
  const { refresh } = useAuth()
  const toast = useToast()
  const [p, setP] = useState(prefs)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const set = <K extends keyof Preferences>(k: K, v: Preferences[K]) => setP((x) => ({ ...x, [k]: v }))
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await api.put('/api/me/preferences', p)
      await refresh()
      toast.success('Preferências salvas.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Card title="Preferências">
      <form className="stack" onSubmit={submit} noValidate>
        <SelectField label="Tema" value={p.theme} onChange={(e) => set('theme', e.target.value as Theme)} options={[{ value: 'SYSTEM', label: 'Padrão EduFin AI (escuro)' }, { value: 'DARK', label: 'Escuro' }, { value: 'LIGHT', label: 'Claro' }]} />
        <TextField label="Meta semanal de aulas" type="number" min={1} max={50} value={p.weeklyLessonTarget} onChange={(e) => set('weeklyLessonTarget', Number(e.target.value))} />
        <fieldset className="stack" style={{ border: 0, padding: 0, margin: 0, gap: 8 }}>
          <legend className="label" style={{ fontWeight: 600, marginBottom: 6 }}>Notificações internas</legend>
          <Checkbox label="Metas (prazos e conclusões)" checked={p.notifyGoals} onChange={(e) => set('notifyGoals', e.target.checked)} />
          <Checkbox label="Estudos (lembretes e cursos concluídos)" checked={p.notifyLearning} onChange={(e) => set('notifyLearning', e.target.checked)} />
          <Checkbox label="Conquistas e desafios" checked={p.notifyAchievements} onChange={(e) => set('notifyAchievements', e.target.checked)} />
          <Checkbox label="Comunidade (comentários nas minhas publicações)" checked={p.notifyCommunity} onChange={(e) => set('notifyCommunity', e.target.checked)} />
        </fieldset>
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={loading}>Salvar preferências</Button>
      </form>
    </Card>
  )
}

function PasswordForm() {
  const toast = useToast()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    if (!PASSWORD_RULE.test(next)) return setError('A nova senha deve ter de 8 a 72 caracteres, com letras e números.')
    if (next !== confirm) return setError('As senhas não conferem.')
    setLoading(true)
    try {
      await api.put('/api/me/password', { currentPassword: current, newPassword: next })
      toast.success('Senha alterada. Outras sessões abertas foram encerradas.')
      setCurrent('')
      setNext('')
      setConfirm('')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Card title="Alterar senha">
      <form className="stack" onSubmit={submit} noValidate>
        <TextField label="Senha atual" type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} />
        <TextField label="Nova senha" type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} />
        <TextField label="Confirme a nova senha" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={loading}>Alterar senha</Button>
      </form>
    </Card>
  )
}

function EmailForm() {
  const { user, refresh } = useAuth()
  const toast = useToast()
  const [email, setEmail] = useState(user!.email)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await api.put('/api/me/email', { email, currentPassword: password })
      await refresh()
      setPassword('')
      toast.success('E-mail atualizado.')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }
  return (
    <Card title="E-mail de acesso">
      <form className="stack" onSubmit={submit} noValidate>
        <TextField label="E-mail" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <TextField label="Senha atual (para confirmar)" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
        {error && <Alert tone="error">{error}</Alert>}
        <Button type="submit" loading={loading} disabled={!password}>Atualizar e-mail</Button>
      </form>
    </Card>
  )
}

function ActivityHistory() {
  const [page, setPage] = useState(0)
  const activity = useQuery({ queryKey: ['activity', page], queryFn: () => api.get<Page<ActivityView>>(`/api/me/activity?page=${page}&size=10`) })
  return (
    <Card title="Histórico de atividades">
      <ul className="list">
        {activity.data?.content.map((a) => (
          <li key={a.id} className="row-between"><span className="small">{a.description}</span><span className="small muted nowrap">{dateTime(a.createdAt)}</span></li>
        ))}
      </ul>
      {activity.data && <Pagination page={page} totalPages={activity.data.totalPages} onChange={setPage} />}
    </Card>
  )
}

function PrivacyCard() {
  const { logout } = useAuth()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const exportData = async () => {
    try {
      const data = await api.get<unknown>('/api/me/export')
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'edufin-meus-dados.json'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const deleteAccount = async () => {
    setLoading(true)
    setError(null)
    try {
      await api.del('/api/me', { password })
      qc.clear()
      await logout().catch(() => undefined)
      navigate('/', { replace: true })
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card title="Privacidade e dados (LGPD)">
      <p className="small muted">Seus registros financeiros são privados. Você pode baixar uma cópia de todos os seus dados ou excluir sua conta a qualquer momento.</p>
      <div className="row">
        <Button variant="secondary" onClick={exportData}><Download size={16} aria-hidden /> Baixar meus dados</Button>
        <Button variant="danger" onClick={() => setOpen(true)}><Trash2 size={16} aria-hidden /> Excluir conta</Button>
      </div>
      <Modal open={open} title="Excluir conta definitivamente?" onClose={() => setOpen(false)} footer={<><Button variant="secondary" onClick={() => setOpen(false)}>Cancelar</Button><Button variant="danger" onClick={deleteAccount} loading={loading} disabled={!password}>Excluir para sempre</Button></>}>
        <div className="stack">
          <Alert tone="warning">Metas, transações, progresso, publicações e demais dados serão apagados e não poderão ser recuperados.</Alert>
          <TextField label="Digite sua senha para confirmar" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
          {error && <Alert tone="error">{error}</Alert>}
        </div>
      </Modal>
    </Card>
  )
}
