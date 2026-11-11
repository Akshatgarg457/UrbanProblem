import { ClipboardList, RefreshCw } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintFilters, applyFilters, type FilterState } from '../../components/shared/ComplaintFilters'
import { ComplaintTable } from '../../components/shared/ComplaintTable'
import { EmptyState, PageHeader } from '../../components/ui'
import { fetchDepartmentComplaints } from '../../services/complaintService'
import type { Complaint, ComplaintStatus } from '../../types'

const QUEUE_STATUSES: ComplaintStatus[] = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED_FOR_REVIEW', 'RESOLVED', 'REJECTED']

const VIEWS: Partial<Record<ComplaintStatus, { title: string; description: string; emptyTitle: string; emptyDescription: string }>> = {
  SUBMITTED: {
    title: 'Unassigned Complaints',
    description: 'New complaints for your department that still need a field worker.',
    emptyTitle: 'No unassigned complaints',
    emptyDescription: 'Every complaint in your department has a worker assigned.',
  },
  ASSIGNED: {
    title: 'Manage Assignments',
    description: 'Complaints already assigned to workers in your department.',
    emptyTitle: 'No assigned complaints',
    emptyDescription: 'Assign a worker from the unassigned queue to see it here.',
  },
}

const DEFAULT_VIEW = {
  title: 'Complaint Queue',
  description: 'Complaints assigned to your department.',
  emptyTitle: 'No complaints match your filters',
  emptyDescription: 'There are currently no matching department complaints.',
}

function toStatus(value: string | null): ComplaintStatus | '' {
  return value && QUEUE_STATUSES.includes(value as ComplaintStatus) ? (value as ComplaintStatus) : ''
}

export function DeptComplaints() {
  const [searchParams, setSearchParams] = useSearchParams()
  const statusParam = toStatus(searchParams.get('status'))
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [filters, setFilters] = useState<FilterState>({ search: '', status: statusParam, category: '', priority: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    setLoading(true)
    setError('')
    fetchDepartmentComplaints({ size: 100 })
      .then(page => setComplaints(page.content))
      .catch(e => setError(e instanceof Error ? e.message : 'Unable to load department complaints.'))
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
  const filtered = useMemo(() => applyFilters(complaints, filters), [complaints, filters])

  return <MotionPage><div className="dashboard-content">
    <PageHeader title={view.title} description={`${view.description} ${complaints.length} complaint${complaints.length === 1 ? '' : 's'} in your department.`} />
    {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={load} type="button"><RefreshCw size={14} /> Retry</button></div>}
    {loading ? <p className="loading-inline"><RefreshCw size={16} className="spin" /> Loading department complaints…</p> : <div className="section-card" style={{ marginTop: 20 }}>
      <ComplaintFilters filters={filters} onChange={handleFilterChange} showPriority={false} />
      {filtered.length === 0 ? <EmptyState icon={<ClipboardList size={24} />} title={view.emptyTitle} description={view.emptyDescription} /> : <ComplaintTable complaints={filtered} basePath="/dashboard/department-admin/complaints" />}
      <p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 12 }}>Showing {filtered.length} of {complaints.length}</p>
    </div>}
  </div></MotionPage>
}
