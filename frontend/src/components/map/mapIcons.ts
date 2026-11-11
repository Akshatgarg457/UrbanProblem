import * as L from 'leaflet'

const DEFAULT_COLOR = '#2b7cf2'

function safeColor(color: string): string {
  return /^#[0-9a-f]{3,8}$/i.test(color) ? color : DEFAULT_COLOR
}

/** Teardrop pin drawn with CSS so no image assets or icon URL rewriting is needed. */
export function pinIcon(color = DEFAULT_COLOR, selected = false): L.DivIcon {
  return L.divIcon({
    className: 'cg-map-marker',
    html: `<span class="cg-map-pin${selected ? ' cg-map-pin-selected' : ''}" style="--pin-color:${safeColor(color)}"><span class="cg-map-pin-core"></span></span>`,
    iconSize: [30, 42],
    iconAnchor: [15, 40],
    popupAnchor: [0, -38],
  })
}
