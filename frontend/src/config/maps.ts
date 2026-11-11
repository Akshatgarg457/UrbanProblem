/**
 * Map provider configuration.
 *
 * Defaults use the public OpenStreetMap tile server and the public Nominatim
 * geocoder. Both are free and require no API key. For higher-volume deployments
 * point these variables at a self-hosted or commercial OSM-compatible provider.
 */
const env = import.meta.env

export const OSM_TILE_URL: string =
  env.VITE_OSM_TILE_URL || 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'

export const OSM_ATTRIBUTION: string =
  env.VITE_OSM_TILE_ATTRIBUTION ||
  '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors'

export const OSM_TILE_MAX_ZOOM = 19

export const NOMINATIM_BASE_URL: string = (
  env.VITE_NOMINATIM_BASE_URL || 'https://nominatim.openstreetmap.org'
).replace(/\/$/, '')

/** Optional contact address sent with Nominatim requests, as requested by their usage policy. */
export const NOMINATIM_EMAIL: string = env.VITE_NOMINATIM_EMAIL || ''

export const OSM_WEB_BASE_URL: string = (
  env.VITE_OSM_WEB_BASE_URL || 'https://www.openstreetmap.org'
).replace(/\/$/, '')

/** Used only as the initial map viewport. A marker is never placed without real coordinates. */
export const DEFAULT_MAP_CENTER: [number, number] = [22.7196, 75.8577]
export const DEFAULT_MAP_ZOOM = 13
