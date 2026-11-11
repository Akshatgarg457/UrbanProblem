import { ArrowUpRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Brand } from '../Brand'

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="container footer-main">
        <div className="footer-about">
          <Brand />
          <p>A clearer way to care for the places we call home.</p>
        </div>
        <div className="footer-links">
          <span className="eyebrow">Explore</span>
          <Link to="/#how-it-works">How it works</Link>
          <Link to="/#categories">Problem categories</Link>
        </div>
        <div className="footer-links">
          <span className="eyebrow">Your account</span>
          <Link to="/signin">Sign in</Link>
          <Link to="/register">Create an account</Link>
        </div>
        <Link className="footer-cta" to="/signin">
          Make a difference nearby <ArrowUpRight size={16} />
        </Link>
      </div>
      <div className="container footer-bottom">
        <span>© {new Date().getFullYear()} CommonGround</span>
        <span>Built for stronger neighborhoods.</span>
      </div>
    </footer>
  )
}
