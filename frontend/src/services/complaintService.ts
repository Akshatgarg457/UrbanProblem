/** Frontend adapter for the actual Spring Boot complaint/assignment/resolution contract. */
import type { Assignment, Complaint, ComplaintCategory, ComplaintHistoryEntry, ComplaintStatus, PageResponse, Priority, ProgressUpdate, ResolutionReview } from '../types'
import { apiClient } from './apiClient'
import { getCurrentUser } from './authService'
import { getApiErrorMessage } from './apiErrors'

interface BackendPage<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
interface ComplaintDto { id: number; publicId: string; title: string; description: string; category: ComplaintCategory; location: string; latitude?: number; longitude?: number; citizenId: number; citizenName: string; departmentId: number; departmentName: string; status: string; createdAt: string; updatedAt: string }
export interface AssignmentDto { id: number; complaintId: number; complaintPublicId: string; complaintTitle: string; workerId: number; workerName: string; assignmentNotes?: string; status: string; createdAt: string; updatedAt: string }
export interface ResolutionDto { id: number; assignmentId: number; complaintId: number; complaintPublicId: string; completionNotes: string; adminNotes?: string; rejectionReason?: string; reviewStatus: 'PENDING' | 'APPROVED' | 'REJECTED'; submittedAt: string; reviewedAt?: string }
interface EvidenceDto { id: number; complaintId: number; fileName: string; fileType: string; fileSize: number; uploadedBy: string; createdAt: string; downloadUrl: string }
export interface ComplaintListParams { page?: number; size?: number; status?: ComplaintStatus | ''; category?: ComplaintCategory | ''; priority?: Priority | ''; search?: string }

