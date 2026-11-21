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
CREATE INDEX IF NOT EXISTS idx_materials_lesson_id ON lecture_materials(lesson_id);
CREATE INDEX IF NOT EXISTS idx_materials_module_id ON lecture_materials(module_id);
CREATE INDEX IF NOT EXISTS idx_materials_uploader_id ON lecture_materials(uploader_id);
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

-- Step 14 / FR-I64: Content Ownership Declaration Indexes
CREATE INDEX IF NOT EXISTS idx_content_declarations_course_id ON content_declarations(course_id);
CREATE INDEX IF NOT EXISTS idx_content_declarations_lesson_id ON content_declarations(lesson_id);
CREATE INDEX IF NOT EXISTS idx_content_declarations_instructor_id ON content_declarations(instructor_id);
CREATE INDEX IF NOT EXISTS idx_content_declarations_takedown_status ON content_declarations(takedown_status);

-- FR-I19: Quizzes, Questions, and Quiz-Questions Indexes
CREATE INDEX idx_quizzes_course_id ON quizzes(course_id);
CREATE INDEX idx_quizzes_module_id ON quizzes(module_id);
CREATE INDEX idx_quizzes_lesson_id ON quizzes(lesson_id);
CREATE INDEX idx_quizzes_status ON quizzes(status);
CREATE INDEX idx_questions_course_id ON questions(course_id);
CREATE INDEX idx_questions_type ON questions(question_type);
CREATE INDEX idx_questions_created_by ON questions(created_by);
CREATE INDEX idx_questions_manual_grading ON questions(requires_manual_grading);
CREATE INDEX idx_quiz_questions_quiz_order ON quiz_questions(quiz_id, order_index);
CREATE INDEX idx_quiz_questions_question_id ON quiz_questions(question_id);

-- FR-I20: Quiz Availability & Server-Side Attempt Enforcement Indexes
CREATE INDEX idx_quizzes_available_window ON quizzes(available_from, available_until);
CREATE INDEX idx_quiz_attempts_quiz_student ON quiz_attempts(quiz_id, student_id);
CREATE INDEX idx_quiz_attempts_status ON quiz_attempts(status);

-- FR-I22: Auto-Graded and Evaluated Attempt Answers Indexes
CREATE INDEX idx_quiz_attempt_answers_attempt ON quiz_attempt_answers(attempt_id);
CREATE INDEX idx_quiz_attempt_answers_question ON quiz_attempt_answers(question_id);
CREATE INDEX idx_quiz_attempt_answers_status ON quiz_attempt_answers(evaluation_status);
-- Step 13 / FR-I13: Video Upload Indexes
CREATE INDEX IF NOT EXISTS idx_video_uploads_lesson_id ON video_uploads(lesson_id);
CREATE INDEX IF NOT EXISTS idx_video_uploads_uploader_id ON video_uploads(uploader_id);
CREATE INDEX IF NOT EXISTS idx_video_uploads_status ON video_uploads(status);

-- FR-I10: Course Pricing & Enrollment Limits Indexes
CREATE INDEX IF NOT EXISTS idx_courses_price ON courses(price);
CREATE INDEX IF NOT EXISTS idx_courses_enrollment_limit ON courses(enrollment_limit);

-- Step 34 / FR-I08: Course Submission History Indexes
CREATE INDEX IF NOT EXISTS idx_course_submissions_course_id ON course_submissions(course_id);
CREATE INDEX IF NOT EXISTS idx_course_submissions_instructor_id ON course_submissions(instructor_id);
CREATE INDEX IF NOT EXISTS idx_course_submissions_status ON course_submissions(status);
CREATE INDEX IF NOT EXISTS idx_course_submissions_submitted_at ON course_submissions(submitted_at);

CREATE INDEX IF NOT EXISTS idx_quiz_attempt_answers_attempt ON quiz_attempt_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_quiz_student ON quiz_attempts(quiz_id, student_id);

-- Step 5 / FR-I70: Notification Centre Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications(user_id, is_read);

