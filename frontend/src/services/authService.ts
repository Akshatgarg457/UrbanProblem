/** Authentication adapter for the Spring Boot API. */
import axios from 'axios'
import type { AuthUser, UserRole } from '../types'
import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'

export interface LoginRequest {
  /** Sent as `email`; the API accepts a full address or its username (local part). */
  email: string
  password: string
  workspace?: 'citizen' | 'worker' | 'admin'
}
export interface RegisterRequest { firstName: string; lastName: string; email: string; phoneNumber: string; password: string; securityQuestion: string; securityAnswer: string }
export interface WorkerRegisterRequest extends RegisterRequest { departmentId: number }
export interface ForgotPasswordQuestionRequest { identifier: string }
export interface ForgotPasswordResetRequest { identifier: string; answer: string; newPassword: string }
export interface SecurityQuestionResponse { question: string | null }
export interface PublicDepartment { id: number; name: string; description?: string }
export interface BackendAuthResponse {
  id: number
  email: string
  firstName: string
  lastName: string
  role: UserRole
  active: boolean
  createdAt: string
  departmentName?: string | null
  departmentId?: number | null
}

function mapAuthResponse(data: BackendAuthResponse): AuthUser {
  return {
    id: data.id,
    email: data.email,
    firstName: data.firstName,
    lastName: data.lastName,
    role: data.role,
    departmentId: data.departmentId ?? undefined,
    department: data.departmentName ?? undefined,
    active: data.active,
    createdAt: data.createdAt,
  }
}

export async function initializeCsrfToken(): Promise<void> {
  try { await apiClient.get('/auth/csrf') }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function login(data: LoginRequest): Promise<AuthUser> {
  try {
    const response = await apiClient.post<BackendAuthResponse>('/auth/login', data)
    return mapAuthResponse(response.data)
  } catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function register(data: RegisterRequest): Promise<void> {
  try {
    await apiClient.post('/auth/register', data)
  } catch (error) { throw new Error(getApiErrorMessage(error)) }
}

/** Creates a pending worker application. No token is issued until an administrator approves it. */
export async function registerWorker(data: WorkerRegisterRequest): Promise<void> {
  try { await apiClient.post('/auth/register/worker', data) }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function fetchPublicDepartments(): Promise<PublicDepartment[]> {
  try { return (await apiClient.get<PublicDepartment[]>('/auth/departments')).data }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function fetchSecurityQuestion(data: ForgotPasswordQuestionRequest): Promise<string | null> {
  try {
    const response = await apiClient.post<SecurityQuestionResponse>('/auth/forgot-password/question', data)
    return response.data.question ?? null
  } catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function resetPasswordWithSecurityAnswer(data: ForgotPasswordResetRequest): Promise<void> {
  try { await apiClient.post('/auth/forgot-password/reset', data) }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function logout(): Promise<void> {
  try { await apiClient.post('/auth/logout') }
  catch (error) { throw new Error(getApiErrorMessage(error)) }
}

/** Loads the current principal from the server-side session cookie. */
export async function getCurrentUser(): Promise<AuthUser | null> {
  try {
    const response = await apiClient.get<BackendAuthResponse>('/auth/me')
    return mapAuthResponse(response.data)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) return null
    throw new Error(getApiErrorMessage(error))
  }
}
