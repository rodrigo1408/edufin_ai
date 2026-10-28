import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { api, ApiError, qs, UNAUTHORIZED_EVENT } from './client'

describe('api client', () => {
  it('envia o token CSRF em requisições que alteram dados', async () => {
    let header: string | null = null
    server.use(
      http.post('/api/goals', ({ request }) => {
        header = request.headers.get('X-XSRF-TOKEN')
        return HttpResponse.json({ ok: true }, { status: 201 })
      }),
    )
    document.cookie = 'XSRF-TOKEN=abc123; path=/'
    await api.post('/api/goals', { title: 'x' })
    expect(header).toBe('abc123')
  })

  it('converte erros da API em ApiError com erros de campo', async () => {
    server.use(
      http.get('/api/transactions', () =>
        HttpResponse.json({ status: 400, code: 'VALIDATION_ERROR', message: 'Verifique os campos.', fieldErrors: [{ field: 'amount', message: 'inválido' }] }, { status: 400 }),
      ),
    )
    const err = (await api.get('/api/transactions').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.code).toBe('VALIDATION_ERROR')
    expect(err.fieldError('amount')).toBe('inválido')
  })

  it('avisa a aplicação quando a sessão expira (401)', async () => {
    server.use(http.get('/api/dashboard', () => HttpResponse.json({ code: 'UNAUTHENTICATED', message: 'Faça login' }, { status: 401 })))
    const listener = vi.fn()
    window.addEventListener(UNAUTHORIZED_EVENT, listener)
    await api.get('/api/dashboard').catch(() => undefined)
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener(UNAUTHORIZED_EVENT, listener)
  })

  it('trata falha de rede com mensagem amigável', async () => {
    server.use(http.get('/api/me', () => HttpResponse.error()))
    const err = (await api.get('/api/me').catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.code).toBe('NETWORK_ERROR')
  })

  it('monta query string ignorando vazios', () => {
    expect(qs({ a: 1, b: '', c: undefined, d: 'x y' })).toBe('?a=1&d=x+y')
    expect(qs({})).toBe('')
  })
})
