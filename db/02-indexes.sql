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

CREATE INDEX IF NOT EXISTS idx_materials_lecture_id ON lecture_materials(lecture_id);
CREATE INDEX IF NOT EXISTS idx_lecture_bookmarks_student ON lecture_bookmarks(student_id);
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
-- instructor_credentials: list credentials for an instructor
CREATE INDEX idx_instructor_credentials_instructor_id ON instructor_credentials(instructor_id);
CREATE INDEX idx_instructor_credentials_status ON instructor_credentials(status);

-- instructor_upload_history: list upload audit trail for an instructor
CREATE INDEX idx_instructor_upload_history_instructor_id ON instructor_upload_history(instructor_id);

-- courses: filter by category
CREATE INDEX idx_courses_category ON courses(category);

-- course_instructors: lookups by course and instructor (FR-I05, FR-I59)
CREATE INDEX idx_course_instructors_course_id ON course_instructors(course_id);
CREATE INDEX idx_course_instructors_instructor_id ON course_instructors(instructor_id);

-- course_prerequisites: lookups for enrollment eligibility checks (FR-I05)
CREATE INDEX idx_course_prerequisites_course_id ON course_prerequisites(course_id);
CREATE INDEX idx_course_prerequisites_required_course_id ON course_prerequisites(required_course_id);

-- course_modules: list ordered modules for a course (FR-I06)
CREATE INDEX idx_course_modules_course_order ON course_modules(course_id, order_index);

-- course_sections: list ordered sections for a module (FR-I06)
CREATE INDEX idx_course_sections_module_order ON course_sections(module_id, order_index);

-- lessons: list ordered lessons for a section and filter by type (FR-I06)
CREATE INDEX idx_lessons_section_order ON lessons(section_id, order_index);
CREATE INDEX idx_lessons_lesson_type ON lessons(lesson_type);

-- course_association_requests: lookups, admin queue, and duplicate pending guard (FR-I58)
CREATE INDEX idx_assoc_req_instructor_id ON course_association_requests(instructor_id);
CREATE INDEX idx_assoc_req_course_id ON course_association_requests(course_id);
CREATE INDEX idx_assoc_req_status ON course_association_requests(status);
CREATE UNIQUE INDEX uq_assoc_req_pending_course_instructor
    ON course_association_requests(course_id, instructor_id)
    WHERE status = 'PENDING';
