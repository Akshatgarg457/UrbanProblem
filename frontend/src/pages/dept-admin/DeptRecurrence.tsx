import { AlertTriangle, RefreshCw } from 'lucide-react'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { CategoryBadge, PageHeader, SectionCard, StatCard } from '../../components/ui'
import { fetchDepartmentRecurrenceReport } from '../../services/analyticsService'
import type { RecurrenceReport } from '../../types'
import { formatDate } from '../../utils/labels'
const severityClass: Record<string, string> = { HIGH: 'badge-red', MEDIUM: 'badge-orange', LOW: 'badge-gray' }
export function DeptRecurrence() {
  const [report, setReport] = useState<RecurrenceReport | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { fetchDepartmentRecurrenceReport().then(setReport).catch(e => setError(e instanceof Error ? e.message : 'Unable to load recurrence reports.')) }, [])
  return <MotionPage><div className="dashboard-content"><PageHeader title="Recurrence Analysis" description="Recurring issue reports available to your department." />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {!report && !error ? <p>Loading recurrence reports…</p> : report && <>
      <div className="stats-grid" style={{ marginBottom: 24 }}><StatCard label="Recurring Reports" value={report.cases.length} icon={<RefreshCw size={19} />} color="orange" /><StatCard label="High Severity" value={report.cases.filter(c => c.severity === 'HIGH').length} icon={<AlertTriangle size={19} />} color="red" /><StatCard label="Historical Occurrences" value={report.cases.reduce((sum, c) => sum + c.occurrenceCount, 0)} icon={<RefreshCw size={19} />} color="teal" /></div>
      <SectionCard title="Recurring issue cases">{report.cases.length === 0 ? <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No recurrence reports have been generated for your department.</p> : <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>{report.cases.map(rc => <div key={rc.id} className="recurrence-card"><div className="recurrence-card-header"><div><CategoryBadge category={rc.category} /><h3 className="recurrence-location">{rc.location}</h3></div><span className={`badge ${severityClass[rc.severity]}`}>{rc.severity} severity</span></div><div className="recurrence-meta"><span><strong>{rc.occurrenceCount}</strong> historical occurrences</span><span>First: {formatDate(rc.firstReportedAt)}</span><span>Latest: {formatDate(rc.lastReportedAt)}</span><span>{rc.relatedComplaintIds.length} related complaints</span></div></div>)}</div>}</SectionCard>
    </>}
  </div></MotionPage>
}
