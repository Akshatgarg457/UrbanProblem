import { CheckCircle2, Eye, XCircle } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import { MotionPage } from '../../components/MotionPage'
import { CategoryBadge, Field, PageHeader, SectionCard } from '../../components/ui'
import { fetchPendingResolutions, fetchComplaintById, reviewResolutionById, type ResolutionDto } from '../../services/complaintService'
import type { Complaint } from '../../types'
import { formatDateTime } from '../../utils/labels'

interface ReviewItem { resolution: ResolutionDto; complaint: Complaint }
export function DeptReview() {
  const { user } = useAuth()
  const workspacePath = user?.role === 'SYSTEM_ADMIN' ? '/dashboard/system-admin' : '/dashboard/department-admin'
  const [items, setItems] = useState<ReviewItem[]>([])
  const [rejectId, setRejectId] = useState<number | null>(null)
  const [reason, setReason] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  async function load() {
    setLoading(true); setError('')
    try {
      const page = await fetchPendingResolutions({ size: 100 })
      const loaded = await Promise.all(page.content.filter(r => r.reviewStatus === 'PENDING').map(async resolution => ({ resolution, complaint: await fetchComplaintById(resolution.complaintPublicId) })))
      setItems(loaded)
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to load pending reviews.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])
  async function decide(id: number, approved: boolean) {
    setBusyId(id); setError(''); setNotice('')
    try { await reviewResolutionById(id, approved, reason); setNotice(approved ? 'Resolution approved. Complaint marked resolved.' : 'Resolution returned to the worker for rework.'); setRejectId(null); setReason(''); await load() }
    catch (e) { setError(e instanceof Error ? e.message : 'Review action failed.') }
    finally { setBusyId(null) }
  }

  return <MotionPage><div className="dashboard-content">
    <PageHeader title="Resolution Review" description={`${items.length} resolution submissions awaiting review.`} />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {notice && <div className="toast-inline toast-info" role="status" style={{ marginBottom: 12 }}><CheckCircle2 size={14} />{notice}</div>}
    {loading ? <p>Loading pending resolutions…</p> : items.length === 0 ? <SectionCard><p style={{ textAlign: 'center', padding: 36, color: 'var(--text-secondary)' }}>No resolution submissions are currently pending review.</p></SectionCard> : <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 20 }}>
      {items.map(({ resolution, complaint }) => <SectionCard key={resolution.id}>
        <div className="review-card-header"><div><CategoryBadge category={complaint.category} /><h3 className="review-card-title">{complaint.title}</h3><span className="review-card-meta">{complaint.referenceId}</span></div><Link to={`${workspacePath}/complaints/${encodeURIComponent(complaint.referenceId)}`} className="button button-secondary button-small"><Eye size={14} /> View Full</Link></div>
        <div className="review-update-box"><div className="review-update-header"><strong>Worker completion notes</strong><span>{formatDateTime(resolution.submittedAt)}</span></div><p className="review-update-text">{resolution.completionNotes}</p></div>
        {rejectId === resolution.id ? <form onSubmit={(e: FormEvent<HTMLFormElement>) => { e.preventDefault(); void decide(resolution.id, false) }}><Field label="Reason for rework" htmlFor={`reason-${resolution.id}`} required><textarea id={`reason-${resolution.id}`} className="field-input field-textarea" rows={3} value={reason} onChange={e => setReason(e.target.value)} /></Field><div className="review-actions"><button className="button button-secondary button-small" type="button" onClick={() => { setRejectId(null); setReason('') }}>Cancel</button><button className="button button-danger button-small" type="submit" disabled={!reason.trim() || busyId === resolution.id}>Submit rework request</button></div></form> : <div className="review-actions"><button className="button button-primary button-small" disabled={busyId === resolution.id} onClick={() => void decide(resolution.id, true)} type="button"><CheckCircle2 size={14} /> Approve</button><button className="button button-secondary button-small" disabled={busyId === resolution.id} onClick={() => { setRejectId(resolution.id); setReason('') }} type="button"><XCircle size={14} /> Return for Rework</button></div>}
      </SectionCard>)}
    </div>}
  </div></MotionPage>
}
