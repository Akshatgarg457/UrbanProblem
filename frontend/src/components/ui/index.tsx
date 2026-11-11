import { AlertCircle, CheckCircle2, Info, Loader2, X } from 'lucide-react'
import { type ReactNode, useEffect } from 'react'
import type { ComplaintCategory, ComplaintStatus, Priority } from '../../types'
import { categoryIconClass, categoryLabel, priorityBadgeClass, priorityLabel, statusBadgeClass, statusLabel } from '../../utils/labels'

// ─── Badge ────────────────────────────────────────────────────────────────────

interface BadgeProps { label: string; className: string }
export function Badge({ label, className }: BadgeProps) {
  return <span className={`badge ${className}`}>{label}</span>
}

export function StatusBadge({ status }: { status: ComplaintStatus }) {
  return <Badge label={statusLabel(status)} className={statusBadgeClass(status)} />
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  return <Badge label={priorityLabel(priority)} className={priorityBadgeClass(priority)} />
}

export function CategoryBadge({ category }: { category: ComplaintCategory }) {
  return <span className={`category-pill ${categoryIconClass(category)}`}>{categoryLabel(category)}</span>
}

// ─── Stat Card ────────────────────────────────────────────────────────────────

interface StatCardProps {
  label: string
  value: number | string
  icon: ReactNode
  color?: 'teal' | 'blue' | 'orange' | 'green' | 'red' | 'purple'
  note?: string
}
export function StatCard({ label, value, icon, color = 'teal', note }: StatCardProps) {
  return (
    <div className="stat-card">
      <div className="stat-card-head">
        <span className="stat-card-label">{label}</span>
        <span className={`stat-card-icon stat-icon-${color}`}>{icon}</span>
      </div>
      <div className="stat-card-value">{value}</div>
      {note && <div className="stat-card-note">{note}</div>}
    </div>
  )
}

// ─── Empty State ──────────────────────────────────────────────────────────────

interface EmptyStateProps { icon: ReactNode; title: string; description: string; action?: ReactNode }
export function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon">{icon}</span>
      <h3 className="empty-state-title">{title}</h3>
      <p className="empty-state-desc">{description}</p>
      {action}
    </div>
  )
}

// ─── Page Header ──────────────────────────────────────────────────────────────

interface PageHeaderProps { title: string; description?: string; actions?: ReactNode; eyebrow?: string }
export function PageHeader({ title, description, actions, eyebrow }: PageHeaderProps) {
  return (
    <div className="page-header">
      <div className="page-header-copy">
        {eyebrow && <span className="page-eyebrow">{eyebrow}</span>}
        <h1 className="page-title">{title}</h1>
        {description && <p className="page-desc">{description}</p>}
      </div>
      {actions && <div className="page-header-actions">{actions}</div>}
    </div>
  )
}

// ─── Section Card ─────────────────────────────────────────────────────────────

interface SectionCardProps { title?: string; children: ReactNode; action?: ReactNode; className?: string }
export function SectionCard({ title, children, action, className = '' }: SectionCardProps) {
  return (
    <div className={`section-card ${className}`}>
      {(title || action) && (
        <div className="section-card-header">
          {title && <h2 className="section-card-title">{title}</h2>}
          {action}
        </div>
      )}
      {children}
    </div>
  )
}

// ─── Modal ────────────────────────────────────────────────────────────────────

interface ModalProps { open: boolean; onClose: () => void; title: string; children: ReactNode; size?: 'sm' | 'md' | 'lg' }
export function Modal({ open, onClose, title, children, size = 'md' }: ModalProps) {
  useEffect(() => {
    if (!open) return
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [open, onClose])

  if (!open) return null
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-labelledby="modal-title" onClick={onClose}>
      <div className={`modal-panel modal-${size}`} onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2 id="modal-title" className="modal-title">{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="Close dialog" type="button"><X size={18} /></button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

// ─── Confirm Dialog ───────────────────────────────────────────────────────────

interface ConfirmDialogProps {
  open: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  description: string
  confirmLabel?: string
  danger?: boolean
  loading?: boolean
}
export function ConfirmDialog({ open, onClose, onConfirm, title, description, confirmLabel = 'Confirm', danger = false, loading = false }: ConfirmDialogProps) {
  return (
    <Modal open={open} onClose={onClose} title={title} size="sm">
      <p style={{ color: 'var(--text-secondary)', fontSize: 13, lineHeight: 1.65, margin: '0 0 20px' }}>{description}</p>
      <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
        <button className="button button-secondary button-small" onClick={onClose} type="button">Cancel</button>
        <button
          className={`button button-small ${danger ? 'button-danger' : 'button-primary'}`}
          onClick={onConfirm}
          disabled={loading}
          type="button"
        >
          {loading ? <Loader2 size={14} className="spin" /> : null}
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}

// ─── Toast ────────────────────────────────────────────────────────────────────

interface ToastProps { message: string; type?: 'success' | 'error' | 'info'; onClose: () => void }
export function Toast({ message, type = 'info', onClose }: ToastProps) {
  useEffect(() => {
    const t = setTimeout(onClose, 4000)
    return () => clearTimeout(t)
  }, [onClose])

  const icons = { success: <CheckCircle2 size={16} />, error: <AlertCircle size={16} />, info: <Info size={16} /> }
  return (
    <div className={`toast toast-${type}`} role="alert">
      {icons[type]}
      <span>{message}</span>
      <button className="toast-close" onClick={onClose} aria-label="Dismiss" type="button"><X size={14} /></button>
    </div>
  )
}

// ─── Skeleton ─────────────────────────────────────────────────────────────────

export function Skeleton({ width = '100%', height = 16, radius = 6 }: { width?: string | number; height?: number; radius?: number }) {
  return <div className="skeleton" style={{ width, height, borderRadius: radius }} />
}

// ─── Form Field ───────────────────────────────────────────────────────────────

interface FieldProps {
  label: string
  htmlFor: string
  error?: string
  required?: boolean
  children: ReactNode
  hint?: string
}
export function Field({ label, htmlFor, error, required, children, hint }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={htmlFor} className="field-label">
        {label}{required && <span aria-hidden="true" className="field-required">*</span>}
      </label>
      {children}
      {hint && !error && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error" role="alert">{error}</span>}
    </div>
  )
}

// ─── Search Input ─────────────────────────────────────────────────────────────

interface SearchInputProps { value: string; onChange: (v: string) => void; placeholder?: string }
export function SearchInput({ value, onChange, placeholder = 'Search…' }: SearchInputProps) {
  return (
    <div className="search-wrap">
      <svg className="search-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden="true">
        <circle cx="11" cy="11" r="8" /><path d="m21 21-4.35-4.35" />
      </svg>
      <input
        type="search"
        className="search-input"
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={placeholder}
      />
    </div>
  )
}

// ─── Loader ───────────────────────────────────────────────────────────────────

export function Loader({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loader-wrap" role="status" aria-label={label}>
      <Loader2 size={28} className="spin" style={{ color: 'var(--teal)' }} />
    </div>
  )
}