const STATUS_MAP: Record<string, ComplaintStatus> = { PENDING_VERIFICATION: 'SUBMITTED_FOR_REVIEW', APPROVED: 'RESOLVED', CANCELLED: 'CLOSED' }
const statusToUi = (status: string): ComplaintStatus => STATUS_MAP[status] ?? status as ComplaintStatus
const STATUS_API_MAP: Partial<Record<ComplaintStatus, string>> = { SUBMITTED_FOR_REVIEW: 'PENDING_VERIFICATION', CLOSED: 'CANCELLED', REWORK_REQUIRED: 'IN_PROGRESS' }
const statusToApi = (status: ComplaintStatus): string => STATUS_API_MAP[status] ?? status
function mapComplaint(dto: ComplaintDto): Complaint {
  return { id: dto.id, referenceId: dto.publicId, title: dto.title, description: dto.description, category: dto.category, status: statusToUi(dto.status), address: dto.location, latitude: dto.latitude, longitude: dto.longitude, citizenId: dto.citizenId, citizenName: dto.citizenName, departmentId: dto.departmentId, departmentName: dto.departmentName, submittedAt: dto.createdAt, updatedAt: dto.updatedAt }
}
function mapPage<T, U>(page: BackendPage<T>, map: (item: T) => U): PageResponse<U> { return { content: page.content.map(map), totalElements: page.totalElements, totalPages: page.totalPages, number: page.number, size: page.size } }
export async function fetchMyComplaints(params: ComplaintListParams = {}): Promise<PageResponse<Complaint>> {
  try { const r = await apiClient.get<BackendPage<ComplaintDto>>('/complaints/my-complaints', { params: { page: params.page ?? 0, size: params.size ?? 20, sort: 'createdAt,desc' } }); return mapPage(r.data, mapComplaint) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function submitComplaint(formData: FormData): Promise<Complaint> {
  try {
    const request = { title: String(formData.get('title') ?? ''), description: String(formData.get('description') ?? ''), category: String(formData.get('category') ?? '') as ComplaintCategory, location: String(formData.get('location') ?? formData.get('address') ?? ''), latitude: formData.get('latitude') ? Number(formData.get('latitude')) : undefined, longitude: formData.get('longitude') ? Number(formData.get('longitude')) : undefined }
    const created = await apiClient.post<ComplaintDto>('/complaints', request)
    const files = formData.getAll('attachments').filter((v): v is File => v instanceof File)
    const uploadFailures: string[] = []
    for (const file of files) { try { const body = new FormData(); body.append('file', file); await apiClient.post(`/complaints/${created.data.id}/evidence/upload`, body) } catch { uploadFailures.push(file.name) } }
    const mapped = mapComplaint(created.data)
    if (uploadFailures.length) mapped.attachmentWarning = `The complaint was created, but these evidence files could not be uploaded: ${uploadFailures.join(', ')}. Do not submit the complaint again.`
    return mapped
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function fetchComplaintById(id: number | string): Promise<Complaint> {
  try { const r = await apiClient.get<ComplaintDto>(`/complaints/${encodeURIComponent(String(id))}`); return mapComplaint(r.data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function fetchDepartmentComplaints(params: ComplaintListParams = {}): Promise<PageResponse<Complaint>> {
  const currentUser = await getCurrentUser()
  const departmentId = currentUser?.departmentId
  if (!departmentId) throw new Error('Your account is not linked to a department.')
  try { const r = await apiClient.get<BackendPage<ComplaintDto>>(`/complaints/department/${departmentId}`, { params: { page: params.page ?? 0, size: params.size ?? 20, sort: 'createdAt,desc', ...(params.status ? { status: statusToApi(params.status) } : {}), ...(params.category ? { category: params.category } : {}) } }); return mapPage(r.data, mapComplaint) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function assignWorker(complaintId: number, data: { workerId: number; notes?: string; dueDate?: string }): Promise<Assignment> {
  try { const r = await apiClient.post<AssignmentDto>(`/assignments/complaints/${complaintId}/assign`, { workerId: data.workerId, assignmentNotes: data.notes }); return { id: r.data.id, complaintId: r.data.complaintId, complaintTitle: r.data.complaintTitle, workerId: r.data.workerId, workerName: r.data.workerName, assignedAt: r.data.createdAt, notes: r.data.assignmentNotes } } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function fetchAssignedComplaints(params: ComplaintListParams = {}): Promise<PageResponse<Complaint>> {
  try {
    const r = await apiClient.get<BackendPage<AssignmentDto>>('/assignments/my-assignments', { params: { page: params.page ?? 0, size: params.size ?? 20, sort: 'createdAt,desc' } })
    const assignments: AssignmentDto[] = r.data.content
    const complaints = await Promise.all(assignments.map(async (assignment: AssignmentDto) => { const detail = await apiClient.get<ComplaintDto>(`/complaints/${encodeURIComponent(assignment.complaintPublicId)}`); return mapComplaint(detail.data) }))
    return { content: complaints, totalElements: r.data.totalElements, totalPages: r.data.totalPages, number: r.data.number, size: r.data.size }
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function getWorkerAssignmentForComplaint(complaintId: number): Promise<AssignmentDto | null> {
  try { const page = await apiClient.get<BackendPage<AssignmentDto>>('/assignments/my-assignments', { params: { page: 0, size: 100, sort: 'createdAt,desc' } }); return page.data.content.find(item => item.complaintId === complaintId) ?? null } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function startAssignment(assignmentId: number): Promise<AssignmentDto> {
  try { return (await apiClient.post<AssignmentDto>(`/assignments/${assignmentId}/start`)).data } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function uploadEvidence(complaintId: number, file: File): Promise<EvidenceDto> {
  try { const body = new FormData(); body.append('file', file); return (await apiClient.post<EvidenceDto>(`/complaints/${complaintId}/evidence/upload`, body)).data } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function downloadEvidence(complaintId: number, evidenceId: number): Promise<Blob> {
  try { return (await apiClient.get(`/complaints/${complaintId}/evidence/${evidenceId}/download`, { responseType: 'blob' })).data as Blob } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

export async function submitResolution(assignmentId: number, completionNotes: string): Promise<ResolutionDto> {
  try { return (await apiClient.post<ResolutionDto>(`/resolutions/assignments/${assignmentId}/submit`, { completionNotes })).data } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function fetchPendingResolutions(params: { page?: number; size?: number } = {}): Promise<BackendPage<ResolutionDto>> {
  try { return (await apiClient.get<BackendPage<ResolutionDto>>('/resolutions/pending', { params: { page: params.page ?? 0, size: params.size ?? 20, sort: 'submittedAt,asc' } })).data } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function reviewResolutionById(resolutionId: number, approved: boolean, rejectionReason?: string): Promise<ResolutionDto> {
  try { const payload = approved ? { reviewStatus: 'APPROVED', adminNotes: '' } : { reviewStatus: 'REJECTED', rejectionReason, adminNotes: '' }; const path = approved ? 'approve' : 'reject'; return (await apiClient.post<ResolutionDto>(`/resolutions/${resolutionId}/${path}`, payload)).data } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function reviewResolution(complaintId: number, data: { approved: boolean; rejectionReason?: string }): Promise<ResolutionReview> {
  try { const pending = await apiClient.get<BackendPage<ResolutionDto>>('/resolutions/pending', { params: { page: 0, size: 100 } }); const resolutions: ResolutionDto[] = pending.data.content; const resolution = resolutions.find((item: ResolutionDto) => item.complaintId === complaintId && item.reviewStatus === 'PENDING'); if (!resolution) throw new Error('No pending resolution was found for this complaint.'); const result = await reviewResolutionById(resolution.id, data.approved, data.rejectionReason); return { id: result.id, complaintId: result.complaintId, reviewerId: 0, reviewerName: '', approved: result.reviewStatus === 'APPROVED', rejectionReason: result.rejectionReason, reviewedAt: result.reviewedAt ?? new Date().toISOString() } } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

const ACTOR_LABEL: Record<string, string> = {
  CITIZEN: 'Citizen', WORKER: 'Field worker', DEPARTMENT_ADMIN: 'Department admin', SYSTEM_ADMIN: 'System admin',
}
export async function fetchComplaintHistory(publicId: string): Promise<ComplaintHistoryEntry[]> {
  interface HistoryDto {
    id?: number
    fromStatus?: string
    toStatus?: string
    actorRole?: string
    note?: string
    changedAt?: string
  }
  try {
    const r = await apiClient.get<HistoryDto[]>(`/complaints/${encodeURIComponent(publicId)}/history`)
    const entries = r.data.map((h, index) => ({
      id: h.id ?? index + 1,
      status: statusToUi(h.toStatus ?? 'SUBMITTED'),
      notes: h.note ?? '',
      actorName: h.actorRole ? (ACTOR_LABEL[h.actorRole] ?? h.actorRole) : 'System',
      createdAt: h.changedAt ?? new Date().toISOString(),
    }))
    // Approval records APPROVED then RESOLVED in one action; the UI has no separate
    // Approved status, so show that as the single Resolved event it represents.
    return entries.filter((entry, index) => {
      const prev = entries[index - 1]
      const next = entries[index + 1]
      if (prev?.status === entry.status) return true
      return next?.status !== entry.status
    })
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}

/** Latest submitted resolution for an assignment, used for worker feedback after review. */
export async function fetchLatestResolution(assignmentId: number): Promise<ResolutionDto | null> {
  try { return (await apiClient.get<ResolutionDto>(`/resolutions/assignments/${assignmentId}/latest`)).data }
  catch { return null }
}

/** Priority is intentionally unsupported: the backend has no priority field or endpoint. */
export async function updatePriority(_complaintId: number, _priority: Priority): Promise<Complaint> { throw new Error('Priority management is not supported by the current backend API.') }
export async function submitProgressUpdate(complaintId: number, data: { notes: string; newStatus: ComplaintStatus; evidenceFiles?: File[] }): Promise<ProgressUpdate> {
  if (data.newStatus !== 'IN_PROGRESS') throw new Error('The backend currently supports starting an assignment and submitting a resolution, not arbitrary progress-note updates.')
  const assigned = await apiClient.get<BackendPage<AssignmentDto>>('/assignments/my-assignments', { params: { page: 0, size: 100 } })
  const assignment = assigned.data.content.find(item => item.complaintId === complaintId)
  if (!assignment) throw new Error('No assignment was found for this complaint.')
  await startAssignment(assignment.id)
  for (const file of data.evidenceFiles ?? []) await uploadEvidence(complaintId, file)
  return { id: assignment.id, complaintId, workerId: assignment.workerId, workerName: assignment.workerName, notes: data.notes, status: 'IN_PROGRESS', createdAt: new Date().toISOString() }
}
export async function fetchAllComplaints(params: ComplaintListParams = {}): Promise<PageResponse<Complaint>> {
  try {
    const paging = { page: params.page ?? 0, size: params.size ?? 100, sort: 'createdAt,desc' }
    if (params.status || params.category) {
      const r = await apiClient.get<BackendPage<ComplaintDto>>('/complaints', { params: { ...paging, ...(params.status ? { status: statusToApi(params.status) } : {}), ...(params.category ? { category: params.category } : {}) } })
      return mapPage(r.data, mapComplaint)
    }
    // The backend defaults an unfiltered GET /complaints to SUBMITTED. Query each actual
    // status explicitly so the system-admin screen can build a genuine all-status view.
    const statuses = ['SUBMITTED', 'ASSIGNED', 'IN_PROGRESS', 'PENDING_VERIFICATION', 'APPROVED', 'REJECTED', 'RESOLVED', 'CANCELLED']
    const pages = await Promise.all(statuses.map(status => apiClient.get<BackendPage<ComplaintDto>>('/complaints', { params: { ...paging, status } })))
    const all = pages.flatMap(response => response.data.content).sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    return { content: all.map(mapComplaint), totalElements: all.length, totalPages: 1, number: 0, size: all.length }
  } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
