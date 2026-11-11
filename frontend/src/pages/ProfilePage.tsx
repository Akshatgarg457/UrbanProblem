import { AlertCircle, Bell, CheckCircle2, ExternalLink, KeyRound, Languages, Loader2, MapPin, Save, UserCircle } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { MotionPage } from '../components/MotionPage'
import { ThemeToggle } from '../components/ThemeToggle'
import { LocationPicker } from '../components/location/LocationPicker'
import { Field, PageHeader, SectionCard } from '../components/ui'
import { useAuth } from '../context/AuthContext'
import { changePassword } from '../services/accountService'
import { fetchMyProfile, updateMyProfile, type Profile } from '../services/profileService'
import type { UserRole } from '../types'
import { formatDate, roleLabel } from '../utils/labels'
import { formatCoordinatePair, osmDirectionsUrl, osmViewUrl } from '../utils/location'

const WORKSPACE_DESCRIPTIONS: Record<UserRole, string> = {
  CITIZEN: 'Report community issues and track their progress.',
  WORKER: 'Manage assigned field tasks and submit work for review.',
  DEPARTMENT_ADMIN: 'Review and manage activity for your department.',
  SYSTEM_ADMIN: 'Manage departments, users, and system-wide reviews.',
}

interface LocationFormState { address: string; city: string; state: string; postalCode: string; latitude: string; longitude: string }
interface FormState { firstName: string; lastName: string; phone: string; location: LocationFormState }

interface NotificationPrefs { email: boolean; inApp: boolean }

function loadNotificationPrefs(): NotificationPrefs {
  try {
    const raw = localStorage.getItem('commonground-notifications')
    if (!raw) return { email: true, inApp: true }
    const parsed = JSON.parse(raw)
    return { email: !!parsed.email, inApp: !!parsed.inApp }
  } catch {
    return { email: true, inApp: true }
  }
}

function saveNotificationPrefs(prefs: NotificationPrefs) {
  localStorage.setItem('commonground-notifications', JSON.stringify(prefs))
}

const EMPTY_LOCATION: LocationFormState = { address: '', city: '', state: '', postalCode: '', latitude: '', longitude: '' }

function validate(form: FormState, supportsPhone: boolean): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstName.trim()) errors.firstName = 'First name is required.'
  if (!form.lastName.trim()) errors.lastName = 'Last name is required.'
  if (supportsPhone && form.phone.trim() && !/^[+\d][\d\s-]{6,19}$/.test(form.phone.trim())) errors.phone = 'Enter a valid phone number.'
  const { latitude, longitude } = form.location
  if (latitude && !longitude) errors.longitude = 'Longitude is required when latitude is set.'
  if (!latitude && longitude) errors.latitude = 'Latitude is required when longitude is set.'
  if (latitude && longitude) {
    const lat = Number(latitude)
    const lng = Number(longitude)
    if (!Number.isFinite(lat) || lat < -90 || lat > 90) errors.latitude = 'Latitude must be between -90 and 90.'
    if (!Number.isFinite(lng) || lng < -180 || lng > 180) errors.longitude = 'Longitude must be between -180 and 180.'
  }
  return errors
}

