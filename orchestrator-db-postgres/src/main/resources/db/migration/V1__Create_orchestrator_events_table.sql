CREATE TABLE orchestrator_events (
    id VARCHAR(255) PRIMARY KEY,
    payload TEXT NOT NULL,
    source_topic_partition VARCHAR(255),
    offset_value BIGINT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0
);

CREATE INDEX idx_status ON orchestrator_events(status);
CREATE INDEX idx_created_at ON orchestrator_events(created_at);
CREATE INDEX idx_source_topic_partition ON orchestrator_events(source_topic_partition);
CREATE INDEX idx_status_retry_count ON orchestrator_events(status, retry_count);
CREATE INDEX idx_created_at_status ON orchestrator_events(created_at, status);