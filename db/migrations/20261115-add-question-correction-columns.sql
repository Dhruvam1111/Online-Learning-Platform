-- Migration: Add correction application columns to reported_questions (FR-I37 Step 54)
ALTER TABLE reported_questions ADD COLUMN IF NOT EXISTS correction_applied BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE reported_questions ADD COLUMN IF NOT EXISTS correction_applied_at TIMESTAMP;
ALTER TABLE reported_questions ADD COLUMN IF NOT EXISTS correction_applied_by INTEGER REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE reported_questions ADD COLUMN IF NOT EXISTS correction_policy VARCHAR(50);
ALTER TABLE reported_questions ADD COLUMN IF NOT EXISTS affected_attempts_count INTEGER NOT NULL DEFAULT 0;
