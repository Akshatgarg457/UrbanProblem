import * as L from 'leaflet'
import { AlertCircle, CheckCircle2, ExternalLink, LocateFixed, Loader2, MapPin, Search, X } from 'lucide-react'
import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { MapContainer, Marker } from 'react-leaflet'
import { DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM } from '../../config/maps'
import {
  geocodingErrorMessage,
  reverseGeocode,
  searchAddresses,
  type AddressSuggestion,
} from '../../services/geocodingService'
import {
  formatCoordinatePair,
  isValidLatitude,
  isValidLongitude,
  osmViewUrl,
} from '../../utils/location'
import { MapFocus, MapClickHandler, KeepMarkerInView, OsmTileLayer, type MapFocusTarget } from '../map/MapPrimitives'
import { pinIcon } from '../map/mapIcons'

export interface LocationValue {
  address: string
  city: string
  state: string
  postalCode: string
  latitude: string
  longitude: string
}

type GeoState = 'idle' | 'loading' | 'success' | 'error'

interface LocationPickerProps {
  value: LocationValue
  onChange: (value: LocationValue) => void
  mapHeight?: number
}

function coordinateText(latitude: number, longitude: number) {
  return {
    latitude: String(Number(latitude.toFixed(7))),
    longitude: String(Number(longitude.toFixed(7))),
  }
}

