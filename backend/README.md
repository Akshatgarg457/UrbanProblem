# Urban Problem Reporting System - Backend API

A comprehensive Spring Boot REST API for managing urban infrastructure complaints, worker assignments, and resolution verification with enterprise-grade role-based access control.

## Quick Start

### Requirements

- **Java**: JDK 21 (https://openjdk.org/projects/jdk/21/)
- **Build Tool**: Maven 3.10.0+ (included: Maven Wrapper)
- **Database**: MySQL 5.7+ (production) or H2 (testing, included)
- **Environment**: Linux, macOS, or Windows

### 1. Clone and Setup Environment

```bash
cd /path/to/project
cp .env.example .env
```

Edit `.env` with your MySQL connection details:
```properties
DB_URL=jdbc:mysql://localhost:3306/upr
DB_USERNAME=root
DB_PASSWORD=your_secure_password
```

Sessions are stored in MySQL. Flyway creates the session tables from `V3__server_side_sessions.sql`.
Set `SESSION_COOKIE_SECURE=true` when serving the application over HTTPS; leave it false for local HTTP.

### 2. Create MySQL Database

```sql
CREATE DATABASE upr 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

Or using Docker:
```bash
docker run --name mysql_urban \
  -e MYSQL_ROOT_PASSWORD=your_password \
  -e MYSQL_DATABASE=upr \
  -p 3306:3306 \
  -d mysql:8.0
```

### 3. Build and Run

```bash
# Linux/macOS
./mvnw clean compile
./mvnw test              # Run tests (uses H2)
./mvnw spring-boot:run   # Start development server

# Windows
.\mvnw.cmd clean compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

The application starts on `http://localhost:8080`

---

## Core Features

### 1. Citizen Complaint Submission
- Register and manage citizen accounts
- Submit location-specific infrastructure complaints
- Track complaint status in real-time
- Upload supporting evidence (photos, documents)

### 2. Department Management
- Automatic complaint routing to appropriate departments
- Pre-configured departments for each complaint category:
  - Roads and Potholes → Public Works
  - Water Supply → Water and Sanitation
  - Electricity & Streetlights → Electricity/Utilities
  - Drainage & Sewage → Water and Sanitation
  - Waste Management → Waste Management

### 3. Worker Assignment & Progress
- Department admins assign complaints to eligible workers
- Workers track assigned tasks and update progress
- Submit completion notes and resolution evidence
- Reject and rework with admin feedback

### 4. Verification & Approval
- Admin review of completed work
- Approve resolutions with optional notes
- Request rework with specific rejection reasons
- Maintain resolution audit trail

### 5. Recurrence Analysis (TODO)
- Identify patterns in repeated complaints
- Location-based proximity analysis
- Category-based trend detection
- Support preventive infrastructure planning

### 6. Role-Based Access Control
- **CITIZEN**: Submit and track complaints
- **WORKER**: Accept assignments and submit completions
- **DEPARTMENT_ADMIN**: Manage assignments and verify resolutions
- **SYSTEM_ADMIN**: Manage users, departments, and system settings

---

## API Reference

### Authentication Endpoints
```
POST   /api/auth/register     Register new citizen
GET    /api/auth/csrf         Initialize CSRF token cookie
POST   /api/auth/login        Login and establish a server-side session
GET    /api/auth/me           Get the current session user
POST   /api/auth/logout       Invalidate the server-side session
```

### Complaint Endpoints
```
POST   /api/complaints                    Submit complaint (CITIZEN)
GET    /api/complaints/{publicId}         Get complaint details
GET    /api/complaints/my-complaints      List my complaints (CITIZEN)
GET    /api/complaints/department/{id}    List department complaints (ADMIN)
GET    /api/complaints                    List all complaints (SYSTEM_ADMIN)
```

### Assignment Endpoints
```
POST   /api/assignments/complaints/{id}/assign    Assign to worker (DEPT_ADMIN)
GET    /api/assignments/my-assignments             Get my assignments (WORKER)
GET    /api/assignments/my-assignments/active      Get active assignments (WORKER)
GET    /api/assignments/{id}                       Get assignment details
```

### Resolution Endpoints
```
POST   /api/resolutions/assignments/{id}/submit   Submit completion (WORKER)
POST   /api/resolutions/{id}/approve              Approve resolution (DEPT_ADMIN)
POST   /api/resolutions/{id}/reject               Reject resolution (DEPT_ADMIN)
GET    /api/resolutions/{id}                      Get resolution details
GET    /api/resolutions/pending                   List pending reviews (DEPT_ADMIN)
```

### Department Endpoints
```
POST   /api/departments                   Create department (SYSTEM_ADMIN)
GET    /api/departments                   List all departments (SYSTEM_ADMIN)
GET    /api/departments/{id}              Get department details
PUT    /api/departments/{id}              Update department (SYSTEM_ADMIN)
PUT    /api/departments/{id}/status       Change status (SYSTEM_ADMIN)
```

### Worker Endpoints
```
POST   /api/workers                           Register worker (SYSTEM_ADMIN)
GET    /api/workers/{id}                      Get worker details
GET    /api/workers/department/{deptId}       List department workers
PUT    /api/workers/{id}/status               Change status (SYSTEM_ADMIN)
PUT    /api/workers/{id}/department           Transfer department (SYSTEM_ADMIN)
```

---

## Example Workflows

### Current session authentication

The API now uses an HttpOnly session cookie, not bearer tokens. For command-line requests,
keep a cookie jar, initialize CSRF once, and send the returned CSRF token on each
state-changing request:

```bash
curl -c cookies.txt http://localhost:8080/api/auth/csrf -o csrf.json
CSRF_TOKEN=$(jq -r .token csrf.json)

curl -b cookies.txt -c cookies.txt \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"email":"citizen@example.org","password":"your-password","workspace":"CITIZEN"}' \
  http://localhost:8080/api/auth/login

curl -b cookies.txt http://localhost:8080/api/auth/me
```

In the workflows below, replace any legacy bearer-token header with `-b cookies.txt`;
add `-H "X-XSRF-TOKEN: $CSRF_TOKEN"` to POST, PUT, PATCH, and DELETE requests.

### Workflow 1: Citizen Submits Complaint

```bash
# 1. Register as citizen
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Smith",
    "phoneNumber": "5551234567",
    "password": "SecurePass123!"
  }'
# Returns account metadata; authenticate with POST /api/auth/login to create a session.

# 2. Submit complaint
TOKEN="eyJhbGc..."
curl -X POST http://localhost:8080/api/complaints \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Large pothole on Main Street",
    "description": "A dangerous pothole at the intersection of Main and Oak",
    "category": "ROADS_AND_POTHOLES",
    "location": "123 Main Street, Downtown",
    "latitude": 40.7128,
    "longitude": -74.0060
  }'
# Returns: { publicId, status: "SUBMITTED", createdAt, ... }

# 3. Check complaint status
PUBLIC_ID="abc-123-def"
curl http://localhost:8080/api/complaints/$PUBLIC_ID
# Returns current status and assignment details
```

### Workflow 2: Admin Assigns Worker

```bash
# 1. Login as department admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@publicworks.gov",
    "password": "AdminPass123!"
  }'
# Returns account details and creates a server-side session.

# 2. Assign complaint to worker
TOKEN="eyJhbGc..."
curl -X POST http://localhost:8080/api/assignments/complaints/1/assign \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "workerId": 5,
    "assignmentNotes": "High priority - intersection safety risk"
  }'
# Returns: { assignmentId, status: "ASSIGNED", ... }

# 3. View assigned workers and their workload
curl http://localhost:8080/api/workers/department/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Workflow 3: Worker Submits Completion

```bash
# 1. Login as worker
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "worker@example.com",
    "password": "WorkerPass123!"
  }'
