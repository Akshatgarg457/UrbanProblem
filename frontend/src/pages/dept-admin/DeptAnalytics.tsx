import { AlertCircle, CheckCircle2, Clock, TrendingUp } from 'lucide-react'
import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { useEffect, useMemo, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { Loader, PageHeader, SectionCard, StatCard } from '../../components/ui'
import { fetchDepartmentAnalytics } from '../../services/analyticsService'
import { fetchDepartmentComplaints } from '../../services/complaintService'
import { useAuth } from '../../context/AuthContext'
import type { AnalyticsSummary, Complaint } from '../../types'
import { statusLabel } from '../../utils/labels'

const PIE_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)', 'var(--chart-6)', 'var(--chart-7)']

export function DeptAnalytics() {
  const { user } = useAuth()
  const [summary, setSummary] = useState<AnalyticsSummary | null>(null)
  const [complaints, setComplaints] = useState<Complaint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    if (!user?.departmentId) {
      setError('Your account is not linked to a department.')
      setLoading(false)
      return
    }
    Promise.all([fetchDepartmentAnalytics(user.departmentId), fetchDepartmentComplaints({ size: 100 })])
      .then(([analytics, page]) => {
        if (cancelled) return
        setSummary(analytics)
        setComplaints(page.content)
      })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : 'Unable to load analytics.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [user?.departmentId])

  // Status distribution is computed from the department's live complaint list.
  const statusData = useMemo(() => {
    const counts = new Map<string, number>()
    for (const c of complaints) counts.set(c.status, (counts.get(c.status) ?? 0) + 1)
    return Array.from(counts.entries()).map(([status, value]) => ({ name: statusLabel(status as Complaint['status']), value }))
  }, [complaints])

  const categoryData = useMemo(
    () => (summary?.categoryBreakdown ?? []).map(c => ({ name: c.category.replaceAll('_', ' '), value: c.count })),
    [summary],
  )

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader title="Department Analytics" description="Performance metrics and complaint distribution for your department." />
        {error && <div className="form-message form-error" role="alert">{error}</div>}
        {loading ? <Loader /> : summary && <>
          <div className="stats-grid" style={{ marginTop: 24 }}>
            <StatCard label="Resolution Rate" value={`${summary.resolutionRate.toFixed(1)}%`} icon={<TrendingUp size={19} />} color="green" />
            <StatCard label="Avg. Resolution Days" value={summary.avgResolutionDays} icon={<Clock size={19} />} color="blue" />
            <StatCard label="Total Resolved" value={summary.resolvedComplaints} icon={<CheckCircle2 size={19} />} color="teal" />
            <StatCard label="Open Backlog" value={summary.openComplaints} icon={<AlertCircle size={19} />} color="orange" />
          </div>

          <div className="dashboard-grid-2col" style={{ marginTop: 24 }}>
            <SectionCard title="Current Status Distribution">
              {statusData.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: 24, textAlign: 'center' }}>No complaints in your department yet.</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={statusData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ percent }: { percent?: number }) => percent != null ? `${(percent * 100).toFixed(0)}%` : ''} labelLine={false} fontSize={9}>
                      {statusData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                    </Pie>
                    <Tooltip contentStyle={{ fontSize: 11, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
                    <Legend wrapperStyle={{ fontSize: 10 }} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </SectionCard>

            <SectionCard title="Category Distribution">
              {categoryData.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: 24, textAlign: 'center' }}>No category breakdown is available yet.</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie data={categoryData} dataKey="value" nameKey="name" cx="50%" cy="50%" outerRadius={80} label={({ percent }: { percent?: number }) => percent != null ? `${(percent * 100).toFixed(0)}%` : ''} labelLine={false} fontSize={9}>
                      {categoryData.map((_, i) => <Cell key={i} fill={PIE_COLORS[(i + 3) % PIE_COLORS.length]} />)}
                    </Pie>
                    <Tooltip contentStyle={{ fontSize: 11, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
                    <Legend wrapperStyle={{ fontSize: 10 }} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </SectionCard>
          </div>

          <p style={{ color: 'var(--text-muted)', fontSize: 11, marginTop: 12 }}>
            The department analytics API exposes current aggregate metrics only. Historical monthly trend charts are omitted rather than filled with sample values.
          </p>
        </>}
      </div>
    </MotionPage>
  )
}
