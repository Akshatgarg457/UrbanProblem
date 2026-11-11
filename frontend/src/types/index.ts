// ─── Enums ────────────────────────────────────────────────────────────────────

export type UserRole = 'CITIZEN' | 'DEPARTMENT_ADMIN' | 'WORKER' | 'SYSTEM_ADMIN'

export type ComplaintStatus =
  | 'SUBMITTED'
  | 'ACKNOWLEDGED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'SUBMITTED_FOR_REVIEW'
  | 'RESOLVED'
  | 'REJECTED'
  | 'REWORK_REQUIRED'
  | 'CLOSED'

export type ComplaintCategory =
  | 'ROADS_AND_POTHOLES'
  | 'WATER_SUPPLY'
  | 'ELECTRICITY_AND_STREETLIGHTS'
  | 'DRAINAGE_AND_SEWAGE'
  | 'WASTE_MANAGEMENT'
  | 'HEALTH_AND_ENVIRONMENT'
  | 'PUBLIC_TRANSPORT'
  | 'PARKS_AND_RECREATION'
  | 'SAFETY_AND_SECURITY'

export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

// ─── User ─────────────────────────────────────────────────────────────────────

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  phone?: string
  role: UserRole
  department?: string
  active: boolean
  createdAt: string
  departmentId?: number
}

export interface AuthUser extends User {}

// ─── Department ───────────────────────────────────────────────────────────────

export interface Department {
  id: number
  name: string
  category?: ComplaintCategory
  adminId?: number
  adminName?: string
  workerCount: number
  activeComplaintCount: number
  active: boolean
}

// ─── Complaint ────────────────────────────────────────────────────────────────

export interface Complaint {
  id: number
  referenceId: string
  title: string
  description: string
  category: ComplaintCategory
  status: ComplaintStatus
  priority?: Priority
  address: string
  landmark?: string
  latitude?: number
  longitude?: number
  citizenId: number
  citizenName: string
  departmentId?: number
  departmentName?: string
  assignedWorkerId?: number
  assignedWorkerName?: string
  attachmentUrls?: string[]
  submittedAt: string
  updatedAt: string
  resolvedAt?: string
  dueDate?: string
  attachmentWarning?: string
}

// ─── Assignment ───────────────────────────────────────────────────────────────

export interface Assignment {
  id: number
  complaintId: number
  complaintTitle: string
  workerId: number
  workerName: string
  assignedAt: string
  dueDate?: string
  notes?: string
}

// ─── Progress Update ─────────────────────────────────────────────────────────

export interface ProgressUpdate {
  id: number
  complaintId: number
  workerId: number
  workerName: string
  notes: string
  status: ComplaintStatus
  evidenceUrls?: string[]
  createdAt: string
}

// ─── Complaint History ────────────────────────────────────────────────────────

export interface ComplaintHistoryEntry {
  id: number
  status: ComplaintStatus
  notes: string
  actorName: string
  createdAt: string
}

// ─── Resolution Review ────────────────────────────────────────────────────────

export interface ResolutionReview {
  id: number
  complaintId: number
  reviewerId: number
  reviewerName: string
  approved: boolean
  rejectionReason?: string
  reviewedAt: string
}

// ─── Recurrence Report ────────────────────────────────────────────────────────

export interface RecurrenceCase {
  id: number
  category: ComplaintCategory
  location: string
  occurrenceCount: number
  firstReportedAt: string
  lastReportedAt: string
  relatedComplaintIds: number[]
  severity: 'LOW' | 'MEDIUM' | 'HIGH'
}

export interface RecurrenceReport {
  generatedAt: string
  periodDays: number
  cases: RecurrenceCase[]
  totalRecurringIssues: number
  mostAffectedCategory: ComplaintCategory
  mostAffectedLocation: string
}

// ─── Analytics ────────────────────────────────────────────────────────────────

export interface MonthlyTrend {
  month: string
  submitted: number
  resolved: number
  inProgress: number
}

export interface CategoryBreakdown {
  category: ComplaintCategory
  count: number
  percentage: number
}

export interface DepartmentPerformance {
  departmentId: number
  departmentName: string
  totalComplaints: number
  resolvedCount: number
  avgResolutionDays: number
  resolutionRate: number
}

export interface AnalyticsSummary {
  totalComplaints: number
  resolvedComplaints: number
  openComplaints: number
  avgResolutionDays: number
  resolutionRate: number
  monthlyTrends: MonthlyTrend[]
  categoryBreakdown: CategoryBreakdown[]
  departmentPerformance: DepartmentPerformance[]
}

// ─── API Response Wrappers ────────────────────────────────────────────────────

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface ApiError {
  message: string
  status: number
  timestamp: string
}

// ─── Form Types ───────────────────────────────────────────────────────────────

export interface ComplaintFormData {
  title: string
  description: string
  category: ComplaintCategory | ''
  address: string
  city: string
  state: string
  postalCode: string
  landmark: string
  latitude: string
  longitude: string
  attachments: File[]
}

export interface RegisterFormData {
  firstName: string
  lastName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

export interface LoginFormData {
  email: string
  password: string
}
