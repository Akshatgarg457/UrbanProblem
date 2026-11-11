import { ClipboardList, RefreshCw } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintFilters, applyFilters, type FilterState } from '../../components/shared/ComplaintFilters'
import { ComplaintTable } from '../../components/shared/ComplaintTable'
import { EmptyState, PageHeader } from '../../components/ui'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { fetchAssignedComplaints } from '../../services/complaintService'
import type { Complaint, ComplaintStatus } from '../../types'

const WORKER_STATUSES: ComplaintStatus[] = ['ASSIGNED', 'IN_PROGRESS', 'SUBMITTED_FOR_REVIEW', 'RESOLVED', 'REJECTED']

const VIEWS: Partial<Record<ComplaintStatus, { title: string; description: string; emptyTitle: string; emptyDescription: string }>> = {
  ASSIGNED: {
    title: 'Assigned Tasks',
    description: 'Assignments from your department that you have not started yet.',
    emptyTitle: 'No newly assigned tasks',
    emptyDescription: 'New assignments from your department will appear here.',
  },
  IN_PROGRESS: {
    title: 'In Progress',
    description: 'Tasks you have started and are currently working on.',
    emptyTitle: 'No tasks in progress',
    emptyDescription: 'Start an assigned task to move it into this list.',
  },
  SUBMITTED_FOR_REVIEW: {
    title: 'Submitted for Review',
    description: 'Work waiting for your department admin to approve or send back for rework.',
    emptyTitle: 'Nothing awaiting review',
    emptyDescription: 'Submit a completed task with evidence to send it for review.',
  },
  RESOLVED: {
    title: 'Completed Tasks',
    description: 'Tasks your department admin has approved as resolved.',
    emptyTitle: 'No completed tasks yet',
    emptyDescription: 'Approved resolutions will appear here once your admin signs off.',
  },
}

const DEFAULT_VIEW = {
  title: 'All Assigned Tasks',
  description: 'Every assignment linked to your account, across all statuses.',
  emptyTitle: 'No assigned tasks',
  emptyDescription: 'New assignments from your department will appear here.',
}

function toStatus(value: string | null): ComplaintStatus | '' {
  return value && WORKER_STATUSES.includes(value as ComplaintStatus) ? (value as ComplaintStatus) : ''
}

export function WorkerTasks() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusParam = toStatus(searchParams.get('status'))
  const [tasks, setTasks] = useState<Complaint[]>([])
  const [filters, setFilters] = useState<FilterState>({ search: '', status: statusParam, category: '', priority: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    fetchAssignedComplaints({ size: 100 })
      .then(page => setTasks(page.content))
      .catch(e => setError(e instanceof Error ? e.message : 'Unable to load assigned tasks.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])
  useEffect(() => { setFilters(current => (current.status === statusParam ? current : { ...current, status: statusParam })) }, [statusParam])

  function handleFilterChange(next: FilterState) {
    setFilters(next)
    const params = new URLSearchParams(searchParams)
    if (next.status) params.set('status', next.status)
    else params.delete('status')
    setSearchParams(params, { replace: true })
  }

  const view = (statusParam && VIEWS[statusParam]) || DEFAULT_VIEW
  const filtered = useMemo(() => applyFilters(tasks, filters), [tasks, filters])

  return <MotionPage><div className="dashboard-content">
    <PageHeader title={view.title} description={`${view.description} ${tasks.length} assignment${tasks.length === 1 ? '' : 's'} linked to your account.`} />
    {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={load} type="button"><RefreshCw size={14} /> Retry</button></div>}
    {loading ? <p className="loading-inline"><RefreshCw size={16} className="spin" /> Loading your tasks…</p> : <div className="section-card" style={{ marginTop: 20 }}>
      <ComplaintFilters filters={filters} onChange={handleFilterChange} showPriority={false} />
      {tasks.length > 0 && (
        <div className="map-card-block">
          <div className="map-section-head">
            <h2 className="section-card-title">Assigned task locations</h2>
            <span className="map-section-hint">Open a marker for task details and directions.</span>
          </div>
          <ComplaintMap
            complaints={filtered}
            basePath="/dashboard/worker/tasks"
            showDirections
            height={330}
            emptyMessage="None of your filtered tasks have stored coordinates."
          />
        </div>
      )}
      {filtered.length === 0 ? <EmptyState icon={<ClipboardList size={24} />} title={view.emptyTitle} description={view.emptyDescription} /> : <ComplaintTable complaints={filtered} basePath="/dashboard/worker/tasks" />}
      <p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 12 }}>Showing {filtered.length} of {tasks.length}</p>
    </div>}
  </div></MotionPage>
}
