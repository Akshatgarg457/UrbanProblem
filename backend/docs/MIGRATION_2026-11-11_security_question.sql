-- Adds security question support for password recovery.
-- Run against the CommonGround database before deploying the matching backend version.

ALTER TABLE citizens
    ADD COLUMN security_question VARCHAR(255) NULL AFTER password_hash,
    ADD COLUMN security_answer_hash VARCHAR(255) NULL AFTER security_question;

ALTER TABLE workers
    ADD COLUMN security_question VARCHAR(255) NULL AFTER password_hash,
    ADD COLUMN security_answer_hash VARCHAR(255) NULL AFTER security_question;

ALTER TABLE admin_users
    ADD COLUMN security_question VARCHAR(255) NULL AFTER password_hash,
    ADD COLUMN security_answer_hash VARCHAR(255) NULL AFTER security_question;
