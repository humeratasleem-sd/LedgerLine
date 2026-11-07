ALTER TABLE users
    ADD COLUMN full_name VARCHAR(120) NOT NULL DEFAULT '' AFTER id,
    ADD COLUMN account_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' AFTER role;

UPDATE users
SET account_status = CASE WHEN active THEN 'ACTIVE' ELSE 'DISABLED' END;

ALTER TABLE users
    ADD CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'DISABLED'));
