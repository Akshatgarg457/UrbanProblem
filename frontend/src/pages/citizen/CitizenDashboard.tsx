import { ArrowRight, CheckCircle2, ChevronRight, ClipboardList, Clock, PlusCircle, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { EmptyState, PageHeader, SectionCard, StatCard, StatusBadge, Loader } from '../../components/ui'
import { fetchMyComplaints } from '../../services/complaintService'
import type { Complaint } from '../../types'
import { formatDate } from '../../utils/labels'

export function CitizenDashboard() {
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { fetchMyComplaints({ size: 100 }).then(p => setComplaints(p.content)).catch(e => setError(e instanceof Error ? e.message : 'Unable to load dashboard.')).finally(() => setLoading(false)) }, [])
  const resolved = complaints.filter(c => c.status === 'RESOLVED').length
  const inProgress = complaints.filter(c => c.status === 'IN_PROGRESS' || c.status === 'ASSIGNED').length
  const open = complaints.length - resolved - inProgress
  return <MotionPage><div className="dashboard-content">
    <PageHeader
      actions={<Link className="button button-primary" to="/dashboard/citizen/report"><PlusCircle size={16} /> Report an issue</Link>}
      description="Track the issues you have reported and follow each one through to resolution."
      eyebrow="Citizen workspace"
      title="My dashboard"
    />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {loading ? <Loader /> : <>
      <div className="stats-grid">
        <StatCard color="teal" icon={<ClipboardList size={18} />} label="Total reported" note="All issues you have filed" value={complaints.length} />
        <StatCard color="orange" icon={<Clock size={18} />} label="Open / Pending" note="Awaiting department review" value={open} />
        <StatCard color="blue" icon={<RefreshCw size={18} />} label="In progress" note="Assigned or being fixed" value={inProgress} />
        <StatCard color="green" icon={<CheckCircle2 size={18} />} label="Resolved" note="Closed by your department" value={resolved} />
      </div>
      <div className="dashboard-grid-main">
        <SectionCard
          action={<Link className="section-card-link" to="/dashboard/citizen/complaints">View all <ArrowRight size={14} /></Link>}
          title="Recent complaints"
        >
          {complaints.length === 0
            ? <EmptyState
                action={<Link className="button button-secondary button-small" to="/dashboard/citizen/report"><PlusCircle size={15} /> Report your first issue</Link>}
                description="When you report a local issue, it appears here with its status and every update from the department."
                icon={<ClipboardList size={22} />}
                title="No complaints yet"
              />
            : <ul className="activity-list">{complaints.slice(0, 5).map(c => <li className="activity-item" key={c.id}><div className="activity-item-body"><Link className="activity-title" to={`/dashboard/citizen/complaints/${encodeURIComponent(c.referenceId)}`}>{c.title}</Link><span className="activity-meta">{c.referenceId} · {formatDate(c.submittedAt)}</span></div><StatusBadge status={c.status} /></li>)}</ul>}
        </SectionCard>
        <SectionCard title="Quick actions">
          <div className="quick-actions">
            <Link className="quick-action-card" to="/dashboard/citizen/report">
              <span className="quick-action-icon"><PlusCircle size={19} /></span>
              <div><div className="quick-action-title">Report a new issue</div><div className="quick-action-desc">Describe and submit a local problem</div></div>
              <ChevronRight aria-hidden="true" className="quick-action-chevron" size={16} />
            </Link>
            <Link className="quick-action-card" to="/dashboard/citizen/complaints">
              <span className="quick-action-icon"><ClipboardList size={19} /></span>
              <div><div className="quick-action-title">View my complaints</div><div className="quick-action-desc">Check status and updates</div></div>
              <ChevronRight aria-hidden="true" className="quick-action-chevron" size={16} />
            </Link>
            <Link className="quick-action-card" to="/dashboard/citizen/notifications">
              <span className="quick-action-icon"><Clock size={19} /></span>
              <div><div className="quick-action-title">Review notifications</div><div className="quick-action-desc">See what changed on your reports</div></div>
              <ChevronRight aria-hidden="true" className="quick-action-chevron" size={16} />
            </Link>
          </div>
        </SectionCard>
      </div>
    </>}
  </div></MotionPage>
}
