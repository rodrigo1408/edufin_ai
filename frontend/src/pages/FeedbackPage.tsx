import { useState, type FormEvent } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { MessageSquareHeart } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { FeedbackType, FeedbackView } from '../api/types'
import { SelectField, TextAreaField, TextField } from '../components/form'
import { hasErrors, type Errors } from '../utils/validation'
import { useToast } from '../components/Toast'
import { Alert, Badge, Button, Card, EmptyState, PageHeader } from '../components/ui'
import { dateTime } from '../utils/format'
import { feedbackStatusLabel, feedbackTypeLabel, options } from '../utils/labels'

export function FeedbackPage() {
  const qc = useQueryClient()
  const toast = useToast()
  const mine = useQuery({ queryKey: ['my-feedback'], queryFn: () => api.get<FeedbackView[]>('/api/feedback') })
  const [type, setType] = useState<FeedbackType>('SUGGESTION')
  const [subject, setSubject] = useState('')
  const [message, setMessage] = useState('')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v: Errors = {
      subject: subject.trim() ? undefined : 'Informe um assunto.',
      message: message.trim().length >= 5 ? undefined : 'Escreva pelo menos 5 caracteres.',
    }
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    setError(null)
    try {
      await api.post('/api/feedback', { type, subject: subject.trim(), message: message.trim() })
      toast.success('Obrigado! Seu feedback foi enviado.')
      setSubject('')
      setMessage('')
      qc.invalidateQueries({ queryKey: ['my-feedback'] })
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <PageHeader title="Feedback" subtitle="Sugestões, problemas ou comentários sobre a experiência. Para avaliar um curso, use a página do curso." />
      <div className="grid grid-2">
        <Card title="Enviar feedback">
          <form className="stack" onSubmit={submit} noValidate>
            {error && <Alert tone="error">{error}</Alert>}
            <SelectField label="Tipo" value={type} onChange={(e) => setType(e.target.value as FeedbackType)} options={options(feedbackTypeLabel)} />
            <TextField label="Assunto" value={subject} onChange={(e) => setSubject(e.target.value)} error={errors.subject} maxLength={120} required />
            <TextAreaField label="Mensagem" value={message} onChange={(e) => setMessage(e.target.value)} error={errors.message} maxLength={2000} required />
            <Button type="submit" loading={loading}>Enviar</Button>
          </form>
        </Card>
        <Card title="Meus envios">
          {mine.data?.length === 0 && <EmptyState icon={<MessageSquareHeart size={24} />} title="Nenhum feedback enviado" />}
          <ul className="list">
            {mine.data?.map((f) => (
              <li key={f.id} className="stack" style={{ gap: 4 }}>
                <div className="row-between"><strong>{f.subject}</strong><Badge tone={f.status === 'RESOLVED' ? 'success' : 'info'}>{feedbackStatusLabel[f.status]}</Badge></div>
                <span className="small muted">{feedbackTypeLabel[f.type]} · {dateTime(f.createdAt)}</span>
                {f.adminResponse && <Alert tone="success"><span><strong>Resposta da equipe:</strong> {f.adminResponse}</span></Alert>}
              </li>
            ))}
          </ul>
        </Card>
      </div>
    </>
  )
}
