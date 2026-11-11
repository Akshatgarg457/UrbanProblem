import { ArrowLeft, Bell, CircleHelp, CircleUserRound, ClipboardList, LayoutDashboard, MapPin, Menu, PlayCircle, Send, CheckCircle2, Settings, X } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, Outlet } from 'react-router-dom'
import { Brand } from '../Brand'
import { ThemeToggle } from '../ThemeToggle'
import { SignOutButton } from './SignOutButton'
import { useAuth } from '../../context/AuthContext'
import { useUnreadNotifications } from '../../hooks'

const navLinks = [
  { to: '/dashboard/worker', label: 'Overview', icon: LayoutDashboard, exact: true },
  { to: '/dashboard/worker/tasks?status=ASSIGNED', label: 'Assigned Tasks', icon: ClipboardList },
  { to: '/dashboard/worker/tasks?status=IN_PROGRESS', label: 'In Progress', icon: PlayCircle },
  { to: '/dashboard/worker/tasks?status=SUBMITTED_FOR_REVIEW', label: 'Submitted for Review', icon: Send },
  { to: '/dashboard/worker/tasks?status=RESOLVED', label: 'Completed Tasks', icon: CheckCircle2 },
  { to: '/dashboard/worker/tasks', label: 'All Tasks & Details', icon: ClipboardList, exact: true },
  { to: '/dashboard/worker/profile', label: 'My Profile', icon: CircleUserRound },
  { to: '/dashboard/worker/profile#saved-location', label: 'Saved Location', icon: MapPin },
  { to: '/dashboard/worker/notifications', label: 'Notifications', icon: Bell },
  { to: '/dashboard/worker/help', label: 'Help & Support', icon: CircleHelp },
  { to: '/dashboard/worker/settings', label: 'Settings', icon: Settings },
]

function isLinkActive(to: string, exact: boolean | undefined, pathname: string, search: string): boolean {
  const [pathWithHash, query] = to.split('?')
  const path = pathWithHash.split('#')[0]
  if (query) return pathname === path && search === `?${query}`
  if (exact) return pathname === path
  return pathname === path || pathname.startsWith(`${path}/`)
}

export function WorkerLayout() {
  const [open, setOpen] = useState(false)
  const { user } = useAuth()
  const unread = useUnreadNotifications()
  const location = useLocation()

  return (
    <div className="dashboard-shell">
      <aside className={`dashboard-sidebar${open ? ' sidebar-open' : ''}`} aria-label="Worker navigation">
        <div className="sidebar-brand">
          <Brand />
          <button className="icon-button sidebar-close" onClick={() => setOpen(false)} aria-label="Close" type="button"><X size={19} /></button>
        </div>
        <div className="sidebar-section-label">Field Worker Workspace</div>
        <nav className="dashboard-nav">
          {navLinks.map(({ to, label, icon: Icon, exact }) => {
            const active = isLinkActive(to, exact, location.pathname, location.search)
            const showBadge = label === 'Notifications' && unread > 0
            return (
              <Link key={to} to={to} onClick={() => setOpen(false)} className={active ? 'active' : undefined} aria-current={active ? 'page' : undefined}>
                <Icon size={17} /> {label}
                {showBadge && <span className="nav-badge">{unread > 99 ? '99+' : unread}</span>}
              </Link>
            )
          })}
        </nav>
        <div className="sidebar-bottom">
          <Link className="back-home-link" to="/"><ArrowLeft size={15} /> Home</Link>
          <p>{user?.email ?? 'Signed in'}<br /><span style={{ color: 'var(--text-muted)' }}>{user?.role?.replaceAll('_', ' ')}</span></p>
          <SignOutButton />
        </div>
      </aside>
      {open && <button className="sidebar-backdrop" onClick={() => setOpen(false)} aria-label="Close" type="button" />}
      <div className="dashboard-main">
        <header className="dashboard-topbar">
          <button className="icon-button dashboard-menu-toggle" onClick={() => setOpen(!open)} aria-expanded={open} aria-label="Toggle navigation" type="button"><Menu size={20} /></button>
          <span className="dashboard-topbar-label">Field Worker Workspace</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginLeft: 'auto' }}>
            <ThemeToggle />
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
