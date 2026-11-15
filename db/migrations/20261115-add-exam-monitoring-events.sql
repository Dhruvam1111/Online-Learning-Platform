-- Step 55 / FR-I29: Review Monitoring Logs Migration
CREATE TABLE IF NOT EXISTS exam_monitoring_events (
    id SERIAL PRIMARY KEY,
    attempt_id INTEGER NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    event_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_seconds INTEGER,
    metadata_json TEXT,
    notes VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_events_attempt ON exam_monitoring_events(attempt_id, event_timestamp ASC);
CREATE INDEX IF NOT EXISTS idx_monitoring_events_severity ON exam_monitoring_events(severity);
