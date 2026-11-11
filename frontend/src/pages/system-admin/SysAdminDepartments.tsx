import { AlertCircle, Building2, CheckCircle2, Loader2, PlusCircle } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { MotionPage } from '../../components/MotionPage'
import { Badge, CategoryBadge, Field, Loader, Modal, PageHeader, SearchInput } from '../../components/ui'
import { createDepartment, fetchDepartments } from '../../services/departmentService'
import type { ComplaintCategory, Department } from '../../types'

interface DepartmentForm { name: string; category: ComplaintCategory | ''; description: string; headEmail: string }

export function SysAdminDepartments() {
  const [departments, setDepartments] = useState<Department[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [addOpen, setAddOpen] = useState(false)
  const [form, setForm] = useState<DepartmentForm>({ name: '', category: '', description: '', headEmail: '' })
  const [formErrors, setFormErrors] = useState<Record<string, string>>({})
  const [saving, setSaving] = useState(false)
  const [notice, setNotice] = useState('')

  async function load() {
    setLoading(true); setError('')
    try { setDepartments(await fetchDepartments()) }
    catch (e) { setError(e instanceof Error ? e.message : 'Unable to load departments.') }
    finally { setLoading(false) }
  }
  useEffect(() => { void load() }, [])

  const filtered = departments.filter(d => d.name.toLowerCase().includes(search.toLowerCase()))

  function validate(): boolean {
    const errs: Record<string, string> = {}
    if (!form.name.trim()) errs.name = 'Department name is required.'
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.headEmail.trim())) errs.headEmail = 'Enter a valid head email address.'
    setFormErrors(errs)
    return Object.keys(errs).length === 0
  }

  async function handleAdd(e: FormEvent) {
    e.preventDefault()
    if (!validate()) return
    setSaving(true); setError(''); setNotice('')
    try {
      await createDepartment({
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        headEmail: form.headEmail.trim(),
      })
      setAddOpen(false)
      setForm({ name: '', category: '', description: '', headEmail: '' })
      setNotice('Department created.')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unable to create the department.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="Departments"
          description={`${departments.length} department${departments.length === 1 ? '' : 's'} managing civic complaints.`}
          actions={<button className="button button-primary button-small" onClick={() => setAddOpen(true)} type="button"><PlusCircle size={15} /> Add Department</button>}
        />
        {error && <div className="form-message form-error" role="alert"><AlertCircle size={14} /> {error}</div>}
        {notice && <div className="toast-inline toast-info" role="status" style={{ marginBottom: 12 }}><CheckCircle2 size={14} />{notice}</div>}

        {loading ? <Loader /> : <div className="section-card" style={{ marginTop: 20 }}>
          <div style={{ marginBottom: 16 }}>
            <SearchInput value={search} onChange={setSearch} placeholder="Search departments…" />
          </div>
          {filtered.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--text-secondary)' }}>
              <Building2 size={28} style={{ margin: '0 auto 12px', display: 'block', color: 'var(--text-muted)' }} />
              <p>{departments.length === 0 ? 'No departments exist yet. Create the first one.' : 'No departments match your search.'}</p>
            </div>
          ) : (
            <div className="departments-grid">
              {filtered.map(d => (
                <div key={d.id} className="dept-card">
                  <div className="dept-card-header">
                    <span className="dept-card-icon"><Building2 size={20} /></span>
                    <Badge label={d.active ? 'Active' : 'Inactive'} className={d.active ? 'badge-green' : 'badge-gray'} />
                  </div>
                  <h3 className="dept-card-name">{d.name}</h3>
                  {d.category && <div style={{ marginBottom: 10 }}><CategoryBadge category={d.category} /></div>}
                  <div className="dept-card-stats">
                    <div className="dept-stat"><span>{d.workerCount}</span><label>Workers</label></div>
                    <div className="dept-stat"><span>{d.activeComplaintCount}</span><label>Active complaints</label></div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>}
      </div>

      <Modal open={addOpen} onClose={() => setAddOpen(false)} title="Add Department">
        <form onSubmit={handleAdd} noValidate>
          <Field label="Department Name" htmlFor="deptName" error={formErrors.name} required>
            <input id="deptName" className={`field-input${formErrors.name ? ' field-input-error' : ''}`} type="text" placeholder="e.g. Parks and Recreation" value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
          </Field>
          <Field label="Primary Category" htmlFor="deptCat">
            <select id="deptCat" className="field-input" value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value as ComplaintCategory | '' }))}>
              <option value="">— Select —</option>
              <option value="ROADS_AND_POTHOLES">Roads & Potholes</option>
              <option value="WATER_SUPPLY">Water Supply</option>
              <option value="ELECTRICITY_AND_STREETLIGHTS">Electricity & Streetlights</option>
              <option value="DRAINAGE_AND_SEWAGE">Drainage & Sewage</option>
              <option value="WASTE_MANAGEMENT">Waste Management</option>
            </select>
          </Field>
          <Field label="Description" htmlFor="deptDesc">
            <textarea id="deptDesc" className="field-input field-textarea" rows={3} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          </Field>
          <Field label="Department Head Email" htmlFor="deptHead" error={formErrors.headEmail} required>
            <input id="deptHead" className={`field-input${formErrors.headEmail ? ' field-input-error' : ''}`} type="email" placeholder="head@city.gov" value={form.headEmail} onChange={e => setForm(f => ({ ...f, headEmail: e.target.value }))} />
          </Field>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
            <button className="button button-secondary button-small" type="button" onClick={() => setAddOpen(false)}>Cancel</button>
            <button className="button button-primary button-small" type="submit" disabled={saving}>
              {saving ? <><Loader2 size={14} className="spin" /> Creating…</> : 'Create Department'}
            </button>
          </div>
        </form>
      </Modal>
    </MotionPage>
  )
}