export function ProfilePage() {
  const { user } = useAuth()
  const role = user?.role ?? 'CITIZEN'
  const supportsPhone = role === 'CITIZEN' || role === 'WORKER'
  const supportsLocation = role === 'CITIZEN' || role === 'WORKER'
  function accountStatus(): { label: string; color: string } {
    const status = profile?.status?.toUpperCase()
    if (status === 'ACTIVE') return { label: 'Active', color: 'var(--accent)' }
    if (role === 'WORKER' && status === 'INACTIVE') return { label: 'Pending administrator approval', color: 'var(--accent-strong)' }
    if (role === 'WORKER' && status === 'SUSPENDED') return { label: 'Application rejected / suspended', color: 'var(--inverse-surface)' }
    if (status) return { label: 'Inactive', color: 'var(--accent-strong)' }
    return { label: 'Unknown', color: 'var(--text-secondary)' }
  }
  const [profile, setProfile] = useState<Profile | null>(null)
  const [form, setForm] = useState<FormState>({ firstName: '', lastName: '', phone: '', location: EMPTY_LOCATION })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [saved, setSaved] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmNewPassword, setConfirmNewPassword] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordChanged, setPasswordChanged] = useState(false)
  const [changingPassword, setChangingPassword] = useState(false)
  const [displayName, setDisplayName] = useState('')
  const [preferredLanguage, setPreferredLanguage] = useState('en')
  const [notifications, setNotifications] = useState<NotificationPrefs>(loadNotificationPrefs())

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    fetchMyProfile(role)
      .then(p => {
        if (cancelled) return
        setProfile(p)
        setForm({
          firstName: p.firstName,
          lastName: p.lastName,
          phone: p.phone,
          location: p.location
            ? {
              address: p.location.address,
              city: p.location.city,
              state: p.location.state,
              postalCode: p.location.postalCode,
              latitude: p.location.latitude != null ? String(p.location.latitude) : '',
              longitude: p.location.longitude != null ? String(p.location.longitude) : '',
            }
            : EMPTY_LOCATION,
        })
      })
      .catch(e => { if (!cancelled) setLoadError(e instanceof Error ? e.message : 'Unable to load your profile.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [role])

  useEffect(() => {
    if (window.location.hash !== '#saved-location') return
    const timer = window.setTimeout(() => {
      document.getElementById('saved-location')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 150)
    return () => window.clearTimeout(timer)
  }, [loading])

  async function handleSave(e: FormEvent) {
    e.preventDefault()
    const errs = validate(form, supportsPhone)
    setErrors(errs)
    if (Object.keys(errs).length > 0) return
    setSaving(true); setSaveError(''); setSaved(false)
    try {
      const updated = await updateMyProfile(role, {
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        phoneNumber: form.phone.trim(),
        location: supportsLocation
          ? {
            address: form.location.address.trim(),
            city: form.location.city.trim(),
            state: form.location.state.trim(),
            postalCode: form.location.postalCode.trim(),
            latitude: form.location.latitude ? Number(form.location.latitude) : null,
            longitude: form.location.longitude ? Number(form.location.longitude) : null,
          }
          : undefined,
      })
      setProfile(updated)
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch (err) {
      setSaveError(err instanceof Error ? err.message : 'Unable to save your profile.')
    } finally {
      setSaving(false)
    }
  }

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault()
    setPasswordError('')
    setPasswordChanged(false)
    if (newPassword.length < 8 || newPassword.length > 128) {
      setPasswordError('New password must be between 8 and 128 characters.')
      return
    }
    if (newPassword !== confirmNewPassword) {
      setPasswordError('New passwords do not match.')
      return
    }
    setChangingPassword(true)
    try {
      await changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmNewPassword('')
      setPasswordChanged(true)
    } catch (err) {
      setPasswordError(err instanceof Error ? err.message : 'Unable to change your password.')
    } finally {
      setChangingPassword(false)
    }
  }

  if (loading) {
    return <MotionPage><div className="dashboard-content"><p style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--text-secondary)' }}><Loader2 size={16} className="spin" /> Loading your profile…</p></div></MotionPage>
  }
  if (loadError) {
    return <MotionPage><div className="dashboard-content"><PageHeader title="My Profile" description="Manage your account details." /><div className="form-message form-error" role="alert"><AlertCircle size={14} /> {loadError}</div></div></MotionPage>
  }

  const savedCoords = profile?.location?.latitude != null && profile?.location?.longitude != null

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader title="My Profile" description="Manage your account details and saved location." />

        <div className="profile-layout">
          <div className="profile-column">
            <form className="profile-form" onSubmit={handleSave} noValidate>
              <SectionCard title="Account Details">
                <div className="form-grid-2">
                  <Field label="First Name" htmlFor="firstName" error={errors.firstName} required>
                    <input id="firstName" className={`field-input${errors.firstName ? ' field-input-error' : ''}`} value={form.firstName} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} />
                  </Field>
                  <Field label="Last Name" htmlFor="lastName" error={errors.lastName} required>
                    <input id="lastName" className={`field-input${errors.lastName ? ' field-input-error' : ''}`} value={form.lastName} onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} />
                  </Field>
                </div>
                <Field label="Email" htmlFor="email" hint="Email cannot be changed after registration.">
                  <input id="email" className="field-input" type="email" value={profile?.email ?? ''} readOnly style={{ background: 'var(--surface-subtle)', cursor: 'not-allowed' }} />
                </Field>
                {supportsPhone && <Field label="Phone Number" htmlFor="phone" error={errors.phone}>
                  <input id="phone" className={`field-input${errors.phone ? ' field-input-error' : ''}`} type="tel" value={form.phone} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} />
                </Field>}
              </SectionCard>

              {supportsLocation && (
                <div id="saved-location">
                  <SectionCard title="My Saved Location">
                    <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '0 0 14px' }}>
                      <MapPin size={13} style={{ verticalAlign: '-2px', marginRight: 4 }} />
                      Your saved location is offered as the starting point when you report a new issue.
                      Search the map, drag the marker, use your current location, or enter the address manually.
                      It is stored on your account and restored the next time you sign in.
                    </p>
                    <LocationPicker
                      value={form.location}
                      onChange={location => setForm(f => ({ ...f, location }))}
                    />
                    {(errors.latitude || errors.longitude) && <span className="field-error" role="alert">{errors.latitude ?? errors.longitude}</span>}
                    {savedCoords && profile?.location && (
                      <div className="map-link-row">
                        <a
                          className="map-link"
                          href={osmViewUrl(profile.location.latitude!, profile.location.longitude!)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <ExternalLink size={14} /> View saved location on OpenStreetMap
                        </a>
                        <a
                          className="map-link"
                          href={osmDirectionsUrl(profile.location.latitude!, profile.location.longitude!)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <ExternalLink size={14} /> Directions
                        </a>
                        <span className="map-link-coords">{formatCoordinatePair(profile.location.latitude!, profile.location.longitude!)}</span>
                      </div>
                    )}
                  </SectionCard>
                </div>
              )}

              {saveError && <div className="form-message form-error" role="alert"><AlertCircle size={14} /> {saveError}</div>}
              <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                <button className="button button-primary button-small" type="submit" disabled={saving}>
                  {saving ? <Loader2 size={14} className="spin" /> : <Save size={14} />} Save Changes
                </button>
                {saved && <span style={{ display: 'flex', alignItems: 'center', gap: 4, color: 'var(--teal)', fontSize: 12 }}><CheckCircle2 size={13} /> Profile updated</span>}
              </div>
            </form>

            <SectionCard title="Change Password">
              <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '0 0 14px' }}>
                Use your current password to set a new password for this account.
              </p>
              <form className="auth-form" noValidate onSubmit={handleChangePassword}>
                <Field label="Current password" htmlFor="currentPassword">
                  <input id="currentPassword" className="field-input" type="password" autoComplete="current-password" value={currentPassword} onChange={event => setCurrentPassword(event.target.value)} required />
                </Field>
                <Field label="New password" htmlFor="newPassword" hint="Use 8 to 128 characters.">
                  <input id="newPassword" className="field-input" type="password" autoComplete="new-password" value={newPassword} onChange={event => setNewPassword(event.target.value)} required minLength={8} maxLength={128} />
                </Field>
                <Field label="Confirm new password" htmlFor="confirmNewPassword">
                  <input id="confirmNewPassword" className="field-input" type="password" autoComplete="new-password" value={confirmNewPassword} onChange={event => setConfirmNewPassword(event.target.value)} required minLength={8} maxLength={128} />
                </Field>
                {passwordError && <div className="form-message form-error" role="alert"><AlertCircle size={14} /> {passwordError}</div>}
                {passwordChanged && <div className="form-message form-notice" role="status"><CheckCircle2 size={14} /> Password changed successfully.</div>}
                <button className="button button-primary button-small" type="submit" disabled={changingPassword || !currentPassword || !newPassword || !confirmNewPassword}>
                  {changingPassword ? <Loader2 size={14} className="spin" /> : <KeyRound size={14} />} Change Password
                </button>
              </form>
            </SectionCard>
          </div>

          <div className="profile-column">
            <SectionCard title="Account Info">
              <div className="detail-info-list">
                <div className="detail-info-item"><span><strong>Account ID:</strong> {profile?.id ?? '—'}</span></div>
                <div className="detail-info-item"><span><strong>Role:</strong> {roleLabel(role)}</span></div>
                {profile?.departmentName && <div className="detail-info-item"><span><strong>Department:</strong> {profile.departmentName}</span></div>}
                <div className="detail-info-item"><span><strong>Member since:</strong> {profile?.createdAt ? formatDate(profile.createdAt) : '—'}</span></div>
                <div className="detail-info-item"><span><strong>Account status:</strong> <span style={{ color: accountStatus().color }}>{accountStatus().label}</span></span></div>
                <div className="detail-info-item"><span><strong>Workspace access:</strong> {WORKSPACE_DESCRIPTIONS[role]}</span></div>
              </div>
            </SectionCard>

            <SectionCard title="Public Profile">
              <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '0 0 14px' }}>
                <UserCircle size={13} style={{ verticalAlign: '-2px', marginRight: 4 }} />
                Choose a display name. Other residents will see this on the complaints you report.
              </p>
              <Field label="Display name" htmlFor="displayName" hint="Optional — leave blank to use your first name.">
                <input id="displayName" className="field-input" type="text" value={displayName} onChange={e => setDisplayName(e.target.value)} placeholder="e.g. Alex" maxLength={50} />
              </Field>
            </SectionCard>

            <SectionCard title="Preferences">
              <div className="settings-row">
                <div>
                  <strong style={{ display: 'flex', alignItems: 'center', gap: 6 }}><Languages size={14} /> Preferred language</strong>
                  <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '4px 0 0' }}>Select the language you want to use for the interface.</p>
                </div>
                <select className="field-input" value={preferredLanguage} onChange={e => setPreferredLanguage(e.target.value)} style={{ width: 140, minHeight: 40 }}>
                  <option value="en">English</option>
                  <option value="hi">Hindi</option>
                  <option value="mr">Marathi</option>
                  <option value="ta">Tamil</option>
                </select>
              </div>
              <div className="settings-row">
                <div>
                  <strong>Appearance</strong>
                  <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '4px 0 0' }}>Switch between light and dark appearance.</p>
                </div>
                <ThemeToggle />
              </div>
            </SectionCard>

            <SectionCard title="Notifications">
              <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '0 0 14px' }}>
                <Bell size={13} style={{ verticalAlign: '-2px', marginRight: 4 }} />
                Choose how you want to be notified about updates to your reports.
              </p>
              <label className="settings-row" style={{ cursor: 'pointer' }}>
                <div>
                  <strong>Email notifications</strong>
                  <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '4px 0 0' }}>Receive updates at {profile?.email ?? 'your email address'}.</p>
                </div>
                <input
                  type="checkbox"
                  className="toggle-input"
                  checked={notifications.email}
                  onChange={e => { const next = { ...notifications, email: e.target.checked }; setNotifications(next); saveNotificationPrefs(next) }}
                />
              </label>
              <label className="settings-row" style={{ cursor: 'pointer' }}>
                <div>
                  <strong>In-app notifications</strong>
                  <p style={{ color: 'var(--text-secondary)', fontSize: 13, margin: '4px 0 0' }}>Show alerts inside CommonGround when your complaint status changes.</p>
                </div>
                <input
                  type="checkbox"
                  className="toggle-input"
                  checked={notifications.inApp}
                  onChange={e => { const next = { ...notifications, inApp: e.target.checked }; setNotifications(next); saveNotificationPrefs(next) }}
                />
              </label>
            </SectionCard>
          </div>
        </div>
      </div>
    </MotionPage>
  )
}
