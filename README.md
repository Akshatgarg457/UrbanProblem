# CommonGround — Urban Problem Reporting System

CommonGround is a role-based civic issue reporting and resolution application with a React/TypeScript frontend and a Java 21/Spring Boot backend.

## Included capabilities

- Public landing page and role chooser: Citizen, Worker, Admin.
- Separate `CITIZEN`, `WORKER`, `DEPARTMENT_ADMIN`, and `SYSTEM_ADMIN` workspaces with independent routes, sidebars, and server-side permissions.
- Workspace sign-in is role-checked by the backend: only provisioned department/system administrator accounts can enter the admin workspace, and approved workers enter the worker workspace.
- Authentication uses MySQL-backed server-side sessions in HttpOnly cookies, CSRF protection, and a 24-hour idle timeout. Logout invalidates the server session; browser clients never receive or store a bearer token.
- System administrators can assign workers and review resolutions across departments; department administrators remain restricted to their own department.
- Citizen registration and complaint submission with address search, map pin, geolocation, and persisted latitude/longitude.
- Public worker application → pending account → system-admin approval or rejection. Only approved (ACTIVE) workers can sign in.
- A person may hold separate Citizen and Worker accounts with the same email; each account has its own password, and worker sign-in is enabled after admin approval. The sign-in workspace selects which account to use.
- Worker workspace: assigned tasks, start work, before/after evidence upload, submit for review, rejection feedback, and resubmission.
- Department-admin workspace: unassigned queue, assignment management, resolution review (approve/reject/rework), history, analytics.
- System-admin workspace: users, worker applications, departments, complaints, recurrence, and system statistics.
- Real in-app notifications for submission, assignment, work started, submitted for review, approval, rejection/rework, and resolution, with unread counts and mark-read.
- Role-aware profile pages with account/workspace details and authenticated password changes, plus Help, Settings, and Sign Out in every workspace.
- The sign-in page includes a Forgot password link. Self-service reset is not implemented yet; the page explains the manual recovery path and does not claim to send an email.
- Leaflet + OpenStreetMap location picker with Nominatim address search, reverse geocoding, draggable marker, click-to-select, browser geolocation, and manual address/coordinate fallback. No API key or billing account required.
- Role-aware maps: complaint detail locations, citizen complaint map, worker task map with directions, and a department-admin complaint map coloured by status.
- Soft-neumorphic responsive UI with light and dark themes and no demo/preview data.

## Prerequisites

- Windows 10/11 with PowerShell.
- Java 21 (`java -version` must report 21).
- MySQL 8 running on port 3306 with an existing database named `upr`. **Do not drop or recreate it**; this project validates an existing schema and preserves existing records.
- Node.js 22.12+ and npm 10+.
- Internet access for the first Maven/npm dependency download.

## First-time setup (PowerShell)

From the extracted project root:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\setup-local.ps1
```

The setup script creates ignored local files:

- `backend/.env`: prompts for MySQL username/password and optionally bootstraps the first system administrator if one does not already exist.
- `frontend/.env.local`: optional OpenStreetMap/Nominatim provider overrides and a Nominatim contact email. The defaults work with no key and no configuration.
- Installs frontend packages with `npm ci` if `frontend/node_modules` is absent.

To configure the files manually instead:

```powershell
Copy-Item backend\.env.example backend\.env
Copy-Item frontend\.env.example frontend\.env.local
notepad backend\.env
notepad frontend\.env.local
```

Backend variables:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | JDBC URL, default `jdbc:mysql://localhost:3306/upr` |
| `DB_USERNAME` | MySQL user |
| `DB_PASSWORD` | MySQL password (never commit) |
| `BOOTSTRAP_SYSTEM_ADMIN_EMAIL` / `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` | Optional first system administrator; leave blank if one already exists |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin, default `http://localhost:5173` |
| `SESSION_COOKIE_SECURE` | Set to `true` behind HTTPS in production; keep `false` for local HTTP development |
| `FILE_UPLOAD_DIR` | Evidence storage directory; defaults to `%USERPROFILE%\urban-problem-uploads` |

Frontend variables:

| Variable | Purpose |
| --- | --- |
| `VITE_API_BASE_URL` | Backend API root, default `http://localhost:8080/api` |
| `VITE_OSM_TILE_URL` | Leaflet tile template, default `https://tile.openstreetmap.org/{z}/{x}/{y}.png` |
| `VITE_OSM_TILE_ATTRIBUTION` | Attribution HTML shown on every map; defaults to the OpenStreetMap copyright notice |
| `VITE_OSM_WEB_BASE_URL` | Base for "Open in OpenStreetMap" and directions links, default `https://www.openstreetmap.org` |
| `VITE_NOMINATIM_BASE_URL` | Nominatim endpoint, default `https://nominatim.openstreetmap.org` |
| `VITE_NOMINATIM_EMAIL` | Optional contact email sent with geocoding requests, as the Nominatim policy recommends |

