# CommonGround revision notes — 2026-11-03

This revision was made from the uploaded `CommonGround-Neumorphic-Complete-Project (1).zip`.

## Changes in this pass
- Added a soft-neumorphic styling layer for dashboard surfaces, statistic cards, navigation, authentication panels, form controls, and buttons, with light/dark shadow tokens.
- Changed a direct visit to `/login` without a role query to route to the role chooser at `/signin`. Role cards continue to open `/login?role=citizen`, `/login?role=worker`, or `/login?role=admin`.
- Confirmed the checked frontend source has no `Database Connected`, `DatabaseStatus`, or `database-status` UI references.
- Replaced the Google Maps picker with a Leaflet + OpenStreetMap location system, including Nominatim search/reverse geocoding, drag/click selection, browser geolocation, and a manual fallback with no API key.

## Important runtime note
If a browser still displays the old Database Connected badge or old page layout, it is likely running a different extracted project folder or a stale Vite process. Stop old Vite terminals, open this revision's `frontend` directory, run `npm run dev`, and hard-refresh the browser.

## Verification limitations
The uploaded ZIP did not contain `node_modules` or Maven `target` output. A fresh dependency install and full Maven/npm verification must be run in the target Windows environment. This revision does not claim those tests passed.
