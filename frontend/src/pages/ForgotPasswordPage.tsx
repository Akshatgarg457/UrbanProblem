import { ArrowLeft, CheckCircle2, Loader2 } from 'lucide-react'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthPage } from './AuthPage'
import { fetchSecurityQuestion, resetPasswordWithSecurityAnswer } from '../services/authService'
import { getLoginIdentifierError, getPasswordError } from '../utils/formValidation'

const WORKSPACES = ['citizen', 'worker', 'admin'] as const

type Step = 'identifier' | 'reset' | 'success'

export function ForgotPasswordPage() {
  const [searchParams] = useSearchParams()
  const requestedRole = searchParams.get('role')
  const role = WORKSPACES.find(workspace => workspace === requestedRole)
  const signInPath = role ? `/login?role=${role}` : '/signin'

  const [step, setStep] = useState<Step>('identifier')
  const [identifier, setIdentifier] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleLookup(e: FormEvent) {
    e.preventDefault()
    setError('')
    const identifierError = getLoginIdentifierError(identifier)
    if (identifierError) {
      setError(identifierError)
      return
    }
    setLoading(true)
    try {
      const q = await fetchSecurityQuestion({ identifier: identifier.trim() })
      if (!q) {
        setError('We could not find an active account with that identifier, or it has no recovery question set.')
        setLoading(false)
        return
      }
      setQuestion(q)
      setStep('reset')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to look up account.')
    } finally {
      setLoading(false)
    }
  }

  async function handleReset(e: FormEvent) {
    e.preventDefault()
    setError('')
    const passwordError = getPasswordError(newPassword)
    if (passwordError) {
      setError(passwordError)
      return
    }
    if (newPassword !== confirmPassword) {
      setError('New passwords do not match.')
      return
    }
    if (answer.trim().length < 2) {
      setError('Enter your security answer.')
      return
    }
    setLoading(true)
    try {
      await resetPasswordWithSecurityAnswer({ identifier: identifier.trim(), answer: answer.trim(), newPassword })
      setStep('success')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to reset password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthPage mode="login">
      <div className="auth-form-wrap">
        <span className="eyebrow">Account recovery</span>
        <h1>Forgot your password?</h1>

        {step === 'identifier' && (
          <>
            <p className="auth-intro">
              Enter the username or email address for your account. We will show the security question you chose when you registered.
            </p>
            <form className="auth-form" noValidate onSubmit={handleLookup}>
              <label htmlFor="recovery-identifier">Username / Email address</label>
              <input
                autoCapitalize="none"
                autoComplete="username"
                id="recovery-identifier"
                onChange={(event) => setIdentifier(event.target.value)}
                placeholder="username or you@example.com"
                spellCheck={false}
                type="text"
                value={identifier}
              />
              {error && <p className="form-message form-error" role="alert">{error}</p>}
              <button className="button button-primary auth-submit" disabled={loading} type="submit">
                {loading ? <><Loader2 size={16} className="spin" /> Looking up…</> : 'Continue'} <ArrowLeft className="button-arrow" size={17} />
              </button>
            </form>
          </>
        )}

        {step === 'reset' && (
          <>
            <p className="auth-intro">
              Answer your security question to set a new password.
            </p>
            <form className="auth-form" noValidate onSubmit={handleReset}>
              <label htmlFor="recovery-question">Security question</label>
              <input id="recovery-question" readOnly type="text" value={question} style={{ background: 'var(--surface-subtle)', cursor: 'default' }} />
              <label htmlFor="recovery-answer">Your answer</label>
              <input autoComplete="off" id="recovery-answer" onChange={(event) => setAnswer(event.target.value)} placeholder="Enter your answer" type="text" value={answer} />
              <label htmlFor="recovery-new-password">New password</label>
              <input autoComplete="new-password" id="recovery-new-password" onChange={(event) => setNewPassword(event.target.value)} placeholder="At least 8 characters" type="password" value={newPassword} />
              <label htmlFor="recovery-confirm-password">Confirm new password</label>
              <input autoComplete="new-password" id="recovery-confirm-password" onChange={(event) => setConfirmPassword(event.target.value)} placeholder="Re-enter your password" type="password" value={confirmPassword} />
              {error && <p className="form-message form-error" role="alert">{error}</p>}
              <button className="button button-primary auth-submit" disabled={loading} type="submit">
                {loading ? <><Loader2 size={16} className="spin" /> Resetting…</> : 'Reset password'} <ArrowLeft className="button-arrow" size={17} />
              </button>
            </form>
          </>
        )}

        {step === 'success' && (
          <>
            <div className="form-message form-notice" role="status">
              <CheckCircle2 size={16} />
              Your password has been reset. You can now sign in with your new password.
            </div>
            <Link className="button button-primary auth-submit" to={signInPath}>Back to sign in <ArrowLeft className="button-arrow" size={17} /></Link>
          </>
        )}

        {step !== 'success' && (
          <Link className="button button-secondary auth-submit" to={signInPath} style={{ marginTop: 12, justifyContent: 'center' }}>
            Back to sign in
          </Link>
        )}
      </div>
    </AuthPage>
  )
}
