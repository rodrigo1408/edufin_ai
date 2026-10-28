import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react'
import { QueryClient, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, ApiError, UNAUTHORIZED_EVENT } from '../api/client'
import type { Me, Permission } from '../api/types'

interface AuthState {
  user: Me | null
  loading: boolean
  login: (email: string, password: string) => Promise<Me>
  register: (data: { email: string; password: string; displayName: string; acceptTerms: boolean; ageConfirmation: boolean }) => Promise<Me>
  logout: () => Promise<void>
  refresh: () => Promise<unknown>
  can: (p: Permission) => boolean
}

const AuthContext = createContext<AuthState | null>(null)
const ME_KEY = ['me']

/**
 * Troca de usuário (login/logout/sessão expirada): descarta os dados em cache de outras consultas,
 * que pertencem à sessão anterior, e atualiza a consulta do usuário SEM removê-la — remover a
 * consulta desconectaria o observador ativo e a interface não perceberia a mudança.
 */
function switchUser(qc: QueryClient, user: Me | null) {
  qc.removeQueries({ predicate: (q) => q.queryKey[0] !== ME_KEY[0] })
  qc.setQueryData(ME_KEY, user)
}

async function fetchMe(): Promise<Me | null> {
  try {
    return await api.get<Me>('/api/me')
  } catch (e) {
    if (e instanceof ApiError && e.status === 401) return null
    throw e
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient()
  const me = useQuery({ queryKey: ME_KEY, queryFn: fetchMe, staleTime: 60_000, retry: false })

  // Qualquer 401 da API (sessão expirada) devolve o usuário para o estado deslogado.
  useEffect(() => {
    const onUnauthorized = () => {
      if (qc.getQueryData(ME_KEY)) {
        switchUser(qc, null)
      }
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [qc])

  const theme = me.data?.preferences.theme
  useEffect(() => {
    const root = document.documentElement
    if (theme === 'DARK') root.dataset.theme = 'dark'
    else if (theme === 'LIGHT') root.dataset.theme = 'light'
    else delete root.dataset.theme
  }, [theme])

  const login = useCallback(
    async (email: string, password: string) => {
      const user = await api.post<Me>('/api/auth/login', { email, password })
      switchUser(qc, user)
      return user
    },
    [qc],
  )

  const register = useCallback<AuthState['register']>(
    async (data) => {
      const user = await api.post<Me>('/api/auth/register', data)
      switchUser(qc, user)
      return user
    },
    [qc],
  )

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout')
    } finally {
      switchUser(qc, null)
    }
  }, [qc])

  const value = useMemo<AuthState>(
    () => ({
      user: me.data ?? null,
      loading: me.isLoading,
      login,
      register,
      logout,
      refresh: () => qc.invalidateQueries({ queryKey: ME_KEY }),
      can: (p) => !!me.data?.permissions.includes(p),
    }),
    [me.data, me.isLoading, login, register, logout, qc],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth fora do AuthProvider')
  return ctx
}
