import * as L from 'leaflet'
import { ExternalLink, MapPin, Navigation } from 'lucide-react'
import { useMemo } from 'react'
import { Link } from 'react-router-dom'
import { MapContainer, Marker, Popup } from 'react-leaflet'
import { DEFAULT_MAP_CENTER, DEFAULT_MAP_ZOOM } from '../../config/maps'
import type { Complaint, ComplaintStatus } from '../../types'
import { statusBadgeClass, statusLabel } from '../../utils/labels'
import {
  complaintStatusColor,
  formatCoordinatePair,
  isValidLatitude,
  isValidLongitude,
  osmDirectionsUrl,
  osmViewUrl,
} from '../../utils/location'
import { FitToMarkers, OsmTileLayer } from './MapPrimitives'
import { pinIcon } from './mapIcons'

interface ComplaintMapProps {
  complaints: Complaint[]
  basePath?: string
  showDepartment?: boolean
  showDirections?: boolean
  height?: number
  emptyMessage?: string
}

export function ComplaintMap({
  complaints,
  basePath,
  showDepartment = false,
  showDirections = false,
  height = 400,
  emptyMessage = 'No complaints with saved coordinates to show on the map yet.',
}: ComplaintMapProps) {
  const mapped = useMemo(() => complaints.flatMap(complaint => {
    const { latitude, longitude } = complaint
    if (!isValidLatitude(latitude) || !isValidLongitude(longitude)) return []
    return [{ complaint, position: [latitude, longitude] as [number, number] }]
  }), [complaints])

  const points = useMemo(() => mapped.map(item => item.position), [mapped])

  const icons = useMemo(() => {
    const byStatus = new Map<ComplaintStatus, L.DivIcon>()
    for (const item of mapped) {
      if (!byStatus.has(item.complaint.status)) {
        byStatus.set(item.complaint.status, pinIcon(complaintStatusColor(item.complaint.status)))
      }
    }
    return byStatus
  }, [mapped])

  const legend = useMemo(
    () => Array.from(new Set(mapped.map(item => item.complaint.status))),
    [mapped],
  )

  if (mapped.length === 0) {
    return (
      <div className="map-empty-state">
        <MapPin size={20} />
        <div>
          <strong>No map locations available</strong>
          <p>{emptyMessage}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="complaint-map-wrap">
      <div className="map-shell">
        <MapContainer
          center={points[0] ?? DEFAULT_MAP_CENTER}
          zoom={mapped.length === 1 ? 15 : DEFAULT_MAP_ZOOM}
          className="complaint-map"
          style={{ height }}
          scrollWheelZoom
        >
          <OsmTileLayer />
          <FitToMarkers points={points} />
          {mapped.map(({ complaint, position }) => (
            <Marker key={complaint.id} position={position} icon={icons.get(complaint.status)} title={complaint.title}>
              <Popup>
                <div className="map-popup">
                  <span className="map-popup-ref">{complaint.referenceId}</span>
                  <strong className="map-popup-title">{complaint.title}</strong>
                  <span className={`badge ${statusBadgeClass(complaint.status)}`}>{statusLabel(complaint.status)}</span>
                  {showDepartment && complaint.departmentName && (
                    <p className="map-popup-line"><strong>Department:</strong> {complaint.departmentName}</p>
                  )}
                  <p className="map-popup-address"><MapPin aria-hidden="true" size={12} /> {complaint.address}</p>
                  <p className="map-popup-coords">
                    {formatCoordinatePair(position[0], position[1])}
                  </p>
                  <div className="map-popup-actions">
                    {basePath && (
                      <Link to={`${basePath}/${encodeURIComponent(complaint.referenceId)}`}>
                        Open complaint details
                      </Link>
                    )}
                    {showDirections && (
                      <a href={osmDirectionsUrl(position[0], position[1])} target="_blank" rel="noopener noreferrer">
                        <Navigation size={12} /> Directions
                      </a>
                    )}
                    <a href={osmViewUrl(position[0], position[1])} target="_blank" rel="noopener noreferrer">
                      <ExternalLink size={12} /> Open in OSM
                    </a>
                  </div>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>
      {legend.length > 1 && (
        <div className="map-legend" aria-label="Complaint status colours">
          {legend.map(status => (
            <span key={status} className="map-legend-item">
              <i style={{ background: complaintStatusColor(status) }} aria-hidden="true" />
              {statusLabel(status)}
            </span>
          ))}
        </div>
      )}
      <p className="map-attribution-note">
        Map data © OpenStreetMap contributors. {mapped.length} complaint{mapped.length === 1 ? '' : 's'} with coordinates shown.
      </p>
    </div>
  )
}
