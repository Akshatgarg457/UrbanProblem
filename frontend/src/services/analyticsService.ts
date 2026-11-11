/** Analytics and recurrence adapters mapped to the endpoints actually implemented by Spring Boot. */
import type { AnalyticsSummary, ComplaintCategory, RecurrenceReport } from '../types'
import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'
interface SystemMetrics { totalComplaints: number; totalDepartments: number; totalWorkers: number; submittedComplaints: number; assignedComplaints: number; inProgressComplaints: number; pendingVerificationComplaints: number; resolvedComplaints: number; rejectedComplaints: number; pendingResolutions: number; totalAssignments: number; resolutionRate: number }
interface AverageTime { averageDays: number; sampleSize: number }
interface DepartmentMetric { departmentId: number; departmentName: string; totalComplaints: number; resolvedComplaints: number; workerCount: number; resolutionRate: number }
interface RecurrenceDto { id: number; complaintId: number; complaintPublicId: string; departmentId: number; category: ComplaintCategory; location: string; latitude?: number; longitude?: number; proximityRadiusKm: number; historicalIssueCount: number; relatedComplaintIds: number[]; analysisNotes: string; createdAt: string; updatedAt: string }
interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface AnalyticsParams { fromDate?: string; toDate?: string; categoryFilter?: string }
export async function fetchSystemAnalytics(_params: AnalyticsParams = {}): Promise<AnalyticsSummary> {
  try {
    const [metrics, average, categories, departments] = await Promise.all([
      apiClient.get<SystemMetrics>('/analytics/system-metrics'),
      apiClient.get<AverageTime>('/analytics/average-resolution-time'),
      apiClient.get<Record<string, number>>('/analytics/complaints-by-category'),
      apiClient.get<Record<string, DepartmentMetric>>('/analytics/department-metrics'),
    ])
    const total = metrics.data.totalComplaints || 0
    const categoryEntries = Object.entries(categories.data) as Array<[string, number]>
    const departmentEntries = Object.values(departments.data)
    const categoryBreakdown = categoryEntries.map(([category, count]) => ({ category: category as ComplaintCategory, count, percentage: total ? Math.round(count / total * 100) : 0 }))
    const departmentPerformance = departmentEntries.map(metric => ({ departmentId: metric.departmentId, departmentName: metric.departmentName, totalComplaints: metric.totalComplaints, resolvedCount: metric.resolvedComplaints, avgResolutionDays: average.data.averageDays, resolutionRate: metric.resolutionRate }))
    return { totalComplaints: total, resolvedComplaints: metrics.data.resolvedComplaints, openComplaints: total - metrics.data.resolvedComplaints, avgResolutionDays: average.data.averageDays, resolutionRate: metrics.data.resolutionRate, monthlyTrends: [], categoryBreakdown, departmentPerformance }
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
/** Department-scoped summary from GET /analytics/department/{id}/summary.
 *  Fields are defensive because the summary payload is a flat metric object. */
interface DepartmentSummaryDto {
  totalComplaints?: number
  resolvedComplaints?: number
  openComplaints?: number
  avgResolutionDays?: number
  resolutionRate?: number
  statusBreakdown?: Record<string, number>
  categoryBreakdown?: Record<string, number>
}
export async function fetchDepartmentAnalytics(departmentId: number): Promise<AnalyticsSummary> {
  try {
    const r = await apiClient.get<DepartmentSummaryDto>(`/analytics/department/${departmentId}/summary`)
    const dto = r.data
    const total = dto.totalComplaints ?? 0
    const categoryEntries = Object.entries(dto.categoryBreakdown ?? {}) as Array<[string, number]>
    return {
      totalComplaints: total,
      resolvedComplaints: dto.resolvedComplaints ?? 0,
      openComplaints: dto.openComplaints ?? Math.max(total - (dto.resolvedComplaints ?? 0), 0),
      avgResolutionDays: dto.avgResolutionDays ?? 0,
      resolutionRate: dto.resolutionRate ?? 0,
      monthlyTrends: [],
      categoryBreakdown: categoryEntries.map(([category, count]) => ({ category: category as ComplaintCategory, count, percentage: total ? Math.round(count / total * 100) : 0 })),
      departmentPerformance: [],
    }
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
function mapRecurrence(reports: RecurrenceDto[]): RecurrenceReport {
  const cases = reports.map(r => ({ id: r.id, category: r.category, location: r.location, occurrenceCount: r.historicalIssueCount, firstReportedAt: r.createdAt, lastReportedAt: r.updatedAt, relatedComplaintIds: r.relatedComplaintIds, severity: r.historicalIssueCount >= 5 ? 'HIGH' as const : r.historicalIssueCount >= 3 ? 'MEDIUM' as const : 'LOW' as const }))
  const top = [...cases].sort((a, b) => b.occurrenceCount - a.occurrenceCount)[0]
  return { generatedAt: new Date().toISOString(), periodDays: 365, cases, totalRecurringIssues: cases.length, mostAffectedCategory: top?.category ?? 'ROADS_AND_POTHOLES', mostAffectedLocation: top?.location ?? '—' }
}
export interface RecurrenceParams { periodDays?: number; minOccurrences?: number; categoryFilter?: string }
export async function fetchSystemRecurrenceReport(params: RecurrenceParams = {}): Promise<RecurrenceReport> {
  try { const r = await apiClient.get<RecurrenceDto[]>('/recurrence/high-risk-areas', { params: { minHistoricalCount: params.minOccurrences ?? 3 } }); return mapRecurrence(r.data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function fetchDepartmentRecurrenceReport(_params: RecurrenceParams = {}): Promise<RecurrenceReport> {
  try { const r = await apiClient.get<Page<RecurrenceDto>>('/recurrence/reports', { params: { page: 0, size: 100, sort: 'updatedAt,desc' } }); return mapRecurrence(r.data.content) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
