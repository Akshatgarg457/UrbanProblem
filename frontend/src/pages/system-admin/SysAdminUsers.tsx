import { Check, RefreshCw, UserCheck, Users, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { Badge, Loader, PageHeader, SearchInput } from '../../components/ui'
import { fetchDepartments } from '../../services/departmentService'
import { approveWorker, fetchDepartmentWorkers, fetchPendingWorkers, rejectWorker } from '../../services/userService'
import type { Department, User } from '../../types'

interface DepartmentWorkers { department: Department; workers: User[] }

/**
 * The backend intentionally exposes no generic /users listing, so this view
 * combines the pending worker-application queue with worker accounts grouped
 * by department.
 */
export function SysAdminUsers() {
  const [groups, setGroups] = useState<DepartmentWorkers[]>([])
  const [pending, setPending] = useState<User[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [departments, pendingWorkers] = await Promise.all([fetchDepartments(), fetchPendingWorkers()])
      const populated = await Promise.all(
        departments.map(async department => ({
          department,
          workers: await fetchDepartmentWorkers(department.id).catch(() => [] as User[]),
        })),
      )
      setGroups(populated)
      setPending(pendingWorkers)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to load users.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { void load() }, [load])

  async function decide(worker: User, approve: boolean) {
    setBusyId(worker.id)
    setError('')
    setNotice('')
    try {
      if (approve) await approveWorker(worker.id)
      else await rejectWorker(worker.id)
      setNotice(`${worker.firstName} ${worker.lastName} was ${approve ? 'approved and can now sign in' : 'rejected'}.`)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Unable to update this worker application.')
    } finally {
      setBusyId(null)
    }
  }

  const totalWorkers = groups.reduce((sum, g) => sum + g.workers.length, 0)
  const filtered = useMemo(
    () => groups
      .map(g => ({
        ...g,
        workers: g.workers.filter(w =>
          `${w.firstName} ${w.lastName} ${w.email}`.toLowerCase().includes(search.toLowerCase()),
        ),
      }))
      .filter(g => g.workers.length > 0),
    [groups, search],
  )

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="User Management"
          description={`${pending.length} pending application${pending.length === 1 ? '' : 's'} · ${totalWorkers} field worker${totalWorkers === 1 ? '' : 's'} across ${groups.length} department${groups.length === 1 ? '' : 's'}.`}
        />
        {error && <div className="form-message form-error" role="alert">{error} <button className="button button-secondary button-small" onClick={() => void load()} type="button"><RefreshCw size={14} /> Retry</button></div>}
        {notice && <div className="form-message form-notice" role="status">{notice}</div>}
        {loading ? <Loader /> : <>
          <section className="section-card" style={{ marginTop: 20 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
              <UserCheck size={18} />
              <h2 style={{ fontSize: 14, fontWeight: 660, margin: 0 }}>Pending Worker Applications</h2>
              {pending.length > 0 && <Badge label={`${pending.length} awaiting review`} className="badge-amber" />}
            </div>
            {pending.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: 12 }}>No worker applications are waiting for review.</p>
            ) : (
              <div className="table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Applicant</th>
                      <th>Email</th>
                      <th>Phone</th>
                      <th>Department</th>
                      <th>Submitted</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pending.map(worker => (
                      <tr key={worker.id}>
                        <td><strong>{worker.firstName} {worker.lastName}</strong></td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{worker.email}</td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{worker.phone ?? '—'}</td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{worker.department ?? '—'}</td>
                        <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{worker.createdAt ? new Date(worker.createdAt).toLocaleDateString() : '—'}</td>
                        <td>
                          <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                            <button className="button button-primary button-small" disabled={busyId === worker.id} onClick={() => void decide(worker, true)} type="button"><Check size={14} /> Approve</button>
                            <button className="button button-secondary button-small" disabled={busyId === worker.id} onClick={() => void decide(worker, false)} type="button"><X size={14} /> Reject</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 10 }}>
              Approved workers become active immediately. Rejected applications stay blocked and cannot sign in.
            </p>
          </section>

          <div className="section-card" style={{ marginTop: 20 }}>
            <div style={{ marginBottom: 16 }}>
              <SearchInput value={search} onChange={setSearch} placeholder="Search by name or email…" />
            </div>
            {filtered.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--text-secondary)' }}>
                <Users size={28} style={{ margin: '0 auto 12px', display: 'block', color: 'var(--text-muted)' }} />
                <p>{totalWorkers === 0 ? 'No worker accounts exist yet. Workers apply from the public worker registration page.' : 'No workers match your search.'}</p>
              </div>
            ) : (
              filtered.map(({ department, workers }) => (
                <div key={department.id} style={{ marginBottom: 24 }}>
                  <h3 style={{ fontSize: 13, fontWeight: 660, margin: '0 0 10px', color: 'var(--text-secondary)' }}>{department.name}</h3>
                  <div className="table-wrap">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Name</th>
                          <th>Email</th>
                          <th>Department</th>
                          <th>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {workers.map(u => (
                          <tr key={u.id}>
                            <td><strong>{u.firstName} {u.lastName}</strong></td>
                            <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{u.email}</td>
                            <td style={{ color: 'var(--text-secondary)', fontSize: 12 }}>{u.department ?? department.name}</td>
                            <td><Badge label={u.active ? 'Active' : 'Inactive'} className={u.active ? 'badge-green' : 'badge-gray'} /></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ))
            )}
            <p style={{ fontSize: 10, color: 'var(--text-muted)', marginTop: 4 }}>
              This view lists real field-worker accounts by department. Citizens and administrators are managed through their own sign-up and profile flows.
            </p>
          </div>
        </>}
      </div>
    </MotionPage>
  )
}
