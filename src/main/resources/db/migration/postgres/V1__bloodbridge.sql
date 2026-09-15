CREATE TABLE accounts(id VARCHAR(36) PRIMARY KEY,name VARCHAR(80) NOT NULL,email VARCHAR(180),password_hash VARCHAR(100),external_id VARCHAR(80) UNIQUE,phone VARCHAR(20) NOT NULL DEFAULT '',provider VARCHAR(20) NOT NULL DEFAULT '');
CREATE TABLE donors(id VARCHAR(36) PRIMARY KEY,account_id VARCHAR(36) NOT NULL UNIQUE REFERENCES accounts(id),name VARCHAR(80) NOT NULL,blood_group VARCHAR(3) NOT NULL,state VARCHAR(80) NOT NULL DEFAULT '',city VARCHAR(80) NOT NULL,area VARCHAR(80) NOT NULL DEFAULT '',email VARCHAR(180) NOT NULL DEFAULT '',phone VARCHAR(10) NOT NULL,available BOOLEAN NOT NULL DEFAULT TRUE,consent BOOLEAN NOT NULL DEFAULT TRUE,created_at BIGINT NOT NULL,updated_at BIGINT NOT NULL);
CREATE INDEX idx_donors_group_available ON donors(blood_group,available);
CREATE INDEX idx_donors_available_updated ON donors(available,updated_at);
CREATE INDEX idx_donors_location ON donors(state,city);
CREATE TABLE donation_records(id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(36) NOT NULL REFERENCES accounts(id),donation_date VARCHAR(10) NOT NULL,center VARCHAR(120) NOT NULL,receipt_reference VARCHAR(100) NOT NULL,reference_key VARCHAR(64) NOT NULL UNIQUE,status VARCHAR(20) NOT NULL DEFAULT 'pending',review_note VARCHAR(500) NOT NULL DEFAULT '',reviewed_by VARCHAR(36),created_at BIGINT NOT NULL,updated_at BIGINT NOT NULL,CONSTRAINT uq_donation_owner_date UNIQUE(owner_id,donation_date));
CREATE INDEX idx_donation_owner_status ON donation_records(owner_id,status);
CREATE TABLE family_requests(id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(36) NOT NULL REFERENCES accounts(id),relationship VARCHAR(30) NOT NULL,patient_name VARCHAR(80) NOT NULL DEFAULT '',requester_name VARCHAR(80) NOT NULL DEFAULT '',state VARCHAR(80) NOT NULL DEFAULT '',email VARCHAR(180) NOT NULL DEFAULT '',hospital VARCHAR(120) NOT NULL,city VARCHAR(80) NOT NULL,blood_group VARCHAR(3) NOT NULL,phone VARCHAR(10) NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'requested',note VARCHAR(500) NOT NULL DEFAULT '',created_at BIGINT NOT NULL,updated_at BIGINT NOT NULL);
CREATE INDEX idx_family_owner_status ON family_requests(owner_id,status);
CREATE TABLE device_access (token_hash VARCHAR(64) PRIMARY KEY, account_id VARCHAR(36) NOT NULL, expires_at BIGINT NOT NULL);
CREATE TABLE blood_matches (
 id VARCHAR(36) PRIMARY KEY, donor_id VARCHAR(36) NOT NULL, receiver_id VARCHAR(36) NOT NULL,
 patient_name VARCHAR(80) NOT NULL, hospital VARCHAR(120) NOT NULL, blood_group VARCHAR(3) NOT NULL,
 state VARCHAR(80) NOT NULL, city VARCHAR(80) NOT NULL, note VARCHAR(500) NOT NULL DEFAULT '',
 donor_approved BOOLEAN NOT NULL DEFAULT FALSE, receiver_approved BOOLEAN NOT NULL DEFAULT FALSE,
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
