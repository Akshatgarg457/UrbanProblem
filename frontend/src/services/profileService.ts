/**
 * Role-aware profile adapter.
 * The backend exposes per-role profile endpoints, so this service picks the
 * correct URL from the authenticated user's role:
 *   CITIZEN                       → GET/PUT /citizens/me
 *   WORKER                        → GET/PUT /workers/me
 *   DEPARTMENT_ADMIN/SYSTEM_ADMIN → GET/PUT /admin/me
 */
import type { UserRole } from '../types'
import { apiClient } from './apiClient'
import { getApiErrorMessage } from './apiErrors'

export interface SavedLocation {
  address: string
  city: string
  state: string
  postalCode: string
  latitude?: number | null
  longitude?: number | null
}

export interface Profile {
  id: number
  email: string
  firstName: string
  lastName: string
  phone: string
  role: UserRole
  departmentName?: string
  createdAt?: string
  status?: string
  location?: SavedLocation
}

export interface UpdateProfileRequest {
  firstName: string
  lastName: string
  phoneNumber: string
  location?: SavedLocation
}

/** Defensive DTOs — the backend wraps optional fields differently per role. */
interface ProfileDto {
  id?: number
  email?: string
  firstName?: string
  lastName?: string
  phoneNumber?: string
  phone?: string
  address?: string | null
  city?: string | null
  state?: string | null
  postalCode?: string | null
  latitude?: number | null
  longitude?: number | null
  departmentId?: number | null
  departmentName?: string
  status?: string
  role?: UserRole
  createdAt?: string
}

function endpointFor(role: UserRole): string {
  switch (role) {
    case 'CITIZEN': return '/citizens/me'
    case 'WORKER': return '/workers/me'
    case 'DEPARTMENT_ADMIN':
    case 'SYSTEM_ADMIN': return '/admin/me'
  }
}

function mapProfile(dto: ProfileDto, role: UserRole): Profile {
  const hasLocation = dto.address != null || dto.city != null || dto.state != null
    || dto.postalCode != null || dto.latitude != null || dto.longitude != null
  return {
    id: dto.id ?? 0,
    email: dto.email ?? '',
    firstName: dto.firstName ?? '',
    lastName: dto.lastName ?? '',
    phone: dto.phoneNumber ?? dto.phone ?? '',
    role: dto.role ?? role,
    departmentName: dto.departmentName,
    createdAt: dto.createdAt,
    status: dto.status,
    location: hasLocation
      ? {
        address: dto.address ?? '',
        city: dto.city ?? '',
        state: dto.state ?? '',
        postalCode: dto.postalCode ?? '',
        latitude: dto.latitude ?? null,
        longitude: dto.longitude ?? null,
      }
      : undefined,
  }
}

export async function fetchMyProfile(role: UserRole): Promise<Profile> {
  try {
    const response = await apiClient.get<ProfileDto>(endpointFor(role))
    return mapProfile(response.data, role)
  } catch (error) { throw new Error(getApiErrorMessage(error)) }
}

export async function updateMyProfile(role: UserRole, data: UpdateProfileRequest): Promise<Profile> {
  try {
    // Admin profile DTOs intentionally do not expose a phone number or location.
    const payload = role === 'DEPARTMENT_ADMIN' || role === 'SYSTEM_ADMIN'
      ? { firstName: data.firstName, lastName: data.lastName }
      : {
        firstName: data.firstName,
        lastName: data.lastName,
        phoneNumber: data.phoneNumber,
        ...(data.location
          ? {
            address: data.location.address || null,
            city: data.location.city || null,
            state: data.location.state || null,
            postalCode: data.location.postalCode || null,
            latitude: data.location.latitude ?? null,
            longitude: data.location.longitude ?? null,
          }
          : {}),
      }
    const response = await apiClient.put<ProfileDto>(endpointFor(role), payload)
    return mapProfile(response.data, role)
  } catch (error) { throw new Error(getApiErrorMessage(error)) }
}
