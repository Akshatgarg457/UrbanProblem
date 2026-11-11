import { Menu, X } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Brand } from '../Brand'
import { ThemeToggle } from '../ThemeToggle'

export function SiteNavbar() {
  const [menuOpen, setMenuOpen] = useState(false)

  return (
    <header className="site-header">
      <nav aria-label="Main navigation" className="site-nav container">
        <Brand />
        <button
          aria-expanded={menuOpen}
          aria-label={menuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          className="icon-button menu-toggle"
          onClick={() => setMenuOpen(!menuOpen)}
          type="button"
        >
          {menuOpen ? <X size={21} /> : <Menu size={21} />}
        </button>
        <div className={`nav-links${menuOpen ? ' nav-links-open' : ''}`}>
          <a href="/#how-it-works" onClick={() => setMenuOpen(false)}>
            How it works
          </a>
          <a href="/#categories" onClick={() => setMenuOpen(false)}>
            Issues we track
          </a>
          <ThemeToggle />
          <Link className="button button-small button-primary nav-cta" to="/signin" onClick={() => setMenuOpen(false)}>
            Get started
          </Link>
        </div>
      </nav>
    </header>
  )
}
