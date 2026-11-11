import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  ImagePlus,
  Loader2,
  MapPin,
  X,
} from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { MotionPage } from '../../components/MotionPage'
import { Field, PageHeader } from '../../components/ui'
import type { ComplaintCategory, ComplaintFormData } from '../../types'
import { submitComplaint } from '../../services/complaintService'
import { fetchMyProfile } from '../../services/profileService'
import { LocationPicker } from '../../components/location/LocationPicker'
import { isValidLatitude, isValidLongitude } from '../../utils/location'

const CATEGORIES: { value: ComplaintCategory; label: string; desc: string }[] = [
  { value: 'ROADS_AND_POTHOLES', label: 'Roads & Potholes', desc: 'Potholes, damaged roads, unsafe crossings' },
  { value: 'WATER_SUPPLY', label: 'Water Supply', desc: 'Leaks, outages, quality issues' },
  { value: 'ELECTRICITY_AND_STREETLIGHTS', label: 'Electricity & Streetlights', desc: 'Non-functioning streetlights, electrical hazards' },
  { value: 'DRAINAGE_AND_SEWAGE', label: 'Drainage & Sewage', desc: 'Blocked drains, flooding, sewage overflow' },
  { value: 'WASTE_MANAGEMENT', label: 'Waste Management', desc: 'Missed collections, overflowing bins' },
  { value: 'HEALTH_AND_ENVIRONMENT', label: 'Health & Environment', desc: 'Vector control, sanitation, pollution, hazardous waste' },
  { value: 'PUBLIC_TRANSPORT', label: 'Public Transport', desc: 'Bus stops, transit shelters, service disruptions' },
  { value: 'PARKS_AND_RECREATION', label: 'Parks & Recreation', desc: 'Playgrounds, parks, sports facilities, trees' },
  { value: 'SAFETY_AND_SECURITY', label: 'Safety & Security', desc: 'Street lighting gaps, broken fencing, public safety risks' },
]

type Step = 'form' | 'submitting' | 'success'

function validate(data: ComplaintFormData): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!data.title.trim()) errors.title = 'Title is required.'
  else if (data.title.trim().length < 10) errors.title = 'Please provide a more descriptive title (at least 10 characters).'
  if (!data.description.trim()) errors.description = 'Description is required.'
  else if (data.description.trim().length < 20) errors.description = 'Please describe the issue in more detail (at least 20 characters).'
  if (!data.category) errors.category = 'Please select a category.'
  if (!data.address.trim()) errors.address = 'Address / location is required. Search, pick a point on the map, or enter it manually.'
  else if (data.address.trim().length < 5) errors.address = 'Please enter a more complete address (at least 5 characters).'

  const hasLatitude = data.latitude.trim() !== ''
  const hasLongitude = data.longitude.trim() !== ''
  if (hasLatitude !== hasLongitude) {
    errors.latitude = 'Provide both latitude and longitude, or leave both empty.'
  } else if (hasLatitude && hasLongitude) {
    const latitude = Number(data.latitude)
    const longitude = Number(data.longitude)
    if (!isValidLatitude(latitude)) errors.latitude = 'Latitude must be between -90 and 90.'
    if (!isValidLongitude(longitude)) errors.longitude = 'Longitude must be between -180 and 180.'
  }

  const combinedLocation = [data.address, data.city, data.state, data.postalCode, data.landmark]
    .map(part => part.trim())
    .filter(Boolean)
    .join(', ')
  if (combinedLocation.length > 500) {
    errors.address = 'The combined location details are too long. Shorten the address or landmark.'
  }
  return errors
}

