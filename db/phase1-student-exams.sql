-- Apply once to Neon PostgreSQL BEFORE starting backend with ddl-auto=validate.
-- Integrate this block into db/01-schema.sql in the group's branch.
CREATE TABLE IF NOT EXISTS exam_attempt_answers (
    id SERIAL PRIMARY KEY,
    attempt_id INTEGER NOT NULL REFERENCES exam_attempts(id) ON DELETE CASCADE,
    question_id INTEGER NOT NULL REFERENCES questions(id),
    answer_text TEXT,
    marks_awarded NUMERIC(8,2),
    feedback TEXT,
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_exam_attempt_answer UNIQUE (attempt_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_exam_attempt_answers_attempt ON exam_attempt_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status_expiry ON exam_attempts(status, expires_at);
