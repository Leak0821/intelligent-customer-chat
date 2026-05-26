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
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_reply_drafts_run_id_created_at (run_id, created_at)
);
