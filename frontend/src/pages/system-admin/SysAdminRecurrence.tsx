import { AlertTriangle, RefreshCw } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { CategoryBadge, PageHeader, SectionCard, StatCard } from '../../components/ui'
import { fetchSystemRecurrenceReport } from '../../services/analyticsService'
import type { RecurrenceReport } from '../../types'
import { formatDate } from '../../utils/labels'
const severityClass: Record<string, string> = { HIGH: 'badge-red', MEDIUM: 'badge-orange', LOW: 'badge-gray' }
export function SysAdminRecurrence() {
  const [report, setReport] = useState<RecurrenceReport | null>(null)
  const [catFilter, setCatFilter] = useState('')
  const [severityFilter, setSeverityFilter] = useState('')
  const [error, setError] = useState('')
  useEffect(() => { fetchSystemRecurrenceReport().then(setReport).catch(e => setError(e instanceof Error ? e.message : 'Unable to load recurrence reports.')) }, [])
  const filtered = useMemo(() => (report?.cases ?? []).filter(c => (!catFilter || c.category === catFilter) && (!severityFilter || c.severity === severityFilter)), [report, catFilter, severityFilter])
  return <MotionPage><div className="dashboard-content"><PageHeader title="Recurrence Analysis" description="System-wide recurring complaint patterns." />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {!report && !error ? <p>Loading recurrence reports…</p> : report && <>
      <div className="stats-grid" style={{ marginBottom: 24 }}><StatCard label="Recurring Reports" value={report.cases.length} icon={<RefreshCw size={19} />} color="orange" /><StatCard label="High Severity" value={report.cases.filter(c => c.severity === 'HIGH').length} icon={<AlertTriangle size={19} />} color="red" /><StatCard label="Historical Occurrences" value={report.cases.reduce((sum, c) => sum + c.occurrenceCount, 0)} icon={<RefreshCw size={19} />} color="teal" /><StatCard label="Categories Affected" value={new Set(report.cases.map(c => c.category)).size} icon={<RefreshCw size={19} />} color="blue" /></div>
      <SectionCard title="Filter recurrence cases" className="mb-4"><div className="filters-row"><select className="filter-select" value={catFilter} onChange={e => setCatFilter(e.target.value)} aria-label="Filter by category"><option value="">All categories</option><option value="ROADS_AND_POTHOLES">Roads & Potholes</option><option value="WATER_SUPPLY">Water Supply</option><option value="ELECTRICITY_AND_STREETLIGHTS">Electricity & Streetlights</option><option value="DRAINAGE_AND_SEWAGE">Drainage & Sewage</option><option value="WASTE_MANAGEMENT">Waste Management</option></select><select className="filter-select" value={severityFilter} onChange={e => setSeverityFilter(e.target.value)} aria-label="Filter by severity"><option value="">All severities</option><option value="HIGH">High</option><option value="MEDIUM">Medium</option><option value="LOW">Low</option></select></div></SectionCard>
      {filtered.length === 0 ? <SectionCard><p style={{ color: 'var(--text-muted)', padding: 24, textAlign: 'center' }}>No recurrence reports match these filters.</p></SectionCard> : <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>{filtered.map(rc => <div key={rc.id} className="recurrence-card"><div className="recurrence-card-header"><div><CategoryBadge category={rc.category} /><h3 className="recurrence-location">{rc.location}</h3></div><span className={`badge ${severityClass[rc.severity]}`}>{rc.severity} severity</span></div><div className="recurrence-meta"><span><strong>{rc.occurrenceCount}</strong> historical occurrences</span><span>First: {formatDate(rc.firstReportedAt)}</span><span>Latest: {formatDate(rc.lastReportedAt)}</span><span>{rc.relatedComplaintIds.length} related complaints</span></div><p style={{ fontSize: 11, color: 'var(--text-muted)', margin: '8px 0 0' }}>Related complaint IDs: {rc.relatedComplaintIds.join(', ')}</p></div>)}</div>}
    </>}
  </div></MotionPage>
}
