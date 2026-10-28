import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react'

interface FieldProps {
  label: string
  error?: string
  hint?: ReactNode
  className?: string
}

/** Campo com rótulo associado, mensagem de ajuda e erro anunciados para leitores de tela. */
export function TextField({ label, error, hint, className = '', ...input }: FieldProps & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {input.required && <span aria-hidden> *</span>}
      </label>
      <input
        id={id}
        className="input"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? id + '-err' : hint ? id + '-hint' : undefined}
        {...input}
      />
      {hint && !error && <span id={id + '-hint'} className="hint">{hint}</span>}
      {error && <span id={id + '-err'} className="error">{error}</span>}
    </div>
  )
}

export function SelectField({
  label,
  error,
  hint,
  className = '',
  options,
  placeholder,
  ...select
}: FieldProps & SelectHTMLAttributes<HTMLSelectElement> & { options: { value: string; label: string }[]; placeholder?: string }) {
  const id = useId()
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {select.required && <span aria-hidden> *</span>}
      </label>
      <select id={id} className="select" aria-invalid={error ? true : undefined} aria-describedby={error ? id + '-err' : undefined} {...select}>
        {placeholder !== undefined && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span id={id + '-err'} className="error">{error}</span>}
    </div>
  )
}

export function TextAreaField({ label, error, hint, className = '', ...area }: FieldProps & TextareaHTMLAttributes<HTMLTextAreaElement>) {
  const id = useId()
  return (
    <div className={`field ${className}`}>
      <label htmlFor={id}>
        {label}
        {area.required && <span aria-hidden> *</span>}
      </label>
      <textarea id={id} className="textarea" aria-invalid={error ? true : undefined} aria-describedby={error ? id + '-err' : undefined} {...area} />
      {hint && !error && <span className="hint">{hint}</span>}
      {error && <span id={id + '-err'} className="error">{error}</span>}
    </div>
  )
}

export function Checkbox({ label, ...input }: { label: ReactNode } & InputHTMLAttributes<HTMLInputElement>) {
  const id = useId()
  return (
    <div className="checkbox">
      <input id={id} type="checkbox" {...input} />
      <label htmlFor={id}>{label}</label>
    </div>
  )
}
