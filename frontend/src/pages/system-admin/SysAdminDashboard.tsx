import { BarChart3, Building2, CheckCircle2, Clock, RefreshCw, TrendingUp, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { Loader, PageHeader, SectionCard, StatCard } from '../../components/ui'
import { fetchSystemAnalytics } from '../../services/analyticsService'
import { fetchDepartments } from '../../services/departmentService'
import type { AnalyticsSummary } from '../../types'
import { categoryLabel } from '../../utils/labels'

const PIE_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']

export function SysAdminDashboard() {
  const [data, setData] = useState<AnalyticsSummary | null>(null)
  const [departmentCount, setDepartmentCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError('')
    Promise.all([fetchSystemAnalytics(), fetchDepartments()])
      .then(([analytics, departments]) => {
        if (cancelled) return
        setData(analytics)
        setDepartmentCount(departments.length)
      })
      .catch(e => { if (!cancelled) setError(e instanceof Error ? e.message : 'Unable to load system overview.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [reloadKey])

  const categoryData = (data?.categoryBreakdown ?? []).map(item => ({ ...item, name: categoryLabel(item.category) }))

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="System Overview"
          description="Platform-wide complaint activity, departments, and performance metrics."
        />
        {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={() => setReloadKey(k => k + 1)} type="button"><RefreshCw size={14} /> Retry</button></div>}
        {loading ? <Loader /> : data && <>
          <div className="stats-grid" style={{ marginTop: 24 }}>
            <StatCard label="Total Complaints" value={data.totalComplaints} icon={<BarChart3 size={19} />} color="teal" />
            <StatCard label="Resolved" value={data.resolvedComplaints} icon={<CheckCircle2 size={19} />} color="green" />
            <StatCard label="Open Backlog" value={data.openComplaints} icon={<Clock size={19} />} color="orange" />
            <StatCard label="Resolution Rate" value={`${data.resolutionRate.toFixed(1)}%`} icon={<TrendingUp size={19} />} color="blue" />
            <StatCard label="Departments" value={departmentCount} icon={<Building2 size={19} />} color="purple" />
            <StatCard label="Avg. Res. Days" value={data.avgResolutionDays} icon={<Clock size={19} />} color="teal" />
          </div>

          <div className="dashboard-grid-2col" style={{ marginTop: 24 }}>
            <SectionCard title="Category Distribution">
              {categoryData.length === 0 ? (
                <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: 24, textAlign: 'center' }}>No complaint data has been recorded yet.</p>
              ) : (
                <ResponsiveContainer width="100%" height={220}>
                  <PieChart>
                    <Pie
                      data={categoryData}
                      dataKey="count"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      outerRadius={80}
                      label={({ percent }: { percent?: number }) => percent != null ? `${(percent * 100).toFixed(0)}%` : ''}
                      fontSize={9}
                    >
                      {categoryData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}
                    </Pie>
                    <Tooltip formatter={(value, name) => [value, String(name).replace(/_/g, ' ')]} contentStyle={{ fontSize: 11, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }} />
                  </PieChart>
                </ResponsiveContainer>
              )}
            </SectionCard>

            <SectionCard title="Quick Navigation">
              <div className="quick-actions">
                <Link to="/dashboard/system-admin/users" className="quick-action-card">
                  <span className="quick-action-icon"><Users size={20} /></span>
                  <div><div className="quick-action-title">Manage Users</div><div className="quick-action-desc">Field workers across departments</div></div>
                </Link>
                <Link to="/dashboard/system-admin/departments" className="quick-action-card">
                  <span className="quick-action-icon"><Building2 size={20} /></span>
                  <div><div className="quick-action-title">Departments</div><div className="quick-action-desc">Manage department records</div></div>
                </Link>
                <Link to="/dashboard/system-admin/complaints" className="quick-action-card">
                  <span className="quick-action-icon"><BarChart3 size={20} /></span>
                  <div><div className="quick-action-title">All Complaints</div><div className="quick-action-desc">System-wide complaint view</div></div>
                </Link>
                <Link to="/dashboard/system-admin/analytics" className="quick-action-card">
                  <span className="quick-action-icon"><TrendingUp size={20} /></span>
                  <div><div className="quick-action-title">Analytics</div><div className="quick-action-desc">Trends and performance</div></div>
                </Link>
              </div>
            </SectionCard>
          </div>

          <SectionCard title="Department Performance" className="mt-4" action={<Link to="/dashboard/system-admin/analytics" className="text-link" style={{ fontSize: 11 }}>Full analytics</Link>}>
            {data.departmentPerformance.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: 13 }}>No department performance data is available yet.</p>
            ) : (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Department</th>
                      <th>Total</th>
                      <th>Resolved</th>
                      <th>Resolution Rate</th>
                      <th>Avg. Days</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.departmentPerformance.map(d => (
                      <tr key={d.departmentId}>
                        <td>{d.departmentName}</td>
                        <td>{d.totalComplaints}</td>
                        <td>{d.resolvedCount}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <div style={{ flex: 1, height: 5, background: 'var(--border)', borderRadius: 3 }}>
                              <div style={{ width: `${Math.min(d.resolutionRate, 100)}%`, height: '100%', background: 'var(--accent)', borderRadius: 3 }} />
                            </div>
                            <span style={{ fontSize: 11, color: 'var(--text-secondary)', minWidth: 38 }}>{d.resolutionRate.toFixed(1)}%</span>
                          </div>
                        </td>
                        <td style={{ color: 'var(--text-secondary)' }}>{d.avgResolutionDays}d</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </SectionCard>
        </>}
      </div>
    </MotionPage>
  )
}
