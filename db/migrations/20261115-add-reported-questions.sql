-- Migration: Add reported questions and student question reports (FR-I35 Step 52, FR-I36 Step 53)
CREATE TABLE IF NOT EXISTS reported_questions (
    id SERIAL PRIMARY KEY,
    assessment_type VARCHAR(20) NOT NULL,
    assessment_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
    course_id INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    report_count INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    latest_reason VARCHAR(255),
    latest_explanation TEXT,
    first_reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    escalate_at TIMESTAMP,
    escalated_to_admin BOOLEAN NOT NULL DEFAULT FALSE,
    escalated_at TIMESTAMP,
    resolution_action VARCHAR(50),
    resolution_comment TEXT,
    resolved_by INTEGER REFERENCES users(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reported_question_assessment UNIQUE (assessment_type, assessment_id, question_id)
);

CREATE TABLE IF NOT EXISTS student_question_reports (
    id SERIAL PRIMARY KEY,
    reported_question_id INTEGER NOT NULL REFERENCES reported_questions(id) ON DELETE CASCADE,
    student_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attempt_id INTEGER,
    reason VARCHAR(255) NOT NULL,
    explanation TEXT,
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_question_report UNIQUE (reported_question_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_reported_questions_course_id ON reported_questions(course_id);
CREATE INDEX IF NOT EXISTS idx_reported_questions_assessment ON reported_questions(assessment_type, assessment_id);
CREATE INDEX IF NOT EXISTS idx_reported_questions_status ON reported_questions(status);
CREATE INDEX IF NOT EXISTS idx_reported_questions_escalation ON reported_questions(escalate_at, escalated_to_admin);
CREATE INDEX IF NOT EXISTS idx_student_question_reports_question ON student_question_reports(reported_question_id);
CREATE INDEX IF NOT EXISTS idx_student_question_reports_student ON student_question_reports(student_id);
