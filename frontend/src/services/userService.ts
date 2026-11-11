/** User administration adapter for the backend's explicit worker/admin endpoints. */
import type { User, UserRole } from '../types'
import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'
interface WorkerDto { id: number; email: string; firstName: string; lastName: string; phoneNumber?: string; departmentId: number; departmentName: string; status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'; createdAt: string }
interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
const mapWorker = (w: WorkerDto): User => ({ id: w.id, email: w.email, firstName: w.firstName, lastName: w.lastName, phone: w.phoneNumber, role: 'WORKER', department: w.departmentName, departmentId: w.departmentId, active: w.status === 'ACTIVE', createdAt: w.createdAt })
export interface CreateWorkerRequest { firstName: string; lastName: string; email: string; phoneNumber: string; password: string; departmentId: number }
export async function createWorker(data: CreateWorkerRequest): Promise<User> { try { return mapWorker((await apiClient.post<WorkerDto>('/workers', data)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) } }
export async function fetchDepartmentWorkers(departmentId?: number): Promise<User[]> {
  if (!departmentId) throw new Error('A department ID is required to list workers.')
  try { const page = await apiClient.get<Page<WorkerDto>>(`/workers/department/${departmentId}`, { params: { page: 0, size: 100, activeOnly: false } }); return page.data.content.map(mapWorker) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function fetchWorkerById(id: number): Promise<User> { try { return mapWorker((await apiClient.get<WorkerDto>(`/workers/${id}`)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) } }
export async function fetchPendingWorkers(): Promise<User[]> {
  try { const page = await apiClient.get<Page<WorkerDto>>('/workers/pending', { params: { page: 0, size: 100, sort: 'createdAt,asc' } }); return page.data.content.map(mapWorker) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function approveWorker(id: number): Promise<User> { try { return mapWorker((await apiClient.post<WorkerDto>(`/workers/${id}/approve`)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) } }
export async function rejectWorker(id: number): Promise<User> { try { return mapWorker((await apiClient.post<WorkerDto>(`/workers/${id}/reject`)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) } }
export async function setWorkerActive(id: number, active: boolean): Promise<User> { try { return mapWorker((await apiClient.put<WorkerDto>(`/workers/${id}/status`, null, { params: { status: active ? 'ACTIVE' : 'INACTIVE' } })).data) } catch (e) { throw new Error(getApiErrorMessage(e)) } }
export interface CreateDepartmentAdminRequest { firstName: string; lastName: string; email: string; password: string; departmentId: number }
export async function createDepartmentAdmin(data: CreateDepartmentAdminRequest): Promise<User> {
  try { const r = await apiClient.post<{ id: number; email: string; firstName: string; lastName: string; role: UserRole; departmentId: number; status: string; createdAt: string }>('/admin/department-admins', data); return { id: r.data.id, email: r.data.email, firstName: r.data.firstName, lastName: r.data.lastName, role: r.data.role, departmentId: r.data.departmentId, active: r.data.status === 'ACTIVE', createdAt: r.data.createdAt } } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
// NOTE: The backend intentionally does not expose generic /users CRUD routes.
// User management goes through the dedicated worker/admin endpoints above, and
// profile reads/updates go through services/profileService.ts (role-aware /me endpoints).
