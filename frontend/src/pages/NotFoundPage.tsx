import { ArrowLeft, MapPinOff } from 'lucide-react'
import { Link } from 'react-router-dom'
import { MotionPage } from '../components/MotionPage'

export function NotFoundPage() {
  return (
    <MotionPage>
      <section className="not-found-page" id="main-content">
        <span className="not-found-icon"><MapPinOff size={27} /></span>
        <span className="eyebrow">404 · Wrong turn</span>
        <h1>This page isn’t on our map.</h1>
        <p>The page may have moved, or the address might need another look.</p>
        <Link className="button button-primary" to="/"><ArrowLeft size={17} /> Back to CommonGround</Link>
      </section>
    </MotionPage>
  )
}
