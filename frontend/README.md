# CommonGround frontend

React 19 + TypeScript + Vite frontend for the Urban Problem Reporting, Resolution & Recurrence Analysis System.

## Prerequisites

- Node.js 22.12+ (Vite 8 requirement)
- npm 10+
- Backend available at `http://localhost:8080`

## Setup and run

From this directory:

```powershell
Copy-Item .env.example .env.local
npm ci
npm run lint
npm run build
npm run dev
```

The repository-root `setup-local.ps1` creates `.env.local` and can install dependencies for you. The app runs at `http://localhost:5173`.

## Environment

```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
VITE_OSM_TILE_URL=https://tile.openstreetmap.org/{z}/{x}/{y}.png
VITE_NOMINATIM_BASE_URL=https://nominatim.openstreetmap.org
VITE_NOMINATIM_EMAIL=
```

`VITE_API_BASE_URL` and the OSM/Nominatim URLs are public browser configuration. Never place database passwords, session secrets, or other server-side credentials in a `VITE_` variable. Authentication uses an HttpOnly backend session cookie and a CSRF token. The map works with no configuration and needs no API key, billing account, or payment verification; the provider URLs are configurable if you later move to a self-hosted or commercial tile/geocoding service.

## Role workspaces

- Citizen: overview, report issue, complaints, profile with saved location, notifications, help, settings.
- Worker: overview with a task-location map, assigned tasks, profile with saved location, notifications, help, settings.
- Department Admin: complaint queue, complaint map coloured by status, review, workers, recurrence, analytics, profile, notifications, help, settings.
- System Admin: overview, users, departments, all complaints, recurrence, analytics, profile, notifications, help, settings.

The public sign-in chooser shows Citizen, Worker, and Admin. The backend-authenticated role determines the destination. Department Admin and System Admin remain separate roles and are not granted each other's permissions.

## Location system

All maps use Leaflet with OpenStreetMap tiles and display the required OpenStreetMap attribution. The reusable `LocationPicker` supports Nominatim address search (debounced, cached, and serialized to respect the public usage policy), click-to-select, a draggable marker, reverse geocoding, browser geolocation, and a six-field manual fallback (address, city, state, postal code, latitude, longitude) that always works without location permission or geocoding.

- `src/config/maps.ts` — provider URLs, attribution, and defaults.
- `src/services/geocodingService.ts` — Nominatim search/reverse geocoding with caching, rate limiting, and error handling.
- `src/components/location/LocationPicker.tsx` — interactive picker used by the profile and complaint forms.
- `src/components/map/` — shared tile layer, map focus helpers, CSS markers, and the complaint map with status colours.
- `src/utils/location.ts` — coordinate validation, formatting, status colours, and OpenStreetMap links.

Complaint detail, citizen complaints, worker tasks, and the department admin map all read the stored complaint latitude/longitude from the backend. Complaints without coordinates show a graceful empty state instead of a fabricated location. Directions links open OpenStreetMap's directions view, so a routing service failure cannot break the map.

## Scripts

- `npm run dev` — Vite development server.
- `npm run lint` — TypeScript project typecheck (`tsc -b`).
- `npm run build` — typecheck and production Vite build.
- `npm run preview` — preview a generated production build (this is Vite's standard build-preview command, not an application demo mode).
