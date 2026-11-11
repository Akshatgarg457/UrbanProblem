# CommonGround delivery notes

## UI direction

The interface uses a soft **neumorphic** visual system based on the supplied dashboard reference: tonal surfaces, paired raised shadows, inset form controls, subtle teal accents, and light/dark themes. This is a styling direction only; data cards and charts continue to use live API responses.

## Frontend setup

1. Install Node.js 22 or a compatible current LTS release.
2. In `frontend`, copy `.env.example` to `.env.local`.
3. Set `VITE_API_BASE_URL=http://localhost:8080/api`.
4. Map support needs no key: the frontend uses Leaflet with OpenStreetMap tiles and Nominatim geocoding. Optional provider overrides (`VITE_OSM_TILE_URL`, `VITE_NOMINATIM_BASE_URL`, `VITE_NOMINATIM_EMAIL`) are documented in `frontend/.env.example`. Never put server-side secrets in a `VITE_` variable.
5. Run `npm install`, `npm run lint`, `npm run build`, and `npm run dev`.

If geocoding or tile loading fails, the complaint form retains address and coordinate fields for manual entry. The app does not render a fake interactive map.

## Backend and MySQL setup

1. Install Java 21 and MySQL 8.
2. Create/use the existing database `upr`; do not drop it.
3. Copy `backend/.env.example` to `backend/.env` and set `DB_URL=jdbc:mysql://localhost:3306/upr`, `DB_USERNAME`, and `DB_PASSWORD`. Set bootstrap administrator credentials only if first-time system-admin bootstrap is required. Set `SESSION_COOKIE_SECURE=true` when serving over HTTPS.
4. Keep `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` after the schema has been initialized. Do not use `create` or `create-drop` on a database containing data. If a local schema bootstrap is necessary, back up first, use the project's documented migration/initialization procedure, inspect the resulting tables and constraints, and return to `validate`.
5. Run `backend\mvnw.cmd clean verify`, then `backend\mvnw.cmd spring-boot:run`.

## Start both applications (Windows PowerShell)

From the project root, run:

```powershell
.\start-dev.ps1
```

This starts MySQL80 if it is installed and stopped, then opens separate PowerShell windows for the Spring Boot backend and Vite frontend. It does not create or alter the database and does not inject credentials.

## User roles

The public sign-in chooser presents Citizen, Worker, and Admin. Authentication uses a server-side JDBC session; the HttpOnly cookie is authoritative and account role checks remain enforced by the backend. Admin users are routed separately as `DEPARTMENT_ADMIN` or `SYSTEM_ADMIN`; their permissions are not merged.

## Verification status for this delivered copy

- `backend\mvnw.cmd clean verify`: **PASS** — 108 tests, 0 failures, 0 errors, BUILD SUCCESS.
- `frontend\npm run lint`: **PASS** — no TypeScript diagnostics.
- `frontend\npm run build`: **PASS** — production bundle generated; only the >500 kB chunk-size warning.
- A live browser walkthrough covered saved location persistence, reporting with map selection and prefill, complaint detail coordinates, worker/admin maps, worker approval, assignment, evidence upload, resolution approval, and citizen notifications. Details are in `VERIFICATION_REPORT.md`.

The walkthrough used an isolated temporary H2 backend; the real MySQL `upr` schema was not modified. Re-run the commands above on the target machine before release deployment.
