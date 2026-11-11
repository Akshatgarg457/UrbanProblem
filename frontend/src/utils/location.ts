import { OSM_WEB_BASE_URL } from '../config/maps'
import type { ComplaintStatus } from '../types'

export function isValidLatitude(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= -90 && value <= 90
}

export function isValidLongitude(value: number | null | undefined): value is number {
  return typeof value === 'number' && Number.isFinite(value) && value >= -180 && value <= 180
}

export function isValidCoordinate(
  latitude: number | null | undefined,
  longitude: number | null | undefined,
): boolean {
  return isValidLatitude(latitude) && isValidLongitude(longitude)
}

export function formatCoordinatePair(latitude: number, longitude: number, digits = 5): string {
  return `${latitude.toFixed(digits)}, ${longitude.toFixed(digits)}`
}

export function osmViewUrl(latitude: number, longitude: number, zoom = 17): string {
  return `${OSM_WEB_BASE_URL}/?mlat=${latitude}&mlon=${longitude}#map=${zoom}/${latitude}/${longitude}`
}

/** Opens OpenStreetMap's own directions UI. No routing service or API key is required. */
export function osmDirectionsUrl(latitude: number, longitude: number): string {
  return `${OSM_WEB_BASE_URL}/directions?to=${latitude}%2C${longitude}`
}

/** Tonal steps of the single azure accent, used by map markers and legends. */
export function complaintStatusColor(status: ComplaintStatus): string {
  switch (status) {
    case 'SUBMITTED': return '#b6d7ff'
    case 'ACKNOWLEDGED': return '#86bbff'
    case 'ASSIGNED': return '#559dff'
    case 'IN_PROGRESS': return '#2b7cf2'
    case 'SUBMITTED_FOR_REVIEW': return '#d9eaff'
    case 'RESOLVED': return '#1a63cf'
    case 'REWORK_REQUIRED': return '#7fa8d4'
    case 'REJECTED': return '#eef4ff'
    case 'CLOSED': return '#6f7d88'
    default: return '#6f7d88'
  }
}
