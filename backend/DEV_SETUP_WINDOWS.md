# Local setup, role accounts, and lifecycle test (Windows, PowerShell)

Status: NOT TESTED. These are the steps to run on your laptop. None of them has been executed in
the verification environment. Report the first step that fails and the exact output.

No passwords are stored in this repository. You choose them when you run the commands.

## 1. Prerequisites

- JDK 21 installed; `java -version` shows 21.
- MySQL 8 running; you know the root (or app) user and password.
- Port 8080 free (or change `server.port`).

## 2. Create `.env` (not committed; git-ignored)

In the project folder, copy `.env.example` to `.env` and edit it. Generate a JWT secret:

```powershell
$b = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

Put the output in `JWT_SECRET`. Set these keys (examples, not secrets):

    DB_URL=jdbc:mysql://localhost:3306/upr
    DB_USERNAME=your_mysql_user
    DB_PASSWORD=your_mysql_password
    JWT_SECRET=<output above>
    FILE_UPLOAD_DIR=C:/urban-problem-uploads/
    CORS_ALLOWED_ORIGINS=http://localhost:5173
    BOOTSTRAP_SYSTEM_ADMIN_EMAIL=you@example.org
    BOOTSTRAP_SYSTEM_ADMIN_PASSWORD=<at least 12 characters, you choose>

Create the upload folder: `New-Item -ItemType Directory -Force C:\urban-problem-uploads`.

## 3. Database

Follow `docs/MIGRATION_RUNBOOK_WINDOWS.md`. For a brand-new empty database, let Hibernate create the
schema once with `SPRING_JPA_HIBERNATE_DDL_AUTO=update` set in your shell, then remove it.

Important: `ddl-auto=validate` must pass at startup. If it fails, the message names the missing column or table.

## 4. Start the backend

    .\mvnw.cmd spring-boot:run

Expected log line once: `Created initial SYSTEM_ADMIN account.` On later starts, nothing is created.
If you see `No SYSTEM_ADMIN exists and BOOTSTRAP_SYSTEM_ADMIN_EMAIL/PASSWORD are not set`, `.env` was not read.
Department rows (Water and Sanitation, Roads and Infrastructure, Electricity, Waste Management) are created
at startup if missing.

After the first system admin exists, remove the `BOOTSTRAP_SYSTEM_ADMIN_*` lines from `.env`.

## 5. Accounts for the four roles

All commands use `curl.exe` (built into Windows 10 and later). Set `$base` once:

```powershell
$base = "http://localhost:8080"
function Login($email, $password) {
  $r = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType "application/json" `
       -Body (@{ email = $email; password = $password } | ConvertTo-Json)
  return $r.accessToken
}
```

Passwords come from `Read-Host` so they are not written into the script:

```powershell
$sysEmail = Read-Host "System admin email"
$sysPass  = Read-Host "System admin password"
$sysToken = Login $sysEmail $sysPass
```

System admin creates a department admin. First read department ids:

```powershell
$h = @{ Authorization = "Bearer $sysToken" }
Invoke-RestMethod -Uri "$base/api/departments?size=50" -Headers $h | Select-Object -ExpandProperty content | Format-Table id, name
```

```powershell
$deptAdminPass = Read-Host "Department admin password (12+ chars)"
Invoke-RestMethod -Method Post -Uri "$base/api/admin/department-admins" -Headers $h -ContentType "application/json" `
  -Body (@{ email = "roads.admin@example.org"; firstName = "Roads"; lastName = "Admin"; password = $deptAdminPass; departmentId = <ROADS_ID> } | ConvertTo-Json)
```

System admin creates a worker in the same department:

```powershell
$workerPass = Read-Host "Worker password (8+ chars)"
Invoke-RestMethod -Method Post -Uri "$base/api/workers" -Headers $h -ContentType "application/json" `
  -Body (@{ email = "worker1@example.org"; firstName = "Ravi"; lastName = "Worker"; phoneNumber = "9999900001"; password = $workerPass; departmentId = <ROADS_ID> } | ConvertTo-Json)
```

Citizen: `POST /api/auth/register` (self-service). A citizen signs up in the app.

Expected role matrix after setup (what each login should be able to do):

| Action | CITIZEN | WORKER | DEPARTMENT_ADMIN | SYSTEM_ADMIN |
|---|---|---|---|---|
| Create complaint | yes | 403 | 403 | 403 |
| Read own complaint | yes | 403 (unless assigned) | 403 (other dept) / yes (own dept) | yes |
| Assign worker | 403 | 403 | own dept only | 403 |
| Create department admin | 403 | 403 | 403 | yes |
| Create worker | 403 | 403 | 403 | yes |
| Upload evidence | own complaint | assigned only | 403 | 403 |
| Approve or reject resolution | 403 | 403 | own dept only | 403 |
| Recurrence reports | 403 | 403 | own dept | yes |
| Analytics | 403 | 403 | 403 | yes |

Confirm each 403 with the matching login. A 403 where "yes" is expected is a failure to report.

## 6. Lifecycle test (manual, end to end)

Use two citizens, one department admin for ROADS, one worker in the same department. Keep the
tokens in variables. Replace the `<...>` values with the ids you see in the responses.

1. Citizen registers and logs in:
```powershell
$cit = Invoke-RestMethod -Method Post -Uri "$base/api/auth/register" -ContentType "application/json" `
  -Body (@{ email="citizen1@example.org"; firstName="Asha"; lastName="K"; phoneNumber="9999911111"; password="CitizenPass123" } | ConvertTo-Json)
$citToken = $cit.accessToken
```

2. Citizen submits a roads complaint. Expect 201, `departmentName` = "Roads and Infrastructure", `status` = "SUBMITTED":
```powershell
$c = Invoke-RestMethod -Method Post -Uri "$base/api/complaints" -Headers @{Authorization="Bearer $citToken"} -ContentType "application/json" `
  -Body (@{ title="Pothole near bus stop"; description="Deep pothole, risky for two wheelers"; category="ROADS_AND_POTHOLES"; location="Main Road, Sector 4"; latitude=22.7196; longitude=75.8577 } | ConvertTo-Json)
$c.id; $c.publicId; $c.departmentName
```

3. Department admin lists pending complaints and assigns the worker. Expect 201; the complaint becomes ASSIGNED:
```powershell
$daToken = Login "roads.admin@example.org" $deptAdminPass
$as = Invoke-RestMethod -Method Post -Uri "$base/api/assignments/complaints/$($c.id)/assign" -Headers @{Authorization="Bearer $daToken"} -ContentType "application/json" `
  -Body (@{ workerId = <WORKER_ID>; assignmentNotes = "Crew 2" } | ConvertTo-Json)
$as.id
```

4. Worker starts work. Expect 200, assignment IN_PROGRESS:
```powershell
$wToken = Login "worker1@example.org" $workerPass
Invoke-RestMethod -Method Post -Uri "$base/api/assignments/$($as.id)/start" -Headers @{Authorization="Bearer $wToken"}
```

5. Worker uploads evidence (real file). `curl.exe` sends multipart. The file must genuinely be a PDF, PNG, or JPG:
```powershell
curl.exe -s -X POST -H "Authorization: Bearer $wToken" -F "file=@C:\temp\pothole.jpg" "$base/api/complaints/$($c.id)/evidence/upload"
```

6. Worker submits resolution. Expect 201, resolution PENDING:
```powershell
$res = Invoke-RestMethod -Method Post -Uri "$base/api/resolutions/assignments/$($as.id)/submit" -Headers @{Authorization="Bearer $wToken"} -ContentType "application/json" `
  -Body (@{ completionNotes = "Patched and compacted the pothole" } | ConvertTo-Json)
```

7. Department admin rejects for rework. Expect 200, complaint back to IN_PROGRESS:
```powershell
$pending = Invoke-RestMethod -Uri "$base/api/resolutions/pending" -Headers @{Authorization="Bearer $daToken"}
$rid = $pending.content[0].id
Invoke-RestMethod -Method Post -Uri "$base/api/resolutions/$rid/reject" -Headers @{Authorization="Bearer $daToken"} -ContentType "application/json" `
  -Body (@{ reviewStatus="REJECTED"; rejectionReason="Surface not compacted enough" } | ConvertTo-Json)
```

8. Worker resubmits. Expect 201:
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/resolutions/assignments/$($as.id)/submit" -Headers @{Authorization="Bearer $wToken"} -ContentType "application/json" `
  -Body (@{ completionNotes = "Compacted and re-surfaced; checked" } | ConvertTo-Json)
```

9. Department admin approves. Expect 200, complaint RESOLVED:
```powershell
Invoke-RestMethod -Method Post -Uri "$base/api/resolutions/$rid/approve" -Headers @{Authorization="Bearer $daToken"} -ContentType "application/json" `
  -Body (@{ reviewStatus="APPROVED"; adminNotes="Verified on site" } | ConvertTo-Json)
Invoke-RestMethod -Uri "$base/api/complaints/$($c.publicId)" -Headers @{Authorization="Bearer $citToken"} | Select-Object status
```

### Invalid transitions to try (each must be rejected)

| Attempt | Expected |
|---|---|
| Assign the same complaint again (step 3 repeated) | 409 |
| Worker calls `start` twice | 409 |
| Worker submits before uploading evidence | 409 |
| Approve a resolution that is already APPROVED | 409 |
| Approve with `reviewStatus="REJECTED"` | 400 |
| Reject with no `rejectionReason` | 400 |
| Citizen calls `/start` or `/approve` | 403 |
| Another citizen reads the complaint by `publicId` | 403 |

Record each result. Anything different from the expected column is a failure.
