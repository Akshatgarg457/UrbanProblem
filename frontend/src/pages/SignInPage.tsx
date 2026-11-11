import { ArrowRight, CircleUserRound, ShieldCheck, UserPlus, Wrench } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { MotionPage } from '../components/MotionPage'

const roles = [
  {
    key: 'citizen',
    icon: CircleUserRound,
    title: 'Citizen',
    description: 'Report local issues, track their progress, and stay in the loop about your neighborhood.',
  },
  {
    key: 'admin',
    icon: ShieldCheck,
    title: 'Admin workspace',
    description: 'Manage departments, review complaint queues, and oversee resolution workflows.',
  },
  {
    key: 'worker',
    icon: Wrench,
    title: 'Worker workspace',
    description: 'View field assignments, document your work, and submit resolutions for review.',
  },
] as const

export function SignInPage() {
  const [searchParams] = useSearchParams()
  const expiredSuffix = searchParams.get('expired') === '1' ? '&expired=1' : ''
  return (
    <MotionPage>
      <section className="section signin-section" id="main-content">
        <div className="container">
          <div className="section-heading">
            <div>
              <span className="eyebrow">Welcome back</span>
              <h1>Sign in to CommonGround</h1>
            </div>
            <p>Choose a workspace, then sign in with the account and password registered for that role.</p>
          </div>
          {searchParams.get('expired') === '1' && <p className="form-message form-notice" role="status">Your session expired. Choose your account type and sign in again.</p>}
          <div className="signin-grid">
            {roles.map(({ key, icon: Icon, title, description }) => (
              <article className="signin-card" key={key}>
                <span className="signin-card-icon"><Icon size={26} strokeWidth={1.8} /></span>
                <h2>{title}</h2>
                <p>{description}</p>
                <div className="signin-card-actions">
                  <Link className="button button-primary button-small" to={`/login?role=${key}${expiredSuffix}`}>
                    Sign in <ArrowRight size={15} />
                  </Link>
                  {key === 'citizen' && (
                    <Link className="text-link" to="/register"><UserPlus size={15} /> Create account</Link>
                  )}
                  {key === 'worker' && (
                    <Link className="text-link" to="/register/worker"><UserPlus size={15} /> Apply as worker</Link>
                  )}
                  {key === 'admin' && (
                    <span className="signin-note">Administrator accounts are provisioned by an existing system administrator.</span>
                  )}
                  <Link className="text-link" to={`/forgot-password?role=${key}`}>Forgot password?</Link>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </MotionPage>
  )
}
