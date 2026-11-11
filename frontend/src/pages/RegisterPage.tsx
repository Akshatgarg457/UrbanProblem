import { Navigate, useSearchParams } from 'react-router-dom'
import { AuthPage, RegisterForm } from './AuthPage'

/** Citizen sign-up: /register */
export function RegisterPage() {
  const [searchParams] = useSearchParams()
  if (searchParams.get('role') === 'worker') return <Navigate replace to="/register/worker" />
  if (searchParams.get('role') === 'admin') return <Navigate replace to="/login?role=admin" />
  return <AuthPage mode="register"><RegisterForm role="citizen" /></AuthPage>
}

/** Field worker application: /register/worker */
export function WorkerRegisterPage() {
  return <AuthPage mode="register"><RegisterForm role="worker" /></AuthPage>
}
