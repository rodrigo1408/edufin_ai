import { useState, type FormEvent } from 'react'
import { ApiError, errorMessage } from '../api/client'
import type { Goal, GoalCategory } from '../api/types'
import { SelectField, TextAreaField, TextField } from '../components/form'
import { Alert, Button } from '../components/ui'
import { parseAmount, todayIso } from '../utils/format'
import { validateGoal, hasErrors, type Errors } from '../utils/validation'
import { goalCategoryLabel, options } from '../utils/labels'

export interface GoalPayload {
  title: string
  description: string
  category: GoalCategory
  targetAmount: number
  deadline: string | null
}

export function GoalForm({ initial, onSubmit, onCancel, submitLabel }: { initial?: Goal; onSubmit: (p: GoalPayload) => Promise<void>; onCancel: () => void; submitLabel: string }) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [category, setCategory] = useState<GoalCategory>(initial?.category ?? 'OTHER')
  const [target, setTarget] = useState(initial ? String(initial.targetAmount) : '')
  const [deadline, setDeadline] = useState(initial?.deadline ?? '')
  const [errors, setErrors] = useState<Errors>({})
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    const v = validateGoal({ title, target, deadline })
    setErrors(v)
    if (hasErrors(v)) return
    setLoading(true)
    setError(null)
    try {
      await onSubmit({ title: title.trim(), description: description.trim(), category, targetAmount: parseAmount(target), deadline: deadline || null })
    } catch (err) {
      if (err instanceof ApiError) setErrors(Object.fromEntries(err.fieldErrors.map((f) => [f.field, f.message])))
      setError(errorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="stack">
      {error && <Alert tone="error">{error}</Alert>}
      <div className="form-grid">
        <TextField className="full" label="Nome da meta" value={title} onChange={(e) => setTitle(e.target.value)} error={errors.title} placeholder="Ex.: Fone de ouvido novo" required maxLength={80} />
        <TextField label="Valor desejado (R$)" inputMode="decimal" value={target} onChange={(e) => setTarget(e.target.value)} error={errors.targetAmount} placeholder="Ex.: 350,00" required />
        <SelectField label="Categoria" value={category} onChange={(e) => setCategory(e.target.value as GoalCategory)} options={options(goalCategoryLabel)} />
        <TextField label="Prazo (opcional)" type="date" value={deadline} min={initial ? undefined : todayIso()} onChange={(e) => setDeadline(e.target.value)} error={errors.deadline} />
        <TextAreaField className="full" label="Descrição (opcional)" value={description} onChange={(e) => setDescription(e.target.value)} maxLength={500} />
      </div>
      <div className="modal-actions">
        <Button variant="secondary" onClick={onCancel}>Cancelar</Button>
        <Button type="submit" loading={loading}>{submitLabel}</Button>
      </div>
    </form>
  )
}
