-- ============================================================
-- Online Learning Platform — Sprint 1 Indexes
-- Run AFTER 01-schema.sql
-- ============================================================

-- users: look up by email during login
-- (The UNIQUE constraint on email already creates an implicit index,
--  so we do NOT need a separate index on users.email)

-- courses: list all courses by a specific instructor
CREATE INDEX idx_courses_instructor_id ON courses(instructor_id);

-- courses: filter catalog by status (WHERE status = 'published')
CREATE INDEX idx_courses_status ON courses(status);

-- lectures: get all lectures for a course, ordered
CREATE INDEX idx_lectures_course_id_order ON lectures(course_id, order_index);

-- enrollments: check if a student is enrolled in a specific course
-- (The UNIQUE constraint on (student_id, course_id) already creates
--  a composite index, so this lookup is already fast)

-- enrollments: list all courses a student is enrolled in
CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);

-- enrollments: count enrollments for a course
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);


CREATE INDEX idx_reviews_course_id ON reviews(course_id);

CREATE INDEX idx_bookmarks_student_id ON bookmarks(student_id);


CREATE INDEX idx_lecture_progress_student_id ON lecture_progress(student_id);
CREATE INDEX idx_lecture_progress_lecture_id ON lecture_progress(lecture_id);

-- Payment simulator lookup and journal indexes.
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_invoice_number ON payments(invoice_number);
CREATE INDEX IF NOT EXISTS idx_payment_student_created ON payments(student_id, created_at);
CREATE INDEX IF NOT EXISTS idx_payment_events_type ON webhook_events(event_type);
CREATE INDEX IF NOT EXISTS idx_payment_refund_payment ON refunds(payment_id);

CREATE INDEX IF NOT EXISTS idx_webhook_event_type_id ON webhook_events(event_type,id);

-- Stable finance pagination under equal timestamps.
CREATE INDEX IF NOT EXISTS idx_payment_created_id ON payments(created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS idx_refund_created_id ON refunds(created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS idx_earning_owner_created_id ON instructor_earnings(instructor_id,created_at DESC,id DESC);
