import { AlertCircle, ArrowLeft, Calendar, CheckCircle2, ExternalLink, MapPin, RefreshCw, User as UserIcon, Upload } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { useCallback, useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { CategoryBadge, PageHeader, PriorityBadge, SectionCard, StatusBadge } from '../../components/ui'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { fetchComplaintById, fetchComplaintHistory, fetchLatestResolution, getWorkerAssignmentForComplaint, startAssignment, submitResolution, uploadEvidence, assignWorker, type AssignmentDto, type ResolutionDto } from '../../services/complaintService'
import { useAuth } from '../../context/AuthContext'
import type { Complaint, ComplaintHistoryEntry, User } from '../../types'
import { fetchDepartmentWorkers } from '../../services/userService'
import { formatDateTime, statusLabel } from '../../utils/labels'
import { formatCoordinatePair, isValidCoordinate, osmDirectionsUrl, osmViewUrl } from '../../utils/location'

const STATUS_ORDER = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED_FOR_REVIEW', 'RESOLVED'] as const
export function ComplaintDetail() {
  const { id = '' } = useParams<{ id: string }>()
  const { user } = useAuth()
  const [complaint, setComplaint] = useState<Complaint | null>(null)
  const [history, setHistory] = useState<ComplaintHistoryEntry[]>([])
  const [assignment, setAssignment] = useState<AssignmentDto | null>(null)
  const [resolution, setResolution] = useState<ResolutionDto | null>(null)
  const [workers, setWorkers] = useState<User[]>([])
  const [selectedWorkerId, setSelectedWorkerId] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [completionNotes, setCompletionNotes] = useState('')
  const [evidenceFiles, setEvidenceFiles] = useState<File[]>([])
  const load = useCallback(async () => {
    setLoading(true); setError('')
    try {
      const result = await fetchComplaintById(id)
      setComplaint(result)
      try { setHistory(await fetchComplaintHistory(result.referenceId)) }
      catch { setHistory([]) } // History is supplementary; the detail view still works without it.
      if (user?.role === 'WORKER') {
        const ownAssignment = await getWorkerAssignmentForComplaint(result.id)
        setAssignment(ownAssignment)
        if (ownAssignment) setResolution(await fetchLatestResolution(ownAssignment.id))
      }
      if (user?.role === 'SYSTEM_ADMIN' && result.departmentId) {
        setWorkers(await fetchDepartmentWorkers(result.departmentId))
      } else if (user?.role === 'DEPARTMENT_ADMIN' && user.departmentId) {
        setWorkers(await fetchDepartmentWorkers(user.departmentId))
      }
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to load complaint.') }
    finally { setLoading(false) }
  }, [id, user?.role])
  useEffect(() => { void load() }, [load])
  const backPath = window.location.pathname.startsWith('/dashboard/system-admin') ? '/dashboard/system-admin/complaints' : window.location.pathname.startsWith('/dashboard/department-admin') ? '/dashboard/department-admin/complaints' : window.location.pathname.startsWith('/dashboard/worker') ? '/dashboard/worker/tasks' : '/dashboard/citizen/complaints'

  async function assignSelectedWorker() {
    if (!complaint || !selectedWorkerId) return
    setBusy(true); setError(''); setNotice('')
    try { await assignWorker(complaint.id, { workerId: Number(selectedWorkerId) }); setNotice('Worker assigned successfully.'); await load() }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to assign worker.') }
    finally { setBusy(false) }
  }
  async function beginWork() {
    if (!assignment) return
    setBusy(true); setError(''); setNotice('')
    try { const updated = await startAssignment(assignment.id); setAssignment(updated); setNotice('Work started. Upload completion evidence when the task is finished.') }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to start work.') }
    finally { setBusy(false) }
  }
  async function completeWork(event: React.FormEvent) {
    event.preventDefault()
    if (!assignment || evidenceFiles.length === 0 || completionNotes.trim().length < 10) return
    setBusy(true); setError(''); setNotice('')
    try {
      for (const file of evidenceFiles) await uploadEvidence(complaint!.id, file)
      await submitResolution(assignment.id, completionNotes.trim())
      setNotice('Resolution submitted successfully. Your department administrator can now review it.')
      setEvidenceFiles([]); setCompletionNotes(''); await load()
    } catch (e) { setError(e instanceof Error ? e.message : 'Unable to submit resolution.') }
    finally { setBusy(false) }
  }

  if (loading) return <div className="dashboard-content"><p>Loading complaint details…</p></div>
  if (error && !complaint) return <div className="dashboard-content"><Link to={backPath} className="button button-secondary button-small" style={{ marginBottom: 24 }}><ArrowLeft size={14} /> Back</Link><div className="form-message form-error" role="alert">{error}</div></div>
  if (!complaint) return null
  const currentStepIndex = STATUS_ORDER.indexOf(complaint.status as typeof STATUS_ORDER[number])
  return <MotionPage><div className="dashboard-content">
    <PageHeader title={complaint.title} description={`Reference: ${complaint.referenceId}`} actions={<Link to={backPath} className="button button-secondary button-small"><ArrowLeft size={14} /> Back</Link>} />
    {error && <div className="form-message form-error" role="alert">{error}</div>}{notice && <div className="toast-inline toast-info" role="status" style={{ marginBottom: 12 }}><CheckCircle2 size={14} />{notice}</div>}
    <div className="detail-grid"><div className="detail-main">
      <SectionCard title="Details"><div className="detail-meta-row"><CategoryBadge category={complaint.category} />{complaint.priority && <PriorityBadge priority={complaint.priority} />}<StatusBadge status={complaint.status} /></div><p className="detail-description">{complaint.description}</p><div className="detail-info-list"><div className="detail-info-item"><MapPin size={14} /><span><strong>Location:</strong> {complaint.address}</span></div><div className="detail-info-item"><Calendar size={14} /><span><strong>Submitted:</strong> {formatDateTime(complaint.submittedAt)}</span></div><div className="detail-info-item"><RefreshCw size={14} /><span><strong>Last updated:</strong> {formatDateTime(complaint.updatedAt)}</span></div></div>
      </SectionCard>
      <SectionCard title="Reported location" className="mt-4">
        {isValidCoordinate(complaint.latitude, complaint.longitude) ? (
          <>
            <ComplaintMap
              complaints={[complaint]}
              height={330}
              showDirections
              emptyMessage="This complaint has no stored coordinates."
            />
            <div className="map-link-row" style={{ marginTop: 10 }}>
              <span className="map-link-coords">
                <MapPin size={12} /> {formatCoordinatePair(complaint.latitude!, complaint.longitude!)}
              </span>
              <a className="map-link" href={osmViewUrl(complaint.latitude!, complaint.longitude!)} target="_blank" rel="noopener noreferrer">
                <ExternalLink size={14} /> Open in OpenStreetMap
              </a>
              <a className="map-link" href={osmDirectionsUrl(complaint.latitude!, complaint.longitude!)} target="_blank" rel="noopener noreferrer">
                <ExternalLink size={14} /> Directions
              </a>
            </div>
          </>
        ) : (
          <div className="map-empty-state">
            <MapPin size={20} />
            <div>
              <strong>No coordinates recorded</strong>
              <p>The reported address is shown above, but this complaint has no map coordinates stored.</p>
            </div>
          </div>
        )}
      </SectionCard>
      {complaint.departmentName && <SectionCard title="Department" className="mt-4"><div className="detail-info-list"><div className="detail-info-item"><UserIcon size={14} /><span><strong>Department:</strong> {complaint.departmentName}</span></div></div></SectionCard>}
      {(user?.role === 'DEPARTMENT_ADMIN' || user?.role === 'SYSTEM_ADMIN') && complaint.status === 'SUBMITTED' && <SectionCard title="Assign a field worker" className="mt-4">
        {workers.length === 0 ? <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>No active workers are available in your department.</p> : <><label htmlFor="assign-worker">Choose worker</label><select id="assign-worker" className="field-input" value={selectedWorkerId} onChange={e => setSelectedWorkerId(e.target.value)}><option value="">Select a worker</option>{workers.filter(w => w.active).map(w => <option key={w.id} value={w.id}>{w.firstName} {w.lastName} — {w.email}</option>)}</select><button className="button button-primary button-small" type="button" disabled={busy || !selectedWorkerId} onClick={() => void assignSelectedWorker()} style={{ marginTop: 12 }}>{busy ? 'Assigning…' : 'Assign worker'}</button></>}
      </SectionCard>}
      {user?.role === 'WORKER' && assignment && <SectionCard title="Worker task actions" className="mt-4">
        {resolution?.reviewStatus === 'REJECTED' && (
          <div className="form-message form-error" role="alert" style={{ marginBottom: 14 }}>
            <AlertCircle size={15} />
            <span><strong>Rework requested:</strong> {resolution.rejectionReason || 'The reviewer requested changes.'}{resolution.adminNotes ? ` — ${resolution.adminNotes}` : ''} Upload new evidence and resubmit below.</span>
          </div>
        )}
        {resolution?.reviewStatus === 'PENDING' && (
          <div className="form-message form-notice" role="status" style={{ marginBottom: 14 }}>
            <CheckCircle2 size={15} /> Submitted for review{resolution.completionNotes ? `: ${resolution.completionNotes}` : '.'}
          </div>
        )}
        {resolution?.reviewStatus === 'APPROVED' && (
          <div className="form-message form-notice" role="status" style={{ marginBottom: 14 }}>
            <CheckCircle2 size={15} /> Your resolution was approved.
          </div>
        )}
        {assignment.status === 'ASSIGNED' ? <button className="button button-primary button-small" disabled={busy} onClick={() => void beginWork()} type="button">{busy ? 'Starting…' : 'Start work'}</button> : assignment.status === 'IN_PROGRESS' ? <form onSubmit={completeWork}>
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Upload at least one piece of evidence and provide completion notes before submitting this task for review.</p>
          <label htmlFor="completion-notes">Completion notes</label><textarea id="completion-notes" className="field-input field-textarea" rows={4} minLength={10} maxLength={2000} value={completionNotes} onChange={e => setCompletionNotes(e.target.value)} placeholder="Describe the work completed…" required />
          <label htmlFor="completion-evidence" style={{ display: 'block', marginTop: 14 }}>Before/after evidence files (at least one required)</label><input id="completion-evidence" type="file" accept=".jpg,.jpeg,.png,.pdf,.doc,.docx" multiple onChange={e => setEvidenceFiles(Array.from(e.target.files ?? []))} required />
          {evidenceFiles.length > 0 && <p style={{ fontSize: 11, color: 'var(--text-secondary)', marginTop: 6 }}>{evidenceFiles.length} file{evidenceFiles.length === 1 ? '' : 's'} selected: {evidenceFiles.map(file => file.name).join(', ')}</p>}
          <button className="button button-primary button-small" type="submit" disabled={busy || evidenceFiles.length === 0 || completionNotes.trim().length < 10} style={{ marginTop: 14 }}><Upload size={14} /> {busy ? 'Submitting…' : 'Submit resolution'}</button>
        </form> : <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>This assignment is currently {assignment.status.toLowerCase().replaceAll('_', ' ')}.</p>}
      </SectionCard>}
      <SectionCard title="Activity history" className="mt-4">
        {history.length === 0 ? (
          <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>No status history has been recorded for this complaint yet.</p>
        ) : (
          <div className="timeline">
            {history.map(h => (
              <div key={h.id} className="timeline-item">
                <div className="timeline-dot" />
                <div className="timeline-body">
                  <div className="timeline-header"><strong>{h.actorName}</strong><span>{formatDateTime(h.createdAt)}</span></div>
                  {h.notes && <p className="timeline-notes">{h.notes}</p>}
                  <StatusBadge status={h.status} />
                </div>
              </div>
            ))}
          </div>
        )}
      </SectionCard>
    </div><div className="detail-sidebar"><SectionCard title="Status Timeline"><div className="status-timeline">{STATUS_ORDER.map((status, index) => <div key={status} className={`status-timeline-step ${currentStepIndex >= 0 && index < currentStepIndex ? 'step-done' : index === currentStepIndex ? 'step-active' : 'step-future'}`}><div className="step-indicator" /><span>{statusLabel(status as never)}</span></div>)}</div></SectionCard></div></div>
  </div></MotionPage>
}