-- Step 12 / FR-I39: Course Announcements Indexes
CREATE INDEX IF NOT EXISTS idx_course_announcements_course_created ON course_announcements(course_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_course_announcements_instructor ON course_announcements(instructor_id);

-- Step 35 / FR-I71: Course Review Status Indexes
CREATE INDEX IF NOT EXISTS idx_course_submissions_reviewed_at ON course_submissions(reviewed_at);

-- Step 36 / FR-I09: Course Audit Indexes
CREATE INDEX IF NOT EXISTS idx_course_audits_course_id ON course_audits(course_id);
CREATE INDEX IF NOT EXISTS idx_course_audits_actor_id ON course_audits(actor_id);
CREATE INDEX IF NOT EXISTS idx_course_audits_created_at ON course_audits(created_at DESC);

-- Step 37 / FR-I12: Course Archiving, Soft Deletion, and Certificates Indexes
CREATE INDEX IF NOT EXISTS idx_courses_is_deleted ON courses(is_deleted);
CREATE INDEX IF NOT EXISTS idx_certificates_course_id ON certificates(course_id);
CREATE INDEX IF NOT EXISTS idx_certificates_student_id ON certificates(student_id);

-- Step 44 / FR-I56: Certificate Completion Criteria Indexes
CREATE INDEX IF NOT EXISTS idx_course_completion_criteria_course_id ON course_completion_criteria(course_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_student_id ON lesson_progress(student_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_lesson_id ON lesson_progress(lesson_id);

-- Step 45 / FR-I57: View Certificates Issued Indexes
CREATE INDEX IF NOT EXISTS idx_certificates_issued_at ON certificates(issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_certificates_course_issued_at ON certificates(course_id, issued_at DESC);

-- Step 46 / FR-I30: Descriptive Submissions Grading Indexes
CREATE INDEX IF NOT EXISTS idx_quiz_attempt_answers_status ON quiz_attempt_answers(evaluation_status);
CREATE INDEX IF NOT EXISTS idx_quiz_attempt_answers_grader ON quiz_attempt_answers(grader_id);
-- Step 19 / FR-I16: Content Reorder, Replace and Remove Indexes
CREATE INDEX IF NOT EXISTS idx_materials_lesson_order ON lecture_materials(lesson_id, order_index);
CREATE INDEX IF NOT EXISTS idx_materials_deleted_at ON lecture_materials(deleted_at);

-- Step 20 / FR-I18: Scheduled Content Release Indexes
CREATE INDEX IF NOT EXISTS idx_materials_release_schedule ON lecture_materials(is_released, release_at);
CREATE INDEX IF NOT EXISTS idx_lessons_release_schedule ON lessons(is_released, release_at);

-- Step 47 / FR-I31 & FR-I23: Feedback on Graded Work Indexes
CREATE INDEX IF NOT EXISTS idx_quizzes_feedback_released ON quizzes(feedback_released);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_published ON quiz_attempts(is_published);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_grader ON quiz_attempts(grader_id);

-- Step 48 / FR-I32: Grade Override & Result Versioning Indexes
CREATE INDEX IF NOT EXISTS idx_grading_versions_attempt ON grading_result_versions(attempt_id);
CREATE INDEX IF NOT EXISTS idx_grading_versions_answer ON grading_result_versions(answer_id);
CREATE INDEX IF NOT EXISTS idx_grading_versions_instructor ON grading_result_versions(instructor_id);
CREATE INDEX IF NOT EXISTS idx_grading_versions_created_at ON grading_result_versions(created_at DESC);

-- Step 25 / FR-I65: Marking Rules Indexes
CREATE INDEX IF NOT EXISTS idx_quizzes_marking_rules ON quizzes(negative_marking, partial_marking);

-- Step 26 / FR-I24: Question Bank & Versioning Indexes
CREATE INDEX IF NOT EXISTS idx_questions_bank_search ON questions(is_bank_question, course_id, is_latest, topic, difficulty);
CREATE INDEX IF NOT EXISTS idx_questions_parent_version ON questions(parent_question_id, version);
CREATE INDEX IF NOT EXISTS idx_questions_origin ON questions(origin);

-- Step 27 / FR-I23: Answer Review and Feedback Toggle Indexes
CREATE INDEX IF NOT EXISTS idx_quizzes_review_toggle ON quizzes(show_correct_answers, show_feedback, answer_release_timing);
CREATE INDEX IF NOT EXISTS idx_quizzes_is_exam ON quizzes(is_exam);

-- Step 28 / FR-I68: Assignments Indexes
CREATE INDEX IF NOT EXISTS idx_assignments_course_id ON assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_assignments_lesson_id ON assignments(lesson_id);
CREATE INDEX IF NOT EXISTS idx_assignments_deadline ON assignments(deadline);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment_id ON assignment_submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_student_id ON assignment_submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_grading_status ON assignment_submissions(grading_status);

-- Step 38 / FR-I25: Create and Schedule Examinations Indexes
CREATE INDEX IF NOT EXISTS idx_exams_course_id ON exams(course_id);
CREATE INDEX IF NOT EXISTS idx_exams_status ON exams(status);
CREATE INDEX IF NOT EXISTS idx_exams_window ON exams(start_window, end_window);
CREATE INDEX IF NOT EXISTS idx_exam_questions_exam_id ON exam_questions(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_student ON exam_attempts(exam_id, student_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status ON exam_attempts(status);

-- Step 39 / FR-I27: Randomised Exam Sets Indexes
CREATE INDEX IF NOT EXISTS idx_exam_blueprints_exam_id ON exam_blueprints(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_set_label ON exam_attempts(exam_id, set_label);

-- Step 40 / FR-I26: Examination Monitoring Rules Indexes
CREATE INDEX IF NOT EXISTS idx_exam_monitoring_exam_id ON exam_monitoring_configs(exam_id);

-- Step 41 / FR-I66: Question Correction Policy Indexes
CREATE INDEX IF NOT EXISTS idx_exams_correction_policy ON exams(question_correction_policy);

-- Step 49 / FR-I74: Result Change History Indexes
CREATE INDEX IF NOT EXISTS idx_result_versions_course_id ON result_versions(course_id);
CREATE INDEX IF NOT EXISTS idx_result_versions_student_id ON result_versions(student_id);
CREATE INDEX IF NOT EXISTS idx_result_versions_assessment ON result_versions(assessment_type, assessment_id);
CREATE INDEX IF NOT EXISTS idx_result_versions_actor_id ON result_versions(actor_id);
CREATE INDEX IF NOT EXISTS idx_result_versions_created_at ON result_versions(created_at DESC);

-- Step 50 / FR-I33: Pending Grading Alerts Indexes
CREATE INDEX IF NOT EXISTS idx_grading_alerts_instructor ON pending_grading_alerts(instructor_id, is_read);
CREATE INDEX IF NOT EXISTS idx_grading_alerts_course ON pending_grading_alerts(course_id);
CREATE INDEX IF NOT EXISTS idx_grading_alerts_sent_at ON pending_grading_alerts(sent_at DESC);

-- Step 51 / FR-I34: Publish or Withhold Results Indexes
CREATE INDEX IF NOT EXISTS idx_quizzes_publish ON quizzes(results_published, results_withheld);
CREATE INDEX IF NOT EXISTS idx_assignments_publish ON assignments(results_published, results_withheld);
CREATE INDEX IF NOT EXISTS idx_exams_publish ON exams(results_published, results_withheld);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_published ON exam_attempts(is_published, is_locked);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_published ON assignment_submissions(is_published, is_locked);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_published ON quiz_attempts(is_published, is_locked);

-- Step 56 / FR-I69: Flag and Escalate Attempts Indexes
CREATE INDEX IF NOT EXISTS idx_exam_attempts_review_status ON exam_attempts(review_status);
CREATE INDEX IF NOT EXISTS idx_exam_attempt_review_history_attempt ON exam_attempt_review_history(attempt_id, created_at DESC);

-- Step 52 / FR-I35: Reported Questions Indexes
CREATE INDEX IF NOT EXISTS idx_reported_questions_course_id ON reported_questions(course_id);
CREATE INDEX IF NOT EXISTS idx_reported_questions_assessment ON reported_questions(assessment_type, assessment_id);
CREATE INDEX IF NOT EXISTS idx_reported_questions_status ON reported_questions(status);
CREATE INDEX IF NOT EXISTS idx_reported_questions_escalation ON reported_questions(escalate_at, escalated_to_admin);
CREATE INDEX IF NOT EXISTS idx_student_question_reports_question ON student_question_reports(reported_question_id);
CREATE INDEX IF NOT EXISTS idx_student_question_reports_student ON student_question_reports(student_id);

-- Step 55 / FR-I29: Examination Monitoring Logs Indexes
CREATE INDEX IF NOT EXISTS idx_monitoring_events_attempt ON exam_monitoring_events(attempt_id, event_timestamp ASC);
CREATE INDEX IF NOT EXISTS idx_monitoring_events_severity ON exam_monitoring_events(severity);

-- Exceptional refund review queue and exact student email lookup.
CREATE INDEX IF NOT EXISTS idx_refund_exception_queue
    ON webhook_events (processed_at, id) WHERE event_type = 'refund.exception.request';
CREATE INDEX IF NOT EXISTS idx_refund_exception_order
    ON webhook_events (substring(event_id, 11)) WHERE event_type = 'refund.exception.request';
CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (lower(email));

-- Step 57: Examination Attempt Answers Indexes
CREATE INDEX IF NOT EXISTS idx_exam_attempt_answers_attempt ON exam_attempt_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_exam_attempts_status_expiry ON exam_attempts(status, expires_at);

-- Step 58 / FR-A31 (ADM-015): Admin Audit Logs Indexes
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_created_at ON admin_audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_actor ON admin_audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_action ON admin_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_admin_audit_logs_target ON admin_audit_logs(target_type, target_id);

-- Step 59: AI Lecture Notes Documents & Synthesis Engine Indexes
CREATE INDEX IF NOT EXISTS idx_lecture_notes_lesson ON lecture_notes_documents(lesson_id);
CREATE INDEX IF NOT EXISTS idx_lecture_notes_uploader ON lecture_notes_documents(uploader_id);
CREATE INDEX IF NOT EXISTS idx_ai_synthesis_lesson ON ai_lecture_syntheses(lesson_id);
CREATE INDEX IF NOT EXISTS idx_ai_synthesis_instructor ON ai_lecture_syntheses(instructor_id);
CREATE INDEX IF NOT EXISTS idx_ai_synthesis_review_status ON ai_lecture_syntheses(review_status);
CREATE INDEX IF NOT EXISTS idx_ai_synthesis_published ON ai_lecture_syntheses(lesson_id, published_to_students);

