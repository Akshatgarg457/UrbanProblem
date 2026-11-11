-- =====================================================================================
-- Schema and data changes for the security and workflow repair (MySQL 8, utf8mb4).
-- Target database: upr
--
-- HISTORICAL ONLY: this one-time script predates Flyway and is retained for reference.
-- Do not run it on current installations; current schema changes are managed by Flyway.
-- Original manual procedure (not for current use):
--   0. Back up the database (see MIGRATION_RUNBOOK_WINDOWS.md)
--   1. Run section A (preflight, read-only). Fix anything it reports BEFORE continuing.
--   2. Run section B (schema changes, additive only).
--   3. Run section C (email normalization). Only after A reports no duplicates.
--   4. Run section D (department seed). Additive, idempotent.
--   5. Review section E (routing of existing complaints). DO NOT run blindly.
--   6. Run section F (verification, read-only).
-- Nothing here drops a table, column, or row.
-- =====================================================================================

USE upr;

-- -------------------------------------------------------------------------------------
-- A. PREFLIGHT (read-only)
-- -------------------------------------------------------------------------------------

-- A1. Emails that collide once lower-cased within the same table (would break section C).
SELECT 'citizens' AS tbl, LOWER(TRIM(email)) AS normalized, COUNT(*) AS n
FROM citizens GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1
UNION ALL
SELECT 'workers', LOWER(TRIM(email)), COUNT(*)
FROM workers GROUP BY LOWER(TRIM(email)) HAVING COUNT(*) > 1;
-- Expected: zero rows. If any rows appear, resolve them by hand before section C.

-- A2. Same address used by BOTH a citizen and a worker (login would be ambiguous).
SELECT LOWER(TRIM(c.email)) AS normalized_email
FROM citizens c JOIN workers w ON LOWER(TRIM(c.email)) = LOWER(TRIM(w.email));
-- Expected: zero rows.

-- A3. Does the Roads department already exist?
SELECT id, name, status FROM departments WHERE name IN ('Roads and Infrastructure', 'Water and Sanitation', 'Electricity', 'Waste Management');

-- A4. Complaints for roads, and their current state (used in section E).
SELECT c.id, c.public_id, c.status, d.name AS current_department
FROM complaints c JOIN departments d ON d.id = c.department_id
WHERE c.category = 'ROADS_AND_POTHOLES';

-- -------------------------------------------------------------------------------------
-- B. SCHEMA CHANGES (additive)
-- -------------------------------------------------------------------------------------

-- B1. Administrator accounts. Matches entity AdminUser.
CREATE TABLE IF NOT EXISTS admin_users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  email         VARCHAR(255) NOT NULL,
  first_name    VARCHAR(255) NOT NULL,
  last_name     VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(32)  NOT NULL,
  department_id BIGINT NULL,
  status        VARCHAR(255) NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  CONSTRAINT uk_admin_users_email UNIQUE (email),
  CONSTRAINT fk_admin_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

-- B2. Evidence uploader identity. Legacy rows keep NULL. Their uploaded_by holds an email, not a role,
--     so they cannot match any uploader. They remain visible to complaint-scoped roles.
ALTER TABLE complaint_evidence ADD COLUMN uploaded_by_id BIGINT NULL;

-- B3. The initial status-history row has no previous status (entity: nullable previousStatus).
ALTER TABLE complaint_status_history MODIFY previous_status VARCHAR(255) NULL;

-- B4. Optional index for recurrence candidate lookup (database-only; not declared on the entity).
CREATE INDEX idx_complaints_category_status_created ON complaints (category, status, created_at);

-- -------------------------------------------------------------------------------------
-- C. EMAIL NORMALIZATION. Run only if A1 and A2 returned nothing.
--    Login now lower-cases the typed email, so stored values must already be lower-case.
--    If any row collides, the UNIQUE constraint makes this statement fail. It does not overwrite.
-- -------------------------------------------------------------------------------------
UPDATE citizens SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));
UPDATE workers  SET email = LOWER(TRIM(email)) WHERE email <> LOWER(TRIM(email));

-- -------------------------------------------------------------------------------------
-- D. DEPARTMENT SEED (idempotent). The application's startup initializer creates the same row
--    if it is missing, so running this first only avoids a window before the first start.
-- -------------------------------------------------------------------------------------
INSERT INTO departments (name, description, head_email, status, created_at, updated_at)
SELECT 'Roads and Infrastructure',
       'Handles ROADS_AND_POTHOLES related complaints',
       'head-roads-and-infrastructure@municipal.gov',
       'ACTIVE', NOW(6), NOW(6)
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Roads and Infrastructure');

-- -------------------------------------------------------------------------------------
-- E. ROUTING OF EXISTING ROADS COMPLAINTS. REVIEW FIRST. Not run automatically.
--    Safe to move: complaints still SUBMITTED. They have no assignment, so no worker is
--    in the wrong department. Uncomment the UPDATE only after reviewing A4 output.
--    Complaints already ASSIGNED or later are NOT moved here: a human must decide them.
-- -------------------------------------------------------------------------------------
-- UPDATE complaints
--    SET department_id = (SELECT id FROM departments WHERE name = 'Roads and Infrastructure')
--  WHERE category = 'ROADS_AND_POTHOLES' AND status = 'SUBMITTED';
--
-- SELECT id, public_id, status FROM complaints
--  WHERE category = 'ROADS_AND_POTHOLES' AND status <> 'SUBMITTED';   -- decide these manually

-- -------------------------------------------------------------------------------------
-- F. VERIFICATION (read-only)
-- -------------------------------------------------------------------------------------
SHOW COLUMNS FROM admin_users;
SHOW COLUMNS FROM complaint_evidence LIKE 'uploaded_by%';
SHOW COLUMNS FROM complaint_status_history LIKE 'previous_status';
SELECT name, status FROM departments ORDER BY name;
SELECT COUNT(*) AS admin_count FROM admin_users;
-- Expected: admin_users exists (0 rows before bootstrap), uploaded_by_id is NULL-able,
-- previous_status is NULL-able, and 'Roads and Infrastructure' is listed.
