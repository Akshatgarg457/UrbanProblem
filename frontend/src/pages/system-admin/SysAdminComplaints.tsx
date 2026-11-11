import { ClipboardList } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { ComplaintFilters, applyFilters, type FilterState } from '../../components/shared/ComplaintFilters'
import { ComplaintTable } from '../../components/shared/ComplaintTable'
import { EmptyState, PageHeader } from '../../components/ui'
import { fetchAllComplaints } from '../../services/complaintService'
import type { Complaint } from '../../types'
export function SysAdminComplaints() {
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [filters, setFilters] = useState<FilterState>({ search: '', status: '', category: '', priority: '' })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => { fetchAllComplaints({ size: 100 }).then(p => setComplaints(p.content)).catch(e => setError(e instanceof Error ? e.message : 'Unable to load complaints.')).finally(() => setLoading(false)) }, [])
  const filtered = useMemo(() => applyFilters(complaints, filters), [complaints, filters])
  return <MotionPage><div className="dashboard-content"><PageHeader title="All Complaints" description={`${complaints.length} complaints system-wide.`} />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {loading ? <p>Loading complaints…</p> : <div className="section-card" style={{ marginTop: 20 }}><ComplaintFilters filters={filters} onChange={setFilters} showPriority={false} />{filtered.length === 0 ? <EmptyState icon={<ClipboardList size={24} />} title="No complaints match" description="Try adjusting the filters." /> : <ComplaintTable complaints={filtered} basePath="/dashboard/system-admin/complaints" />}<p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 12 }}>Showing {filtered.length} of {complaints.length}</p></div>}
  </div></MotionPage>
}
