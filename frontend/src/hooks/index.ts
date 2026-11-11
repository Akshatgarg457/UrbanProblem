/**
 * Custom hooks for the Urban Problem Reporting frontend.
 * Each hook is self-contained and framework-only (no data fetching here).
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { fetchUnreadNotificationCount } from '../services/notificationService'

// ─── useToast ─────────────────────────────────────────────────────────────────

export type ToastType = 'success' | 'error' | 'info'

interface ToastState {
  id: number
  message: string
  type: ToastType
}

/**
 * Simple toast queue — call `showToast('message', 'success')` anywhere.
 * Toasts auto-dismiss after 4 s.
 *
 * Usage:
 *   const { toasts, showToast, dismissToast } = useToast()
 */
export function useToast() {
  const [toasts, setToasts] = useState<ToastState[]>([])
  const counter = useRef(0)

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    const id = ++counter.current
    setToasts((prev) => [...prev, { id, message, type }])
    setTimeout(() => dismissToast(id), 4000)
  }, [])

  const dismissToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return { toasts, showToast, dismissToast }
}

// ─── usePagination ────────────────────────────────────────────────────────────

interface PaginationState {
  page: number
  size: number
  totalPages: number
  totalElements: number
}

/**
 * Manages pagination state.
 *
 * Usage:
 *   const { pagination, setPage, setTotalPages } = usePagination()
 */
export function usePagination(defaultSize = 10) {
  const [pagination, setPagination] = useState<PaginationState>({
    page: 0,
    size: defaultSize,
    totalPages: 1,
    totalElements: 0,
  })

  const setPage = useCallback((page: number) => {
    setPagination((prev) => ({ ...prev, page }))
  }, [])

  const updateFromResponse = useCallback(
    (totalPages: number, totalElements: number) => {
      setPagination((prev) => ({ ...prev, totalPages, totalElements }))
    },
    [],
  )

  return { pagination, setPage, updateFromResponse }
}

// ─── useDebounce ──────────────────────────────────────────────────────────────

/**
 * Returns a debounced copy of `value` that only updates after `delay` ms.
 * Useful for search inputs to avoid firing a request on every keystroke.
 *
 * Usage:
 *   const debouncedSearch = useDebounce(searchText, 350)
 */
export function useDebounce<T>(value: T, delay = 350): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])

  return debounced
}

// ─── useLocalStorage ─────────────────────────────────────────────────────────

/**
 * Like useState, but persists to localStorage.
 * Never store sensitive data (tokens live in sessionStorage via apiClient).
 *
 * Usage:
 *   const [theme, setTheme] = useLocalStorage('theme', 'light')
 */
export function useLocalStorage<T>(key: string, defaultValue: T) {
  const [value, setValue] = useState<T>(() => {
    try {
      const stored = localStorage.getItem(key)
      return stored !== null ? (JSON.parse(stored) as T) : defaultValue
    } catch {
      return defaultValue
    }
  })

  const set = useCallback(
    (newValue: T) => {
      try {
        localStorage.setItem(key, JSON.stringify(newValue))
      } catch {
        // Silently ignore storage errors (private browsing, quota exceeded)
      }
      setValue(newValue)
    },
    [key],
  )

  return [value, set] as const
}

// ─── useUnreadNotifications ──────────────────────────────────────────────────

/** Unread notification count for the authenticated user, for the sidebar badge. */
export function useUnreadNotifications(): number {
  const [count, setCount] = useState(0)
  useEffect(() => {
    let cancelled = false
    void fetchUnreadNotificationCount().then(value => { if (!cancelled) setCount(value) })
    return () => { cancelled = true }
  }, [])
  return count
}

// ─── useConfirm ───────────────────────────────────────────────────────────────

/**
 * Manages open/loading state for a confirmation dialog.
 *
 * Usage:
 *   const { open, loading, ask, confirm, cancel } = useConfirm()
 *   // ask() opens the dialog, confirm() runs the action + closes on success
 */
export function useConfirm() {
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)

  const ask = useCallback(() => setOpen(true), [])
  const cancel = useCallback(() => setOpen(false), [])

  const confirm = useCallback(async (action: () => Promise<void>) => {
    setLoading(true)
    try {
      await action()
      setOpen(false)
    } finally {
      setLoading(false)
    }
  }, [])

  return { open, loading, ask, cancel, confirm }
}
