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
