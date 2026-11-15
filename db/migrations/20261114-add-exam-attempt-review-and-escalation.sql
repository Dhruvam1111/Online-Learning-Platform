-- Migration: Step 56 / FR-I69: Flag and Escalate Attempts
-- Date: 2026-11-14

ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS review_status VARCHAR(30) NOT NULL DEFAULT 'NONE';
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS review_notes TEXT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS reviewed_by INTEGER REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMP;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS escalation_reason TEXT;
ALTER TABLE exam_attempts ADD COLUMN IF NOT EXISTS escalation_recommendation VARCHAR(100);

CREATE TABLE IF NOT EXISTS exam_attempt_review_history (
    id SERIAL PRIMARY KEY,
    attempt_id INTEGER NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    action VARCHAR(50) NOT NULL,
    notes TEXT,
    recommendation VARCHAR(100),
    reviewed_by INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exam_attempts_review_status ON exam_attempts(review_status);
CREATE INDEX IF NOT EXISTS idx_exam_attempt_review_history_attempt ON exam_attempt_review_history(attempt_id, created_at DESC);
