# Test report — Spring Boot 4 verification (2026-10-31)

## Result

All required Maven lifecycle commands complete successfully on JDK 21.

| Command | Result | Details |
|---|---|---|
| `.\mvnw.cmd clean compile` | PASS | 83 main source files compiled |
| `.\mvnw.cmd clean test` | PASS | 59 tests, 0 failures, 0 errors, 0 skipped |
| `.\mvnw.cmd package` | PASS | `target/urban-problem-reporting-0.0.1-SNAPSHOT.jar` produced |
| `.\mvnw.cmd verify` | PASS | Full lifecycle verified |

Runtime used: Temurin JDK 21.0.12.1, Maven Wrapper (Maven 3.10+ enforced), Spring Boot
4.0.9, Spring Framework 7.0.x, Spring Security 7.0.x, Hibernate 7.2.25.Final, Jackson 3.1.7,
H2 in-memory.

## Root causes and fixes

### 1. Spring Boot 4 relocated the test annotations

Spring Boot 4 split `spring-boot-test-autoconfigure` into module-specific test artifacts and
moved the annotations out of the old `org.springframework.boot.test.autoconfigure.*` packages.
The compiler errors (`package ... does not exist`) came from the Boot 3 import paths, not from a
missing starter.

| Boot 3 package | Spring Boot 4 package |
|---|---|
| `org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |

The correct modular dependencies were already declared in `pom.xml` and were retained with test
scope:

- `spring-boot-starter-webmvc-test` (provides `AutoConfigureMockMvc` and MockMvc support)
- `spring-boot-starter-data-jpa-test` (provides `DataJpaTest`)
- `spring-boot-starter-security-test`, `spring-boot-starter-validation-test`, `spring-boot-starter-test`

No dependency had to be added; only the imports were stale.

### 2. Hibernate 7 rejects `scale` on floating-point columns

`Dialect$SizeStrategyImpl` throws `scale has no meaning for SQL floating point types` for the
`Double` coordinate fields annotated with `@Column(precision, scale)`. The documented production
schema uses `DECIMAL(10,8)` / `DECIMAL(11,8)`, so the fields now declare
`@JdbcTypeCode(SqlTypes.DECIMAL)` while keeping the existing `precision`/`scale`. The Java API
type remains `Double`, and the generated column type stays `DECIMAL`; no migration is required.

### 3. Spring Framework 7 exception-handler ambiguity

`ResponseEntityExceptionHandler` now maps `MethodArgumentNotValidException` through its own
`@ExceptionHandler`. A second `@ExceptionHandler` for the same type in
`GlobalExceptionHandler` made the resolver fail at startup. The handler now overrides
`handleMethodArgumentNotValid(...)`, keeping the custom `VALIDATION_ERROR` response unchanged.

### 4. Jackson 3 has no Jackson 2 `ObjectMapper` bean

Spring Boot 4 auto-configures Jackson 3 (`tools.jackson.databind.ObjectMapper`).
`SecurityRegressionTest` autowired the Jackson 2 mapper, which is no longer a bean. The test now
creates its own mapper, matching the other test classes; the application itself has no change.

### 5. Incorrect test fixtures

These were test expectations that contradicted the real API contracts:

- Two complaint fixtures used a 4-character location (`"Test"`) while `ComplaintRequestDto`
  enforces `@Size(min = 5)`. They reached validation (400) before the intended 403 check.
- The response-structure contract requested `/api/complaints/{numericId}`; the public endpoint is
  `/api/complaints/{publicId}`. The test now stores and uses the `publicId`.
- The upload-presence test sent a non-multipart POST, which is a malformed request. It now sends
  a multipart request without a file, so the missing part produces the expected 400.
- The security regression fixture created a Roads complaint while the "own department" admin was
  Water, so legitimate same-department assignment was denied. The fixture now uses `WATER_SUPPLY`.

No assertions were weakened, no test was removed, and no test was disabled.

### 6. Lombok builder warning

`AuthResponseDto.tokenType` has an initializer (`"Bearer"`), so `@Builder` ignored it and produced
`null` through the builder. `@Builder.Default` restores the intended `Bearer` default without
changing the response contract.

### 7. Removed Hibernate dialect

Hibernate 7.2 no longer ships `MySQL8Dialect`. The production configuration now uses
`org.hibernate.dialect.MySQLDialect`, which auto-detects the MySQL 8 server version.

## Tests executed

| Class | Tests | Result |
|---|---|---|
| `UrbanProblemReportingApplicationTests` | 1 | PASS |
| `controller/AuthControllerIntegrationTest` | 9 | PASS |
| `controller/APIContractIntegrationTest` | 24 | PASS |
| `security/SecurityRegressionTest` | 18 | PASS |
| `service/ComplaintServiceTest` | 7 | PASS |
| **Total** | **59** | **59 passed, 0 failed, 0 skipped** |

Security coverage remains active: unauthenticated 401, tampered/expired/forged JWT 401, ownership
and department isolation, assignment authorization, resolution review authorization, evidence
access, content/extension validation, and recurrence visibility.

## Test isolation

- Every Spring test activates the `test` profile.
- The configured MySQL database is `upr`
  (`DB_URL=jdbc:mysql://localhost:3306/upr`).
- `application-test.properties` overrides the datasource with H2
  (`jdbc:h2:mem:upr_test;MODE=MySQL`), `create-drop` DDL, and a test-only JWT
  secret.
- `ProductionDatabaseConfiguration`, `ApplicationStartupInitializer`, and
  `AdminBootstrapInitializer` are `@Profile("!test")`, so tests never touch MySQL or production
  credentials. No `jdbc:mysql` connection was opened during the test run.

## Commands

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd clean test
.\mvnw.cmd package
.\mvnw.cmd verify
```

Surefire reports are written to `target\surefire-reports\`.

## Remaining issues

None known for this verification scope. Tests exercise the H2 profile; a live MySQL 8 startup has
not been executed in this pass, but the dialect and DECIMAL mappings are configured for it.
