import { ArrowLeft, BarChart3, Bell, CircleHelp, CircleUserRound, ClipboardList, Eye, LayoutDashboard, Map, Menu, RefreshCw, Settings, UserCheck, Users, X } from 'lucide-react'
import { useState } from 'react'
import { Link, Outlet, useLocation } from 'react-router-dom'
import { Brand } from '../Brand'
import { ThemeToggle } from '../ThemeToggle'
import { SignOutButton } from './SignOutButton'
import { useAuth } from '../../context/AuthContext'
import { useUnreadNotifications } from '../../hooks'

const navLinks = [
  { to: '/dashboard/department-admin', label: 'Overview', icon: LayoutDashboard, exact: true },
  { to: '/dashboard/department-admin/complaints?status=SUBMITTED', label: 'Unassigned Complaints', icon: UserCheck },
  { to: '/dashboard/department-admin/complaints?status=ASSIGNED', label: 'Manage Assignments', icon: ClipboardList },
  { to: '/dashboard/department-admin/complaints', label: 'Complaint Queue', icon: ClipboardList, exact: true },
  { to: '/dashboard/department-admin/map', label: 'Complaint Map', icon: Map },
  { to: '/dashboard/department-admin/review', label: 'Resolution Review', icon: Eye },
  { to: '/dashboard/department-admin/workers', label: 'Workers', icon: Users },
  { to: '/dashboard/department-admin/recurrence', label: 'Recurrence', icon: RefreshCw },
  { to: '/dashboard/department-admin/analytics', label: 'Analytics', icon: BarChart3 },
  { to: '/dashboard/department-admin/profile', label: 'My Profile', icon: CircleUserRound },
  { to: '/dashboard/department-admin/notifications', label: 'Notifications', icon: Bell },
  { to: '/dashboard/department-admin/help', label: 'Help & Support', icon: CircleHelp },
  { to: '/dashboard/department-admin/settings', label: 'Settings', icon: Settings },
]

function isLinkActive(to: string, exact: boolean | undefined, pathname: string, search: string): boolean {
  const [path, query] = to.split('?')
  if (query) return pathname === path && search === `?${query}`
  if (exact) return pathname === path
  return pathname === path || pathname.startsWith(`${path}/`)
}

export function DeptAdminLayout() {
  const [open, setOpen] = useState(false)
  const { user } = useAuth()
  const unread = useUnreadNotifications()
  const location = useLocation()

  return (
    <div className="dashboard-shell">
      <aside className={`dashboard-sidebar${open ? ' sidebar-open' : ''}`} aria-label="Department admin navigation">
        <div className="sidebar-brand">
          <Brand />
          <button className="icon-button sidebar-close" onClick={() => setOpen(false)} aria-label="Close" type="button"><X size={19} /></button>
        </div>
        <div className="sidebar-section-label">Department Admin Workspace</div>
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
          <span className="dashboard-topbar-label">Department Admin Workspace</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginLeft: 'auto' }}>
            <ThemeToggle />
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
