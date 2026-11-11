import { ArrowLeft, BarChart3, Bell, Building2, CircleHelp, CircleUserRound, ClipboardList, Eye, LayoutDashboard, Menu, RefreshCw, Settings, Users, X } from 'lucide-react'
import { useState } from 'react'
import { Link, NavLink, Outlet } from 'react-router-dom'
import { Brand } from '../Brand'
import { ThemeToggle } from '../ThemeToggle'
import { SignOutButton } from './SignOutButton'
import { useAuth } from '../../context/AuthContext'
import { useUnreadNotifications } from '../../hooks'

const navLinks = [
  { to: '/dashboard/system-admin', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/dashboard/system-admin/users', label: 'Users', icon: Users, end: false },
  { to: '/dashboard/system-admin/departments', label: 'Departments', icon: Building2, end: false },
  { to: '/dashboard/system-admin/complaints', label: 'All Complaints', icon: ClipboardList, end: false },
  { to: '/dashboard/system-admin/review', label: 'Resolution Review', icon: Eye, end: false },
  { to: '/dashboard/system-admin/recurrence', label: 'Recurrence', icon: RefreshCw, end: false },
  { to: '/dashboard/system-admin/analytics', label: 'Analytics', icon: BarChart3, end: false },
  { to: '/dashboard/system-admin/profile', label: 'My Profile', icon: CircleUserRound, end: false },
  { to: '/dashboard/system-admin/notifications', label: 'Notifications', icon: Bell, end: false },
  { to: '/dashboard/system-admin/help', label: 'Help & Support', icon: CircleHelp, end: false },
  { to: '/dashboard/system-admin/settings', label: 'Settings', icon: Settings, end: false },
]

export function SysAdminLayout() {
  const [open, setOpen] = useState(false)
  const { user } = useAuth()
  const unread = useUnreadNotifications()

  return (
    <div className="dashboard-shell">
      <aside className={`dashboard-sidebar${open ? ' sidebar-open' : ''}`} aria-label="System admin navigation">
        <div className="sidebar-brand">
          <Brand />
          <button className="icon-button sidebar-close" onClick={() => setOpen(false)} aria-label="Close" type="button"><X size={19} /></button>
        </div>
        <div className="sidebar-section-label">System Admin Workspace</div>
        <nav className="dashboard-nav">
          {navLinks.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end} onClick={() => setOpen(false)}>
              <Icon size={17} /> {label}
              {label === 'Notifications' && unread > 0 && <span className="nav-badge">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
          ))}
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
          <span className="dashboard-topbar-label">System Administration</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginLeft: 'auto' }}>
            <ThemeToggle />
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
