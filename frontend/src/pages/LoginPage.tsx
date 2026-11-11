import { Navigate, useSearchParams } from 'react-router-dom'
import { AuthPage, LoginForm } from './AuthPage'

export function LoginPage() {
  const [searchParams] = useSearchParams()
  // Make the role chooser the default entry point. The role cards link back
  // here with ?role=citizen|worker|admin to display the actual login form.
  if (!searchParams.has('role')) {
    const query = searchParams.toString()
    return <Navigate to={`/signin${query ? `?${query}` : ''}`} replace />
  }
  return <AuthPage mode="login"><LoginForm /></AuthPage>
}
