import { Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CheckCircle2, Clock, TrendingUp, AlertCircle } from 'lucide-react'
import { useEffect, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { PageHeader, SectionCard, StatCard } from '../../components/ui'
import { fetchSystemAnalytics } from '../../services/analyticsService'
import type { AnalyticsSummary } from '../../types'
import { categoryLabel } from '../../utils/labels'
const PIE_COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)']
export function SysAdminAnalytics() {
  const [data, setData] = useState<AnalyticsSummary | null>(null)
  const [error, setError] = useState('')
  useEffect(() => { fetchSystemAnalytics().then(setData).catch(e => setError(e instanceof Error ? e.message : 'Unable to load analytics.')) }, [])
  const categoryData = data?.categoryBreakdown.map(item => ({ ...item, name: categoryLabel(item.category) })) ?? []
  return <MotionPage><div className="dashboard-content"><PageHeader title="System Analytics" description="Platform-wide complaint totals, category distribution, and department performance." />
    {error && <div className="form-message form-error" role="alert">{error}</div>}
    {!data && !error ? <p>Loading analytics…</p> : data && <>
      <div className="stats-grid" style={{ marginTop: 24 }}><StatCard label="Resolution Rate" value={`${data.resolutionRate.toFixed(1)}%`} icon={<TrendingUp size={19} />} color="green" /><StatCard label="Avg. Resolution Days" value={data.avgResolutionDays} icon={<Clock size={19} />} color="blue" /><StatCard label="Total Resolved" value={data.resolvedComplaints} icon={<CheckCircle2 size={19} />} color="teal" /><StatCard label="Open Backlog" value={data.openComplaints} icon={<AlertCircle size={19} />} color="orange" /></div>
      <div className="dashboard-grid-2col" style={{ marginTop: 24 }}><SectionCard title="Category Distribution"><ResponsiveContainer width="100%" height={240}><PieChart><Pie data={categoryData} dataKey="count" nameKey="name" cx="50%" cy="50%" outerRadius={82} label={({ percent }: { percent?: number }) => percent != null ? `${(percent * 100).toFixed(0)}%` : ''} fontSize={9}>{categoryData.map((_, i) => <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />)}</Pie><Tooltip /></PieChart></ResponsiveContainer></SectionCard>
        <SectionCard title="Department Resolution Rate"><ResponsiveContainer width="100%" height={240}><BarChart data={data.departmentPerformance} layout="vertical" barSize={14}><CartesianGrid strokeDasharray="3 3" stroke="var(--chart-grid)" horizontal={false} /><XAxis type="number" tick={{ fontSize: 10, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} domain={[0, 100]} tickFormatter={v => `${v}%`} /><YAxis type="category" dataKey="departmentName" tick={{ fontSize: 10, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} width={110} /><Tooltip formatter={v => [`${Number(v).toFixed(1)}%`, 'Resolution Rate']} /><Bar dataKey="resolutionRate" fill="var(--chart-1)" name="Resolution Rate" radius={[0, 4, 4, 0]} /></BarChart></ResponsiveContainer></SectionCard></div>
      <SectionCard title="Category Complaint Counts" className="mt-4"><div className="table-wrap"><table className="data-table"><thead><tr><th>Category</th><th>Total Complaints</th><th>Share</th></tr></thead><tbody>{data.categoryBreakdown.map(c => <tr key={c.category}><td>{categoryLabel(c.category)}</td><td>{c.count}</td><td>{c.percentage.toFixed(1)}%</td></tr>)}</tbody></table></div></SectionCard>
      <p style={{ color: 'var(--text-muted)', fontSize: 11, marginTop: 12 }}>The backend currently exposes aggregate metrics, not historical monthly trend data. Trend charts are omitted rather than filled with sample values.</p>
    </>}
  </div></MotionPage>
}
