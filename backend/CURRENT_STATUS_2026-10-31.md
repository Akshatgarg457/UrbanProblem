# Backend status (verification pass, 2026-10-31)

This file supersedes the previous version of this file and the older reports listed under
"Superseded reports". Read `TEST_REPORT.md`, `SECURITY_AUDIT.md`, and
`FRONTEND_BACKEND_API_COMPATIBILITY.md` for the detail behind each line.

## Overall

NOT PRODUCTION-READY. The Maven build has not run in this environment, no Spring test has executed,
the database migration has not been applied, and the frontend was not available to compare.

## Labels

- VERIFIED: checked by running real project code (only the logic harness does this).
- FIXED: changed in source and checked by reading. Not executed.
- NOT TESTED: changed or reviewed, but not executed.
- FAILED: known failing. None known after this pass, but none has been executed either.
- BLOCKED: cannot proceed here; the blocker is named.

## Modules

| Module | Status | Notes |
|---|---|---|
| Source syntax (all of `src/main` and `src/test`) | VERIFIED | javac 21, 0 parse errors. |
| Authorization rules (`AccessRules`) | VERIFIED | 14 harness checks. |
| Complaint status transitions (`ComplaintStatus`) | VERIFIED | 11 harness checks. |
| Department routing (`ComplaintCategory`) | VERIFIED | 6 harness checks. ROADS now routes to "Roads and Infrastructure". |
| Recurrence matching (`RecurrenceService` matching functions) | VERIFIED | 11 harness checks. Category and time filters are NOT TESTED (JPA query). |
| JWT (`JwtTokenProvider`, `JwtAuthenticationFilter`) | FIXED, NOT TESTED | JJWT 0.12 API. Compile not checked. |
| Authentication and four roles (login path) | FIXED, NOT TESTED | `CustomUserDetailsService` admin branch added. |
| Admin creation and bootstrap | FIXED, NOT TESTED | Bootstrap email normalization fixed this pass. |
| Complaint, assignment, resolution services and controllers | FIXED, NOT TESTED | Review-action status check added this pass. |
| Evidence upload and download | FIXED, NOT TESTED | Content and ownership checks. |
| Database migration | NOT TESTED | No MySQL in this environment. Runbook written. |
| Frontend API compatibility | BLOCKED | Frontend ZIP not provided. Backend contract documented. |
| Maven `clean test`, `package`, `verify` | BLOCKED | Maven Central unreachable. Exact output in `TEST_REPORT.md`. |

## Migration status

Script: `docs/MIGRATION_2026-10-31_security.sql`. Runbook: `docs/MIGRATION_RUNBOOK_WINDOWS.md`.
Schema changes: new table `admin_users`; new column `complaint_evidence.uploaded_by_id`; column
`complaint_status_history.previous_status` made nullable. Data changes are gated: email normalization
after a preflight check, and routing of existing roads complaints only after manual review.
Not run. No MySQL server here. Hibernate's `validate` at startup is the authoritative schema check.

## Four-role authentication

Implemented in source for CITIZEN (register and login), WORKER (created by SYSTEM_ADMIN and login),
DEPARTMENT_ADMIN (created by SYSTEM_ADMIN and login), SYSTEM_ADMIN (created by bootstrap env vars and login).
Not run. The role matrix and the manual procedure to verify it are in `DEV_SETUP_WINDOWS.md`.
No password is stored in source.

## Complaint lifecycle

Flow implemented: submit (SUBMITTED), assign (ASSIGNED), start (IN_PROGRESS), upload evidence,
submit resolution (PENDING_VERIFICATION), reject for rework (back to IN_PROGRESS), resubmit,
approve (APPROVED then RESOLVED). Invalid transitions are rejected at the single transition check.
NOT TESTED at runtime. The transition table is VERIFIED.

## Recurrence

Matching rules: same category, RESOLVED, created within 365 days, distance at most 0.5 km by Haversine
when both complaints have coordinates, otherwise equal normalized location text. VERIFIED for the
matching function. The category and time filter in the database query is NOT TESTED.

## Build

`./mvnw clean test` BLOCKED. `./mvnw package` BLOCKED. `./mvnw verify` BLOCKED.
Blocker: the wrapper cannot download Apache Maven 3.10.0 (no network). Run the commands in `TEST_REPORT.md` on your laptop.

## Frontend

BLOCKED. Frontend ZIP not present. Backend contract is documented in `FRONTEND_BACKEND_API_COMPATIBILITY.md`.
Mismatches found against the master specification: 24 rows. Four endpoints in the specification do not
exist (`/api/auth/me`, `/api/complaints/assigned`, a progress-notes endpoint, `PATCH .../priority`), and
one analytics view for department admins does not exist. Decisions are needed before frontend work.

## Corrections to the previous status (this pass)

Found and fixed in this pass, not caught in the previous one:
- Department routing for roads still pointed at Water and Sanitation.
- Approve endpoint accepted a `REJECTED` payload.
- Bootstrap admin email was stored un-normalized, so the system admin could not log in.
- `ComplaintServiceTest` and `APIContractIntegrationTest` contained expectations the code does not produce (listed in `TEST_REPORT.md`).
- `.env.example` and default upload path used a Linux path, which breaks on Windows.
- Migration lacked preflight checks, and its rollback could be run without warning.

## Superseded reports

`COMPLETION_REPORT.md`, `FINAL_AUDIT_SUMMARY.txt`, `FINAL_SUMMARY.txt`, `PRE_DEPLOYMENT_CHECKLIST.md`,
`BACKEND_AUDIT_REPORT.md`, `API_COMPATIBILITY_MATRIX.md`, `DELIVERY_SUMMARY.md`, and the other earlier reports
were written before these checks. They contain claims (passing builds, complete test suites, production
readiness) that this environment cannot confirm, and that the source did not support. Do not rely on them.

## Files changed in this pass

See `CHANGED_FILES_2026-10-31.txt` (regenerated).
