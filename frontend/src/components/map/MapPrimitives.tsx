import * as L from 'leaflet'
import { useEffect } from 'react'
import { TileLayer, useMap, useMapEvents } from 'react-leaflet'
import { OSM_ATTRIBUTION, OSM_TILE_MAX_ZOOM, OSM_TILE_URL } from '../../config/maps'

export function OsmTileLayer() {
  return (
    <TileLayer
      url={OSM_TILE_URL}
      attribution={OSM_ATTRIBUTION}
      maxZoom={OSM_TILE_MAX_ZOOM}
      detectRetina
    />
  )
}

export function MapClickHandler({ onPick }: { onPick: (latitude: number, longitude: number) => void }) {
  useMapEvents({
    click(event) {
      onPick(event.latlng.lat, event.latlng.lng)
    },
  })
  return null
}

export interface MapFocusTarget {
  latitude: number
  longitude: number
  zoom?: number
  token: number
}

export function MapFocus({ target }: { target: MapFocusTarget | null }) {
  const map = useMap()
  useEffect(() => {
    if (!target) return
    map.flyTo(
      [target.latitude, target.longitude],
      target.zoom ?? Math.max(map.getZoom(), 15),
      { duration: 0.7 },
    )
  }, [target?.token]) // eslint-disable-line react-hooks/exhaustive-deps
  return null
}

/** Pans only when a manually typed coordinate falls outside the visible area. */
export function KeepMarkerInView({ latitude, longitude }: { latitude: number; longitude: number }) {
  const map = useMap()
  useEffect(() => {
    const point = L.latLng(latitude, longitude)
    if (!map.getBounds().pad(-0.15).contains(point)) map.panTo(point)
  }, [latitude, longitude, map])
  return null
}

export function FitToMarkers({ points, maxZoom = 15 }: { points: Array<[number, number]>; maxZoom?: number }) {
  const map = useMap()
  const key = points.map(point => `${point[0].toFixed(5)},${point[1].toFixed(5)}`).join('|')
  useEffect(() => {
    if (points.length === 0) return
    if (points.length === 1) {
      map.setView(points[0], Math.min(maxZoom, 15))
      return
    }
    map.fitBounds(L.latLngBounds(points), { padding: [36, 36], maxZoom })
  }, [key]) // eslint-disable-line react-hooks/exhaustive-deps
  return null
}
