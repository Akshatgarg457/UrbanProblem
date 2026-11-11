import { ArrowLeft, CheckCircle2, Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { fetchPublicDepartments, register as apiRegister, registerWorker, type PublicDepartment } from '../services/authService'
import { Brand } from '../components/Brand'
import { MotionPage } from '../components/MotionPage'
import { ThemeToggle } from '../components/ThemeToggle'
import { getEmailError, getLoginIdentifierError, getPasswordError } from '../utils/formValidation'

interface AuthPageProps {
  mode: 'login' | 'register'
  children: ReactNode
}

export function AuthPage({ mode, children }: AuthPageProps) {
  return (
    <div className="auth-shell">
      <a className="skip-link" href="#main-content">Skip to form</a>
      <header className="auth-topbar">
        <Brand />
        <div className="auth-topbar-actions">
          <Link aria-label="Back to home" className="auth-back" to="/"><ArrowLeft size={16} /><span>Back to home</span></Link>
          <ThemeToggle />
        </div>
      </header>
      <MotionPage>
        <div className="auth-page">
          <aside className="auth-side">
            <div className="auth-side-content">
              <span className="auth-side-mark"><ShieldCheck size={22} /></span>
              <span className="eyebrow eyebrow-light">A shared space for local change</span>
              <h2>{mode === 'login' ? 'Welcome back to your neighborhood.' : 'Your neighborhood has a voice. Make yours heard.'}</h2>
              <p>Stay connected to the places and people around you, one thoughtful report at a time.</p>
            </div>
            <p className="auth-side-bottom">CommonGround <span>·</span> Built for stronger neighborhoods</p>
            <div aria-hidden="true" className="auth-decoration auth-decoration-one" />
            <div aria-hidden="true" className="auth-decoration auth-decoration-two" />
          </aside>
          <section className="auth-main" id="main-content" tabIndex={-1}>
            {children}
          </section>
        </div>
      </MotionPage>
    </div>
  )
}

/** UI-only context from /signin. Does not affect authentication or post-login routing. */
const ROLE_HEADINGS: Record<string, string> = {
  citizen: 'Sign in as Citizen',
  admin: 'Sign in as Admin',
  worker: 'Sign in as Worker',
}

export function LoginForm() {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const roleContext = searchParams.get('role')
  const heading = roleContext ? ROLE_HEADINGS[roleContext] : undefined
  const workspace = roleContext === 'citizen' || roleContext === 'worker' || roleContext === 'admin'
    ? roleContext
    : undefined
  const sessionExpired = searchParams.get('expired') === '1'

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setNotice('')
    const identifierError = getLoginIdentifierError(identifier)
    const passwordError = getPasswordError(password)

    if (identifierError || passwordError) {
      setError(identifierError ?? passwordError ?? 'Check your details and try again.')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const authenticatedUser = await login({ email: identifier.trim(), password, workspace })
      const destination = authenticatedUser.role === 'SYSTEM_ADMIN' ? '/dashboard/system-admin' : authenticatedUser.role === 'DEPARTMENT_ADMIN' ? '/dashboard/department-admin' : authenticatedUser.role === 'WORKER' ? '/dashboard/worker' : '/dashboard/citizen'
      navigate(destination, { replace: true })
    } catch (err) { setError(err instanceof Error ? err.message : 'Unable to sign in.') }
    finally { setSubmitting(false) }
  }

  return (
    <div className="auth-form-wrap">
      <span className="eyebrow">Good to see you</span>
      <h1>{heading ?? 'Sign in'}</h1>
      <p className="auth-intro">
        {workspace === 'admin'
          ? 'Sign in with your username or email address to open the admin workspace.'
          : workspace === 'worker'
            ? 'Sign in with your approved worker username or email address to open your field workspace.'
            : 'Sign in with your username or email address and pick up where your community left off.'}
      </p>
      <form className="auth-form" noValidate onSubmit={handleSubmit}>
        <label htmlFor="login-username">Username / Email address</label>
        <input autoCapitalize="none" autoComplete="username" id="login-username" onChange={(event) => setIdentifier(event.target.value)} placeholder="username or you@example.com" spellCheck={false} type="text" value={identifier} />
        <label htmlFor="login-password">Password</label>
        <div className="password-input">
          <input autoComplete="current-password" id="login-password" onChange={(event) => setPassword(event.target.value)} placeholder="Enter your password" type={showPassword ? 'text' : 'password'} value={password} />
          <button aria-label={showPassword ? 'Hide password' : 'Show password'} className="password-toggle" onClick={() => setShowPassword(!showPassword)} type="button">
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        <Link className="auth-forgot-link" to={`/forgot-password${workspace ? `?role=${workspace}` : ''}`}>Forgot password?</Link>
        {error && <p className="form-message form-error" role="alert">{error}</p>}
        {notice && <p className="form-message form-notice" role="status">{notice}</p>}
        {sessionExpired && !error && !notice && <p className="form-message form-notice" role="status">Your session expired. Please sign in again.</p>}
        <button className="button button-primary auth-submit" disabled={submitting} type="submit">{submitting ? 'Signing in…' : 'Continue'} <ArrowLeft className="button-arrow" size={17} /></button>
      </form>
      {(workspace === undefined || workspace === 'citizen') && <p className="auth-switch">New to CommonGround? <Link to="/register">Create a citizen account</Link></p>}
      {(workspace === undefined || workspace === 'worker') && <p className="auth-switch">Working for a department? <Link to="/register/worker">Apply as a field worker</Link></p>}
      <p className="auth-no-backend">
        {workspace === 'admin'
          ? 'Only provisioned department and system administrator accounts can access the admin workspace.'
          : workspace === 'worker'
            ? 'Worker accounts stay inactive until a system administrator approves them.'
            : 'Sign in with your username or the email address registered to your account.'}
      </p>
    </div>
  )
}

