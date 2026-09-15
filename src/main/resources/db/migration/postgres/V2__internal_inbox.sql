CREATE TABLE inbox_threads (
 id VARCHAR(36) PRIMARY KEY, owner_id VARCHAR(36) NOT NULL REFERENCES accounts(id),
 source_key VARCHAR(120) NOT NULL UNIQUE, kind VARCHAR(20) NOT NULL, subject VARCHAR(180) NOT NULL,
 draft_text VARCHAR(4000) NOT NULL DEFAULT '', draft_revision INTEGER NOT NULL DEFAULT 0,
 sent_revision INTEGER NOT NULL DEFAULT 0, admin_unread BOOLEAN NOT NULL DEFAULT TRUE,
 member_unread BOOLEAN NOT NULL DEFAULT TRUE, created_at BIGINT NOT NULL, updated_at BIGINT NOT NULL
);
CREATE INDEX idx_inbox_owner_updated ON inbox_threads(owner_id,updated_at);
CREATE INDEX idx_inbox_admin_unread ON inbox_threads(admin_unread,updated_at);
CREATE TABLE inbox_messages (
 id VARCHAR(36) PRIMARY KEY, thread_id VARCHAR(36) NOT NULL REFERENCES inbox_threads(id),
 client_key VARCHAR(80) NOT NULL, sender_id VARCHAR(36) NOT NULL DEFAULT '',
 sender_role VARCHAR(20) NOT NULL, message_body VARCHAR(6000) NOT NULL, created_at BIGINT NOT NULL,
 CONSTRAINT uq_inbox_message_key UNIQUE(thread_id,client_key)
);
CREATE INDEX idx_inbox_message_thread ON inbox_messages(thread_id,created_at);
