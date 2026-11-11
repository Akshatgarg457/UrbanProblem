# CommonGround delivery verification report

Date: 2026-11-09
Scope: Leaflet + OpenStreetMap location system and existing role workflows

## Commands run

| Check | Result | Evidence |
|---|---|---|
| Backend `.\mvnw.cmd clean verify` | **PASS** | 108 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS in 3:12 min (final re-run after the frontend fix). |
| Frontend `npm run lint` (`tsc -b`) | **PASS** | Completed with no diagnostics (re-run after the multipart fix). |
| Frontend `npm run build` | **PASS** | TypeScript project build plus Vite production bundle; 3,216 modules transformed, `dist/` produced. Only a >500 kB chunk-size warning (re-run after the multipart fix). |

Test classes: 27 API contract, 14 auth, 4 account service, 21 backend gaps, 4 complaint lifecycle, 7 complaint service, 18 security regression, 1 session/CSRF, 10 worker application, 1 admin bootstrap, 1 application context. Runner summary: `Tests run: 108, Failures: 0, Errors: 0, Skipped: 0`.

## Location feature coverage

| Requirement | Status | Evidence |
|---|---|---|
| Leaflet + React-Leaflet + OpenStreetMap tiles | Implemented | `frontend/src/components/map/`, `frontend/src/config/maps.ts`; `leaflet/dist/leaflet.css` loaded in `main.tsx`. |
| Official tile URL and attribution on every map | Implemented | `OsmTileLayer` uses `https://tile.openstreetmap.org/{z}/{x}/{y}.png` and the OSM attribution; `ComplaintMap` also prints an attribution note. |
| Search, click-to-select, draggable marker, current location | Implemented | `LocationPicker.tsx`. |
| Nominatim forward/reverse geocoding with debounce, cache, rate limiting | Implemented | 650 ms debounce, 1.1 s request serialization, 10/30 min caches, 429/503 handling in `geocodingService.ts`. |
| Manual fallback without permission/geocoding | Implemented | Six manual fields plus "Show typed coordinates on map"; errors never disable them. |
| Profile "My Saved Location" persistence and restore | Implemented; backend persistence automated | Existing profile API columns from `V2__profile_saved_location.sql`; `BackendGapsIntegrationTest` verifies saved profile location round-trip. UI restore is code-verified. |
| Complaint location prefill and submission | Implemented; coordinate validation automated | `ReportComplaint.tsx`; `ComplaintLifecycleIntegrationTest` covers valid and invalid coordinates, including a missing pair and out-of-range latitude. |
| Complaint detail shows stored coordinates | Implemented | `ComplaintDetail.tsx` renders the fetched complaint's coordinates; missing coordinates show an empty state. |
| Citizen, worker, and department-admin maps | Implemented | `MyComplaints`, `WorkerDashboard`/`WorkerTasks`, `DeptDashboard`/`DeptComplaintMap`; department map colours markers by status and links to details. |
| No Google Maps, API key, billing, or payment | Confirmed | No Google Maps references remain in `frontend/src` or `backend/src`; `.env.example` contains only OSM/Nominatim settings. |
| Backend coordinate range validation | Implemented and tested | DTO `@DecimalMin`/`@DecimalMax` plus `ComplaintService` both-or-neither/range checks. |
| Role-based access preserved | Verified by automated tests | 18 security-regression tests plus worker-application, auth, and contract suites pass; no location data is fetched outside the existing scoped endpoints. |
| Missing coordinates handled gracefully | Implemented | `ComplaintMap` filters invalid coordinates and renders `map-empty-state`; complaint detail shows "No coordinates recorded". |

## Live browser verification (2026-11-09)

A full browser walkthrough was run against a temporary H2 backend on port 8081 and Vite on port 5175. The temporary stack was isolated from the pre-existing backend on 8080 and the real MySQL `upr` schema, which were not touched.

1. **Citizen saves a profile location** — PASS. Manual entry plus reverse geocoding in "My Saved Location"; the profile API returned exactly `23.2599` / `77.4126` after save.
2. **Saved location remains after reload** — PASS. The values were restored from `/api/citizens/me` after a full page reload.
3. **Citizen reports an issue using the map** — PASS. Map click selected the point, the address filled from reverse geocoding, and the complaint was created with the selected coordinates.
4. **Profile location pre-fills the complaint form** — PASS. The report form opened with the saved profile location and allowed changes.
5. **Complaint detail shows the correct location** — PASS. Detail map rendered the stored `23.25957, 77.41357` with OSM and directions links; no hard-coded coordinates.
6. **Worker sees assigned complaint locations** — PASS. The worker dashboard and task page rendered the assigned complaint marker, popup with task details, and a working OSM directions link.
7. **Department admin sees complaint locations and statuses** — PASS. Dashboard map and the full Complaint Map rendered the marker, a status badge, department, address, coordinates, and a details link. The marker changed to Resolved after approval.
8. **Map works without any API key** — PASS. Tiles loaded from `tile.openstreetmap.org` and Nominatim calls worked with no key, account, or billing configuration.
9. **Manual entry works when geolocation is denied** — PASS. Permission denial surfaced a clear message while manual address and coordinate fields stayed usable.
10. **Role permissions and approval workflows still work** — PASS. Worker registration created an INACTIVE account; sign-in was blocked until SYSTEM_ADMIN approval; citizen, department-admin, and worker workspaces each opened only their own routes.

Full lifecycle exercised: citizen submits → system admin approves worker → department admin assigns → worker starts, uploads evidence, submits → department admin approves → citizen sees Resolved, the full history, and five notifications ("submitted", "assigned", "work started", "submitted for review", "resolved").

### Defect found and fixed during the live run

Evidence upload returned HTTP 500 because `apiClient` applied a global `Content-Type: application/json` to `FormData` requests, so the browser sent JSON without a multipart boundary. `frontend/src/services/apiClient.ts` now deletes that header for `FormData`, letting the browser set `multipart/form-data` with the boundary. Re-running the worker submission succeeded (`201` on upload, resolution accepted, status Pending Review → Resolved).

## Limitations

- Tests use H2 in MySQL compatibility mode; they do not prove live MySQL behaviour. The existing `upr` database was not modified during verification.
- Live browser checks used the temporary H2 backend, not the user's MySQL instance, and were verified through the browser accessibility tree and network responses rather than saved screenshots because the in-app browser surface was hidden.
- Public OSM tiles and Nominatim require internet connectivity at runtime. They need no account, key, or payment.
- The Vite bundle exceeds 500 kB due to Leaflet and charting dependencies; this is a warning, not a build failure.
