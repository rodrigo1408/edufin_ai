import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { AlertTriangle, CheckCircle2, Sparkles, X } from 'lucide-react'
import type { Reward } from '../api/types'

type Kind = 'success' | 'error' | 'reward'

interface ToastItem {
  id: number
  kind: Kind
  message: string
}

interface ToastApi {
  success: (message: string) => void
  error: (message: string) => void
  /** Feedback visual de recompensas concedidas pelo servidor (pontos, conquistas, desafios). */
  reward: (reward: Reward | null | undefined) => void
}

const ToastContext = createContext<ToastApi | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const dismiss = useCallback((id: number) => setItems((list) => list.filter((t) => t.id !== id)), [])

  const push = useCallback(
    (kind: Kind, message: string) => {
      const id = Date.now() + Math.random()
      setItems((list) => [...list.slice(-3), { id, kind, message }])
      window.setTimeout(() => dismiss(id), kind === 'error' ? 7000 : 5000)
    },
    [dismiss],
  )

  const api = useMemo<ToastApi>(
    () => ({
      success: (m) => push('success', m),
      error: (m) => push('error', m),
      reward: (r) => {
        if (!r) return
        if (r.points > 0) push('reward', `+${r.points} pontos!`)
        r.achievements.forEach((a) => push('reward', `🏅 Conquista desbloqueada: ${a.title}`))
        r.challenges.forEach((c) => push('reward', `🎯 Desafio concluído: ${c}`))
      },
    }),
    [push],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" aria-live="polite" aria-atomic="false">
        {items.map((t) => (
          <div key={t.id} className={`toast ${t.kind}`} role={t.kind === 'error' ? 'alert' : 'status'}>
            {t.kind === 'error' ? <AlertTriangle size={18} aria-hidden /> : t.kind === 'reward' ? <Sparkles size={18} aria-hidden /> : <CheckCircle2 size={18} aria-hidden />}
            <span style={{ flex: 1 }}>{t.message}</span>
            <button className="btn btn-ghost btn-icon btn-sm" onClick={() => dismiss(t.id)} aria-label="Fechar aviso">
              <X size={14} aria-hidden />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast fora do ToastProvider')
  return ctx
}
