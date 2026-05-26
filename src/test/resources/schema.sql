CREATE TABLE workflow_runs (
    run_id VARCHAR(64) PRIMARY KEY,
    message_id VARCHAR(128) NOT NULL,
    thread_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    status_reason VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_workflow_runs_message_id ON workflow_runs (message_id);
CREATE INDEX idx_workflow_runs_thread_id ON workflow_runs (thread_id);
CREATE INDEX idx_workflow_runs_created_at ON workflow_runs (created_at);

CREATE TABLE workflow_events (
    event_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_workflow_events_run_id_created_at ON workflow_events (run_id, created_at);
CREATE INDEX idx_workflow_events_message_id ON workflow_events (message_id);

CREATE TABLE reply_drafts (
    draft_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body CLOB NOT NULL,
    status VARCHAR(64) NOT NULL,
    review_notes VARCHAR(512) NOT NULL,
    draft_version INT NOT NULL,
    last_edited_by VARCHAR(128) NOT NULL,
    send_readiness VARCHAR(64) NOT NULL,
    next_action VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_reply_drafts_run_id_created_at ON reply_drafts (run_id, created_at);

CREATE TABLE reply_dispatches (
    dispatch_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_snapshot CLOB NOT NULL,
    attempt_count INT NOT NULL,
    max_attempts INT NOT NULL,
    status VARCHAR(64) NOT NULL,
    latest_trigger_source VARCHAR(64) NOT NULL,
    latest_triggered_by VARCHAR(128) NOT NULL,
    latest_trigger_reason VARCHAR(512) NOT NULL,
    provider_message_id VARCHAR(255),
    error_message VARCHAR(512),
    last_attempt_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_reply_dispatches_run_id_created_at ON reply_dispatches (run_id, created_at);
CREATE INDEX idx_reply_dispatches_draft_id_created_at ON reply_dispatches (draft_id, created_at);
CREATE INDEX idx_reply_dispatches_status_next_retry_at ON reply_dispatches (status, next_retry_at);

CREATE TABLE review_records (
    review_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    reviewer VARCHAR(128) NOT NULL,
    review_note VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_review_records_run_id_created_at ON review_records (run_id, created_at);
CREATE INDEX idx_review_records_draft_id_created_at ON review_records (draft_id, created_at);

CREATE TABLE mail_receipts (
    receipt_id VARCHAR(64) PRIMARY KEY,
    source_key VARCHAR(255) NOT NULL,
    folder_name VARCHAR(255) NOT NULL,
    mail_uid BIGINT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255) NOT NULL,
    sender VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    raw_body CLOB NOT NULL,
    received_at TIMESTAMP NOT NULL,
    status VARCHAR(64) NOT NULL,
    workflow_run_id VARCHAR(64),
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_mail_receipts_source_folder_uid ON mail_receipts (source_key, folder_name, mail_uid);
CREATE INDEX idx_mail_receipts_message_id ON mail_receipts (message_id);
CREATE INDEX idx_mail_receipts_created_at ON mail_receipts (created_at);
CREATE INDEX idx_mail_receipts_status_created_at ON mail_receipts (status, created_at);

CREATE TABLE conversation_summaries (
    summary_id VARCHAR(64) PRIMARY KEY,
    thread_id VARCHAR(128) NOT NULL,
    summary_text CLOB NOT NULL,
    summary_source VARCHAR(64) NOT NULL,
    covered_message_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_conversation_summaries_thread_id_created_at ON conversation_summaries (thread_id, created_at);
