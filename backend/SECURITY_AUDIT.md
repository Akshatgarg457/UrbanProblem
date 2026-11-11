# Security audit: status after verification pass (2026-10-31)

Method: source review of the current code, with each claim tied to a file. Runtime verification is
NOT possible in this environment (see `TEST_REPORT.md`), so every item below is a code-review result,
not a test result. Status labels: VERIFIED (checked by a harness that runs real code), FIXED (changed,
verified by reading only), NOT TESTED (changed, not executed), FAILED (known to fail), BLOCKED.

## Fixes confirmed in current source

| # | Issue | Fix | Where | Status |
|---|---|---|---|---|
| 1 | Admin roles unreachable: no admin accounts, login ignored admin table | `AdminUser` entity, `CustomUserDetailsService` admin branch, role-based principal | `domain/entity/AdminUser.java`, `service/CustomUserDetailsService.java` | FIXED (read); login NOT TESTED |
| 2 | Admin account creation insecure / unauthenticated | System-admin-only endpoint; bootstrap from env vars only; password minimum 12 | `controller/AdminController.java`, `config/AdminBootstrapInitializer.java` | FIXED (read); NOT TESTED |
| 3 | Bootstrap email stored un-normalized (system admin could not log in) | Trim and lower-case on bootstrap | `config/AdminBootstrapInitializer.java` | FIXED (read) |
| 4 | Complaint readable by any authenticated user | Object-level check on every read | `security/AccessRules.requireComplaintVisible`, `service/ComplaintService.java` | VERIFIED (harness, 14 authz checks) |
| 5 | Department admins act across departments | Department scope on list, assign, review, analyze | `security/AccessRules.java`, `service/AssignmentService.java`, `service/ResolutionService.java`, `service/RecurrenceService.java` | VERIFIED (harness) |
| 6 | Worker reads another worker's assignment | Assignee check | `service/AssignmentService.getAssignment` | FIXED (read); NOT TESTED |
| 7 | Worker submits resolution for another worker's task | Assignee check before submit | `service/ResolutionService.submitResolution` | FIXED (read); NOT TESTED |
| 8 | Evidence readable/deletable by any user with the role | Complaint visibility check on list, download, upload; uploader-only delete | `service/FileUploadService.java` | FIXED (read); NOT TESTED |
| 9 | Evidence served as public static files (`/uploads/**`) | Handler removed; files served only via service | `config/FileUploadConfiguration.java` | FIXED (read); test in `SecurityRegressionTest` NOT TESTED |
| 10 | File type trusted from client MIME/extension | Extension allow-list fails closed; content must match magic bytes | `service/FileUploadService.validateFile` | FIXED (read); NOT TESTED |
| 11 | Stored filename from client | Disk name is random UUID; path checked to stay inside upload dir; display name sanitized | `service/FileUploadService.java` | FIXED (read) |
| 12 | Password hash exposed in JSON | `@JsonIgnore` on `passwordHash` in Citizen, Worker, AdminUser; recurrence returns DTO | `domain/entity/*.java`, `dto/RecurrenceReportResponseDto.java` | FIXED (read; grep-confirmed on all 3 entities) |
| 13 | JPA entities returned from controllers | All controllers return DTOs (grep-confirmed) | `controller/*` | FIXED (read) |
| 14 | Mass assignment via registration (role) | Registration DTO has no role field; citizen role forced | `service/AuthService.registerCitizen` | FIXED (read); test NOT TESTED |
| 15 | Cross-table email collision (one address as citizen and worker) | Uniqueness checked across citizen, worker, admin at every registration path | `AuthService`, `WorkerService`, `AdminService` | FIXED (read) |
| 16 | Email case mismatch login | Login and all writes normalize to lower case | `AuthService.login`, all writes | FIXED (read) |
| 17 | JWT code used wrong API (compile risk) | JJWT 0.12 `Jwts.parser().verifyWith(...).parseSignedClaims(...)` | `security/JwtTokenProvider.java` | FIXED (read); compile NOT TESTED |
| 18 | Weak or missing JWT secret accepted | Startup fails if secret missing or under 32 bytes | `JwtTokenProvider.init` (@PostConstruct) | FIXED (read); NOT TESTED |
| 19 | Unauthenticated gets 403 | 401 via authentication entry point; 403 via access-denied handler | `config/SecurityConfiguration.java` | FIXED (read); NOT TESTED |
| 20 | Login error reveals account existence | Same 401 for unknown email and wrong password | `controller/AuthController.login` | FIXED (read) |
| 21 | No CORS for React frontend | Explicit origin list from env; credentials disabled | `config/SecurityConfiguration.corsConfigurationSource` | FIXED (read); NOT TESTED |
| 22 | Recurrence reports visible to citizens and workers | Restricted to department and system admins | `controller/RecurrenceController.java` | FIXED (read) |
| 23 | Resolution approve accepted `REJECTED` payload | Action must match reviewStatus | `service/ResolutionService.approveResolution`/`rejectResolution` | FIXED (read) |
| 24 | Rejection without reason | Reason required | `service/ResolutionService.rejectResolution` | FIXED (read) |
| 25 | Self-review check compared ids across tables | Compare only when reviewer role is WORKER | `service/ValidationService.validateResolutionReview` | FIXED (read) |

## Remaining issues

| # | Severity | Issue | Status / recommendation |
|---|---|---|---|
| R1 | High (until verified) | No runtime test has executed any security control. Every "FIXED (read)" item above is unproven until `./mvnw verify` passes. | BLOCKED. Run the Windows commands in `TEST_REPORT.md`. |
| R2 | Medium | JWTs are stateless. A deactivated citizen or worker keeps access until the token expires (24 h). Logout does not revoke. | Open. Fix by checking account status per request, or a shorter TTL plus refresh. |
| R3 | Medium | No rate limiting or lockout on `/api/auth/login` or registration. | Open. Add a limiter at the gateway or a filter before production. |
| R4 | Medium | Legacy evidence rows (before this change) store an email in `uploaded_by` and have no `uploaded_by_id`. They can never be deleted by their uploader and have no uploader identity. | Accepted. Documented in migration. |
| R5 | Medium | Evidence download uses `Authorization` header. A plain `<a href>` in the browser cannot send it. The frontend must fetch as a blob. | Open. Frontend concern. See compatibility doc. |
| R6 | Low | `AuditLogService` prints email addresses to stdout. Nothing calls it, so no data leaves through it today. | Open. Replace with structured logging before any use. |
| R7 | Low | Upload count limit is checked without a lock. Two concurrent uploads can exceed the maximum by one. | Open. Low impact. |
| R8 | Low | Evidence files on local disk. No antivirus scanning. Total storage is not capped beyond file count and per-file size. | Accepted for local development. |
| R9 | Low | Workers can keep uploading evidence after a complaint is RESOLVED. Not blocked. | Open. Add a status check in `FileUploadService.uploadEvidence` if the business rule requires it. |
| R10 | Info | Default BCrypt strength (10). | Acceptable. |
| R11 | Info | `ApiResponse` error body (`GlobalExceptionHandler`) and the 401/403 body from the security layer differ in fields (`timestamp`, `data`). | Not a security issue. See compatibility doc. |

## Status

No critical issue is known from source review. None of the controls has been executed at runtime, so this
system is NOT production-ready. Do not deploy until `./mvnw verify` passes on your machine and the
lifecycle procedure in `DEV_SETUP_WINDOWS.md` has been run against a real database.
