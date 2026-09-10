CREATE TABLE accounts (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 name VARCHAR(80) NOT NULL,
 email VARCHAR(180) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL
);
CREATE TABLE donors (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 account_id VARCHAR(36) NOT NULL UNIQUE,
 name VARCHAR(80) NOT NULL,
 blood_group VARCHAR(3) NOT NULL,
 city VARCHAR(80) NOT NULL,
 area VARCHAR(80) NOT NULL DEFAULT '',
 phone VARCHAR(10) NOT NULL,
 available BIT(1) NOT NULL DEFAULT b'1',
 consent BIT(1) NOT NULL DEFAULT b'1',
 created_at BIGINT NOT NULL,
 updated_at BIGINT NOT NULL,
 CONSTRAINT fk_donors_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
 INDEX idx_donors_group_available (blood_group,available),
 INDEX idx_donors_available_updated (available,updated_at)
);
