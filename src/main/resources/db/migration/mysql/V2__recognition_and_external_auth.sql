ALTER TABLE accounts
 DROP INDEX email,
 MODIFY COLUMN email VARCHAR(180) NULL,
 MODIFY COLUMN password_hash VARCHAR(100) NULL,
 ADD COLUMN external_id VARCHAR(80) NULL UNIQUE,
 ADD COLUMN phone VARCHAR(20) NOT NULL DEFAULT '',
 ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT '';
CREATE TABLE donation_records (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 owner_id VARCHAR(36) NOT NULL,
 donation_date VARCHAR(10) NOT NULL,
 center VARCHAR(120) NOT NULL,
 receipt_reference VARCHAR(100) NOT NULL,
 reference_key VARCHAR(64) NOT NULL UNIQUE,
 status VARCHAR(20) NOT NULL DEFAULT 'pending',
 review_note VARCHAR(500) NOT NULL DEFAULT '',
 reviewed_by VARCHAR(36),
 created_at BIGINT NOT NULL,
 updated_at BIGINT NOT NULL,
 CONSTRAINT uq_donation_owner_date UNIQUE (owner_id, donation_date),
 INDEX idx_donation_owner_status (owner_id,status),
 CONSTRAINT fk_donation_owner FOREIGN KEY (owner_id) REFERENCES accounts(id)
);
CREATE TABLE family_requests (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 owner_id VARCHAR(36) NOT NULL,
 relationship VARCHAR(30) NOT NULL,
 hospital VARCHAR(120) NOT NULL,
 city VARCHAR(80) NOT NULL,
 blood_group VARCHAR(3) NOT NULL,
 phone VARCHAR(10) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'requested',
 note VARCHAR(500) NOT NULL DEFAULT '',
 created_at BIGINT NOT NULL,
 updated_at BIGINT NOT NULL,
 INDEX idx_family_owner_status (owner_id,status),
 CONSTRAINT fk_family_owner FOREIGN KEY (owner_id) REFERENCES accounts(id)
);
