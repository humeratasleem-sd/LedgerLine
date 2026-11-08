CREATE TABLE system_ledger_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_system_ledger_accounts_code UNIQUE (code)
) ENGINE=InnoDB;

INSERT INTO system_ledger_accounts (code, description)
VALUES ('EXTERNAL_CLEARING', 'Counter-entry for externally settled operational postings');

ALTER TABLE ledger_entries
    MODIFY COLUMN account_id BIGINT NULL,
    ADD COLUMN system_account_id BIGINT NULL AFTER account_id,
    ADD CONSTRAINT fk_ledger_entries_system_account
        FOREIGN KEY (system_account_id) REFERENCES system_ledger_accounts (id),
    ADD CONSTRAINT chk_ledger_entries_single_account
        CHECK ((account_id IS NOT NULL AND system_account_id IS NULL)
            OR (account_id IS NULL AND system_account_id IS NOT NULL));

ALTER TABLE transactions
    ADD COLUMN transaction_type VARCHAR(16) NOT NULL DEFAULT 'UNSPECIFIED' AFTER amount_paise,
    ADD COLUMN source_account_id BIGINT NULL AFTER transaction_type,
    ADD COLUMN destination_account_id BIGINT NULL AFTER source_account_id,
    ADD COLUMN failure_code VARCHAR(64) NULL AFTER destination_account_id,
    ADD COLUMN external_reference VARCHAR(128) NULL AFTER failure_code,
    ADD INDEX ix_transactions_source_created (source_account_id, created_at, id),
    ADD INDEX ix_transactions_destination_created (destination_account_id, created_at, id),
    ADD CONSTRAINT chk_transactions_type
        CHECK (transaction_type IN ('UNSPECIFIED', 'TRANSFER', 'DEPOSIT', 'WITHDRAWAL')),
    ADD CONSTRAINT chk_transactions_account_shape
        CHECK (
            transaction_type = 'UNSPECIFIED'
            OR (transaction_type = 'TRANSFER'
                AND source_account_id IS NOT NULL AND destination_account_id IS NOT NULL
                AND external_reference IS NULL)
            OR (transaction_type = 'DEPOSIT'
                AND source_account_id IS NULL AND destination_account_id IS NOT NULL
                AND external_reference IS NOT NULL)
            OR (transaction_type = 'WITHDRAWAL'
                AND source_account_id IS NOT NULL AND destination_account_id IS NULL
                AND external_reference IS NOT NULL)
        ),
    ADD CONSTRAINT fk_transactions_source_account
        FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    ADD CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts (id);

CREATE TABLE external_settlements (
    external_reference VARCHAR(128) NOT NULL,
    transaction_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (external_reference),
    CONSTRAINT uq_external_settlements_transaction UNIQUE (transaction_id),
    CONSTRAINT fk_external_settlements_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id)
) ENGINE=InnoDB;

CREATE TRIGGER transactions_no_update
BEFORE UPDATE ON transactions
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'transactions are immutable';

CREATE TRIGGER transactions_no_delete
BEFORE DELETE ON transactions
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'transactions are append-only';

CREATE TRIGGER external_settlements_no_update
BEFORE UPDATE ON external_settlements
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'external_settlements are immutable';

CREATE TRIGGER external_settlements_no_delete
BEFORE DELETE ON external_settlements
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'external_settlements are append-only';