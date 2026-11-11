# Frontend / backend API compatibility

## Status: BLOCKED

The React frontend was not in the workspace, so no frontend API service file was read. Nothing in the
frontend column below is verified. This document records the backend contract as implemented in source,
and compares it with the endpoints your master specification expected. When you provide the frontend ZIP,
the next step is to fill the "Frontend" column from its `src/**/api*.js`/`services` files, not to change the backend.

Base URL (default): `http://localhost:8080`. CORS origin default: `http://localhost:5173`
(Vite). If your frontend runs elsewhere (for example `http://localhost:3000`), set
`CORS_ALLOWED_ORIGINS` in `.env`.

## Conventions (verified in source)

- Auth: `Authorization: Bearer <accessToken>`. JSON in and out.
- Dates: `Instant`, serialized as ISO-8601 UTC strings (for example `2026-10-31T09:15:00Z`).
- Enums: strings. Complaint status values are `SUBMITTED, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION, APPROVED, REJECTED, RESOLVED, CANCELLED`.
- Categories: `ROADS_AND_POTHOLES, WATER_SUPPLY, ELECTRICITY_AND_STREETLIGHTS, DRAINAGE_AND_SEWAGE, WASTE_MANAGEMENT`.
- Review status: `PENDING, APPROVED, REJECTED`.
- Pagination: Spring `Page` JSON (`content`, `totalElements`, `totalPages`, `number`, `size`, ...). Query params `page`, `size`, `sort`. **VERIFY ON RUN:** Spring Boot 4 / Spring Data may change this serialization. Check one response before relying on field names.
- Error bodies:
  - From controllers and `GlobalExceptionHandler`: `{ "success": false, "data": null, "message": "...", "error": "NOT_FOUND|BAD_REQUEST|CONFLICT|FORBIDDEN|VALIDATION_ERROR|INTERNAL_ERROR", "timestamp": "..." }`.
  - From the security layer (401 and 403 on the filter chain): `{ "success": false, "error": "UNAUTHORIZED|FORBIDDEN", "message": "..." }` (no `timestamp`, no `data`).
  - Frontend error handling must accept both shapes.
- Status codes: 400 validation or bad input, 401 missing or invalid token or bad login, 403 role or ownership denied, 404 not found, 409 invalid state transition or conflict.

## Endpoints: backend contract (from controller annotations and DTOs)

### Authentication

| Method and path | Auth | Request | Response | Notes |
|---|---|---|---|---|
| `POST /api/auth/register` | none | `{email, firstName, lastName, phoneNumber, password}` | 201 `{accessToken, tokenType, userId, email, role, type, departmentId}` | Creates a CITIZEN only. Email must be a valid address, password 8-128 characters, phone 10-20 characters. |
| `POST /api/auth/login` | none | `{email, password}` | 200 same as register | Works for CITIZEN, WORKER, DEPARTMENT_ADMIN, SYSTEM_ADMIN. `role` tells which. 401 for any credential error. |
| `POST /api/auth/logout` | none (permitAll) | none | 200 | Client must discard the token. Server does not revoke. |

### Complaints

| Method and path | Role | Request | Response | Notes |
|---|---|---|---|---|
| `POST /api/complaints` | CITIZEN | `{title, description, category, location, latitude, longitude}` | 201 `ComplaintResponseDto` | Title 5-200, description 10-2000, location 5-500. Department chosen from category. |
| `GET /api/complaints/my-complaints` | CITIZEN | paging | 200 `Page<ComplaintResponseDto>` | |
| `GET /api/complaints/{publicId}` | owner citizen, assigned worker, same-department admin, system admin | none | 200 `ComplaintResponseDto` | Keyed by `publicId` (UUID string), not numeric id. |
| `GET /api/complaints/internal/{id}` | DEPARTMENT_ADMIN, SYSTEM_ADMIN | none | 200 `ComplaintResponseDto` | Keyed by numeric id. Object-level check applies. |
| `GET /api/complaints/department/{departmentId}` | DEPARTMENT_ADMIN (own department), SYSTEM_ADMIN | paging | 200 `Page<ComplaintResponseDto>` | |
| `GET /api/complaints` | SYSTEM_ADMIN | `status`, `category`, paging | 200 `Page<ComplaintResponseDto>` | Default filter is SUBMITTED when neither is given. |

`ComplaintResponseDto`: `id, publicId, title, description, category, location, latitude, longitude, citizenId, citizenName, departmentId, departmentName, status, createdAt, updatedAt`.

### Assignment and progress (worker)

