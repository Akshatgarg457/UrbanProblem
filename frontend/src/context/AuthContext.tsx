import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import type { AuthUser } from '../types'
import { getCurrentUser, initializeCsrfToken, login as apiLogin, logout as apiLogout } from '../services/authService'
import type { LoginRequest } from '../services/authService'

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  error: string | null
  login: (data: LoginRequest) => Promise<AuthUser>
  logout: () => Promise<void>
}
const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    initializeCsrfToken()
      .then(() => getCurrentUser())
      .then(currentUser => { if (!cancelled) setUser(currentUser) })
      .catch(err => { if (!cancelled) setError(err instanceof Error ? err.message : 'Unable to restore your session.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [])

  const login = useCallback(async (data: LoginRequest) => {
    setError(null)
    try {
      const authenticatedUser = await apiLogin(data)
      setUser(authenticatedUser)
      return authenticatedUser
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Login failed.'
      setError(message)
      throw new Error(message)
    }
  }, [])

  const logout = useCallback(async () => {
    await apiLogout()
    setUser(null)
    window.location.href = '/'
  }, [])

  const value = useMemo(
    () => ({ user, loading, error, login, logout }),
    [user, loading, error, login, logout]
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>')
  return context
}
