import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import type { Me } from '../api/types'

export const ME: Me = {
  id: 1,
  email: 'ana@teste.com',
  displayName: 'ana',
  role: 'USER',
  permissions: [],
  createdAt: '2026-01-01T00:00:00Z',
  lastLoginAt: null,
  preferences: { theme: 'SYSTEM', notifyGoals: true, notifyLearning: true, notifyAchievements: true, notifyCommunity: true, weeklyLessonTarget: 3 },
}

export const emptyPage = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }

/** Handlers padrão: usuário autenticado e listas vazias. Cada teste sobrescreve o que precisa. */
export const defaultHandlers = [
  http.get('/api/auth/csrf', () => HttpResponse.json({ headerName: 'X-XSRF-TOKEN' }, { headers: { 'Set-Cookie': 'XSRF-TOKEN=test-token; Path=/' } })),
  http.get('/api/me', () => HttpResponse.json(ME)),
  http.get('/api/notifications/unread-count', () => HttpResponse.json({ count: 0 })),
]

export const server = setupServer(...defaultHandlers)
