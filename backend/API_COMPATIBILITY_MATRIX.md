# API Compatibility Matrix
## Urban Problem Reporting System - Backend API

**Document Version:** 1.0  
**Last Updated:** October 26, 2026  
**Scope:** Complete API contract for React frontend integration

---

## Authentication APIs

### 1. Citizen Registration
```
POST /api/auth/register
```

**Authentication:** None (public endpoint)

**Request Body:**
```json
{
  "email": "string (email format, unique)",
  "firstName": "string",
  "lastName": "string",
  "phoneNumber": "string",
  "password": "string"
}
```

**Response (201 Created):**
```json
{
  "accessToken": "string (JWT token)",
  "userId": "number",
  "email": "string",
  "role": "CITIZEN"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid email format, duplicate email, weak password, missing fields
- **409 Conflict:** Email already registered

**Frontend Notes:**
- Store `accessToken` in secure storage (localStorage with caution or sessionStorage)
- Use `userId` for identifying current user
- Role is always "CITIZEN" for registration endpoint
- Email must be unique in system

---

### 2. Citizen Login
```
POST /api/auth/login
```

**Authentication:** None (public endpoint)

**Request Body:**
```json
{
  "email": "string",
  "password": "string"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "string",
  "userId": "number",
  "email": "string",
  "role": "CITIZEN"
}
```

**Error Responses:**
- **400 Bad Request:** Missing email or password
- **401 Unauthorized:** Invalid credentials (email not found or password incorrect)
- **403 Forbidden:** Account suspended or inactive

**Frontend Notes:**
- Return token to local state management
- Implement auto-logout on 401 response
- Do NOT log or display passwords

---

### 3. Logout
```
POST /api/auth/logout
```

**Authentication:** Required (Bearer token)

**Request Body:** Empty (no body required)

**Response (200 OK):**
```json
{}
```

**Error Responses:**
- **401 Unauthorized:** Invalid or missing token

**Frontend Notes:**
- Call on logout button click
- Clear stored token from client
- Redirect to login page
- Token-based auth means server doesn't maintain session state; logout is primarily client-side

---

## Complaint APIs

### 4. Create Complaint
```
POST /api/complaints
```

**Authentication:** Required (CITIZEN role)

**Authorization:** Only CITIZEN role can create

**Request Body:**
```json
{
  "title": "string (1-200 characters)",
  "description": "string (10-2000 characters)",
  "category": "enum: ROAD_MAINTENANCE | WATER_SUPPLY | ELECTRICITY | PUBLIC_HEALTH | SANITATION",
  "location": "string (description of location)",
  "latitude": "number (decimal: -90 to 90)",
  "longitude": "number (decimal: -180 to 180)"
}
```

**Response (201 Created):**
```json
{
  "id": "number",
  "publicId": "string (UUID for public tracking)",
  "title": "string",
  "description": "string",
  "category": "string",
  "location": "string",
  "latitude": "number",
  "longitude": "number",
  "status": "SUBMITTED",
  "departmentId": "number",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid coordinates, missing fields, text too long/short
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not a CITIZEN

**Frontend Notes:**
- `publicId` is the UUID for public complaint tracking/sharing (used in public view)
- `id` is the internal database ID (not for sharing)
- Store `publicId` to create shareable public tracking links
- Validate coordinates before submission (23.1815°N, 79.9864°E for Indore center)
- Category determines which department handles the complaint
- Status is always "SUBMITTED" for new complaints

---

### 5. Get Complaint By Public ID (Public Tracking)
```
GET /api/complaints/{publicId}
```

**Authentication:** None (public endpoint)

**Path Parameters:**
- `publicId`: UUID string (e.g., `550e8400-e29b-41d4-a716-446655440000`)

**Response (200 OK):**
```json
{
  "id": "number",
  "publicId": "string",
  "title": "string",
  "description": "string",
  "category": "string",
  "location": "string",
  "latitude": "number",
  "longitude": "number",
  "status": "string (SUBMITTED | ASSIGNED | IN_PROGRESS | PENDING_VERIFICATION | APPROVED | REJECTED | RESOLVED | CANCELLED)",
  "departmentId": "number",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **404 Not Found:** publicId does not exist

**Frontend Notes:**
- Publicly accessible; use for complaint tracking links
- Citizens can share this link to allow public tracking
- No authentication required
- Returns only complaint details, not assignments or resolutions

---

### 6. Get My Complaints (Citizen's Own)
```
GET /api/complaints/my-complaints
```

**Authentication:** Required (CITIZEN role)

**Query Parameters (Pagination):**
- `page`: number (0-indexed, default: 0)
- `size`: number (default: 20, max: 100)
- `sort`: string (default: `createdAt,desc`; format: `field,direction`)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "number",
      "publicId": "string",
      "title": "string",
      "description": "string",
      "category": "string",
      "location": "string",
      "latitude": "number",
      "longitude": "number",
      "status": "string",
      "departmentId": "number",
      "createdAt": "ISO-8601 timestamp",
      "updatedAt": "ISO-8601 timestamp"
    }
  ],
  "totalElements": "number",
  "totalPages": "number",
  "currentPage": "number",
  "pageSize": "number"
}
```

**Error Responses:**
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not a CITIZEN

**Frontend Notes:**
- Only returns complaints created by logged-in citizen
- Supports pagination; implement "Load More" or page navigation
- Sort options: `createdAt`, `status`, `title`
- Use for citizen's complaint history/dashboard

---

## Assignment APIs

### 7. Create Assignment (Department Admin Only)
```
POST /api/assignments
```

**Authentication:** Required (DEPARTMENT_ADMIN role)

**Authorization:** Only DEPARTMENT_ADMIN for their department

**Request Body:**
```json
{
  "complaintId": "number",
  "workerId": "number",
  "assignmentNotes": "string (optional)"
}
```

**Response (201 Created):**
```json
{
  "id": "number",
  "complaintId": "number",
  "workerId": "number",
  "workerName": "string",
  "assignmentNotes": "string",
  "status": "ASSIGNED",
  "createdAt": "ISO-8601 timestamp",
  "updatedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **400 Bad Request:** Worker not in department, complaint already assigned, worker inactive
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not a DEPARTMENT_ADMIN
- **404 Not Found:** Complaint or worker does not exist

**Frontend Notes:**
- ⚠️ **BLOCKED:** Admin endpoints require DEPARTMENT_ADMIN authentication (not yet implemented)
- Only one active assignment per complaint
- Worker must be in the same department as complaint's department

---

### 8. Get Worker's Assignments
```
GET /api/assignments/{workerId}
```

**Authentication:** Required (WORKER role for self, DEPARTMENT_ADMIN for their worker)

**Path Parameters:**
- `workerId`: number

**Query Parameters (Pagination):**
- `page`: number (0-indexed)
- `size`: number (default: 20)

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "number",
      "complaintId": "number",
      "complaintTitle": "string",
      "complaintLocation": "string",
      "assignmentNotes": "string",
      "status": "ASSIGNED | IN_PROGRESS | SUBMITTED_FOR_REVIEW | APPROVED | REJECTED | COMPLETED | CANCELLED",
      "createdAt": "ISO-8601 timestamp",
      "updatedAt": "ISO-8601 timestamp"
    }
  ],
  "totalElements": "number",
  "totalPages": "number",
  "currentPage": "number",
  "pageSize": "number"
}
```

**Error Responses:**
- **401 Unauthorized:** Missing or invalid token
- **404 Not Found:** Worker does not exist

**Frontend Notes:**
- Workers view their own assignments
- Shows complaints assigned to them for action

---

## Resolution APIs

### 9. Submit Resolution
```
POST /api/resolutions
```

**Authentication:** Required (WORKER role)

**Request Body:**
```json
{
  "assignmentId": "number",
  "completionNotes": "string (required, 10-2000 characters)"
}
```

**Response (201 Created):**
```json
{
  "id": "number",
  "assignmentId": "number",
  "complaintId": "number",
  "completionNotes": "string",
  "reviewStatus": "PENDING",
  "submittedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **400 Bad Request:** Invalid assignment, missing notes
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not assigned worker
- **409 Conflict:** Resolution already submitted for this assignment

**Frontend Notes:**
- Worker submits work completion
- Notes should describe what was done
- Creates PENDING_VERIFICATION status on complaint
- One resolution per assignment

---

### 10. Approve Resolution
```
POST /api/resolutions/{resolutionId}/approve
```

**Authentication:** Required (DEPARTMENT_ADMIN role)

**Authorization:** ⚠️ Blocked by admin auth gap

**Path Parameters:**
- `resolutionId`: number

**Request Body:**
```json
{
  "reviewStatus": "APPROVED",
  "adminNotes": "string (optional)"
}
```

**Response (200 OK):**
```json
{
  "id": "number",
  "assignmentId": "number",
  "reviewStatus": "APPROVED",
  "adminNotes": "string",
  "reviewedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **400 Bad Request:** Already reviewed, not pending
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not a DEPARTMENT_ADMIN, worker attempting self-approval
- **404 Not Found:** Resolution does not exist

**Frontend Notes:**
- ⚠️ **BLOCKED:** Requires DEPARTMENT_ADMIN role (not implemented)
- Security: Prevents worker self-approval
- Approval marks complaint as RESOLVED

---

### 11. Reject Resolution
```
POST /api/resolutions/{resolutionId}/reject
```

**Authentication:** Required (DEPARTMENT_ADMIN role)

**Authorization:** ⚠️ Blocked by admin auth gap

**Path Parameters:**
- `resolutionId`: number

**Request Body:**
```json
{
  "reviewStatus": "REJECTED",
  "rejectionReason": "string (required)",
  "adminNotes": "string (optional)"
}
```

**Response (200 OK):**
```json
{
  "id": "number",
  "assignmentId": "number",
  "reviewStatus": "REJECTED",
  "rejectionReason": "string",
  "adminNotes": "string",
  "reviewedAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **400 Bad Request:** Already reviewed, not pending
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not a DEPARTMENT_ADMIN
- **404 Not Found:** Resolution does not exist

**Frontend Notes:**
- ⚠️ **BLOCKED:** Requires DEPARTMENT_ADMIN role
- Returns assignment to IN_PROGRESS status for rework
- Rejection reason is required

---

## File Upload APIs

### 12. Upload Evidence
```
POST /api/complaints/{complaintId}/evidence/upload
```

**Authentication:** Required (CITIZEN or WORKER)

**Path Parameters:**
- `complaintId`: number

**Request:**
- Content-Type: `multipart/form-data`
- Field name: `file`

**File Constraints:**
- Max size: 10 MB
- Allowed types: jpg, jpeg, png, pdf, doc, docx
- Max 5 files per complaint

**Response (201 Created):**
```json
{
  "id": "number",
  "complaintId": "number",
  "fileName": "string (original filename)",
  "fileType": "string (MIME type)",
  "fileSize": "number (bytes)",
  "uploadedBy": "string (email of uploader)",
  "createdAt": "ISO-8601 timestamp",
  "downloadUrl": "string (/api/complaints/{complaintId}/evidence/{evidenceId}/download)"
}
```

**Error Responses:**
- **400 Bad Request:** File too large, invalid type, empty file
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not CITIZEN or WORKER
- **404 Not Found:** Complaint does not exist
- **409 Conflict:** Maximum 5 files already uploaded

**Frontend Notes:**
- Implement file drag-and-drop UI
- Show file size limit warnings
- Validate file type client-side before upload
- After fix: prevents downloading of unrelated files
- `uploadedBy` shows who submitted evidence (helps with accountability)

---

### 13. List Evidence for Complaint
```
GET /api/complaints/{complaintId}/evidence
```

**Authentication:** Required (CITIZEN, WORKER, or DEPARTMENT_ADMIN)

**Path Parameters:**
- `complaintId`: number

**Response (200 OK):**
```json
[
  {
    "id": "number",
    "complaintId": "number",
    "fileName": "string",
    "fileType": "string",
    "fileSize": "number",
    "uploadedBy": "string",
    "createdAt": "ISO-8601 timestamp",
    "downloadUrl": "string"
  }
]
```

**Error Responses:**
- **401 Unauthorized:** Missing or invalid token
- **404 Not Found:** Complaint does not exist

**Frontend Notes:**
- Returns array of evidence for complaint
- Download URLs are included in response
- Shows who uploaded each file (audit trail)

---

### 14. Download Evidence
```
GET /api/complaints/{complaintId}/evidence/{evidenceId}/download
```

**Authentication:** Required (CITIZEN, WORKER, or DEPARTMENT_ADMIN)

**Path Parameters:**
- `complaintId`: number
- `evidenceId`: number

**Response (200 OK):**
- Binary file content
- Header: `Content-Disposition: attachment; filename="evidence.bin"`
- Content-Type: `application/octet-stream`

**Error Responses:**
- **400 Bad Request:** Evidence doesn't belong to this complaint (after security fix)
- **401 Unauthorized:** Missing or invalid token
- **404 Not Found:** Evidence does not exist

**Frontend Notes:**
- After security fix: Verifies evidence ownership (IDOR prevented)
- Browser will download file automatically
- Original filename sent in response header
- Requires authentication; not publicly downloadable

---

### 15. Delete Evidence
```
DELETE /api/complaints/{complaintId}/evidence/{evidenceId}
```

**Authentication:** Required (CITIZEN or DEPARTMENT_ADMIN)

**Path Parameters:**
- `complaintId`: number
- `evidenceId`: number

**Response (204 No Content):** Empty response

**Error Responses:**
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Only CITIZEN (uploader) or DEPARTMENT_ADMIN can delete
- **404 Not Found:** Evidence does not exist

**Frontend Notes:**
- No response body on success (204)
- Implement confirmation dialog before delete
- Only creator or admin can delete evidence

---

## Department APIs

### 16. Get Department Details
```
GET /api/departments/{departmentId}
```

**Authentication:** Required (DEPARTMENT_ADMIN or SYSTEM_ADMIN)

**Authorization:** ⚠️ BLOCKED by admin auth gap

**Path Parameters:**
- `departmentId`: number

**Response (200 OK):**
```json
{
  "id": "number",
  "name": "string",
  "description": "string",
  "headEmail": "string",
  "status": "ACTIVE | INACTIVE",
  "createdAt": "ISO-8601 timestamp"
}
```

**Error Responses:**
- **401 Unauthorized:** Missing or invalid token
- **403 Forbidden:** Not an admin
- **404 Not Found:** Department does not exist

---

## Analytics APIs

### 17. System Metrics
```
GET /api/analytics/system-metrics
```

**Authentication:** Required (SYSTEM_ADMIN)

**Authorization:** ⚠️ BLOCKED by admin auth gap

**Query Parameters:**
- `dateFrom`: ISO-8601 date (optional)
- `dateTo`: ISO-8601 date (optional)

**Response (200 OK):**
```json
{
  "totalComplaints": "number",
  "totalResolved": "number",
  "totalPending": "number",
  "averageResolutionDays": "number",
  "completionRate": "number (percentage)"
}
```

---

## Recurrence Analysis APIs

### 18. Analyze Recurrence
```
GET /api/recurrence/analysis
```

**Authentication:** Required (DEPARTMENT_ADMIN or SYSTEM_ADMIN)

**Authorization:** ⚠️ BLOCKED by admin auth gap

**Query Parameters:**
- `complaintId`: number

**Response (200 OK):**
```json
{
  "currentComplaints": "number",
  "historicalCount": "number",
  "category": "string",
  "location": "string",
  "proximityKm": "number (0.5)",
  "analysisNotes": "string",
  "relatedComplaints": ["array of complaint IDs"]
}
```

---

## General Conventions

### Authentication Header
All protected endpoints require:
```
Authorization: Bearer <accessToken>
```

### Response Status Codes
- **200 OK:** Successful GET
- **201 Created:** Successful POST (resource created)
- **204 No Content:** Successful DELETE or operation with no response body
- **400 Bad Request:** Invalid input, validation failure
- **401 Unauthorized:** Missing or invalid authentication
- **403 Forbidden:** Authenticated but insufficient permissions
- **404 Not Found:** Resource does not exist
- **409 Conflict:** Business logic conflict (duplicate, state violation)
- **500 Internal Server Error:** Server error (shouldn't happen)

### Pagination Response Format
```json
{
  "content": [...],
  "totalElements": number,
  "totalPages": number,
  "currentPage": number,
  "pageSize": number
}
```

### Error Response Format
```json
{
  "error": "string",
  "message": "string",
  "timestamp": "ISO-8601 timestamp"
}
```

### Timestamp Format
All timestamps use ISO-8601: `2026-10-26T11:30:00Z`

### Enum Values

**ComplaintStatus:**
- SUBMITTED
- ASSIGNED
- IN_PROGRESS
- PENDING_VERIFICATION
- APPROVED
- REJECTED
- RESOLVED
- CANCELLED

**ComplaintCategory:**
- ROAD_MAINTENANCE
- WATER_SUPPLY
- ELECTRICITY
- PUBLIC_HEALTH
- SANITATION

**AssignmentStatus:**
- ASSIGNED
- IN_PROGRESS
- SUBMITTED_FOR_REVIEW
- APPROVED
- REJECTED
- COMPLETED
- CANCELLED

**UserRole:**
- CITIZEN
- WORKER
- DEPARTMENT_ADMIN ⚠️ UNREACHABLE
- SYSTEM_ADMIN ⚠️ UNREACHABLE

---

## Frontend Implementation Checklist

- [ ] Implement citizen registration with password requirements
- [ ] Implement citizen login with token storage
- [ ] Implement complaint creation with map integration
- [ ] Implement complaint public tracking view (publicId)
- [ ] Implement citizen complaint history view
- [ ] Implement file upload with validation
- [ ] Implement file list and download
- [ ] Implement token refresh logic (optional - tokens valid 24h)
- [ ] Implement logout (client-side token removal)
- [ ] Implement error handling for 401/403 responses
- [ ] Implement pagination for list endpoints
- [ ] ⚠️ **ADMIN UI:** Blocked pending admin authentication implementation
  - Department management
  - Worker management
  - Assignment creation
  - Resolution approval/rejection
  - Analytics dashboard
  - Recurrence analysis

---

## Known Limitations & Blockers

1. **Admin Authentication Missing**
   - Status: Critical blocker for admin features
   - Impact: Cannot approve resolutions, manage departments, view analytics
   - Required Fix: Implement AdminUser entity or role escalation

2. **File Download IDOR**
   - Status: ✅ FIXED in audit
   - Impact: Now validates evidence ownership

3. **Audit Trail on File Upload**
   - Status: ✅ FIXED in audit
   - Impact: Captures user email for accountability

4. **Worker Self-Approval**
   - Status: ✅ FIXED in audit
   - Impact: Validation prevents self-approval

5. **No Email Notifications**
   - Status: Stub only (prints to console)
   - Impact: Users don't receive notifications
   - Required: Integrate JavaMailSender + SMTP config

---

## Deployment Requirements

**Environment Variables:**
```bash
DB_URL=jdbc:mysql://localhost:3306/upr
DB_USERNAME=app_user
DB_PASSWORD=<secure_password>
JWT_SECRET=<256-bit-minimum-32-characters>
```

**File Upload Directory:**
```bash
/var/uploads/  (must be writable by application)
```

**Database:**
- MySQL 8.0 or higher
- Database name: `upr`

---

**Document End**
