import { LogOut } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { ConfirmDialog } from '../ui'

/** Clearly visible sign-out control for dashboard sidebars, with a confirmation step. */
export function SignOutButton() {
  const { logout } = useAuth()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [signingOut, setSigningOut] = useState(false)

  async function handleConfirm() {
    setSigningOut(true)
    try {
      await logout() // clears token + user and redirects to the landing page
    } finally {
      setSigningOut(false)
      setConfirmOpen(false)
    }
  }

  return (
    <>
      <button
        type="button"
        className="sidebar-signout"
        onClick={() => setConfirmOpen(true)}
        aria-label="Sign out of your account"
      >
        <LogOut size={15} /> Sign out
      </button>
      <ConfirmDialog
        open={confirmOpen}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => void handleConfirm()}
        title="Sign out"
        description="You will be signed out of CommonGround and returned to the home page. Any unsaved changes will be lost."
        confirmLabel="Sign out"
        loading={signingOut}
      />
    </>
  )
}
