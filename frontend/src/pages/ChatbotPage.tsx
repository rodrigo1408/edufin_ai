import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BookOpen, HelpCircle, Lightbulb, ShieldCheck, Sparkles } from 'lucide-react'
import { api } from '../api/client'
import type { LearningItem } from '../api/types'
import { SelectField } from '../components/form'
import { TutorChat } from '../components/TutorChat'
import { PageHeader } from '../components/ui'

/**
 * Página do tutor. O aluno pode conversar livremente ou escolher um curso em andamento para que o
 * tutor considere o tema da próxima aula como contexto.
 */
export function ChatbotPage() {
  const learning = useQuery({ queryKey: ['my-learning'], queryFn: () => api.get<LearningItem[]>('/api/courses/mine') })
  const [lessonId, setLessonId] = useState('')
  const options = (learning.data ?? []).filter((c) => c.nextLessonId).map((c) => ({ value: String(c.nextLessonId), label: `${c.title} (próxima aula)` }))

  return (
    <>
      <PageHeader title="Tutor IA" subtitle="Tire dúvidas, peça explicações alternativas e exemplos. Dentro das aulas e dos quizzes, o tutor também está disponível." />
      <div className="grid two-col">
        <section className="card ai-surface stack" style={{ padding: 20 }} aria-label="Conversa com o tutor">
          <div className="row-between">
            <div className="tutor-head">
              <span className="ai-mark" aria-hidden><Sparkles size={18} /></span>
              <div>
                <strong>Tutor EduFin AI</strong>
                <span className="small muted">Educação financeira e uso da plataforma</span>
              </div>
            </div>
            {options.length > 0 && (
              <div style={{ minWidth: 240 }}>
                <SelectField label="Estudar com contexto" value={lessonId} onChange={(e) => setLessonId(e.target.value)} placeholder="Sem contexto de aula" options={options} />
              </div>
            )}
          </div>
          {/* A chave reinicia a conversa quando o contexto muda. */}
          <TutorChat key={lessonId || 'free'} lessonId={lessonId ? Number(lessonId) : undefined} />
        </section>

        <aside className="stack" aria-label="Como o tutor ajuda">
          <div className="card stack" style={{ gap: 14 }}>
            <h2 style={{ margin: 0 }}>O que o tutor faz</h2>
            <div className="row" style={{ alignItems: 'flex-start', flexWrap: 'nowrap' }}><span className="next-icon" aria-hidden><BookOpen size={16} /></span><span className="small"><strong>Explica conceitos</strong> de orçamento, poupança, juros, crédito e investimentos em linguagem simples.</span></div>
            <div className="row" style={{ alignItems: 'flex-start', flexWrap: 'nowrap' }}><span className="next-icon" aria-hidden><Lightbulb size={16} /></span><span className="small"><strong>Explica de outro jeito</strong> e dá exemplos práticos quando a primeira explicação não ajudou.</span></div>
            <div className="row" style={{ alignItems: 'flex-start', flexWrap: 'nowrap' }}><span className="next-icon" aria-hidden><HelpCircle size={16} /></span><span className="small"><strong>Ajuda nos quizzes</strong> relembrando o conceito da pergunta, sem entregar a resposta.</span></div>
          </div>
          <div className="card stack" style={{ gap: 10 }}>
            <h2 style={{ margin: 0 }} className="row"><ShieldCheck size={18} aria-hidden color="var(--cyan)" /> Limites</h2>
            <ul className="small muted" style={{ margin: 0, paddingLeft: 18 }}>
              <li>Respostas vêm de uma base de explicações revisada pela equipe; ainda não há um modelo de IA generativa conectado.</li>
              <li>Não recomenda investimentos nem promete rentabilidade.</li>
              <li>Nunca peça ou envie senhas, cartões ou documentos.</li>
              <li>Quando não souber, o tutor avisa e indica conteúdos.</li>
            </ul>
          </div>
        </aside>
      </div>
    </>
  )
}
