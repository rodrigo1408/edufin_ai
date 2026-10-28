import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Lightbulb, MessageCircleQuestion, RefreshCw, Send, Sparkles } from 'lucide-react'
import { api, ApiError, errorMessage, qs } from '../api/client'
import type { ChatLink, ChatResponse, TutorInfo, TutorMode } from '../api/types'
import { Badge, Button } from './ui'

interface Message {
  from: 'user' | 'bot'
  text: string
  error?: boolean
  links?: ChatLink[]
  entryId?: string | null
  canRephrase?: boolean
  canGiveExample?: boolean
  mode?: TutorMode
}

const modeLabel: Record<TutorMode, string> = {
  ASK: 'Pergunta',
  ALTERNATIVE: 'Explicação alternativa',
  EXAMPLE: 'Exemplo prático',
}

/**
 * Conversa com o tutor. Com {@code lessonId}, o tutor considera o tema da aula em estudo.
 * Mostra estados de processamento, erro, indisponibilidade (limite de uso) e as ações
 * "explicar de outro jeito" e "dar um exemplo" quando a resposta anterior as suporta.
 */
export function TutorChat({ lessonId, compact = false }: { lessonId?: number; compact?: boolean }) {
  const info = useQuery({
    queryKey: ['tutor-info', lessonId ?? null],
    queryFn: () => api.get<TutorInfo>('/api/chatbot/info' + qs({ lessonId })),
    staleTime: 5 * 60_000,
  })
  const greeting = lessonId
    ? 'Estou acompanhando esta aula. Pergunte algo sobre o conteúdo ou peça um exemplo.'
    : 'Oi! Posso explicar conceitos de finanças e ajudar você a usar a plataforma. Sobre o que quer aprender?'
  const [messages, setMessages] = useState<Message[]>([{ from: 'bot', text: greeting }])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)
  const [unavailable, setUnavailable] = useState<string | null>(null)
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, loading])

  const send = async (body: { message?: string; mode?: TutorMode; entryId?: string | null }, userText: string) => {
    if (loading) return
    setMessages((m) => [...m, { from: 'user', text: userText }])
    setLoading(true)
    try {
      const r = await api.post<ChatResponse>('/api/chatbot/ask', { ...body, lessonId })
      setUnavailable(null)
      setMessages((m) => [...m, { from: 'bot', text: r.answer, links: r.links, entryId: r.entryId, canRephrase: r.canRephrase, canGiveExample: r.canGiveExample, mode: r.mode }])
    } catch (e) {
      if (e instanceof ApiError && e.status === 429) setUnavailable(e.message)
      setMessages((m) => [...m, { from: 'bot', error: true, text: 'Não consegui responder agora. ' + errorMessage(e) }])
    } finally {
      setLoading(false)
    }
  }

  const ask = (q: string) => {
    const question = q.trim()
    if (!question) return
    setText('')
    send({ message: question, mode: 'ASK' }, question)
  }

  const submit = (e: FormEvent) => {
    e.preventDefault()
    ask(text)
  }

  const lastBot = [...messages].reverse().find((m) => m.from === 'bot' && !m.error && m.entryId)
  const suggestions = messages.length === 1 ? info.data?.suggestions.slice(0, compact ? 3 : 6) ?? [] : []

  return (
    <div className="stack" style={{ gap: 12 }}>
      {info.data?.context && (
        <div className="row small muted" style={{ gap: 6 }}>
          <Badge tone="ai">Contexto</Badge> Aula: <strong style={{ color: 'var(--text)' }}>{info.data.context.title}</strong> · {info.data.context.topicLabel}
        </div>
      )}
      <div className="chat" ref={listRef} role="log" aria-live="polite" aria-label="Conversa com o tutor" style={compact ? { minHeight: 200, maxHeight: '46vh' } : undefined}>
        {messages.map((m, i) => (
          <div key={i} className={`bubble ${m.from} ${m.error ? 'error' : ''}`}>
            <span className="sr-only">{m.from === 'user' ? 'Você:' : 'Tutor:'}</span>
            {m.mode && m.mode !== 'ASK' && <div className="small" style={{ color: 'var(--cyan)', fontWeight: 600, marginBottom: 4 }}>{modeLabel[m.mode]}</div>}
            {m.text}
            {m.links && m.links.length > 0 && (
              <div className="bubble-meta">
                {m.links.map((l) => <Link key={l.path + l.label} to={l.path} className="btn btn-secondary btn-sm">{l.label}</Link>)}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div className="bubble bot" aria-label="Tutor preparando a resposta">
            <span className="typing" aria-hidden><span /><span /><span /></span>
          </div>
        )}
      </div>

      {unavailable && (
        <div className="alert alert-warning" role="status">
          <RefreshCw size={16} aria-hidden /> <span>Tutor temporariamente indisponível: {unavailable}</span>
        </div>
      )}

      {lastBot && !loading && (lastBot.canRephrase || lastBot.canGiveExample) && (
        <div className="chip-group" aria-label="Aprofundar a última resposta">
          {lastBot.canRephrase && (
            <button type="button" className="chip ai" onClick={() => send({ mode: 'ALTERNATIVE', entryId: lastBot.entryId }, 'Explique de outro jeito')}>
              <Sparkles size={13} aria-hidden style={{ verticalAlign: '-2px' }} /> Explicar de outro jeito
            </button>
          )}
          {lastBot.canGiveExample && (
            <button type="button" className="chip ai" onClick={() => send({ mode: 'EXAMPLE', entryId: lastBot.entryId }, 'Me dê um exemplo')}>
              <Lightbulb size={13} aria-hidden style={{ verticalAlign: '-2px' }} /> Dar um exemplo
            </button>
          )}
        </div>
      )}

      {suggestions.length > 0 && (
        <div className="chip-group" aria-label="Perguntas sugeridas">
          {suggestions.map((s) => (
            <button key={s} className="chip" type="button" onClick={() => ask(s)}>
              <MessageCircleQuestion size={13} aria-hidden style={{ verticalAlign: '-2px' }} /> {s}
            </button>
          ))}
        </div>
      )}

      <form className="row" onSubmit={submit} style={{ flexWrap: 'nowrap' }}>
        <label htmlFor={lessonId ? 'tutor-input-lesson' : 'tutor-input'} className="sr-only">Sua pergunta</label>
        <input
          id={lessonId ? 'tutor-input-lesson' : 'tutor-input'}
          className="input"
          style={{ flex: 1, minWidth: 0 }}
          value={text}
          onChange={(e) => setText(e.target.value)}
          maxLength={500}
          placeholder={lessonId ? 'Pergunte sobre esta aula…' : 'Ex.: O que são juros compostos?'}
          autoComplete="off"
        />
        <Button type="submit" variant="ai" loading={loading} disabled={!text.trim()} aria-label="Enviar pergunta">
          <Send size={16} aria-hidden /> {compact ? '' : 'Enviar'}
        </Button>
      </form>
      <p className="small muted" style={{ margin: 0 }}>
        {info.data?.disclaimer ?? 'Tutor educativo com explicações curadas. Não é consultoria financeira.'} As conversas não são armazenadas.
      </p>
    </div>
  )
}
