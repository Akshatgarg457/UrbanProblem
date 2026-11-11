import { CheckCircle2, ClipboardList, Eye, MapPin, RefreshCw } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { Loader, PageHeader, SectionCard, StatCard, StatusBadge } from '../../components/ui'
import { fetchAssignedComplaints } from '../../services/complaintService'
import type { Complaint } from '../../types'
import { formatDate } from '../../utils/labels'

export function WorkerDashboard() {
  const [tasks, setTasks] = useState<Complaint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)
  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    fetchAssignedComplaints({ size: 100 })
      .then(p => { if (!cancelled) setTasks(p.content) })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : 'Unable to load assigned tasks.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [reloadKey])
  const assigned = tasks.filter(t => t.status === 'ASSIGNED').length
  const inProgress = tasks.filter(t => t.status === 'IN_PROGRESS').length
  const awaiting = tasks.filter(t => t.status === 'SUBMITTED_FOR_REVIEW').length
  const completed = tasks.filter(t => t.status === 'RESOLVED').length
  return <MotionPage><div className="dashboard-content">
    <PageHeader title="My Tasks" description="Overview of your assigned complaints and progress." />
    {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={() => setReloadKey(k => k + 1)} type="button"><RefreshCw size={14} /> Retry</button></div>}
    {loading ? <Loader /> : <>
      <div className="stats-grid" style={{ marginTop: 24 }}><StatCard label="Assigned" value={assigned} icon={<ClipboardList size={19} />} color="teal" /><StatCard label="In Progress" value={inProgress} icon={<RefreshCw size={19} />} color="blue" /><StatCard label="Awaiting Review" value={awaiting} icon={<Eye size={19} />} color="purple" /><StatCard label="Completed" value={completed} icon={<CheckCircle2 size={19} />} color="green" /></div>
      {tasks.length > 0 && <SectionCard title="Task Locations" className="mt-4" action={<Link to="/dashboard/worker/tasks" className="text-link" style={{ fontSize: 11 }}>All tasks</Link>}>
        <p className="map-section-hint" style={{ marginBottom: 12 }}><MapPin size={13} /> Select a marker for task details and directions.</p>
        <ComplaintMap complaints={tasks} basePath="/dashboard/worker/tasks" showDirections height={330} emptyMessage="None of your tasks have stored coordinates yet." />
      </SectionCard>}
      <SectionCard title="Active Tasks" className="mt-4" action={<Link to="/dashboard/worker/tasks" className="text-link" style={{ fontSize: 11 }}>All tasks</Link>}>{tasks.length === 0 ? <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: 12 }}>No tasks assigned yet — new assignments from your department will appear here.</p> : <ul className="activity-list">{tasks.slice(0, 6).map(c => <li key={c.id} className="activity-item"><div className="activity-item-body"><Link to={`/dashboard/worker/tasks/${encodeURIComponent(c.referenceId)}`} className="activity-title">{c.title}</Link><span className="activity-meta">{c.referenceId} · {formatDate(c.submittedAt)}</span></div><StatusBadge status={c.status} /></li>)}</ul>}</SectionCard>
    </>}
  </div></MotionPage>
}
