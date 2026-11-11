import { CheckCircle2, ClipboardList, Clock, Eye, MapPin, RefreshCw, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { Loader, PageHeader, SectionCard, StatCard, StatusBadge, Badge } from '../../components/ui'
import { fetchDepartmentComplaints } from '../../services/complaintService'
import { fetchDepartmentWorkers } from '../../services/userService'
import { useAuth } from '../../context/AuthContext'
import type { Complaint, User } from '../../types'
import { formatDate } from '../../utils/labels'

export function DeptDashboard() {
  const { user } = useAuth()
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [workers, setWorkers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    const requests: Promise<void>[] = [
      fetchDepartmentComplaints({ size: 100 })
        .then(page => { if (!cancelled) setComplaints(page.content) }),
    ]
    if (user?.departmentId) {
      requests.push(fetchDepartmentWorkers(user.departmentId).then(list => { if (!cancelled) setWorkers(list) }))
    }
    Promise.all(requests)
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : 'Unable to load dashboard.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [user?.departmentId, reloadKey])

  const open = complaints.filter(c => c.status === 'SUBMITTED').length
  const inProgress = complaints.filter(c => c.status === 'IN_PROGRESS' || c.status === 'ASSIGNED').length
  const pendingReview = complaints.filter(c => c.status === 'SUBMITTED_FOR_REVIEW').length
  const resolved = complaints.filter(c => c.status === 'RESOLVED').length
  const recent = complaints.slice(0, 5)

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="Department Dashboard"
          description={user?.department ? `${user.department} — overview of complaints, workload, and queue.` : 'Overview of complaints, workload, and queue.'}
        />
        {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={() => setReloadKey(k => k + 1)} type="button"><RefreshCw size={14} /> Retry</button></div>}
        {loading ? <Loader /> : <>
          <div className="stats-grid" style={{ marginTop: 24 }}>
            <StatCard label="Total Complaints" value={complaints.length} icon={<ClipboardList size={19} />} color="teal" />
            <StatCard label="Open Queue" value={open} icon={<Clock size={19} />} color="orange" />
            <StatCard label="In Progress" value={inProgress} icon={<RefreshCw size={19} />} color="blue" />
            <StatCard label="Pending Review" value={pendingReview} icon={<Eye size={19} />} color="purple" />
            <StatCard label="Resolved" value={resolved} icon={<CheckCircle2 size={19} />} color="green" />
          </div>

          <div className="dashboard-grid-2col" style={{ marginTop: 24 }}>
            <SectionCard
              title="Recent Activity"
              action={<Link to="/dashboard/department-admin/complaints" className="text-link" style={{ fontSize: 11 }}>View all</Link>}
            >
              {recent.length === 0 ? (
                <div className="empty-state-inline"><Clock size={18} /><span>No complaints yet — new reports for your department will appear here.</span></div>
              ) : (
                <ul className="activity-list">
                  {recent.map(c => (
                    <li key={c.id} className="activity-item">
                      <div className="activity-item-body">
                        <Link to={`/dashboard/department-admin/complaints/${encodeURIComponent(c.referenceId)}`} className="activity-title">{c.title}</Link>
                        <span className="activity-meta">{c.referenceId} · {formatDate(c.submittedAt)}</span>
                      </div>
                      <StatusBadge status={c.status} />
                    </li>
                  ))}
                </ul>
              )}
            </SectionCard>

            <SectionCard title="Quick Actions">
              <div className="quick-actions">
                <Link to="/dashboard/department-admin/complaints" className="quick-action-card">
                  <span className="quick-action-icon"><ClipboardList size={20} /></span>
                  <div>
                    <div className="quick-action-title">Manage Complaints</div>
                    <div className="quick-action-desc">View and assign incoming reports</div>
                  </div>
                </Link>
                <Link to="/dashboard/department-admin/review" className="quick-action-card">
                  <span className="quick-action-icon"><Eye size={20} /></span>
                  <div>
                    <div className="quick-action-title">Review Submissions</div>
                    <div className="quick-action-desc">{pendingReview} resolution{pendingReview === 1 ? '' : 's'} pending</div>
                  </div>
                </Link>
                <Link to="/dashboard/department-admin/workers" className="quick-action-card">
                  <span className="quick-action-icon"><Users size={20} /></span>
                  <div>
                    <div className="quick-action-title">Worker Overview</div>
                    <div className="quick-action-desc">{workers.length} worker{workers.length === 1 ? '' : 's'} in your department</div>
                  </div>
                </Link>
                <Link to="/dashboard/department-admin/recurrence" className="quick-action-card">
                  <span className="quick-action-icon"><RefreshCw size={20} /></span>
                  <div>
                    <div className="quick-action-title">Recurrence Analysis</div>
                    <div className="quick-action-desc">Recurring patterns in your area</div>
                  </div>
                </Link>
              </div>
            </SectionCard>
          </div>

          <SectionCard
            title="Complaint Locations"
            className="mt-4"
            action={<Link to="/dashboard/department-admin/map" className="text-link" style={{ fontSize: 11 }}>Open map</Link>}
          >
            {complaints.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No department complaints to plot yet.</p>
            ) : (
              <>
                <p className="map-section-hint" style={{ marginBottom: 12 }}>
                  <MapPin size={13} /> Marker colours reflect complaint status. Select a marker for details.
                </p>
                <ComplaintMap
                  complaints={complaints}
                  basePath="/dashboard/department-admin/complaints"
                  height={340}
                  emptyMessage="None of your department's complaints have stored coordinates yet."
                />
              </>
            )}
          </SectionCard>

          <SectionCard title="Workers" className="mt-4" action={<Link to="/dashboard/department-admin/workers" className="text-link" style={{ fontSize: 11 }}>All workers</Link>}>
            {workers.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No workers are registered in your department yet.</p>
            ) : (
              <ul className="activity-list">
                {workers.map(w => (
                  <li key={w.id} className="activity-item">
                    <div className="activity-item-body">
                      <span className="activity-title">{w.firstName} {w.lastName}</span>
                      <span className="activity-meta">{w.email}</span>
                    </div>
                    <Badge label={w.active ? 'Active' : 'Inactive'} className={w.active ? 'badge-green' : 'badge-gray'} />
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>
        </>}
      </div>
    </MotionPage>
  )
}
