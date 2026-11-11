import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  headers: {
    Accept: 'application/json',
    'Content-Type': 'application/json',
  },
  timeout: 15_000,
  withCredentials: true,
  withXSRFToken: true,
})

if (typeof window !== 'undefined') {
  window.sessionStorage.removeItem('cg_token')
}

let csrfInitialization: Promise<void> | null = null

function csrfCookie(): string | undefined {
  if (typeof document === 'undefined') return undefined
  const cookie = document.cookie.split('; ').find(value => value.startsWith('XSRF-TOKEN='))
  return cookie ? decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)) : undefined
}

apiClient.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    const method = config.method?.toUpperCase()
    if (method && !['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method) && !csrfCookie()) {
      csrfInitialization ??= apiClient.get('/auth/csrf')
        .then(() => undefined)
        .finally(() => { csrfInitialization = null })
      await csrfInitialization
    }
    const token = csrfCookie()
    if (token) config.headers.set('X-XSRF-TOKEN', token)
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      config.headers.delete('Content-Type')
    }
    return config
  },
  error => Promise.reject(error),
)

apiClient.interceptors.response.use(
  response => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url ?? ''
      const isAuthBootstrapRequest = /\/auth\/(login|me|csrf)$/.test(requestUrl)
      const isPublicAuthPage = ['/login', '/signin', '/forgot-password'].includes(window.location.pathname)
      if (!isAuthBootstrapRequest && !isPublicAuthPage) {
        window.location.href = '/login?expired=1'
      }
    }
    return Promise.reject(error)
  },
)
