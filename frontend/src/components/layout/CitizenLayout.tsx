import { ArrowLeft, Bell, ChevronRight, CircleHelp, CircleUserRound, ClipboardList, LayoutDashboard, Menu, PlusCircle, Settings, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { Brand } from '../Brand'
import { ThemeToggle } from '../ThemeToggle'
import { SignOutButton } from './SignOutButton'
import { useAuth } from '../../context/AuthContext'
import { useUnreadNotifications } from '../../hooks'

const navLinks = [
  { to: '/dashboard/citizen', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/dashboard/citizen/report', label: 'Report Issue', icon: PlusCircle, end: false },
  { to: '/dashboard/citizen/complaints', label: 'My Complaints', icon: ClipboardList, end: false },
  { to: '/dashboard/citizen/profile', label: 'My Profile', icon: CircleUserRound, end: false },
  { to: '/dashboard/citizen/notifications', label: 'Notifications', icon: Bell, end: false },
  { to: '/dashboard/citizen/help', label: 'Help & Support', icon: CircleHelp, end: false },
  { to: '/dashboard/citizen/settings', label: 'Settings', icon: Settings, end: false },
]

export function CitizenLayout() {
  const [open, setOpen] = useState(false)
  const { user } = useAuth()
  const unread = useUnreadNotifications()
  const location = useLocation()
  const currentLabel = [...navLinks]
    .reverse()
    .find(({ to, end }) => (end ? location.pathname === to : location.pathname === to || location.pathname.startsWith(`${to}/`)))
    ?.label ?? 'Overview'
  const email = user?.email ?? 'Signed in'
  const initial = (email[0] ?? 'C').toUpperCase()

  return (
    <div className="dashboard-shell">
      <aside className={`dashboard-sidebar${open ? ' sidebar-open' : ''}`} aria-label="Citizen navigation">
        <div className="sidebar-brand">
          <Brand />
          <button className="icon-button sidebar-close" onClick={() => setOpen(false)} aria-label="Close navigation" type="button"><X size={19} /></button>
        </div>
        <div className="sidebar-section-label">Citizen Workspace</div>
        <nav className="dashboard-nav">
          {navLinks.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}>
              <Icon size={17} /> {label}
              {label === 'Notifications' && unread > 0 && <span className="nav-badge">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <Link className="back-home-link" to="/"><ArrowLeft size={15} /> Back to home</Link>
          <div className="sidebar-user">
            <span aria-hidden="true" className="sidebar-user-avatar">{initial}</span>
            <span className="sidebar-user-copy">
              <strong title={email}>{email}</strong>
              <small>Citizen account</small>
            </span>
          </div>
          <SignOutButton />
        </div>
      </aside>
      {open && <button className="sidebar-backdrop" onClick={() => setOpen(false)} aria-label="Close navigation" type="button" />}
      <div className="dashboard-main">
        <header className="dashboard-topbar">
          <button className="icon-button dashboard-menu-toggle" onClick={() => setOpen(!open)} aria-expanded={open} aria-label="Toggle navigation" type="button"><Menu size={20} /></button>
          <nav aria-label="Breadcrumb" className="topbar-breadcrumb">
            <span>Citizen workspace</span>
            <ChevronRight aria-hidden="true" size={13} />
            <strong>{currentLabel}</strong>
          </nav>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginLeft: 'auto' }}>
            <ThemeToggle />
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
