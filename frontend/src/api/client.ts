/**
 * Cliente HTTP da API. Usa cookies de sessão (mesma origem) e o padrão "double submit" de CSRF:
 * lê o cookie XSRF-TOKEN e o envia no cabeçalho X-XSRF-TOKEN em requisições que alteram dados.
 */

export interface FieldError {
  field: string
  message: string
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message)
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }

  fieldError(field: string): string | undefined {
    return this.fieldErrors.find((f) => f.field === field)?.message
  }
}

export const UNAUTHORIZED_EVENT = 'edufin:unauthorized'

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(name + '='))
  return match ? decodeURIComponent(match.split('=')[1]) : null
}

async function ensureCsrfToken(): Promise<string | null> {
  let token = readCookie('XSRF-TOKEN')
  if (!token) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' })
    token = readCookie('XSRF-TOKEN')
  }
  return token
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    const token = await ensureCsrfToken().catch(() => null)
    if (token) {
      headers['X-XSRF-TOKEN'] = token
    }
  }
  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'NETWORK_ERROR', 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.')
  }
  if (response.status === 204) {
    return undefined as T
  }
  const text = await response.text()
  const data = text ? safeJson(text) : null
  if (!response.ok) {
    if (response.status === 401) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }
    const err = (data ?? {}) as { code?: string; message?: string; fieldErrors?: FieldError[] }
    throw new ApiError(
      response.status,
      err.code ?? 'HTTP_' + response.status,
      err.message ?? 'Ocorreu um erro inesperado. Tente novamente.',
      err.fieldErrors ?? [],
    )
  }
  return data as T
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

export const api = {
  get: <T>(path: string) => request<T>('GET', path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  del: <T>(path: string, body?: unknown) => request<T>('DELETE', path, body),
}

/** Monta query string ignorando valores vazios. */
export function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') {
      p.set(k, String(v))
    }
  })
  const s = p.toString()
  return s ? '?' + s : ''
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) return e.message
  return 'Ocorreu um erro inesperado. Tente novamente.'
}
