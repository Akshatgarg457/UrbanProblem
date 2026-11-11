import { CheckCheck, Inbox, Loader2 } from 'lucide-react'
import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { MotionPage } from '../../components/MotionPage'
import { useAuth } from '../../context/AuthContext'
import { EmptyState, PageHeader } from '../../components/ui'
import { fetchNotifications, markAllNotificationsRead, markNotificationRead, type NotificationItem } from '../../services/notificationService'

export function NotificationsPage() {
  const { user } = useAuth()
  const workspace = user?.role === 'SYSTEM_ADMIN' ? 'system-admin' : user?.role === 'DEPARTMENT_ADMIN' ? 'department-admin' : user?.role === 'WORKER' ? 'worker' : 'citizen'
  const [items, setItems] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const load = useCallback(() => { setLoading(true); setError(''); fetchNotifications().then(page => setItems(page.content)).catch(e => setError(e instanceof Error ? e.message : 'Unable to load notifications.')).finally(() => setLoading(false)) }, [])
  useEffect(() => { load() }, [load])
  async function readOne(id: number) { try { const updated = await markNotificationRead(id); setItems(current => current.map(item => item.id === id ? updated : item)) } catch (e) { setError(e instanceof Error ? e.message : 'Unable to update notification.') } }
  async function readAll() { setBusy(true); setError(''); try { await markAllNotificationsRead(); setItems(current => current.map(item => ({ ...item, read: true }))) } catch (e) { setError(e instanceof Error ? e.message : 'Unable to update notifications.') } finally { setBusy(false) } }
  return <MotionPage><div className="dashboard-content"><PageHeader title="Notifications" description="Updates about your complaints and assigned work." actions={<button className="button button-secondary button-small" onClick={() => void readAll()} disabled={busy || !items.some(item => !item.read)} type="button"><CheckCheck size={15} /> Mark all read</button>} />
    {error && <p className="form-message form-error" role="alert">{error}</p>}
    {loading ? <p className="loading-inline"><Loader2 size={17} className="spin" /> Loading notifications…</p> : items.length === 0 ? <EmptyState icon={<Inbox size={25} />} title="You're all caught up" description="New complaint and work updates will appear here." /> : <div className="notification-list">{items.map(item => <article className={`notification-item${item.read ? '' : ' notification-item-unread'}`} key={item.id}>
      <span className={`notification-dot${item.read ? ' notification-dot-read' : ''}`} />
      <div className="notification-body"><h2 className="notification-title">{item.title}</h2><p className="notification-message">{item.message}</p><p className="notification-meta">{new Date(item.createdAt).toLocaleString()}</p>{item.complaintPublicId && <Link className="text-link" to={`/dashboard/${workspace}/${workspace === 'worker' ? 'tasks' : 'complaints'}/${encodeURIComponent(item.complaintPublicId)}`}>View complaint</Link>}</div>
      {!item.read && <button className="button button-secondary button-small" type="button" onClick={() => void readOne(item.id)}>Mark read</button>}
    </article>)}</div>}
  </div></MotionPage>
}
