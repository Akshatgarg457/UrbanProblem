import { MapPin, RefreshCw } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintFilters, applyFilters, type FilterState } from '../../components/shared/ComplaintFilters'
import { ComplaintMap } from '../../components/map/ComplaintMap'
import { PageHeader } from '../../components/ui'
import { fetchDepartmentComplaints } from '../../services/complaintService'
import type { Complaint } from '../../types'

export function DeptComplaintMap() {
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [filters, setFilters] = useState<FilterState>({ search: '', status: '', category: '', priority: '' })
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

  const filtered = useMemo(() => applyFilters(complaints, filters), [complaints, filters])
  const withCoordinates = useMemo(
    () => filtered.filter(c => Number.isFinite(c.latitude) && Number.isFinite(c.longitude)).length,
    [filtered],
  )

  return <MotionPage><div className="dashboard-content">
    <PageHeader
      title="Complaint Map"
      description="Locations of complaints assigned to your department. Marker colours reflect complaint status."
    />
    {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={load} type="button"><RefreshCw size={14} /> Retry</button></div>}
    {loading ? <p className="loading-inline"><RefreshCw size={16} className="spin" /> Loading complaint locations…</p> : (
      <div className="section-card map-page-card" style={{ marginTop: 20 }}>
        <div className="map-section-head">
          <h2 className="section-card-title"><MapPin size={16} /> Department complaint locations</h2>
          <span className="map-section-hint">
            {withCoordinates} of {filtered.length} filtered complaint{filtered.length === 1 ? '' : 's'} have saved coordinates.
          </span>
        </div>
        <ComplaintFilters filters={filters} onChange={setFilters} showPriority={false} />
        <ComplaintMap
          complaints={filtered}
          basePath="/dashboard/department-admin/complaints"
          showDepartment
          height={520}
          emptyMessage="None of the filtered complaints have stored coordinates yet. Complaints reported through the map or with manual coordinates will appear here."
        />
      </div>
    )}
  </div></MotionPage>
}
