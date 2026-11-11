import { ClipboardList, PlusCircle } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintFilters, applyFilters, type FilterState } from '../../components/shared/ComplaintFilters'
import { ComplaintTable } from '../../components/shared/ComplaintTable'
import { EmptyState, PageHeader } from '../../components/ui'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { fetchMyComplaints } from '../../services/complaintService'
import type { Complaint } from '../../types'

export function MyComplaints() {
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [filters, setFilters] = useState<FilterState>({ search: '', status: '', category: '', priority: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { fetchMyComplaints({ size: 100 }).then(page => setComplaints(page.content)).catch(e => setError(e instanceof Error ? e.message : 'Unable to load complaints.')).finally(() => setLoading(false)) }, [])
  const filtered = useMemo(() => applyFilters(complaints, filters), [complaints, filters])

  return <MotionPage><div className="dashboard-content">
    <PageHeader title="My Complaints" description={`${complaints.length} complaints submitted`} actions={<Link to="/dashboard/citizen/report" className="button button-primary button-small"><PlusCircle size={15} /> Report New</Link>} />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {loading ? <p>Loading your complaints…</p> : <div className="section-card" style={{ marginTop: 20 }}>
      <ComplaintFilters filters={filters} onChange={setFilters} showPriority={false} />
      {complaints.length > 0 && (
        <div className="map-card-block">
          <div className="map-section-head">
            <h2 className="section-card-title">Your complaint locations</h2>
            <span className="map-section-hint">Only complaints with stored coordinates are shown.</span>
          </div>
          <ComplaintMap
            complaints={filtered}
            basePath="/dashboard/citizen/complaints"
            height={320}
            emptyMessage="None of your filtered complaints have stored coordinates yet."
          />
        </div>
      )}
      {filtered.length === 0 ? <EmptyState icon={<ClipboardList size={24} />} title="No complaints found" description="Submit a complaint or adjust your filters." /> : <ComplaintTable complaints={filtered} basePath="/dashboard/citizen/complaints" />}
      <p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 12 }}>Showing {filtered.length} of {complaints.length} complaints</p>
    </div>}
  </div></MotionPage>
}