No Google Maps key, Google Cloud project, billing account, or payment verification is needed. All map tiles and geocoding use public OpenStreetMap services, and every provider URL can be redirected to a self-hosted or commercial provider through the variables above. The public OSM tile server and Nominatim instance are intended for development and low-volume use; review their usage policies before production traffic and switch providers if needed.

## Database

`backend/.env.example` points to `jdbc:mysql://localhost:3306/upr`. Production configuration forces Hibernate to use `validate`, so it checks the existing schema and will not create, alter, drop, or reset tables—even if an environment override requests another schema action. `create-drop` is reserved for H2 test contexts. Flyway applies additive migrations, including the `SPRING_SESSION` tables for server-side sessions. Existing `upr` records are left untouched.

If Hibernate reports a missing table or column, stop the backend, back up `upr`, and review the runbook and migration notes in `backend/docs/`. Never switch to `create` or `create-drop` against a database containing real records. Configure production with HTTPS and `SESSION_COOKIE_SECURE=true`; keep the frontend and API on the same site so the Lax session cookie works.

### Schema migrations (automatic)

Schema changes are applied automatically by **Flyway** on startup, before Hibernate validates the schema. Migration scripts live in `backend/src/main/resources/db/migration/` and run exactly once against `upr` (Flyway records them in a `flyway_schema_history` table and `spring.flyway.baseline-on-migrate=true` adopts the pre-existing schema as-is). All migrations so far are additive — nullable columns only — and never modify or delete existing rows. There is no manual step: just start the backend and pending migrations apply themselves.

### Provision a single system administrator

Back up the database and ensure `backend/.env` targets the intended database. Then run once
from PowerShell:

```powershell
cd backend
.\provision-goku-admin.ps1
```

The script asks for the admin email (press Enter to use `akshatgarg1260@gmail.com`), requires
explicit confirmation, and prompts securely for a new password (12–128 characters). It
creates or updates Goku as a system administrator and marks other administrator accounts
inactive without deleting their records. This account can manage the whole system and
perform department assignment/review actions. The password is passed only through the
process environment and is not written to `.env` or source files. Provisioning runs without
starting an HTTP server, so it can run while the backend is already listening on port 8080.

## Run the application (PowerShell)

```powershell
.\start-dev.ps1
```

The script starts the `MySQL80` service if needed and opens separate PowerShell windows for the backend (`http://localhost:8080`) and frontend (`http://localhost:5173`).

Manual commands:

```powershell
# Terminal 1 — backend
cd backend
.\mvnw.cmd spring-boot:run

# Terminal 2 — frontend
cd frontend
npm ci
npm run dev
```

## Role access summary

| Capability | CITIZEN | WORKER | DEPARTMENT_ADMIN | SYSTEM_ADMIN |
| --- | --- | --- | --- | --- |
| Register publicly | Yes | Application only (pending) | No | No |
| Submit complaint | Yes | No | No | No |
| View complaint | Own only | Assigned only | Own department | All |
| Assign worker | No | No | Own department | All departments |
| Start/submit work | No | Own assignment | No | No |
| Approve/reject resolution | No | No | Own department | All departments |
| Manage users/departments | No | No | No | Yes |
| Approve/reject worker applications | No | No | No | Yes |
| Save profile location (map/manual) | Yes | Yes | No | No |

## Verification

Performed on 2026-11-09 in this workspace:

```powershell
# Backend
cd backend
.\mvnw.cmd clean verify

# Frontend
cd frontend
npm run lint
npm run build
```

Results:

- Backend: **108 tests, 0 failures, 0 errors** (BUILD SUCCESS, 3:38 min).
- Frontend: `tsc -b` clean; Vite production build succeeded (`dist/`).
- The backend suite runs on in-memory H2 in MySQL compatibility mode. It verifies application behavior and authorization, but it does **not** prove live MySQL integration.
- The test suite covers profile location persistence, complaint coordinates (including invalid ranges and a missing pair), complaint lifecycle, cross-role access controls, worker approval, and department/system admin separation. It does not drive the Leaflet UI in a browser.
- Live map tiles and Nominatim calls require internet access but no account or key. Interactive browser verification of the map screens requires signing in with accounts from your own `upr` database; credentials were not available here, so no live UI session was created against the database.
- The production build reports a >500 kB single JS chunk warning; it is not an error.

See `VERIFICATION_REPORT.md` and `INTEGRATION_STATUS.md` for additional detail and known limitations. Location-provider setup, Nominatim policy compliance, and a role-by-role map testing checklist are in `docs/OPENSTREETMAP.md`.
