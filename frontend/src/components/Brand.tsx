import { MapPinHouse } from 'lucide-react'
import { Link } from 'react-router-dom'

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link aria-label="CommonGround home" className="brand" to="/">
      <span className="brand-mark" aria-hidden="true">
        <MapPinHouse size={21} strokeWidth={2.1} />
      </span>
      {!compact && (
        <span className="brand-name">
          Common<span>Ground</span>
        </span>
      )}
    </Link>
  )
}
