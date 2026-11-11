# Database migration runbook (Windows, PowerShell)

Schema changes are applied by Flyway during backend startup, before Hibernate validates the
schema. Do not pipe migration scripts into MySQL, set Hibernate to `update`, or drop/recreate
the production database. Flyway tracks completed scripts in `flyway_schema_history`.

## Before deploying a version with a new migration

1. Back up the database using your normal backup procedure. For a local MySQL installation:

   ```powershell
   New-Item -ItemType Directory -Force C:\db-backups | Out-Null
   $stamp = Get-Date -Format "yyyyMMdd-HHmm"
   mysqldump -u root -p --single-transaction --routines --triggers `
       --result-file="C:\db-backups\upr_before_migration_$stamp.sql" upr
   ```

2. Confirm `backend/.env` points to the intended database and contains valid credentials.
3. Start the backend. Flyway applies each pending migration once; Hibernate then validates
   the resulting schema. Existing databases are baselined and not recreated.
4. Confirm startup completes and inspect the backend log for Flyway migration results.

## If startup fails

- Stop the backend and preserve the complete error log.
- Check the database name, user permissions, and `flyway_schema_history`.
- Do not delete the Flyway history table or rerun SQL by hand. Resolve the specific migration
  problem against a backup before restarting.
- If Hibernate reports a missing column after Flyway succeeds, compare the entity with the
  versioned scripts in `src/main/resources/db/migration/` and investigate before changing data.

## Historical migration

`MIGRATION_2026-10-31_security.sql` is retained as a historical record of a one-time migration
performed before Flyway was introduced. Do not execute it on current installations.
