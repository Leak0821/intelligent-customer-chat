CREATE TABLE IF NOT EXISTS workflow_runs (
    run_id VARCHAR(64) PRIMARY KEY,
    message_id VARCHAR(128) NOT NULL,
    thread_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    status_reason VARCHAR(512) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_workflow_runs_message_id (message_id),
    KEY idx_workflow_runs_thread_id (thread_id),
    KEY idx_workflow_runs_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS workflow_events (
    event_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    summary VARCHAR(512) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_workflow_events_run_id_created_at (run_id, created_at),
    KEY idx_workflow_events_message_id (message_id)
);

CREATE TABLE IF NOT EXISTS reply_drafts (
    draft_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    status VARCHAR(64) NOT NULL,
    review_notes VARCHAR(512) NOT NULL,
    send_readiness VARCHAR(64) NOT NULL,
    next_action VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_reply_drafts_run_id_created_at (run_id, created_at)
);

CREATE TABLE IF NOT EXISTS reply_dispatches (
    dispatch_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_snapshot TEXT NOT NULL,
    attempt_count INT NOT NULL,
    max_attempts INT NOT NULL,
    status VARCHAR(64) NOT NULL,
    latest_trigger_source VARCHAR(64) NOT NULL,
    latest_triggered_by VARCHAR(128) NOT NULL,
    latest_trigger_reason VARCHAR(512) NOT NULL,
    provider_message_id VARCHAR(255) NULL,
    error_message VARCHAR(512) NULL,
    last_attempt_at DATETIME(3) NULL,
    next_retry_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_reply_dispatches_run_id_created_at (run_id, created_at),
    KEY idx_reply_dispatches_draft_id_created_at (draft_id, created_at),
    KEY idx_reply_dispatches_status_next_retry_at (status, next_retry_at)
);

CREATE TABLE IF NOT EXISTS review_records (
    review_id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    reviewer VARCHAR(128) NOT NULL,
    review_note VARCHAR(512) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_review_records_run_id_created_at (run_id, created_at),
    KEY idx_review_records_draft_id_created_at (draft_id, created_at)
);

CREATE TABLE IF NOT EXISTS mail_receipts (
    receipt_id VARCHAR(64) PRIMARY KEY,
    source_key VARCHAR(255) NOT NULL,
    folder_name VARCHAR(255) NOT NULL,
    mail_uid BIGINT NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    thread_id VARCHAR(255) NOT NULL,
    sender VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    received_at DATETIME(3) NOT NULL,
    status VARCHAR(64) NOT NULL,
    workflow_run_id VARCHAR(64) NULL,
    error_message VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_mail_receipts_source_folder_uid (source_key, folder_name, mail_uid),
    KEY idx_mail_receipts_message_id (message_id),
    KEY idx_mail_receipts_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS conversation_summaries (
    summary_id VARCHAR(64) PRIMARY KEY,
    thread_id VARCHAR(128) NOT NULL,
    summary_text TEXT NOT NULL,
    summary_source VARCHAR(64) NOT NULL,
    covered_message_count INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_conversation_summaries_thread_id_created_at (thread_id, created_at)
);
