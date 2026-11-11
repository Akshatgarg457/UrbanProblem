/** Department API adapter. The backend exposes paginated reads and PUT updates. */
import type { Department } from '../types'
import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'
interface DepartmentDto { id: number; name: string; description?: string; headEmail?: string; status: 'ACTIVE' | 'INACTIVE'; workerCount: number; complaintCount: number; createdAt: string; updatedAt: string }
interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
const mapDepartment = (d: DepartmentDto): Department => ({ id: d.id, name: d.name, workerCount: d.workerCount, activeComplaintCount: d.complaintCount, active: d.status === 'ACTIVE' })
export async function fetchDepartments(): Promise<Department[]> {
  try { const page = await apiClient.get<Page<DepartmentDto>>('/departments', { params: { page: 0, size: 100 } }); return page.data.content.map(mapDepartment) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function fetchDepartmentById(id: number): Promise<Department> {
  try { return mapDepartment((await apiClient.get<DepartmentDto>(`/departments/${id}`)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export interface CreateDepartmentRequest { name: string; description?: string; headEmail: string }
export async function createDepartment(data: CreateDepartmentRequest): Promise<Department> {
  try { return mapDepartment((await apiClient.post<DepartmentDto>('/departments', data)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export interface UpdateDepartmentRequest { name: string; description?: string; headEmail: string }
export async function updateDepartment(id: number, data: UpdateDepartmentRequest): Promise<Department> {
  try { return mapDepartment((await apiClient.put<DepartmentDto>(`/departments/${id}`, data)).data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
export async function setDepartmentActive(id: number, active: boolean): Promise<Department> {
  try { return mapDepartment((await apiClient.put<DepartmentDto>(`/departments/${id}/status`, null, { params: { status: active ? 'ACTIVE' : 'INACTIVE' } })).data) } catch (e) { throw new Error(getApiErrorMessage(e)) }
}
