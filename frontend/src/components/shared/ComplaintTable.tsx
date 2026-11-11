import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { Complaint } from '../../types'
import { CategoryBadge, PriorityBadge, StatusBadge } from '../ui'
import { formatDate } from '../../utils/labels'

interface Props {
  complaints: Complaint[]
  basePath: string
  emptyMessage?: string
}

export function ComplaintTable({ complaints, basePath, emptyMessage = 'No complaints found.' }: Props) {
  if (complaints.length === 0) {
    return <p style={{ color: 'var(--text-muted)', fontSize: 13, padding: '24px 0' }}>{emptyMessage}</p>
  }

  return (
    <div className="table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Reference</th>
            <th>Title</th>
            <th>Category</th>
            <th>Priority</th>
            <th>Status</th>
            <th>Date</th>
            <th aria-label="View" />
          </tr>
        </thead>
        <tbody>
          {complaints.map(c => (
            <tr key={c.id}>
              <td><span className="ref-id">{c.referenceId}</span></td>
              <td className="complaint-title-cell">{c.title}</td>
              <td><CategoryBadge category={c.category} /></td>
              <td>{c.priority ? <PriorityBadge priority={c.priority} /> : <span style={{ color: 'var(--text-muted)' }}>Not set</span>}</td>
              <td><StatusBadge status={c.status} /></td>
              <td style={{ whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>{formatDate(c.submittedAt)}</td>
              <td>
                <Link to={`${basePath}/${encodeURIComponent(c.referenceId)}`} className="table-action-link" aria-label={`View complaint ${c.referenceId}`}>
                  <ChevronRight size={16} />
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
