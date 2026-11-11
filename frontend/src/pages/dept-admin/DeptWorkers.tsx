import { AlertCircle, CheckCircle2, Loader2, PlusCircle, Users } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { Badge, Field, Loader, Modal, PageHeader, SearchInput } from '../../components/ui'
import { createWorker, fetchDepartmentWorkers } from '../../services/userService'
import { useAuth } from '../../context/AuthContext'
import type { User } from '../../types'

interface WorkerForm { firstName: string; lastName: string; email: string; phone: string; password: string }

const EMPTY_FORM: WorkerForm = { firstName: '', lastName: '', email: '', phone: '', password: '' }

export function DeptWorkers() {
  const { user } = useAuth()
  const [workers, setWorkers] = useState<User[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const [form, setForm] = useState<WorkerForm>(EMPTY_FORM)
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  async function load() {
    if (!user?.departmentId) {
      setError('Your account is not linked to a department.')
      setLoading(false)
      return
    }
    setLoading(true); setError('')
    try { setWorkers(await fetchDepartmentWorkers(user.departmentId)) }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to load workers.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [user?.departmentId])

  const filtered = workers.filter(w =>
    `${w.firstName} ${w.lastName} ${w.email}`.toLowerCase().includes(search.toLowerCase()),
  )

  function validate(): boolean {
    const errs: Record<string, string> = {}
    if (!form.firstName.trim()) errs.firstName = 'First name is required.'
    if (!form.lastName.trim()) errs.lastName = 'Last name is required.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) errs.email = 'Enter a valid email address.'
    if (form.phone.trim() && !/^[+\d][\d\s-]{6,19}$/.test(form.phone.trim())) errs.phone = 'Enter a valid phone number.'
    if (form.password.length < 8) errs.password = 'Password must be at least 8 characters.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleAdd(e: FormEvent) {
    e.preventDefault()
    if (!user?.departmentId || !validate()) return
    setSaving(true); setError(''); setNotice('')
    try {
      await createWorker({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phone.trim(),
        password: form.password,
        departmentId: user.departmentId,
      })
      setAddOpen(false)
      setForm(EMPTY_FORM)
      setNotice('Worker account created.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to create the worker account.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="Workers"
          description={`${workers.length} field worker${workers.length === 1 ? '' : 's'} in your department.`}
          actions={<button className="button button-primary button-small" onClick={() => setAddOpen(true)} type="button"><PlusCircle size={15} /> Add Worker</button>}
        />
        {error && <div className="form-message form-error" role="alert"><AlertCircle size={14} /> {error}</div>}
        {notice && <div className="toast-inline toast-info" role="status" style={{ marginBottom: 12 }}><CheckCircle2 size={14} />{notice}</div>}

        {loading ? <Loader /> : <div className="section-card" style={{ marginTop: 20 }}>
          <div style={{ marginBottom: 16 }}>
            <SearchInput value={search} onChange={setSearch} placeholder="Search workers…" />
          </div>
          {filtered.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--text-secondary)' }}>
              <Users size={28} style={{ margin: '0 auto 12px', display: 'block', color: 'var(--text-muted)' }} />
              <p>{workers.length === 0 ? 'No workers are registered in your department yet.' : 'No workers match your search.'}</p>
            </div>
          ) : (
            <div className="workers-grid">
              {filtered.map(w => (
                <div key={w.id} className="worker-card">
                  <div className="worker-avatar-lg">{w.firstName[0]}{w.lastName[0]}</div>
                  <div className="worker-card-info">
                    <div className="worker-card-name">{w.firstName} {w.lastName}</div>
                    <div className="worker-card-dept">{w.department ?? '—'}</div>
                    <div className="worker-card-email">{w.email}</div>
                  </div>
                  <Badge label={w.active ? 'Active' : 'Inactive'} className={w.active ? 'badge-green' : 'badge-gray'} />
                </div>
              ))}
            </div>
          )}
        </div>}
      </div>

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Add Worker">
        <form onSubmit={handleAdd} noValidate>
          <div className="form-grid-2">
            <Field label="First Name" htmlFor="workerFirst" error={formErrors.firstName} required>
              <input id="workerFirst" className={`field-input${formErrors.firstName ? ' field-input-error' : ''}`} value={form.firstName} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} />
            </Field>
            <Field label="Last Name" htmlFor="workerLast" error={formErrors.lastName} required>
              <input id="workerLast" className={`field-input${formErrors.lastName ? ' field-input-error' : ''}`} value={form.lastName} onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} />
            </Field>
          </div>
          <Field label="Email" htmlFor="workerEmail" error={formErrors.email} required>
            <input id="workerEmail" className={`field-input${formErrors.email ? ' field-input-error' : ''}`} type="email" value={form.email} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} />
          </Field>
          <Field label="Phone Number" htmlFor="workerPhone" error={formErrors.phone}>
            <input id="workerPhone" className={`field-input${formErrors.phone ? ' field-input-error' : ''}`} type="tel" value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
          </Field>
          <Field label="Temporary Password" htmlFor="workerPass" error={formErrors.password} required hint="The worker should change this on first login.">
            <input id="workerPass" className={`field-input${formErrors.password ? ' field-input-error' : ''}`} type="password" value={form.password} onChange={e => setForm(f => ({ ...f, password: e.target.value }))} />
          </Field>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
            <button className="button button-secondary button-small" type="button" onClick={() => setAddOpen(false)}>Cancel</button>
            <button className="button button-primary button-small" type="submit" disabled={saving}>
              {saving ? <><Loader2 size={14} className="spin" /> Creating…</> : 'Create Worker'}
            </button>
          </div>
        </form>
      </Modal>
    </MotionPage>
  )
}
