import { Outlet } from 'react-router-dom'
import { SiteFooter } from './SiteFooter'
import { SiteNavbar } from './SiteNavbar'

export function PublicLayout() {
  return (
    <div className="site-shell">
      <a className="skip-link" href="#main-content">
        Skip to content
      </a>
      <SiteNavbar />
      <Outlet />
      <SiteFooter />
    </div>
  )
}