export function ReportComplaint() {
  const [step, setStep] = useState<Step>('form')
  const [form, setForm] = useState<ComplaintFormData>({
    title: '', description: '', category: '', address: '', city: '', state: '', postalCode: '', landmark: '', latitude: '', longitude: '', attachments: [],
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [previews, setPreviews] = useState<string[]>([])
  const [submitError, setSubmitError] = useState('')
  const [createdReference, setCreatedReference] = useState('')
  const [attachmentWarning, setAttachmentWarning] = useState('')
  const [prefilledFromProfile, setPrefilledFromProfile] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  // Offer the citizen's saved profile location as the starting point for new reports.
  useEffect(() => {
    let cancelled = false
    fetchMyProfile('CITIZEN')
      .then(profile => {
        if (cancelled || !profile.location) return
        setForm(current => {
          if (current.address || current.latitude || current.longitude) return current
          return {
            ...current,
            address: current.address || profile.location!.address,
            city: current.city || profile.location!.city,
            state: current.state || profile.location!.state,
            postalCode: current.postalCode || profile.location!.postalCode,
            latitude: current.latitude || (profile.location!.latitude != null ? String(profile.location!.latitude) : ''),
            longitude: current.longitude || (profile.location!.longitude != null ? String(profile.location!.longitude) : ''),
          }
        })
        const saved = profile.location
        if (saved.address || saved.city || saved.latitude != null || saved.longitude != null) setPrefilledFromProfile(true)
      })
      .catch(() => { /* A missing profile location is fine; the form starts empty. */ })
    return () => { cancelled = true }
  }, [])

  function set(field: keyof ComplaintFormData, value: string) {
    setForm(f => ({ ...f, [field]: value }))
    if (errors[field]) setErrors(e => { const n = { ...e }; delete n[field]; return n })
  }

  function handleFiles(files: FileList | null) {
    if (!files) return
    const valid = Array.from(files).filter(f => {
      if (!['image/jpeg', 'image/png'].includes(f.type)) return false
      if (f.size > 10 * 1024 * 1024) return false
      return true
    })
    setForm(f => ({ ...f, attachments: [...f.attachments, ...valid].slice(0, 5) }))
    valid.forEach(file => {
      const reader = new FileReader()
      reader.onload = e => setPreviews(p => [...p, e.target?.result as string].slice(0, 5))
      reader.readAsDataURL(file)
    })
  }

  function removeFile(index: number) {
    setForm(f => ({ ...f, attachments: f.attachments.filter((_, i) => i !== index) }))
    setPreviews(p => p.filter((_, i) => i !== index))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    const errs = validate(form)
    if (Object.keys(errs).length > 0) { setErrors(errs); return }
    setSubmitError('')
    setStep('submitting')

    try {
      const payload = new FormData()
      payload.append('title', form.title.trim())
      payload.append('description', form.description.trim())
      payload.append('category', form.category)
      payload.append('location', [form.address.trim(), form.city.trim(), form.state.trim(), form.postalCode.trim(), form.landmark.trim()].filter(Boolean).join(', '))
      if (form.latitude) payload.append('latitude', form.latitude)
      if (form.longitude) payload.append('longitude', form.longitude)
      form.attachments.forEach(file => payload.append('attachments', file))
      const created = await submitComplaint(payload)
      setCreatedReference(created.referenceId)
      setAttachmentWarning(created.attachmentWarning ?? '')
      setStep('success')
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : 'Unable to submit complaint.')
      setStep('form')
    }
  }

  if (step === 'submitting') {
    return (
      <div className="dashboard-content">
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', minHeight: 300, gap: 16 }}>
          <Loader2 size={36} className="spin" style={{ color: 'var(--teal)' }} />
          <p style={{ color: 'var(--text-secondary)', fontSize: 14 }}>Submitting your complaint…</p>
        </div>
      </div>
    )
  }

  if (step === 'success') {
    return (
      <div className="dashboard-content">
        <div className="success-panel">
          <span className="success-icon"><CheckCircle2 size={32} /></span>
          <h2>Complaint Submitted</h2>
          <p>Your report has been received and saved. You can track its progress in My Complaints.</p>
          {attachmentWarning && <p className="form-message form-error" role="alert">{attachmentWarning}</p>}
          <p className="success-refid">Reference: {createdReference}</p>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'center', flexWrap: 'wrap', marginTop: 20 }}>
            <button className="button button-secondary button-small" onClick={() => { setStep('form'); setPrefilledFromProfile(false); setForm({ title: '', description: '', category: '', address: '', city: '', state: '', postalCode: '', landmark: '', latitude: '', longitude: '', attachments: [] }); setPreviews([]) }} type="button">
              Report Another
            </button>
            <Link to="/dashboard/citizen/complaints" className="button button-primary button-small">View My Complaints</Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <MotionPage>
      <div className="dashboard-content">
        <PageHeader
          title="Report a Local Issue"
          description="Describe what you found and where. Clear details help resolve issues faster."
          actions={<Link to="/dashboard/citizen" className="button button-secondary button-small"><ArrowLeft size={14} /> Back</Link>}
        />


        <form onSubmit={handleSubmit} noValidate>
          <div className="form-section">
            <h2 className="form-section-title">Category</h2>
            <div className="category-select-grid" role="radiogroup" aria-label="Complaint category">
              {CATEGORIES.map(cat => (
                <label key={cat.value} className={`category-option${form.category === cat.value ? ' selected' : ''}`}>
                  <input
                    type="radio"
                    name="category"
                    value={cat.value}
                    checked={form.category === cat.value}
                    onChange={() => set('category', cat.value)}
                    className="sr-only"
                  />
                  <strong>{cat.label}</strong>
                  <span>{cat.desc}</span>
                </label>
              ))}
            </div>
            {errors.category && <span className="field-error" role="alert">{errors.category}</span>}
          </div>

          <div className="form-section">
            <h2 className="form-section-title">Issue Details</h2>
            <div className="form-grid-2">
              <Field label="Title" htmlFor="title" error={errors.title} required>
                <input
                  id="title"
                  className={`field-input${errors.title ? ' field-input-error' : ''}`}
                  type="text"
                  value={form.title}
                  onChange={e => set('title', e.target.value)}
                  placeholder="e.g. Large pothole on MG Road near bus stop"
                  maxLength={200}
                  aria-describedby={errors.title ? 'title-err' : undefined}
                />
              </Field>
            </div>
            <Field label="Description" htmlFor="description" error={errors.description} required hint="Describe the problem in detail — size, duration, any safety risk.">
              <textarea
                id="description"
                className={`field-input field-textarea${errors.description ? ' field-input-error' : ''}`}
                value={form.description}
                onChange={e => set('description', e.target.value)}
                placeholder="Describe the issue in detail — size, duration, safety risk…"
                rows={4}
                maxLength={2000}
              />
            </Field>
          </div>

          <div className="form-section">
            <h2 className="form-section-title">Location</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: 12, margin: '0 0 14px' }}>
              Select the exact spot on the map so the field team can find the issue. You can search, click the map,
              drag the marker, use your current location, or type the address manually.
            </p>
            {prefilledFromProfile && (
              <div className="form-message form-notice" role="status" style={{ marginBottom: 14 }}>
                <MapPin size={14} />
                <span>Prefilled from your saved profile location. Change it here if this issue is somewhere else.</span>
              </div>
            )}
            <LocationPicker
              value={{ address: form.address, city: form.city, state: form.state, postalCode: form.postalCode, latitude: form.latitude, longitude: form.longitude }}
              onChange={location => {
                setForm(current => ({ ...current, ...location }))
                if (errors.address || errors.latitude || errors.longitude) {
                  setErrors(current => {
                    const next = { ...current }
                    delete next.address
                    delete next.latitude
                    delete next.longitude
                    return next
                  })
                }
              }}
            />
            {errors.address && <span className="field-error" role="alert">{errors.address}</span>}
            {(errors.latitude || errors.longitude) && <span className="field-error" role="alert">{errors.latitude ?? errors.longitude}</span>}
            {(form.address || form.city) && (
              <p className="location-summary">
                <strong>Selected location:</strong> {[form.address, form.city, form.state, form.postalCode].filter(Boolean).join(', ')}
                {form.latitude && form.longitude && isValidLatitude(Number(form.latitude)) && isValidLongitude(Number(form.longitude))
                  ? ` · ${Number(form.latitude).toFixed(5)}, ${Number(form.longitude).toFixed(5)}`
                  : ''}
              </p>
            )}
            <Field label="Nearest Landmark" htmlFor="landmark" hint="Optional — helps workers locate the issue faster.">
              <input
                id="landmark"
                className="field-input"
                type="text"
                value={form.landmark}
                onChange={e => set('landmark', e.target.value)}
                placeholder="e.g. Opposite State Bank of India"
                maxLength={120}
              />
            </Field>
          </div>


          <div className="form-section">
            <h2 className="form-section-title">Photos (optional)</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: 12, margin: '0 0 14px' }}>
              Attach up to 5 photos (max 10MB each, JPG/PNG). Files are securely uploaded to the complaint.
            </p>
            <div
              className="upload-zone"
              onClick={() => fileRef.current?.click()}
              onKeyDown={e => e.key === 'Enter' && fileRef.current?.click()}
              role="button"
              tabIndex={0}
              aria-label="Click or drag to upload photos"
            >
              <ImagePlus size={28} style={{ color: 'var(--text-muted)' }} />
              <span>Click to select photos or drag and drop here</span>
              <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>JPG, PNG — max 10MB each</span>
            </div>
            <input
              ref={fileRef}
              type="file"
              accept="image/*"
              multiple
              className="sr-only"
              onChange={e => handleFiles(e.target.files)}
              aria-label="Upload photos"
            />
            {previews.length > 0 && (
              <div className="upload-previews">
                {previews.map((src, i) => (
                  <div key={i} className="upload-preview-item">
                    <img src={src} alt={`Attachment ${i + 1}`} className="upload-preview-img" />
                    <button
                      type="button"
                      className="upload-preview-remove"
                      onClick={() => removeFile(i)}
                      aria-label={`Remove photo ${i + 1}`}
                    >
                      <X size={12} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {submitError && (
            <div className="form-message form-error" role="alert">
              <AlertCircle size={14} style={{ flexShrink: 0 }} /> {submitError}
            </div>
          )}

          <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end', paddingTop: 8 }}>
            <Link to="/dashboard/citizen" className="button button-secondary">Cancel</Link>
            <button className="button button-primary" type="submit">
              Submit Report
            </button>
          </div>
        </form>
      </div>
    </MotionPage>
  )
}
