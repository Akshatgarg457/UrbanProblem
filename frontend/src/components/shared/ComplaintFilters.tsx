import type { ComplaintCategory, ComplaintStatus, Priority } from '../../types'
import { SearchInput } from '../ui'

export interface FilterState {
  search: string
  status: ComplaintStatus | ''
  category: ComplaintCategory | ''
  priority: Priority | ''
}

interface Props {
  filters: FilterState
  onChange: (f: FilterState) => void
  showPriority?: boolean
}

const STATUS_OPTIONS: { value: ComplaintStatus | ''; label: string }[] = [
  { value: '', label: 'All statuses' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'ASSIGNED', label: 'Assigned' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'SUBMITTED_FOR_REVIEW', label: 'Pending Review' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'REJECTED', label: 'Rejected' },
]

const CATEGORY_OPTIONS: { value: ComplaintCategory | ''; label: string }[] = [
  { value: '', label: 'All categories' },
  { value: 'ROADS_AND_POTHOLES', label: 'Roads & Potholes' },
  { value: 'WATER_SUPPLY', label: 'Water Supply' },
  { value: 'ELECTRICITY_AND_STREETLIGHTS', label: 'Electricity & Streetlights' },
  { value: 'DRAINAGE_AND_SEWAGE', label: 'Drainage & Sewage' },
  { value: 'WASTE_MANAGEMENT', label: 'Waste Management' },
  { value: 'HEALTH_AND_ENVIRONMENT', label: 'Health & Environment' },
  { value: 'PUBLIC_TRANSPORT', label: 'Public Transport' },
  { value: 'PARKS_AND_RECREATION', label: 'Parks & Recreation' },
  { value: 'SAFETY_AND_SECURITY', label: 'Safety & Security' },
]

const PRIORITY_OPTIONS: { value: Priority | ''; label: string }[] = [
  { value: '', label: 'All priorities' },
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
]

export function ComplaintFilters({ filters, onChange, showPriority = true }: Props) {
  return (
    <div className="filters-row">
      <SearchInput
        value={filters.search}
        onChange={v => onChange({ ...filters, search: v })}
        placeholder="Search by title or reference…"
      />
      <select
        className="filter-select"
        value={filters.status}
        onChange={e => onChange({ ...filters, status: e.target.value as ComplaintStatus | '' })}
        aria-label="Filter by status"
      >
        {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <select
        className="filter-select"
        value={filters.category}
        onChange={e => onChange({ ...filters, category: e.target.value as ComplaintCategory | '' })}
        aria-label="Filter by category"
      >
        {CATEGORY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      {showPriority && (
        <select
          className="filter-select"
          value={filters.priority}
          onChange={e => onChange({ ...filters, priority: e.target.value as Priority | '' })}
          aria-label="Filter by priority"
        >
          {PRIORITY_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      )}
    </div>
  )
}

export function applyFilters(complaints: import('../../types').Complaint[], filters: FilterState) {
  return complaints.filter(c => {
    if (filters.search) {
      const q = filters.search.toLowerCase()
      if (!c.title.toLowerCase().includes(q) && !c.referenceId.toLowerCase().includes(q)) return false
    }
    if (filters.status && c.status !== filters.status) return false
    if (filters.category && c.category !== filters.category) return false
    if (filters.priority && c.priority !== filters.priority) return false
    return true
  })
}
