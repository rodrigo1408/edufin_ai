import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from './server'

// jsdom não implementa ResizeObserver (usado pelos gráficos) nem scrollTo.
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
Element.prototype.scrollTo = () => {}

beforeAll(() => {
  server.listen({ onUnhandledRequest: 'error' })
  // O fetch do Node não aceita URLs relativas; resolvemos contra a origem do jsdom.
  const patched = globalThis.fetch
  globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) =>
    patched(typeof input === 'string' && input.startsWith('/') ? new URL(input, window.location.origin).toString() : input, init)) as typeof fetch
})

afterEach(() => {
  cleanup()
  server.resetHandlers()
  document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/'
})

afterAll(() => server.close())