| Method and path | Role | Request | Response | Notes |
|---|---|---|---|---|
| `POST /api/assignments/complaints/{complaintId}/assign` | DEPARTMENT_ADMIN (complaint's department only) | `{workerId, assignmentNotes?}` | 201 `AssignmentResponseDto` | Worker must be ACTIVE and in same department. Complaint must be SUBMITTED. Returns 409 otherwise. |
| `GET /api/assignments/my-assignments` | WORKER | paging | 200 `Page<AssignmentResponseDto>` | Returns assignments, not complaints. |
| `GET /api/assignments/my-assignments/active` | WORKER | paging | 200 `Page<AssignmentResponseDto>` | Status ASSIGNED only. |
| `GET /api/assignments/{assignmentId}` | WORKER (assignee), DEPARTMENT_ADMIN (complaint's department) | none | 200 `AssignmentResponseDto` | |
| `POST /api/assignments/{assignmentId}/start` | WORKER (assignee) | none | 200 `AssignmentResponseDto` | Moves ASSIGNED to IN_PROGRESS. Only from ASSIGNED. |

`AssignmentResponseDto`: `id, complaintId, complaintPublicId, complaintTitle, workerId, workerName, assignmentNotes, status, createdAt, updatedAt`.

### Evidence

| Method and path | Role | Request | Response | Notes |
|---|---|---|---|---|
| `POST /api/complaints/{complaintId}/evidence/upload` | CITIZEN (owner), WORKER (assigned) | `multipart/form-data`, field `file` | 201 `ComplaintEvidenceResponseDto` | PDF, PNG, JPG, DOC, DOCX. Content must match extension. Max 10 MB, max 5 files per complaint. |
| `GET /api/complaints/{complaintId}/evidence` | CITIZEN, WORKER, DEPARTMENT_ADMIN, SYSTEM_ADMIN (all subject to complaint visibility) | none | 200 `ComplaintEvidenceResponseDto[]` | |
| `GET /api/complaints/{complaintId}/evidence/{evidenceId}/download` | same as list | none | 200 binary, `Content-Disposition: attachment` | Requires Bearer header. See frontend notes. |
| `DELETE /api/complaints/{complaintId}/evidence/{evidenceId}` | CITIZEN or WORKER who uploaded it | none | 204 | Uploader only. |

`ComplaintEvidenceResponseDto`: `id, complaintId, fileName, fileType, fileSize, uploadedBy, createdAt, downloadUrl`.
Note: `uploadedBy` now holds the uploader's **role** (`CITIZEN` or `WORKER`), not an email. `downloadUrl` is a relative path.

### Resolution (worker submits, department admin reviews)

| Method and path | Role | Request | Response | Notes |
|---|---|---|---|---|
| `POST /api/resolutions/assignments/{assignmentId}/submit` | WORKER (assignee) | `{completionNotes}` (10-2000) | 201 `ResolutionResponseDto` | Requires status IN_PROGRESS and at least one evidence file uploaded by this worker on the complaint. After a rejection, this same endpoint resubmits. |
| `GET /api/resolutions/pending` | DEPARTMENT_ADMIN (own department), SYSTEM_ADMIN | paging | 200 `Page<ResolutionResponseDto>` | Use this to find `resolutionId` for review. |
| `POST /api/resolutions/{resolutionId}/approve` | DEPARTMENT_ADMIN (own department) | `{reviewStatus: "APPROVED", adminNotes?}` | 200 `ResolutionResponseDto` | `reviewStatus` must be `APPROVED`. Complaint becomes RESOLVED. |
| `POST /api/resolutions/{resolutionId}/reject` | DEPARTMENT_ADMIN (own department) | `{reviewStatus: "REJECTED", rejectionReason, adminNotes?}` | 200 `ResolutionResponseDto` | `reviewStatus` must be `REJECTED`. `rejectionReason` required. Complaint returns to IN_PROGRESS (rework). |
| `GET /api/resolutions/{resolutionId}` | WORKER, DEPARTMENT_ADMIN, SYSTEM_ADMIN (subject to visibility) | none | 200 `ResolutionResponseDto` | |
| `GET /api/resolutions/complaints/{complaintId}` | CITIZEN, WORKER, DEPARTMENT_ADMIN, SYSTEM_ADMIN (subject to visibility) | paging | 200 `Page<ResolutionResponseDto>` | Approved resolutions only. |

`ResolutionResponseDto`: `id, assignmentId, complaintId, complaintPublicId, completionNotes, adminNotes, rejectionReason, reviewStatus, submittedAt, reviewedAt`.

### Recurrence (department admin and system admin only)

| Method and path | Role | Response |
|---|---|---|
| `POST /api/recurrence/complaints/{complaintId}/analyze` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | 200 `RecurrenceReportResponseDto` |
| `GET /api/recurrence/reports` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | 200 `Page<RecurrenceReportResponseDto>` |
| `GET /api/recurrence/reports/{reportId}` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | 200 |
| `GET /api/recurrence/complaints/{complaintId}/report` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | 200 or 404 if not analyzed yet |
| `GET /api/recurrence/category/{category}/count` | DEPARTMENT_ADMIN (own count), SYSTEM_ADMIN (all) | 200 `number` |
| `GET /api/recurrence/high-risk-areas?minHistoricalCount=3` | SYSTEM_ADMIN | 200 `RecurrenceReportResponseDto[]` |

`RecurrenceReportResponseDto`: `id, complaintId, complaintPublicId, departmentId, category, location, latitude, longitude, proximityRadiusKm, historicalIssueCount, relatedComplaintIds, analysisNotes, createdAt, updatedAt`.

### Departments, workers, administrators, analytics

| Method and path | Role | Notes |
|---|---|---|
| `POST /api/departments` | SYSTEM_ADMIN | `{name, description, headEmail}` |
| `GET /api/departments` | SYSTEM_ADMIN | paging, `activeOnly` |
| `GET /api/departments/{departmentId}` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | `DepartmentResponseDto` |
| `PUT /api/departments/{departmentId}` | SYSTEM_ADMIN | |
| `PUT /api/departments/{departmentId}/status?status=ACTIVE|INACTIVE` | SYSTEM_ADMIN | 409 if open complaints exist. |
| `POST /api/workers` | SYSTEM_ADMIN | `{email, firstName, lastName, phoneNumber, password, departmentId}` |
| `GET /api/workers/{workerId}` | WORKER (self), DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | |
| `GET /api/workers/department/{departmentId}?activeOnly=` | DEPARTMENT_ADMIN (own), SYSTEM_ADMIN | |
| `PUT /api/workers/{workerId}/status?status=` | SYSTEM_ADMIN | |
| `PUT /api/workers/{workerId}/department?newDepartmentId=` | SYSTEM_ADMIN | 409 if open assignments. |
| `POST /api/admin/department-admins` | SYSTEM_ADMIN | `{email, firstName, lastName, password (12-128), departmentId}` returns `AdminResponseDto` |
| `GET /api/analytics/system-metrics` | SYSTEM_ADMIN | `SystemMetrics` object |
| `GET /api/analytics/complaints-by-category` | SYSTEM_ADMIN | map of category to count |
| `GET /api/analytics/complaints-by-department` | SYSTEM_ADMIN | map of department name to count |
| `GET /api/analytics/complaints-by-status` | SYSTEM_ADMIN | map of status name to count |
| `GET /api/analytics/average-resolution-time` | SYSTEM_ADMIN | `{averageDays, sampleSize}` (last 90 days) |
| `GET /api/analytics/department-metrics` | SYSTEM_ADMIN | map keyed by department id |

## Mismatches against the master specification

The master specification is the list of endpoints your frontend was expected to call. "Frontend" is not verified (frontend not provided). "Backend" is what exists now.

| # | Expected (spec) | Actual backend | Request differences | Response differences | Recommended correction |
|---|---|---|---|---|---|
| 1 | `POST /api/auth/register` | Exists | Fields match. Citizen only. | Returns token, `role=CITIZEN`. | If the frontend lets users register as worker or admin, that path does not exist. Remove it from the frontend. |
| 2 | `POST /api/auth/login` | Exists | Fields match. | Returns `accessToken`, not `token`. | Read `accessToken`. |
| 3 | `GET /api/auth/me` | **MISSING** | n/a | n/a | Add a read-only endpoint returning the principal (id, email, role, departmentId), or have the frontend decode the JWT claims. Do not add it silently: decide first. |
| 4 | `POST /api/complaints` | Exists | Fields match. | Returns `ComplaintResponseDto` with `publicId` and `id`. | Use `publicId` for citizen views. |
| 5 | `GET /api/complaints` | Exists, SYSTEM_ADMIN only | n/a | `Page` | If department admins need a list, use #6. |
| 6 | `GET /api/complaints/my` | Actual `GET /api/complaints/my-complaints` | Path differs. | Same DTO. | Change the frontend path. |
| 7 | `GET /api/complaints/{id}` | Two routes: citizen `GET /api/complaints/{publicId}`; admin `GET /api/complaints/internal/{id}` | Key differs (publicId vs numeric id). | Same DTO. | Choose the route by role. Do not pass numeric ids to the citizen route. |
| 8 | `GET /api/complaints/department` | Actual `GET /api/complaints/department/{departmentId}` | Path parameter required. The frontend must know its department id (available in the login response as `departmentId`). | Same DTO. | Use `departmentId` from login. |
| 9 | `GET /api/complaints/assigned` | **MISSING** | n/a | Worker assignments are at `/api/assignments/my-assignments`, which returns `AssignmentResponseDto`, not `ComplaintResponseDto`. | Use `/api/assignments/my-assignments` and read `complaintId`/`complaintPublicId`. Or add a complaints view. Decide first. |
| 10 | `POST /api/complaints/{id}/assign` | Actual `POST /api/assignments/complaints/{complaintId}/assign` | Path differs. Body `{workerId, assignmentNotes}` matches. | Returns `AssignmentResponseDto` (201). | Change path. |
| 11 | `POST /api/complaints/{id}/progress` | **MISSING**. Closest: `POST /api/assignments/{assignmentId}/start` (no body). | Different resource (assignment id), no notes, only the first step. | n/a | No progress notes endpoint exists. Decide whether to add one. Do not map `progress` onto `start`. |
| 12 | `POST /api/complaints/{id}/review` | Actual `POST /api/resolutions/{resolutionId}/approve` and `/reject`. | Different resource (resolution id, not complaint id). The frontend must get `resolutionId` from `GET /api/resolutions/pending`. `reviewStatus` must match the action. | `ResolutionResponseDto`. | Change the frontend to two calls. Obtain `resolutionId` from the pending list. |
| 13 | `PATCH /api/complaints/{id}/priority` | **MISSING**. No priority field exists in the domain. | n/a | n/a | Priority is a new domain concept. Decide whether to add it (entity, migration, DTO, endpoint) before frontend work. |
| 14 | Worker submits resolution | Actual `POST /api/resolutions/assignments/{assignmentId}/submit` | Needs assignment id, `{completionNotes}`, and evidence uploaded first. | `ResolutionResponseDto` (201). | Frontend must upload evidence first and handle 409 if none. |
| 15 | Rework request | Not a separate endpoint. Rejection with `rejectionReason` is the rework path. Worker resubmits via #14. | n/a | n/a | Use reject with reason. |
| 16 | Department analytics (department admin) | **MISSING**. `/api/analytics/*` is SYSTEM_ADMIN only. `GET /api/departments/{id}` returns `complaintCount` and `workerCount` only. | n/a | n/a | Decide whether department admins need their own metrics. If so, add a scoped endpoint. |
| 17 | System analytics | Exists, SYSTEM_ADMIN only | n/a | `SystemMetrics` object | Match field names in the frontend (`totalComplaints`, `resolutionRate`, etc.). |
| 18 | Recurrence reports | Exists, DEPARTMENT_ADMIN and SYSTEM_ADMIN only | Analyze uses complaint id. | `RecurrenceReportResponseDto`. | Citizens and workers cannot call these. |
| 19 | File upload | Exists | Multipart field name is `file`. | `ComplaintEvidenceResponseDto`. `uploadedBy` is a role now. | Use `file` as the field name. |
| 20 | File download | Exists | **Needs `Authorization` header.** | Binary with `Content-Disposition: attachment`. | Do not use `<a href>`. Fetch with the token, then create an object URL. |
| 21 | Error responses | Two shapes (see Conventions). | n/a | Frontend must accept both. | Parse `message` and `error` only. |
| 22 | Token location | Bearer header only. No cookie. | n/a | n/a | Store the token in memory or `sessionStorage`. Do not expect a cookie. |
| 23 | Lifecycle status names | Complaint uses `PENDING_VERIFICATION` where the spec says `SUBMITTED_FOR_REVIEW`. There is no `REWORK_REQUIRED` or `ACKNOWLEDGED` status. | n/a | Status strings in responses are the enum names above. | Map UI labels to these names. |
| 24 | Admin self-registration | Not supported (by design). Admins are created by SYSTEM_ADMIN. | n/a | n/a | Remove any admin sign-up screen. |

## What must be decided before frontend work

1. Items 3, 9, 11, 13, 16: add the missing endpoints, or drop them from the frontend.
2. Item 23: confirm the status vocabulary for the UI.
3. Item 8: confirm that the frontend stores `departmentId` from the login response.

Once you provide the frontend ZIP, I will fill the "Frontend" column for each row above, by reading its API service files. I will not change the backend contract without your decision on the items above.
