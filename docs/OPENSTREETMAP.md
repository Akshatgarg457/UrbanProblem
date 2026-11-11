# OpenStreetMap location system

CommonGround uses **Leaflet 1.9**, **React-Leaflet 5**, **OpenStreetMap tiles**, **Nominatim** geocoding, and the browser **Geolocation API**. There is no Google Maps dependency, no API key, no billing account, and no payment verification at any point.

## Provider configuration

Defaults work with zero configuration. Override them in `frontend/.env.local` when needed:

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_OSM_TILE_URL` | `https://tile.openstreetmap.org/{z}/{x}/{y}.png` | Leaflet tile template |
| `VITE_OSM_TILE_ATTRIBUTION` | OpenStreetMap copyright HTML | Attribution rendered on every map |
| `VITE_OSM_WEB_BASE_URL` | `https://www.openstreetmap.org` | Base for "Open in OSM" and directions links |
| `VITE_NOMINATIM_BASE_URL` | `https://nominatim.openstreetmap.org` | Forward/reverse geocoding endpoint |
| `VITE_NOMINATIM_EMAIL` | empty | Optional contact email sent with geocoding requests |

Tile and geocoding URLs are not hard-coded in components, so switching to a self-hosted or commercial OSM provider is a configuration-only change. Map containers are isolated from the theme, and tiles are explicitly excluded from CSS filters so labels stay readable in dark mode.

## Nominatim usage policy compliance

`frontend/src/services/geocodingService.ts` implements the public-instance etiquette:

- Search runs only after the user stops typing for **650 ms**; there is no per-keystroke or true autocomplete stream.
- Requests are **serialized** with at least **1.1 s** between calls, including reverse geocoding.
- Results are **cached** (10 minutes for searches, 30 minutes for reverse lookups, bounded cache size).
- The component sends a normal browser `Referer`; no spoofed `User-Agent` headers are used. An optional `email` parameter is supported through `VITE_NOMINATIM_EMAIL`.
- No bulk geocoding, prefetching, or background geocoding exists.
- `429`/`503` responses produce a readable rate-limit message; network failures produce a retry-friendly message.
- Geocoding errors never disable manual address or coordinate entry.

Public OSM tile and Nominatim services are appropriate for development and low-volume use. Review their policies before production traffic and configure a different provider if your volume exceeds their limits.

## Component architecture

| File | Responsibility |
| --- | --- |
| `frontend/src/config/maps.ts` | Provider URLs, attribution, and map defaults |
| `frontend/src/services/geocodingService.ts` | Nominatim search/reverse geocoding, caching, rate limiting, errors |
| `frontend/src/components/location/LocationPicker.tsx` | Search, suggestions, click/drag marker, reverse geocoding, geolocation, manual fields |
| `frontend/src/components/map/MapPrimitives.tsx` | OSM tile layer, click handler, focus/fit helpers |
| `frontend/src/components/map/mapIcons.ts` | CSS teardrop markers (no image assets) |
| `frontend/src/components/map/ComplaintMap.tsx` | Shared complaint map with status colours, popups, and links |
| `frontend/src/utils/location.ts` | Coordinate range validation, formatting, OSM links, status colours |

## Where maps appear

- **Citizen profile** — "My Saved Location": search, map selection, drag, current location, manual entry; saves through the existing profile API and restores the marker on reopen.
- **Report Issue** — location section prefilled from the saved profile location, fully editable before submission.
- **Complaint detail** — actual stored latitude/longitude, address, coordinates, OpenStreetMap link, and directions link. Missing coordinates show an empty state.
- **My Complaints** — map of the citizen's own complaints.
- **Worker dashboard and tasks** — assigned task locations, task popups, and directions links.
- **Department admin dashboard and Complaint Map** — department complaints coloured by status, filterable, with popup details and links to complaint details.

Directions links open OpenStreetMap's directions view. No routing service request is made by the app, so a routing outage cannot break a map.

## Backend coordinate handling

- Complaint coordinates are validated in the DTO (`@DecimalMin`/`@DecimalMax`) and again in `ComplaintService` (both-or-neither, latitude −90…90, longitude −180…180).
- Profile location coordinates are validated by the existing citizen/worker profile DTOs.
- Coordinates are stored in the existing `citizens`, `workers`, and `complaints` columns; **no new database migration is required**. `V2__profile_saved_location.sql` already adds the profile location columns.
- Complaint queries remain scoped by role: citizens see their own complaints, workers see assignments, department admins see their department, system admins see all.

## Manual and offline behaviour

- A location can always be chosen by clicking the map or by typing the six fields manually; browser permission is optional.
- If tile loading fails, the Leaflet container still renders and the manual fields remain usable.
- If reverse geocoding fails, coordinates are kept and the address fields are left editable.
- If a complaint has no coordinates, maps render a clear empty state rather than a fabricated point.

## Testing checklist

1. Sign in as a citizen, open **My Profile → My Saved Location**, search or click the map, save, sign out, sign back in, and confirm the marker and address restore.
2. Open **Report Issue** and confirm the saved location is prefilled; change it on the map and submit; open the complaint detail and confirm the marker matches the stored coordinates.
3. Deny browser location permission and confirm search, map click, marker drag, and manual entry still work.
4. Sign in as a worker and confirm assigned task markers appear with task details and directions.
5. Sign in as a department admin and open **Complaint Map**; confirm status colours, popups with reference/title/department/status/address, and detail links.
6. Confirm no map requires an API key, and that all maps show OpenStreetMap attribution.