# Returns account details and creates a server-side session.

# 2. View assigned tasks
TOKEN="eyJhbGc..."
curl http://localhost:8080/api/assignments/my-assignments/active \
  -H "Authorization: Bearer $TOKEN"

# 3. Update progress
curl -X POST http://localhost:8080/api/resolutions/assignments/1/submit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "completionNotes": "Filled pothole with asphalt, repacked and sealed. Work verified safe."
  }'
# Returns: { resolutionId, status: "PENDING", ... }
```

### Workflow 4: Admin Reviews and Approves

```bash
# 1. Login as admin
TOKEN="eyJhbGc..."

# 2. View pending resolutions
curl http://localhost:8080/api/resolutions/pending \
  -H "Authorization: Bearer $TOKEN"

# 3. Approve resolution
curl -X POST http://localhost:8080/api/resolutions/1/approve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewStatus": "APPROVED",
    "adminNotes": "Excellent work, verified on-site"
  }'
# Returns: { status: "APPROVED", reviewedAt, ... }
```

---

## Database Schema

### Entities

| Entity | Description |
|--------|-------------|
| **Citizen** | Residents who report complaints |
| **Department** | Municipal agencies (Public Works, Utilities, etc.) |
| **Worker** | Department employees assigned to fix issues |
| **Complaint** | Infrastructure issues reported |
| **Assignment** | Worker assignment to a complaint |
| **Resolution** | Work completion details |
| **RecurrenceReport** | Pattern analysis for repeated issues |
| **ComplaintStatusHistory** | Audit trail of status changes |

See `IMPLEMENTATION_CHECKLIST.md` for complete ER diagram and field details.

---

## Security

### Authentication
- **Type**: Server-side Spring Security session stored in MySQL through Spring Session JDBC
- **Cookie**: `CGSESSION`, HttpOnly, SameSite=Lax; Secure is enabled with `SESSION_COOKIE_SECURE=true`
- **Expiration**: 24 hours of inactivity; logout invalidates the server-side session
- **CSRF**: Enabled for state-changing requests using the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header

### Authorization
- Role-based access control via `@PreAuthorize` annotations
- Entity-level access checks in services (IDOR prevention)
- Department-scoped access for admins
- User scoped access for citizens and workers

### Password Security
- **Hashing**: BCrypt with 10+ rounds
- **Never**: Passwords logged, stored plaintext, returned in API responses
- **Validation**: 8-128 character passwords with requirements

### Data Protection
- SQL injection: Prevented via JPA parameterized queries
- CSRF: Enabled for session-authenticated requests
- XSS: Handled by frontend (not applicable to REST API)
- Secrets: Environment variables only, never in code/logs

### Limitations (TODO)
- ⚠️ Rate limiting (recommended: implement in production)
- ⚠️ HTTPS enforcement (required in production)
- ⚠️ Audit logging (recommended for compliance)
- ⚠️ File upload validation (in progress)

---

## Testing

### Run Test Suite
```bash
./mvnw clean test                    # Compile and run all tests
./mvnw verify                        # Full verification lifecycle
./mvnw test -Dtest=ComplaintServiceTest  # Run one test class
```

### Test Configuration
- **Database**: H2 in-memory (no MySQL required)
- **Profile**: `test`, activated with `@ActiveProfiles("test")` on the test classes
- **Isolation**: Each test context uses `create-drop` DDL and an H2-backed session table
- **Spring Boot 4 modules**: `spring-boot-starter-webmvc-test` provides `AutoConfigureMockMvc`;
  `spring-boot-starter-data-jpa-test` provides `DataJpaTest`

### Covered Test Areas
- [x] Service tests (`ComplaintServiceTest`)
- [x] Controller integration tests (`AuthControllerIntegrationTest`, `APIContractIntegrationTest`)
- [x] Authorization tests (`SecurityRegressionTest`)
- [x] Cross-role access denial tests
- [x] Happy path and error scenarios

---

## Configuration Reference

### Environment Variables
```bash
# Required
DB_URL              # JDBC URL: jdbc:mysql://host:port/database
DB_USERNAME         # MySQL username
DB_PASSWORD         # MySQL password
SESSION_COOKIE_SECURE # Set true when using HTTPS

