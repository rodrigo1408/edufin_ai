import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Bot, Send } from 'lucide-react'
import { api, errorMessage } from '../api/client'
import type { ChatLink, ChatResponse } from '../api/types'
import { Alert, Button, Card, PageHeader } from '../components/ui'

interface Message {
  from: 'user' | 'bot'
  text: string
  links?: ChatLink[]
  suggestions?: string[]
}

export function ChatbotPage() {
  const info = useQuery({ queryKey: ['chatbot-info'], queryFn: () => api.get<{ disclaimer: string; suggestions: string[] }>('/api/chatbot/info') })
  const [messages, setMessages] = useState<Message[]>([
    { from: 'bot', text: 'Oi! Sou o assistente educativo do EduFin. Posso explicar conceitos de finanças e ajudar a usar a plataforma. Sobre o que você quer conversar?' },
  ])
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)
  const listRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight })
  }, [messages])

  const ask = async (question: string) => {
    const q = question.trim()
    if (!q || loading) return
    setText('')
    setMessages((m) => [...m, { from: 'user', text: q }])
    setLoading(true)
    try {
      const r = await api.post<ChatResponse>('/api/chatbot/ask', { message: q })
      setMessages((m) => [...m, { from: 'bot', text: r.answer, links: r.links, suggestions: r.suggestions }])
    } catch (e) {
      setMessages((m) => [...m, { from: 'bot', text: 'Não consegui responder agora: ' + errorMessage(e) }])
    } finally {
      setLoading(false)
    }
  }

  const submit = (e: FormEvent) => {
    e.preventDefault()
    ask(text)
  }

  const last = messages[messages.length - 1]
  const suggestions = last.from === 'bot' && last.suggestions?.length ? last.suggestions : messages.length === 1 ? info.data?.suggestions ?? [] : []

  return (
    <>
      <PageHeader title="Assistente educativo" subtitle="Tire dúvidas sobre conceitos financeiros e sobre a plataforma." />
      <Alert tone="info">
        <Bot size={18} aria-hidden />
        <span>{info.data?.disclaimer ?? 'Assistente educativo automático. Não é consultoria financeira.'} Nunca compartilhe senhas ou dados bancários.</span>
      </Alert>
      <Card className="stack" title={undefined}>
        <div className="chat" ref={listRef} aria-live="polite" aria-label="Conversa com o assistente" role="log">
          {messages.map((m, i) => (
            <div key={i} className={`bubble ${m.from}`}>
              <span className="sr-only">{m.from === 'user' ? 'Você:' : 'Assistente:'}</span>
              {m.text}
              {m.links && m.links.length > 0 && (
                <div className="row" style={{ marginTop: 8 }}>
                  {m.links.map((l) => <Link key={l.path + l.label} to={l.path} className="btn btn-secondary btn-sm">{l.label}</Link>)}
                </div>
              )}
            </div>
          ))}
          {loading && <div className="bubble bot" aria-label="Assistente digitando">…</div>}
        </div>
        {suggestions.length > 0 && (
          <div className="chip-group" aria-label="Sugestões de perguntas">
            {suggestions.map((s) => <button key={s} className="chip" type="button" onClick={() => ask(s)}>{s}</button>)}
          </div>
        )}
        <form className="row" onSubmit={submit}>
          <label htmlFor="chat-input" className="sr-only">Sua pergunta</label>
          <input id="chat-input" className="input" style={{ flex: 1 }} value={text} onChange={(e) => setText(e.target.value)} maxLength={500} placeholder="Ex.: O que são juros compostos?" autoComplete="off" />
          <Button type="submit" loading={loading} disabled={!text.trim()} aria-label="Enviar pergunta"><Send size={16} aria-hidden /> Enviar</Button>
        </form>
        <p className="small muted">As conversas não são armazenadas. As respostas vêm de uma base de conteúdo revisada; quando o assistente não souber, ele avisa.</p>
      </Card>
    </>
  )
}
