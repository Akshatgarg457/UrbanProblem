/**
 * Nominatim geocoding adapter.
 *
 * Public Nominatim is rate limited to roughly one request per second and must
 * not be used for bulk autocomplete. This module serialises every request,
 * keeps a short-lived cache, and relies on the caller to debounce typing.
 */
import { NOMINATIM_BASE_URL, NOMINATIM_EMAIL } from '../config/maps'
import { isValidCoordinate } from '../utils/location'

export interface AddressSuggestion {
  id: string
  label: string
  address: string
  city: string
  state: string
  postalCode: string
  latitude: number
  longitude: number
}

export type GeocodingFailure = 'rate-limit' | 'network'

export class GeocodingError extends Error {
  readonly kind: GeocodingFailure

  constructor(message: string, kind: GeocodingFailure) {
    super(message)
    this.name = 'GeocodingError'
    this.kind = kind
  }
}

interface NominatimAddressDetails {
  house_number?: string
  road?: string
  pedestrian?: string
  footway?: string
  neighbourhood?: string
  suburb?: string
  hamlet?: string
  village?: string
  town?: string
  city?: string
  municipality?: string
  county?: string
  state?: string
  state_district?: string
  postcode?: string
}

interface NominatimPlace {
  place_id?: number
  display_name?: string
  lat?: string
  lon?: string
  address?: NominatimAddressDetails
}

interface NominatimReverse extends NominatimPlace {
  error?: string
}

const MIN_REQUEST_INTERVAL_MS = 1_100
const SEARCH_CACHE_TTL_MS = 10 * 60 * 1000
const REVERSE_CACHE_TTL_MS = 30 * 60 * 1000
const MAX_CACHE_ENTRIES = 80

const cache = new Map<string, { value: unknown; expiresAt: number }>()

function readCache<T>(key: string): T | undefined {
  const entry = cache.get(key)
  if (!entry) return undefined
  if (entry.expiresAt <= Date.now()) {
    cache.delete(key)
    return undefined
  }
  return entry.value as T
}

function writeCache(key: string, value: unknown, ttlMs: number) {
  if (cache.size >= MAX_CACHE_ENTRIES) {
    const oldest = cache.keys().next().value
    if (oldest !== undefined) cache.delete(oldest)
  }
  cache.set(key, { value, expiresAt: Date.now() + ttlMs })
}

function sleep(ms: number) {
  return new Promise<void>(resolve => setTimeout(resolve, ms))
}

let requestChain: Promise<void> = Promise.resolve()
let lastRequestAt = 0

/** Serialises requests and enforces Nominatim's ~1 request/second limit. */
function enqueueRequest<T>(task: () => Promise<T>): Promise<T> {
  const result = requestChain.then(async () => {
    const wait = Math.max(0, lastRequestAt + MIN_REQUEST_INTERVAL_MS - Date.now())
    if (wait > 0) await sleep(wait)
    lastRequestAt = Date.now()
    return task()
  })
  requestChain = result.then(() => undefined, () => undefined)
  return result
}