# Optional (defaults shown)
spring.session.timeout=24h  # Session idle timeout
spring.jpa.hibernate.ddl-auto=validate  # Don't modify schema
```

Schema changes are managed by Flyway. On startup, Flyway applies pending scripts from
`src/main/resources/db/migration/` before Hibernate validates the schema. Back up the
database before deploying an application version with new migrations; do not run migration
scripts manually or change `ddl-auto` to `update`. Production configuration forces
Hibernate's schema action to `validate` even if an environment override requests creation
or deletion. `create-drop` is reserved for test contexts backed by in-memory H2.

### Application Properties
```properties
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.show-sql=false
spring.datasource.hikari.maximum-pool-size=10
logging.level.com.urbanproblems=INFO
```

### Test Properties
```properties
spring.datasource.url=jdbc:h2:mem:upr_test
spring.jpa.hibernate.ddl-auto=create-drop
jwt.secret=test-secret-key-for-testing-only-32-chars-minimum
```

---

## Troubleshooting

### "Set DB_URL to the MySQL JDBC URL" Error
**Cause**: Environment variable not set  
**Solution**: 
```bash
export DB_URL=jdbc:mysql://localhost:3306/upr
```

### "Communications link failure" to MySQL
**Cause**: MySQL not running or connection details wrong  
**Solution**:
```bash
# Check MySQL running
sudo systemctl status mysql

