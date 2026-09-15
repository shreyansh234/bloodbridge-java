ALTER TABLE donors ADD COLUMN state VARCHAR(80) NOT NULL DEFAULT '', ADD COLUMN email VARCHAR(180) NOT NULL DEFAULT '';
UPDATE donors d JOIN accounts a ON d.account_id=a.id SET d.email=COALESCE(a.email,'');
ALTER TABLE family_requests ADD COLUMN patient_name VARCHAR(80) NOT NULL DEFAULT '', ADD COLUMN requester_name VARCHAR(80) NOT NULL DEFAULT '', ADD COLUMN state VARCHAR(80) NOT NULL DEFAULT '', ADD COLUMN email VARCHAR(180) NOT NULL DEFAULT '';
CREATE INDEX idx_donors_location ON donors(state,city);
CREATE TABLE device_access (token_hash VARCHAR(64) PRIMARY KEY, account_id VARCHAR(36) NOT NULL, expires_at BIGINT NOT NULL);
CREATE TABLE blood_matches (
 id VARCHAR(36) PRIMARY KEY, donor_id VARCHAR(36) NOT NULL, receiver_id VARCHAR(36) NOT NULL,
 patient_name VARCHAR(80) NOT NULL, hospital VARCHAR(120) NOT NULL, blood_group VARCHAR(3) NOT NULL,
 state VARCHAR(80) NOT NULL, city VARCHAR(80) NOT NULL, note VARCHAR(500) NOT NULL DEFAULT '',
 donor_approved BIT(1) NOT NULL DEFAULT b'0', receiver_approved BIT(1) NOT NULL DEFAULT b'0',
 status VARCHAR(20) NOT NULL DEFAULT 'pending', received_at BIGINT,
 created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL,
 CONSTRAINT fk_match_donor FOREIGN KEY(donor_id) REFERENCES accounts(id),
 CONSTRAINT fk_match_receiver FOREIGN KEY(receiver_id) REFERENCES accounts(id),
 CONSTRAINT ck_distinct_people CHECK(donor_id <> receiver_id),
 CONSTRAINT ck_receipt_approvals CHECK(status <> 'received' OR (donor_approved AND receiver_approved AND received_at IS NOT NULL))
);
CREATE INDEX idx_match_donor ON blood_matches(donor_id,created_at);
CREATE INDEX idx_match_receiver ON blood_matches(receiver_id,created_at);
CREATE TABLE email_notices (
 id VARCHAR(36) PRIMARY KEY, subject VARCHAR(200) NOT NULL, message_body VARCHAR(6000) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'pending', attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt BIGINT NOT NULL DEFAULT 0, provider_id VARCHAR(180) NOT NULL DEFAULT '', last_error VARCHAR(180) NOT NULL DEFAULT ''
);
CREATE INDEX idx_notice_retry ON email_notices(status,next_attempt);
