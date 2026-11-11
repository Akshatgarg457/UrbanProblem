import type { ComplaintCategory, ComplaintStatus, Priority, UserRole } from '../types'

export function categoryLabel(cat: ComplaintCategory): string {
  const map: Record<ComplaintCategory, string> = {
    ROADS_AND_POTHOLES: 'Roads & Potholes',
    WATER_SUPPLY: 'Water Supply',
    ELECTRICITY_AND_STREETLIGHTS: 'Electricity & Streetlights',
    DRAINAGE_AND_SEWAGE: 'Drainage & Sewage',
    WASTE_MANAGEMENT: 'Waste Management',
    HEALTH_AND_ENVIRONMENT: 'Health & Environment',
    PUBLIC_TRANSPORT: 'Public Transport',
    PARKS_AND_RECREATION: 'Parks & Recreation',
    SAFETY_AND_SECURITY: 'Safety & Security',
  }
  return map[cat] ?? cat
}

export function statusLabel(s: ComplaintStatus): string {
  const map: Record<ComplaintStatus, string> = {
    SUBMITTED: 'Submitted',
    ACKNOWLEDGED: 'Acknowledged',
    ASSIGNED: 'Assigned',
    IN_PROGRESS: 'In Progress',
    SUBMITTED_FOR_REVIEW: 'Pending Review',
    RESOLVED: 'Resolved',
    REJECTED: 'Rejected',
    REWORK_REQUIRED: 'Rework Required',
    CLOSED: 'Closed',
  }
  return map[s] ?? s
}

export function priorityLabel(p: Priority): string {
  const map: Record<Priority, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    CRITICAL: 'Critical',
  }
  return map[p] ?? p
}

export function roleLabel(r: UserRole): string {
  const map: Record<UserRole, string> = {
    CITIZEN: 'Citizen',
    DEPARTMENT_ADMIN: 'Dept. Admin',
    WORKER: 'Field Worker',
    SYSTEM_ADMIN: 'System Admin',
  }
  return map[r] ?? r
}

/** Returns CSS class names for status badges */
export function statusBadgeClass(s: ComplaintStatus): string {
  switch (s) {
    case 'RESOLVED': return 'badge-green'
    case 'IN_PROGRESS': return 'badge-blue'
    case 'SUBMITTED_FOR_REVIEW': return 'badge-purple'
    case 'ASSIGNED': return 'badge-teal'
    case 'REWORK_REQUIRED': return 'badge-orange'
    case 'REJECTED': return 'badge-red'
    case 'CLOSED': return 'badge-gray'
    default: return 'badge-gray'
  }
}

/** Returns CSS class names for priority badges */
export function priorityBadgeClass(p: Priority): string {
  switch (p) {
    case 'CRITICAL': return 'badge-red'
    case 'HIGH': return 'badge-orange'
    case 'MEDIUM': return 'badge-amber'
    default: return 'badge-gray'
  }
}

/** Returns CSS class names for category icons */
export function categoryIconClass(cat: ComplaintCategory): string {
  switch (cat) {
    case 'ROADS_AND_POTHOLES': return 'category-coral'
    case 'WATER_SUPPLY': return 'category-blue'
    case 'ELECTRICITY_AND_STREETLIGHTS': return 'category-amber'
    case 'DRAINAGE_AND_SEWAGE': return 'category-violet'
    case 'WASTE_MANAGEMENT': return 'category-green'
    case 'HEALTH_AND_ENVIRONMENT': return 'category-blue'
    case 'PUBLIC_TRANSPORT': return 'category-amber'
    case 'PARKS_AND_RECREATION': return 'category-green'
    case 'SAFETY_AND_SECURITY': return 'category-violet'
  }
}

export function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })
}

export function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 60) return `${mins}m ago`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}h ago`
  const days = Math.floor(hrs / 24)
  return `${days}d ago`
}
