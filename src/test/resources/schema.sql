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
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_reply_drafts_run_id_created_at ON reply_drafts (run_id, created_at);