export function LocationPicker({ value, onChange, mapHeight = 340 }: LocationPickerProps) {
  const [query, setQuery] = useState('')
  const [suggestions, setSuggestions] = useState<AddressSuggestion[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState('')
  const [searchOpen, setSearchOpen] = useState(false)
  const [highlighted, setHighlighted] = useState(-1)
  const [locating, setLocating] = useState(false)
  const [geoState, setGeoState] = useState<GeoState>('idle')
  const [geoMessage, setGeoMessage] = useState('')
  const [focus, setFocus] = useState<MapFocusTarget | null>(null)

  const valueRef = useRef(value)
  const onChangeRef = useRef(onChange)
  const reverseAbort = useRef<AbortController | null>(null)
  const searchAbort = useRef<AbortController | null>(null)
  const skipSearchRef = useRef('')
  const queryRef = useRef('')
  const initialFocusApplied = useRef(false)
  const listboxId = useId()
  const searchInputId = useId()

  useEffect(() => { valueRef.current = value }, [value])
  useEffect(() => { onChangeRef.current = onChange }, [onChange])
  useEffect(() => { queryRef.current = query }, [query])

  useEffect(() => () => {
    reverseAbort.current?.abort()
    searchAbort.current?.abort()
  }, [])

  const latitudeText = value.latitude.trim()
  const longitudeText = value.longitude.trim()
  const hasLatitude = latitudeText !== ''
  const hasLongitude = longitudeText !== ''
  const latitude = hasLatitude ? Number(latitudeText) : null
  const longitude = hasLongitude ? Number(longitudeText) : null

  const coordinateError = hasLatitude !== hasLongitude
    ? 'Enter both latitude and longitude, or clear both.'
    : latitude !== null && !isValidLatitude(latitude)
      ? 'Latitude must be a number between -90 and 90.'
      : longitude !== null && !isValidLongitude(longitude)
        ? 'Longitude must be a number between -180 and 180.'
        : ''

  const markerPosition = useMemo<[number, number] | null>(() => {
    if (coordinateError || latitude === null || longitude === null) return null
    return [latitude, longitude]
  }, [coordinateError, latitude, longitude])

  const initialCenter = useMemo<[number, number]>(() => {
    const lat = Number(value.latitude)
    const lng = Number(value.longitude)
    if (value.latitude.trim() && value.longitude.trim() && isValidLatitude(lat) && isValidLongitude(lng)) {
      return [lat, lng]
    }
    return DEFAULT_MAP_CENTER
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const markerIcon = useMemo(() => pinIcon('#2b7cf2', true), [])

  useEffect(() => {
    if (initialFocusApplied.current || latitude === null || longitude === null || coordinateError) return
    initialFocusApplied.current = true
    setFocus({ latitude, longitude, zoom: 16, token: Date.now() })
  }, [latitude, longitude, coordinateError])

  // Debounced search: no request until typing pauses, and results are cached in the service.
  useEffect(() => {
    const normalized = query.trim()
    if (skipSearchRef.current && normalized === skipSearchRef.current) {
      skipSearchRef.current = ''
      return
    }
    if (normalized.length < 3) {
      searchAbort.current?.abort()
      setSuggestions([])
      setSearching(false)
      setSearchOpen(false)
      setSearchError('')
      return
    }

    const timer = window.setTimeout(() => {
      searchAbort.current?.abort()
      const controller = new AbortController()
      searchAbort.current = controller
      setSearching(true)
      setSearchError('')
      searchAddresses(normalized, controller.signal)
        .then(results => {
          if (controller.signal.aborted || queryRef.current.trim() !== normalized) return
          setSuggestions(results)
          setHighlighted(results.length > 0 ? 0 : -1)
          setSearchOpen(true)
        })
        .catch(error => {
          if (controller.signal.aborted || queryRef.current.trim() !== normalized) return
          const message = geocodingErrorMessage(error)
          if (!message) return
          setSuggestions([])
          setSearchError(message)
          setSearchOpen(true)
        })
        .finally(() => {
          if (!controller.signal.aborted) setSearching(false)
        })
    }, 650)

    return () => window.clearTimeout(timer)
  }, [query])

  function fillAddressFromMap(lat: number, lng: number) {
    reverseAbort.current?.abort()
    const controller = new AbortController()
    reverseAbort.current = controller
    setGeoState('loading')
    setGeoMessage('Finding the address for this point…')

    reverseGeocode(lat, lng, controller.signal)
      .then(result => {
        if (controller.signal.aborted) return
        if (!result) {
          setGeoState('error')
          setGeoMessage('No address was found for this point. You can type the address manually below.')
          return
        }
        const current = valueRef.current
        onChangeRef.current({
          address: result.address || current.address,
          city: result.city || current.city,
          state: result.state || current.state,
          postalCode: result.postalCode || current.postalCode,
          ...coordinateText(lat, lng),
        })
        setGeoState('success')
        setGeoMessage('Address filled from OpenStreetMap. Edit it below if needed.')
      })
      .catch(error => {
        if (controller.signal.aborted) return
        const message = geocodingErrorMessage(error)
        if (!message) return
        setGeoState('error')
        setGeoMessage(message)
      })
  }

  function applyCoordinates(lat: number, lng: number, reverse = true) {
    if (!isValidLatitude(lat) || !isValidLongitude(lng)) return
    setFocus({ latitude: lat, longitude: lng, zoom: 16, token: Date.now() })
    onChangeRef.current({ ...valueRef.current, ...coordinateText(lat, lng) })
    if (reverse) fillAddressFromMap(lat, lng)
    else {
      setGeoState('success')
      setGeoMessage('Coordinates updated.')
    }
  }

  function handleDragEnd(event: L.LeafletEvent) {
    const point = (event.target as L.Marker).getLatLng()
    applyCoordinates(point.lat, point.lng, true)
  }

  function selectSuggestion(suggestion: AddressSuggestion) {
    skipSearchRef.current = suggestion.label
    setQuery(suggestion.label)
    setSuggestions([])
    setSearchOpen(false)
    setSearchError('')
    setHighlighted(-1)
    setFocus({ latitude: suggestion.latitude, longitude: suggestion.longitude, zoom: 17, token: Date.now() })
    onChangeRef.current({
      address: suggestion.address,
      city: suggestion.city,
      state: suggestion.state,
      postalCode: suggestion.postalCode,
      ...coordinateText(suggestion.latitude, suggestion.longitude),
    })
    setGeoState('success')
    setGeoMessage('Address selected from the OpenStreetMap search results.')
  }

  function handleSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Escape') {
      setSearchOpen(false)
      return
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      if (!searchOpen) setSearchOpen(true)
      setHighlighted(current => Math.min(current + 1, Math.max(suggestions.length - 1, 0)))
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setHighlighted(current => Math.max(current - 1, 0))
      return
    }
    if (event.key === 'Enter') {
      // The picker lives inside the profile/complaint form; never submit it from a search.
      event.preventDefault()
      if (searchOpen && suggestions.length > 0) {
        selectSuggestion(suggestions[highlighted] ?? suggestions[0])
      }
    }
  }

  function clearSearch() {
    searchAbort.current?.abort()
    skipSearchRef.current = ''
    setQuery('')
    setSuggestions([])
    setSearchOpen(false)
    setSearchError('')
    setSearching(false)
    setHighlighted(-1)
  }

  function useCurrentLocation() {
    if (!('geolocation' in navigator)) {
      setGeoState('error')
      setGeoMessage('This browser does not support location detection. Search for your address or click the map instead.')
      return
    }
    setLocating(true)
    setGeoState('loading')
    setGeoMessage('Detecting your current location…')
    navigator.geolocation.getCurrentPosition(
      position => {
        setLocating(false)
        applyCoordinates(position.coords.latitude, position.coords.longitude, true)
      },
      error => {
        setLocating(false)
        // 1 = permission denied, 3 = timeout
        const message = error.code === 1
          ? 'Location permission was denied. Search for your address, click the map, or type it manually — no permission is required for those.'
          : error.code === 3
            ? 'Location detection timed out. Try again, or pick the location directly on the map.'
            : 'Your current location could not be determined. Search or click the map instead.'
        setGeoState('error')
        setGeoMessage(message)
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 },
    )
  }

  function updateCoordinatesOnMap() {
    if (coordinateError) {
      setGeoState('error')
      setGeoMessage(coordinateError)
      return
    }
    if (latitude === null || longitude === null) {
      setGeoState('error')
      setGeoMessage('Enter latitude and longitude first.')
      return
    }
    applyCoordinates(latitude, longitude, true)
  }

  function setField(field: keyof LocationValue, next: string) {
    onChangeRef.current({ ...value, [field]: next })
    if (geoState !== 'idle') {
      setGeoState('idle')
      setGeoMessage('')
    }
  }

  const showSuggestions = searchOpen && query.trim().length >= 3

  return (
    <div className="location-picker">
      <div className="location-picker-toolbar">
        <div className="location-picker-search map-search">
          <Search aria-hidden="true" size={16} />
          <input
            id={searchInputId}
            className="field-input"
            type="search"
            value={query}
            onChange={event => setQuery(event.target.value)}
            onFocus={() => { if (suggestions.length > 0) setSearchOpen(true) }}
            onKeyDown={handleSearchKeyDown}
            placeholder="Search for an address, street or area"
            aria-label="Search for an address on OpenStreetMap"
            aria-autocomplete="list"
            aria-controls={listboxId}
            aria-expanded={showSuggestions}
            autoComplete="off"
          />
          {searching && <Loader2 aria-hidden="true" className="spin map-search-spinner" size={15} />}
          {query && !searching && (
            <button className="map-search-clear" type="button" onClick={clearSearch} aria-label="Clear address search">
              <X size={14} />
            </button>
          )}
          {showSuggestions && (
            <div className="map-suggestions" id={listboxId} role="listbox" aria-label="Address suggestions">
              {searchError && <p className="map-suggestion-empty" role="alert">{searchError}</p>}
              {!searchError && suggestions.length === 0 && !searching && (
                <p className="map-suggestion-empty">No matching addresses. Click the map or type the address manually.</p>
              )}
              {suggestions.map((suggestion, index) => (
                <button
                  key={suggestion.id}
                  id={`${listboxId}-${index}`}
                  className={`map-suggestion${index === highlighted ? ' is-active' : ''}`}
                  type="button"
                  role="option"
                  aria-selected={index === highlighted}
                  onMouseEnter={() => setHighlighted(index)}
                  onClick={() => selectSuggestion(suggestion)}
                >
                  <MapPin aria-hidden="true" size={14} />
                  <span>{suggestion.label}</span>
                </button>
              ))}
            </div>
          )}
        </div>
        <button className="button button-secondary button-small" type="button" onClick={useCurrentLocation} disabled={locating}>
          {locating ? <Loader2 size={15} className="spin" /> : <LocateFixed size={15} />}
          {locating ? 'Locating…' : 'Use my location'}
        </button>
      </div>

      <div className="map-shell">
        <MapContainer
          center={initialCenter}
          zoom={DEFAULT_MAP_ZOOM}
          className="location-picker-map"
          style={{ height: mapHeight }}
          scrollWheelZoom
        >
          <OsmTileLayer />
          <MapClickHandler onPick={(lat, lng) => applyCoordinates(lat, lng, true)} />
          <MapFocus target={focus} />
          {markerPosition && latitude !== null && longitude !== null && (
            <>
              <Marker
                position={markerPosition}
                icon={markerIcon}
                draggable
                eventHandlers={{ dragend: handleDragEnd }}
                title="Selected location. Drag to adjust."
              />
              <KeepMarkerInView latitude={latitude} longitude={longitude} />
            </>
          )}
        </MapContainer>
        {!markerPosition && (
          <div className="map-empty-overlay" aria-hidden="true">
            <MapPin size={18} />
            <span>Search, click the map, or use your current location</span>
          </div>
        )}
      </div>

      {geoMessage && (
        <p
          className={`map-status ${geoState === 'error' ? 'map-status-error' : geoState === 'success' ? 'map-status-success' : ''}`}
          role={geoState === 'error' ? 'alert' : 'status'}
        >
          {geoState === 'loading' && <Loader2 aria-hidden="true" size={14} className="spin" />}
          {geoState === 'success' && <CheckCircle2 aria-hidden="true" size={14} />}
          {geoState === 'error' && <AlertCircle aria-hidden="true" size={14} />}
          <span>{geoMessage}</span>
        </p>
      )}

      <div className="location-picker-fields">
        <label className="location-picker-field location-picker-field-wide">
          <span>Address</span>
          <input
            className="field-input"
            value={value.address}
            onChange={event => setField('address', event.target.value)}
            placeholder="Street, area or full address"
            autoComplete="street-address"
          />
        </label>
        <label className="location-picker-field">
          <span>City</span>
          <input className="field-input" value={value.city} onChange={event => setField('city', event.target.value)} placeholder="City" autoComplete="address-level2" />
        </label>
        <label className="location-picker-field">
          <span>State</span>
          <input className="field-input" value={value.state} onChange={event => setField('state', event.target.value)} placeholder="State" autoComplete="address-level1" />
        </label>
        <label className="location-picker-field">
          <span>Postal code</span>
          <input className="field-input" value={value.postalCode} onChange={event => setField('postalCode', event.target.value)} placeholder="Postal code" autoComplete="postal-code" />
        </label>
        <label className="location-picker-field">
          <span>Latitude</span>
          <input
            className={`field-input${coordinateError ? ' field-input-error' : ''}`}
            type="number"
            step="any"
            value={value.latitude}
            onChange={event => setField('latitude', event.target.value)}
            placeholder="e.g. 22.7196"
          />
        </label>
        <label className="location-picker-field">
          <span>Longitude</span>
          <input
            className={`field-input${coordinateError ? ' field-input-error' : ''}`}
            type="number"
            step="any"
            value={value.longitude}
            onChange={event => setField('longitude', event.target.value)}
            placeholder="e.g. 75.8577"
          />
        </label>
      </div>

      {coordinateError && <p className="field-error" role="alert">{coordinateError}</p>}

      <div className="location-picker-footer">
        <button className="button button-secondary button-small" type="button" onClick={updateCoordinatesOnMap}>
          <MapPin size={14} /> Show typed coordinates on map
        </button>
        {markerPosition && latitude !== null && longitude !== null && (
          <div className="location-picker-coords">
            <MapPin aria-hidden="true" size={13} />
            <span>{formatCoordinatePair(latitude, longitude)}</span>
            <a href={osmViewUrl(latitude, longitude)} target="_blank" rel="noopener noreferrer">
              Open in OpenStreetMap <ExternalLink size={12} />
            </a>
          </div>
        )}
      </div>

      <p className="location-picker-hint">
        Pick a point on the map, drag the marker, or search. Browser location permission is optional — manual address entry always works.
      </p>
    </div>
  )
}
