import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'

export interface NotificationItem { id: number; title: string; message: string; complaintPublicId?: string; read: boolean; createdAt: string }
interface NotificationPage { content: NotificationItem[]; totalElements: number; totalPages: number; number: number; size: number }
interface UnreadCount { count: number }
export async function fetchNotifications(page = 0): Promise<NotificationPage> {
  try { return (await apiClient.get<NotificationPage>('/notifications/my', { params: { page, size: 30, sort: 'createdAt,desc' } })).data }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}
export async function markNotificationRead(id: number): Promise<NotificationItem> {
  try { return (await apiClient.put<NotificationItem>(`/notifications/${id}/read`)).data }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}
export async function markAllNotificationsRead(): Promise<UnreadCount> {
  try { return (await apiClient.put<UnreadCount>('/notifications/read-all')).data }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}
export async function fetchUnreadNotificationCount(): Promise<number> {
  try { return (await apiClient.get<UnreadCount>('/notifications/my/unread-count')).data.count }
  catch { return 0 }
}