interface RegisterFormProps {
  /** Each registration page renders exactly one account type. */
  role: 'citizen' | 'worker'
}

export function RegisterForm({ role }: RegisterFormProps) {
  const isWorker = role === 'worker'
  const [name, setName] = useState('')
  const [phoneNumber, setPhoneNumber] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [departments, setDepartments] = useState<PublicDepartment[]>([])
  const [departmentsError, setDepartmentsError] = useState('')
  const [departmentId, setDepartmentId] = useState('')
  const [securityQuestion, setSecurityQuestion] = useState('')
  const [securityAnswer, setSecurityAnswer] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  const SECURITY_QUESTIONS = [
    'What was the name of your first pet?',
    'What is your mother\'s maiden name?',
    'What was the make of your first car?',
    'What is your favourite childhood teacher\'s name?',
    'What city were you born in?',
  ]

  useEffect(() => {
    if (!isWorker) return
    let cancelled = false
    fetchPublicDepartments()
      .then(list => { if (!cancelled) setDepartments(list) })
      .catch(err => { if (!cancelled) setDepartmentsError(err instanceof Error ? err.message : 'Unable to load departments.') })
    return () => { cancelled = true }
  }, [isWorker])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setNotice('')
    const emailError = getEmailError(email)
    const passwordError = getPasswordError(password)

    if (!name.trim()) {
      setError('Enter your name.')
      return
    }
    if (phoneNumber.trim().length < 10) { setError('Enter a valid phone number (at least 10 characters).'); return }
    if (emailError || passwordError) {
      setError(emailError ?? passwordError ?? 'Check your details and try again.')
      return
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.')
      return
    }
    if (isWorker && !departmentId) {
      setError('Select the department you work for.')
      return
    }
    if (!securityQuestion.trim()) {
      setError('Select a security question.')
      return
    }
    if (securityAnswer.trim().length < 2) {
      setError('Enter a security answer (at least 2 characters).')
      return
    }

    setError('')
    setSubmitting(true)
    try {
      const parts = name.trim().split(/\s+/)
      const base = {
        firstName: parts[0],
        lastName: parts.slice(1).join(' ') || parts[0],
        email: email.trim(),
        phoneNumber: phoneNumber.trim(),
        password,
        securityQuestion: securityQuestion.trim(),
        securityAnswer: securityAnswer.trim(),
      }
      if (isWorker) {
        await registerWorker({ ...base, departmentId: Number(departmentId) })
        setSubmitted(true)
        return
      }
      await apiRegister(base)
      await login({ email: email.trim(), password, workspace: 'citizen' })
      navigate('/dashboard/citizen', { replace: true })
    } catch (err) { setError(err instanceof Error ? err.message : 'Unable to create account.') }
    finally { setSubmitting(false) }
  }

  if (submitted) {
    return (
      <div className="auth-form-wrap">
        <span className="eyebrow">Application received</span>
        <h1>Your worker application is pending approval</h1>
        <p className="auth-intro">A system administrator must review and approve your worker application before you can sign in and access the worker workspace.</p>
        <div className="form-message form-notice" role="status"><CheckCircle2 size={15} /> Application submitted for {email.trim()}.</div>
        <Link className="button button-primary auth-submit" to="/login?role=worker">Go to worker sign-in</Link>
      </div>
    )
  }

  return (
    <div className="auth-form-wrap">
      <span className="eyebrow">{isWorker ? 'Join the field team' : 'Be part of the picture'}</span>
      <h1>{isWorker ? 'Apply as a field worker' : 'Create your citizen account'}</h1>
      <p className="auth-intro">
        {isWorker
          ? 'Tell us which department you work for. A system administrator reviews and approves every application before sign-in is enabled.'
          : 'A few details to get started. You can report local issues and follow them through to a resolution.'}
      </p>
      <form className="auth-form auth-register-form" noValidate onSubmit={handleSubmit}>
        <div className="auth-field">
          <label htmlFor="register-name">Full name</label>
          <input autoComplete="name" id="register-name" onChange={(event) => setName(event.target.value)} placeholder="Your name" type="text" value={name} />
        </div>
        <div className="auth-field">
          <label htmlFor="register-phone">Phone number</label>
          <input autoComplete="tel" id="register-phone" onChange={(event) => setPhoneNumber(event.target.value)} placeholder="10-digit phone number" type="tel" value={phoneNumber} />
        </div>
        <div className="auth-field auth-field-wide">
          <label htmlFor="register-email">Email address</label>
          <input autoComplete="email" id="register-email" onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" type="email" value={email} />
        </div>
        <div className="auth-field">
          <label htmlFor="register-password">Password</label>
          <div className="password-input">
            <input autoComplete="new-password" id="register-password" onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" type={showPassword ? 'text' : 'password'} value={password} />
            <button aria-label={showPassword ? 'Hide password' : 'Show password'} className="password-toggle" onClick={() => setShowPassword(!showPassword)} type="button">
              {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          </div>
        </div>
        <div className="auth-field">
          <label htmlFor="register-confirm-password">Confirm password</label>
          <input autoComplete="new-password" id="register-confirm-password" onChange={(event) => setConfirmPassword(event.target.value)} placeholder="Re-enter your password" type={showPassword ? 'text' : 'password'} value={confirmPassword} />
        </div>
        {isWorker && <div className="auth-field auth-field-wide">
          <label htmlFor="register-department">Department</label>
          <select id="register-department" className="field-input" value={departmentId} onChange={(event) => setDepartmentId(event.target.value)} disabled={!!departmentsError}>
            <option value="">{departments.length === 0 && !departmentsError ? 'Loading departments…' : 'Select your department'}</option>
            {departments.map(department => <option key={department.id} value={department.id}>{department.name}</option>)}
          </select>
          {departmentsError && <p className="form-message form-error" role="alert">{departmentsError}</p>}
        </div>}
        <div className="auth-field auth-field-wide">
          <label htmlFor="register-security-question">Security question</label>
          <select id="register-security-question" className="field-input" value={securityQuestion} onChange={(event) => setSecurityQuestion(event.target.value)}>
            <option value="">Choose a security question</option>
            {SECURITY_QUESTIONS.map(q => <option key={q} value={q}>{q}</option>)}
          </select>
        </div>
        <div className="auth-field auth-field-wide">
          <label htmlFor="register-security-answer">Security answer</label>
          <input autoComplete="off" id="register-security-answer" onChange={(event) => setSecurityAnswer(event.target.value)} placeholder="Your answer" type="text" value={securityAnswer} />
        </div>
        {error && <p className="form-message form-error" role="alert">{error}</p>}
        {notice && <p className="form-message form-notice" role="status">{notice}</p>}
        <button className="button button-primary auth-submit" disabled={submitting} type="submit">
          {submitting ? (isWorker ? 'Submitting application…' : 'Creating account…') : (isWorker ? 'Submit application' : 'Create account')} <ArrowLeft className="button-arrow" size={17} />
        </button>
      </form>
      <p className="auth-switch">Already have an account? <Link to={`/login?role=${role}`}>Sign in</Link></p>
      <p className="auth-no-backend">
        {isWorker
          ? 'Worker applications require system administrator approval.'
          : 'This form creates a citizen account.'}
      </p>
    </div>
  )
}
