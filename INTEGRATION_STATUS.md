# CommonGround integration status

This file is the current source-tree summary. The detailed, evidence-based verification is in `VERIFICATION_REPORT.md`.

## Architecture

- Backend: Java 21, Spring Boot 4.0.9, Spring Security/JWT, Spring Data JPA, MySQL 8; H2 for tests.
- Frontend: React 19, TypeScript, Vite 8, Tailwind CSS v4, Axios, Recharts, Motion.
- Roles: `CITIZEN`, `WORKER`, `DEPARTMENT_ADMIN`, `SYSTEM_ADMIN`.
- Public role chooser: Citizen, Worker, Admin. Admin is routed by the authenticated role into the distinct Department Admin or System Admin workspace.

## Frontend capabilities in this delivery

- Removed the demo data source tree, Preview page, and DatabaseStatus UI.
- Public landing page and role-selection sign-in page.
- API-connected citizen, worker, department-admin, and system-admin workspaces.
- Role-aware profile service and page, with editable fields restricted to those supported by each backend DTO.
- Complaint submission, evidence upload, complaint history, worker task actions, admin assignment/review, recurrence, and analytics integrations.
- Notification list/read actions, plus Help and Settings routes for all four roles.
- Leaflet/OpenStreetMap picker with Nominatim address search, click/drag marker, browser geolocation, reverse geocoding, and manual fallback. No API key or billing account.
- Role maps: complaint detail, citizen complaints, worker tasks with directions, and department-admin complaint map coloured by status.
- Responsive soft-neumorphic styling based on the supplied reference, with light/dark themes.
- More visible sign-out action with confirmation.

## Backend capabilities in this delivery

- Public worker applications with system-admin approval/rejection and login enforcement.
- Citizen, worker, and administrator profile self-service endpoints.
- Notification entity, persistence, service, controller, and lifecycle triggers (submission, assignment, work started, review submission, approval, rejection/rework, resolution).
- Complaint status history endpoint and persistence.
- Duplicate-submission guard, coordinate validation, and worker evidence-upload window enforcement.
- Role- and object-level authorization for complaints, assignments, resolutions, evidence, workers, departments, recurrence, and analytics.

## Local setup

Run `setup-local.ps1` from the project root. It creates ignored local environment files, generates a JWT secret, prompts for the local MySQL password, optionally bootstraps the first System Admin, records an optional Nominatim contact email, and installs frontend dependencies if absent. Then run `start-dev.ps1`.

The database defaults to `upr` and `ddl-auto=validate`. The setup scripts do not drop or recreate the database.

## Verification

See `VERIFICATION_REPORT.md` for the current results. Backend `.\mvnw.cmd clean verify` and frontend `npm run lint` / `npm run build` must be re-run for the final package; tests run on H2, not live MySQL. Maps need no API key; live tile/Nominatim access requires internet connectivity but no account.