# Verify connection
mysql -h localhost -u root -p -e "SHOW DATABASES;"

# Or test Docker container
docker ps | grep mysql
```

### "Tests fail with 'No qualifying bean'"
**Cause**: Test profile not active  
**Solution**: Verify `src/test/resources/application-test.properties` exists

---

## Performance Tuning

### Connection Pooling
- **Type**: HikariCP (default, optimized)
- **Max Pool Size**: 10 connections
- **Min Idle**: 5 connections
- **Timeout**: 20 seconds

### Query Optimization
- ✅ Lazy loading on entity relationships
- ✅ Pagination on all list endpoints
- ✅ Indexed foreign keys and unique constraints
- 📋 TODO: Add caching for departments (changes infrequently)
- 📋 TODO: Add database query result caching

### Monitoring
- Configure Spring Boot Actuator (TODO)
- Enable query logging for slow queries (via properties)
- Monitor memory/CPU usage in production

---

## Project Status

See `IMPLEMENTATION_CHECKLIST.md` for complete implementation status:

### ✅ Complete (Modules 1-8)
- Build configuration with Maven
- 7 core domain entities + repositories
- JDBC-backed session authentication and security
- Complaint lifecycle management
- Assignment and verification workflows
- Department and worker administration

### 🔄 In Progress
- File upload and evidence storage
- Recurrence analysis algorithms
- Integration tests and test coverage

### 📋 Planned
- Pagination utilities
- OpenAPI/Swagger documentation
- Email notifications
- System admin provisioning
- Audit logging

---

## Directory Structure

```
src/main/java/com/urbanproblems/
├── config/              # Spring configuration
├── controller/          # REST endpoint handlers
├── domain/
│   ├── entity/         # JPA entities
│   └── enums/          # Status enums
├── dto/                # Request/response models
├── exception/          # Custom exceptions
├── repository/         # Data access layer (JPA)
├── security/           # Session, CSRF, and authentication
└── service/            # Business logic layer

src/main/resources/
├── application.properties       # Production config
└── db/migration/               # Versioned Flyway schema migrations

src/test/java/...              # Integration tests
src/test/resources/
└── application-test.properties # Test config
```

---

## Contributing

1. Create feature branch from `develop`
2. Add tests for new functionality
3. Follow existing code style (Lombok for models)
4. Update documentation
5. Submit PR with clear description

### Code Guidelines
- Use Lombok `@Getter @Setter @Builder` for entities
- Services contain business logic, not controllers
- DTOs for all API contracts (never expose entities)
- `@Transactional` on service methods modifying data
- `@PreAuthorize` for all protected endpoints

---

## Deployment

### Local Development
```bash
./mvnw spring-boot:run
```

### Docker Build
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY target/urban-problem-reporting-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t urban-problem-reporting:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://mysql:3306/upr \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=password \
  -e SESSION_COOKIE_SECURE=true \
  urban-problem-reporting:latest
```

### Cloud Deployment (AWS, Azure, GCP)
- Set environment variables in cloud platform
- Ensure MySQL is accessible from application
- Use `ddl-auto=validate` (don't auto-modify schema)
- Enable HTTPS in application or load balancer
- Monitor logs via cloud logging service

---

## Support & Documentation

- **API Docs**: See endpoint reference above or check controllers
- **Schema**: See `IMPLEMENTATION_CHECKLIST.md` for entity details
- **Status**: See `IMPLEMENTATION_CHECKLIST.md` for project progress
- **Issues**: Check logs and troubleshooting section above

---

**Last Updated**: October 25, 2026  
**Version**: 0.0.1-SNAPSHOT  
**Status**: Core features complete, testing and deployment in progress