async function requestJson<T>(url: string, signal: AbortSignal): Promise<T> {
  let response: Response
  try {
    response = await fetch(url, {
      signal,
      headers: { Accept: 'application/json' },
      referrerPolicy: 'strict-origin-when-cross-origin',
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new GeocodingError(
      'Address lookup could not reach OpenStreetMap. Check your connection, or pick the location on the map and type the address manually.',
      'network',
    )
  }

  if (response.status === 429 || response.status === 503) {
    throw new GeocodingError(
      'The free OpenStreetMap address service is busy or rate limited. Wait a moment and try again, or pick the location directly on the map.',
      'rate-limit',
    )
  }
  if (!response.ok) {
    throw new GeocodingError(
      'Address lookup is unavailable right now. You can still pick the location on the map or type the address manually.',
      'network',
    )
  }

  return (await response.json()) as T
}

function truncate(value: string, max: number): string {
  return value.length > max ? value.slice(0, max).trim() : value
}

function streetAddress(details: NominatimAddressDetails, displayName: string): string {
  const street = [details.house_number, details.road || details.pedestrian || details.footway]
    .filter(Boolean)
    .join(' ')
  if (street) return truncate(street, 255)
  const area = details.neighbourhood || details.suburb || details.hamlet
    || details.village || details.town || details.city
  if (area) return truncate(area, 255)
  return truncate(displayName.split(',').slice(0, 2).join(',').trim(), 255)
}

function cityOf(details: NominatimAddressDetails): string {
  return details.city || details.town || details.village || details.municipality
    || details.hamlet || details.county || ''
}

function toSuggestion(place: NominatimPlace): AddressSuggestion | null {
  const latitude = Number(place.lat)
  const longitude = Number(place.lon)
  if (!isValidCoordinate(latitude, longitude)) return null
  const details = place.address ?? {}
  const displayName = place.display_name ?? ''
  return {
    id: String(place.place_id ?? `${latitude},${longitude}`),
    label: displayName,
    address: streetAddress(details, displayName),
    city: cityOf(details),
    state: details.state || details.state_district || '',
    postalCode: details.postcode || '',
    latitude,
    longitude,
  }
}

function language(): string {
  return typeof navigator !== 'undefined' && navigator.language ? navigator.language : 'en'
}

function withContactEmail(params: URLSearchParams): URLSearchParams {
  if (NOMINATIM_EMAIL) params.set('email', NOMINATIM_EMAIL)
  return params
}

/** Forward search. Callers must debounce; results are also cached for ten minutes. */
export async function searchAddresses(query: string, signal: AbortSignal): Promise<AddressSuggestion[]> {
  const normalized = query.trim()
  if (normalized.length < 3) return []

  const cacheKey = `search:${normalized.toLowerCase()}`
  const cached = readCache<AddressSuggestion[]>(cacheKey)
  if (cached) return cached

  const params = withContactEmail(new URLSearchParams({
    q: normalized,
    format: 'jsonv2',
    addressdetails: '1',
    limit: '5',
    'accept-language': language(),
  }))

  const places = await enqueueRequest(() =>
    requestJson<NominatimPlace[]>(`${NOMINATIM_BASE_URL}/search?${params.toString()}`, signal),
  )

  const suggestions = places.map(toSuggestion).filter((item): item is AddressSuggestion => item !== null)
  writeCache(cacheKey, suggestions, SEARCH_CACHE_TTL_MS)
  return suggestions
}

/** Reverse geocoding for a selected point. Results are cached by rounded coordinates. */
export async function reverseGeocode(
  latitude: number,
  longitude: number,
  signal: AbortSignal,
): Promise<AddressSuggestion | null> {
  if (!isValidCoordinate(latitude, longitude)) return null

  const roundedLat = Number(latitude.toFixed(5))
  const roundedLng = Number(longitude.toFixed(5))
  const cacheKey = `reverse:${roundedLat},${roundedLng}`
  const cached = readCache<AddressSuggestion | null>(cacheKey)
  if (cached !== undefined) return cached

  const params = withContactEmail(new URLSearchParams({
    lat: String(roundedLat),
    lon: String(roundedLng),
    format: 'jsonv2',
    addressdetails: '1',
    zoom: '18',
    'accept-language': language(),
  }))

  const place = await enqueueRequest(() =>
    requestJson<NominatimReverse>(`${NOMINATIM_BASE_URL}/reverse?${params.toString()}`, signal),
  )

  const suggestion = place && 'error' in place && place.error ? null : toSuggestion(place)
  writeCache(cacheKey, suggestion, REVERSE_CACHE_TTL_MS)
  return suggestion
}

export function geocodingErrorMessage(error: unknown): string {
  if (error instanceof GeocodingError) return error.message
  if (error instanceof DOMException && error.name === 'AbortError') return ''
  return 'Address lookup is unavailable right now. You can still pick the location on the map or type the address manually.'
}
