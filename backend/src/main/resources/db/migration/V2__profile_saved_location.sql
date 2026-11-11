-- Saved location columns for profile pages (citizens and workers).
-- All new columns are NULLable, so every existing row is preserved untouched.
-- No tables are dropped, renamed, or truncated, and no existing column is modified.
-- Applied automatically by Flyway on startup (baseline-on-migrate handles the
-- pre-existing `upr` schema); never run this file manually.

ALTER TABLE citizens
    ADD COLUMN address VARCHAR(255) NULL,
    ADD COLUMN city VARCHAR(100) NULL,
    ADD COLUMN state VARCHAR(100) NULL,
    ADD COLUMN postal_code VARCHAR(20) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;

ALTER TABLE workers
    ADD COLUMN address VARCHAR(255) NULL,
    ADD COLUMN city VARCHAR(100) NULL,
    ADD COLUMN state VARCHAR(100) NULL,
    ADD COLUMN postal_code VARCHAR(20) NULL,
    ADD COLUMN latitude DOUBLE NULL,
    ADD COLUMN longitude DOUBLE NULL;
