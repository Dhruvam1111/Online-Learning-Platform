-- ============================================================
-- Online Learning Platform — Sprint 1 Schema
-- Run this FIRST. Creates all tables from scratch.
-- ============================================================

-- Clean slate (safe to re-run during development)
DROP TABLE IF EXISTS quiz_attempts CASCADE;
DROP TABLE IF EXISTS quiz_questions CASCADE;
DROP TABLE IF EXISTS questions CASCADE;
DROP TABLE IF EXISTS quizzes CASCADE;
DROP TABLE IF EXISTS course_association_requests CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS course_sections CASCADE;
DROP TABLE IF EXISTS course_modules CASCADE;
DROP TABLE IF EXISTS course_prerequisites CASCADE;
DROP TABLE IF EXISTS course_instructors CASCADE;
DROP TABLE IF EXISTS lecture_progress CASCADE;
DROP TABLE IF EXISTS bookmarks CASCADE;
DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS lectures CASCADE;
DROP TABLE IF EXISTS instructor_upload_history CASCADE;
DROP TABLE IF EXISTS instructor_credentials CASCADE;
DROP TABLE IF EXISTS instructor_profiles CASCADE;
DROP TABLE IF EXISTS student_profiles CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =========================
-- TABLE: users
-- =========================
-- Stores both students and instructors in one table.
-- The 'role' column distinguishes them.
-- Admin role is NOT needed this sprint.

CREATE TABLE users (
    id              SERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    account_status  VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- Enforce that role is one of the allowed values
    CONSTRAINT chk_users_role CHECK (role IN ('student', 'instructor', 'admin')),

    -- Enforce account lifecycle status
    CONSTRAINT chk_users_account_status CHECK (account_status IN ('active', 'pending', 'suspended', 'rejected'))
);

-- Why VARCHAR for role instead of ENUM?
-- PostgreSQL ENUMs are hard to alter later (adding 'admin' in Sprint 2
-- would require ALTER TYPE). A VARCHAR with a CHECK constraint is
-- easier to extend: just ALTER the CHECK constraint.


-- =========================
-- TABLE: courses
-- =========================
-- Each course belongs to exactly one instructor.
-- Status: 'draft' means only associated instructors and admins see it.
--         'pending_review' means submitted for admin approval.
--         'published' means students can see it in the catalog.
--         'archived' means hidden from catalog while preserving data.
--         'rejected' means returned by admin with feedback.

CREATE TABLE courses (
    id              SERIAL PRIMARY KEY,
    instructor_id   INTEGER      NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    category        VARCHAR(100) NOT NULL DEFAULT 'General',
    learning_outcomes TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    rejection_reason TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_courses_instructor
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_courses_status CHECK (status IN ('draft', 'pending_review', 'published', 'archived', 'rejected'))
);

-- ON DELETE CASCADE: If an instructor account is deleted, their courses
-- are also deleted. This is acceptable for Sprint 1. In production you
-- might want RESTRICT or soft-delete instead.


-- =========================
-- TABLE: course_instructors
-- =========================
-- Many-to-many relationship supporting co-instructors and ownership (FR-I05, FR-I59)
CREATE TABLE course_instructors (
    id            SERIAL PRIMARY KEY,
    course_id     INTEGER NOT NULL,
    instructor_id INTEGER NOT NULL,
    role          VARCHAR(50) NOT NULL DEFAULT 'owner',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_course_instructors_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_instructors_user
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_course_instructor
        UNIQUE (course_id, instructor_id),

    CONSTRAINT chk_course_instructor_role
        CHECK (role IN ('owner', 'co-instructor', 'OWNER', 'CO_INSTRUCTOR'))
);


-- =========================
-- TABLE: course_association_requests
-- =========================
-- Instructor requests to join existing courses as co-instructors (FR-I58, FR-I59)
CREATE TABLE course_association_requests (
    id             SERIAL PRIMARY KEY,
    course_id      INTEGER     NOT NULL,
    instructor_id  INTEGER     NOT NULL,
    requested_role VARCHAR(50) NOT NULL DEFAULT 'co-instructor',
    message        TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_notes    TEXT,
    reviewed_by    INTEGER,
    reviewed_at    TIMESTAMP,
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_assoc_req_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assoc_req_instructor
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_assoc_req_reviewer
        FOREIGN KEY (reviewed_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_assoc_req_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),

    CONSTRAINT chk_assoc_req_role
        CHECK (requested_role IN ('co-instructor'))
);


-- =========================
-- TABLE: course_prerequisites
-- =========================
-- Structured prerequisite requirements for courses and skills (FR-I05)
-- Reusable for student enrollment eligibility verification.
CREATE TABLE course_prerequisites (
    id                  SERIAL PRIMARY KEY,
    course_id           INTEGER NOT NULL,
    prerequisite_type   VARCHAR(20) NOT NULL DEFAULT 'SKILL',
    required_course_id  INTEGER,
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_course_prerequisites_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_course_prerequisites_required_course
        FOREIGN KEY (required_course_id)
        REFERENCES courses(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_course_prerequisite_type
        CHECK (prerequisite_type IN ('COURSE', 'SKILL'))
);


-- =========================
-- TABLE: course_modules
-- =========================
-- Course hierarchy Level 1: Modules grouping content within a course (FR-I06)
CREATE TABLE course_modules (
    id          SERIAL PRIMARY KEY,
    course_id   INTEGER      NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    order_index INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_course_modules_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);


-- =========================
-- TABLE: course_sections
-- =========================
-- Course hierarchy Level 2: Sections dividing a module into logical topics (FR-I06)
CREATE TABLE course_sections (
    id          SERIAL PRIMARY KEY,
    module_id   INTEGER      NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    order_index INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_course_sections_module
        FOREIGN KEY (module_id)
        REFERENCES course_modules(id)
        ON DELETE CASCADE
);


-- =========================
-- TABLE: lessons
-- =========================
-- Course hierarchy Level 3: Individual learning units with specific types (FR-I06)
-- Extensible for quizzes, assignments, materials, and SCORM/xAPI packaging (DR-14)
CREATE TABLE lessons (
    id               SERIAL PRIMARY KEY,
    section_id       INTEGER      NOT NULL,
    title            VARCHAR(200) NOT NULL,
    lesson_type      VARCHAR(50)  NOT NULL DEFAULT 'VIDEO',
    content          TEXT,
    content_url      VARCHAR(500),
    duration_minutes INTEGER,
    order_index      INTEGER      NOT NULL DEFAULT 0,
    metadata         TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_lessons_section
        FOREIGN KEY (section_id)
        REFERENCES course_sections(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_lesson_type
        CHECK (lesson_type IN ('VIDEO', 'MATERIAL', 'QUIZ', 'ASSIGNMENT'))
);


-- =========================
-- TABLE: lectures
-- =========================
-- Each lecture belongs to exactly one course.
-- A lecture is a YouTube link (no file upload this sprint).
-- order_index controls the display order within a course.

CREATE TABLE lectures (
    id              SERIAL PRIMARY KEY,
    course_id       INTEGER      NOT NULL,
    title           VARCHAR(200) NOT NULL,
    youtube_url     VARCHAR(500) NOT NULL,
    video_id        VARCHAR(20),
    order_index     INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_lectures_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE
);

-- video_id: The extracted YouTube video ID (e.g., 'dQw4w9WgXcQ').
-- The backend should extract this from youtube_url at insert time.
-- Storing it separately makes it trivial to build embed URLs and
-- thumbnail URLs on the frontend without parsing every time.
--
-- ON DELETE CASCADE: If a course is deleted, its lectures go too.


-- =========================
-- TABLE: enrollments
-- =========================
-- Join table between students and courses.
-- A student can enroll in many courses; a course can have many students.
-- No payment this sprint — enrollment is free and instant.

CREATE TABLE enrollments (
    id              SERIAL PRIMARY KEY,
    student_id      INTEGER      NOT NULL,
    course_id       INTEGER      NOT NULL,
    enrolled_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    -- Prevent the same student from enrolling in the same course twice
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id)
);


-- =========================
-- TABLE: reviews
-- =========================
CREATE TABLE reviews (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   INTEGER  NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, course_id)
);

-- =========================
-- TABLE: bookmarks
-- =========================
CREATE TABLE bookmarks (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   INTEGER   NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, course_id)
);

-- =========================
-- TABLE: student_profiles
-- =========================
CREATE TABLE student_profiles (
    id              SERIAL PRIMARY KEY,
    student_id      INT NOT NULL UNIQUE,
    education_level VARCHAR(100),
    interests       VARCHAR(1000),
    learning_goals  VARCHAR(1000),
    bio             VARCHAR(2000),
    updated_at      TIMESTAMP,
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: instructor_profiles
-- =========================
CREATE TABLE instructor_profiles (
    id                  SERIAL PRIMARY KEY,
    instructor_id       INTEGER NOT NULL UNIQUE,
    qualifications      TEXT,
    expertise_areas     TEXT,
    bio                 TEXT,
    profile_picture_url VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_instructor_profiles_user FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: instructor_credentials
-- =========================
-- Stores private credential verification documents (degrees, certificates, identity)
-- uploaded by instructors. Access is restricted to the owning instructor and admins.
CREATE TABLE instructor_credentials (
    id                 SERIAL PRIMARY KEY,
    instructor_id      INTEGER NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name   VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    file_size          BIGINT NOT NULL,
    document_type      VARCHAR(100) NOT NULL DEFAULT 'CREDENTIAL',
    status             VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    uploaded_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_instructor_credentials_user
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_credential_status
        CHECK (status IN ('ACTIVE', 'REPLACED', 'DELETED'))
);

-- =========================
-- TABLE: instructor_upload_history
-- =========================
-- Audit trail tracking upload, replacement, and deletion lifecycle events
-- for instructor profile pictures and credential documents.
CREATE TABLE instructor_upload_history (
    id                 SERIAL PRIMARY KEY,
    instructor_id      INTEGER NOT NULL,
    category           VARCHAR(50) NOT NULL,
    action             VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name   VARCHAR(255) NOT NULL,
    content_type       VARCHAR(100) NOT NULL,
    file_size          BIGINT NOT NULL,
    details            TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_instructor_upload_history_user
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_upload_category
        CHECK (category IN ('PROFILE_PICTURE', 'CREDENTIAL')),

    CONSTRAINT chk_upload_action
        CHECK (action IN ('UPLOADED', 'REPLACED', 'DELETED'))
);

-- =========================
-- TABLE: lecture_progress
-- =========================
CREATE TABLE lecture_progress (
    id                         SERIAL PRIMARY KEY,
    student_id                 INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lecture_id                 INTEGER   NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    completed_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    playback_position_seconds  INTEGER   NOT NULL DEFAULT 0,
    UNIQUE (student_id, lecture_id)
);


-- =========================
-- TABLE: lecture_materials (FR-S14)
-- =========================
CREATE TABLE IF NOT EXISTS lecture_materials (
    id                SERIAL PRIMARY KEY,
    lecture_id        INTEGER NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    title             VARCHAR(255) NOT NULL,
    file_url          VARCHAR(500) NOT NULL,
    file_type         VARCHAR(50) NOT NULL DEFAULT 'pdf',
    is_downloadable   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =========================
-- TABLE: lecture_bookmarks (FR-S17)
-- =========================
CREATE TABLE IF NOT EXISTS lecture_bookmarks (
    id                SERIAL PRIMARY KEY,
    student_id        INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lecture_id        INTEGER NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_student_lecture_bookmark UNIQUE (student_id, lecture_id)
);
-- Simulated payments. Apply on a disposable DB first, then reviewed Neon dev migration.
CREATE TABLE IF NOT EXISTS payments (amount numeric(10,2) not null, course_id integer not null REFERENCES courses(id), discount_amount numeric(10,2), id integer generated by default as identity, instructor_earning numeric(10,2), platform_fee numeric(10,2), refund_amount numeric(10,2), risk_score integer, student_id integer not null REFERENCES users(id), wallet_amount_used numeric(10,2), created_at timestamp(6) not null, updated_at timestamp(6) not null, currency varchar(10) not null, refund_status varchar(20), status varchar(20) not null, coupon_code varchar(50), payment_method varchar(50), invoice_number varchar(100), order_id varchar(100) not null unique, payment_id varchar(100), failure_reason TEXT, refund_reason TEXT, signature varchar(255), primary key (id));
CREATE TABLE IF NOT EXISTS wallets (balance numeric(10,2) not null, id integer generated by default as identity, student_id integer not null REFERENCES users(id) unique, updated_at timestamp(6) not null, primary key (id));
CREATE TABLE IF NOT EXISTS wallet_transactions (amount numeric(10,2) not null, id integer generated by default as identity, wallet_id integer not null REFERENCES wallets(id), created_at timestamp(6) not null, type varchar(20) not null, description varchar(255) not null, primary key (id));
CREATE TABLE IF NOT EXISTS coupons (fixed_discount_amount numeric(10,2), discount_percentage numeric(5,2) not null, id integer generated by default as identity, is_active boolean, max_discount_amount numeric(10,2), min_order_amount numeric(10,2), times_used integer, usage_limit integer, created_at timestamp(6) not null, valid_until timestamp(6) not null, code varchar(50) not null unique, primary key (id));
CREATE TABLE IF NOT EXISTS coupon_usages (coupon_id integer not null REFERENCES coupons(id), discount_applied numeric(10,2) not null, id integer generated by default as identity, student_id integer not null REFERENCES users(id), used_at timestamp(6) not null, order_id varchar(100) not null, primary key (id));
CREATE TABLE IF NOT EXISTS refunds (amount numeric(10,2) not null, id integer generated by default as identity, payment_id integer not null REFERENCES payments(id), created_at timestamp(6) not null, status varchar(20) not null, razorpay_refund_id varchar(100) not null unique, reason TEXT, primary key (id));
CREATE TABLE IF NOT EXISTS instructor_earnings (course_id integer not null REFERENCES courses(id), gross_amount numeric(10,2) not null, id integer generated by default as identity, instructor_id integer not null REFERENCES users(id), net_instructor_earning numeric(10,2) not null, payment_id integer not null REFERENCES payments(id), platform_commission numeric(10,2) not null, created_at timestamp(6) not null, payout_status varchar(20) not null, primary key (id));
CREATE TABLE IF NOT EXISTS webhook_events (id integer generated by default as identity, processed_at timestamp(6) not null, event_type varchar(50) not null, event_id varchar(100) not null unique, payload TEXT, primary key (id));
ALTER TABLE coupons ADD COLUMN IF NOT EXISTS fixed_discount_amount NUMERIC(10,2);

-- Payment audit hardening; incremental migration is also supplied.
-- Apply only after 20261110-payment-claims.sql. No automatic Hibernate DDL.
-- Indefinite retention: no scheduled deletion. Operators must maintain verified backups.
BEGIN;
CREATE TABLE IF NOT EXISTS financial_ledger (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name text NOT NULL,
    operation text NOT NULL,
    database_actor text NOT NULL DEFAULT session_user,
    recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    old_record jsonb,
    new_record jsonb
);
CREATE INDEX IF NOT EXISTS idx_financial_ledger_recorded_at ON financial_ledger(recorded_at);
CREATE OR REPLACE FUNCTION protect_financial_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Financial history is append-only; corrections require new entries';
END;
$$;
DROP TRIGGER IF EXISTS protect_financial_ledger ON financial_ledger;
CREATE TRIGGER protect_financial_ledger BEFORE UPDATE OR DELETE OR TRUNCATE ON financial_ledger
FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history();
DROP TRIGGER IF EXISTS protect_webhook_history ON webhook_events;
CREATE TRIGGER protect_webhook_history BEFORE UPDATE OR DELETE OR TRUNCATE ON webhook_events
FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history();
CREATE OR REPLACE FUNCTION public.record_financial_change() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public AS $$
BEGIN
    IF TG_TABLE_SCHEMA <> 'public' OR TG_RELID NOT IN ('public.payments'::regclass,'public.refunds'::regclass,'public.instructor_earnings'::regclass,'public.wallets'::regclass,'public.wallet_transactions'::regclass,'public.coupons'::regclass,'public.coupon_usages'::regclass) THEN
        RAISE EXCEPTION 'Unexpected financial audit source';
    END IF;
    INSERT INTO public.financial_ledger(table_name, operation, old_record, new_record)
    VALUES(TG_TABLE_NAME,TG_OP,
      CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) - 'signature' ELSE NULL END,
      CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) - 'signature' ELSE NULL END);
    RETURN NULL;
END;
$$;
DO $$ DECLARE item text; BEGIN
    FOREACH item IN ARRAY ARRAY['payments','refunds','instructor_earnings','wallets','wallet_transactions','coupons','coupon_usages'] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS financial_history ON %I',item);
        EXECUTE format('CREATE TRIGGER financial_history AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION record_financial_change()',item);
        EXECUTE format('DROP TRIGGER IF EXISTS prevent_financial_truncate ON %I',item);
        EXECUTE format('CREATE TRIGGER prevent_financial_truncate BEFORE TRUNCATE ON %I FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history()',item);
    END LOOP;
END $$;

-- Step 14 / FR-I64: Content Ownership Declarations
CREATE TABLE IF NOT EXISTS content_declarations (
    id SERIAL PRIMARY KEY,
    course_id INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    lesson_id INT REFERENCES lessons(id) ON DELETE CASCADE,
    instructor_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    declaration_text TEXT NOT NULL,
    license_type VARCHAR(50) NOT NULL,
    source_attribution TEXT,
    declared_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    takedown_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    takedown_reason TEXT,
    takedown_flagged_at TIMESTAMP,
    instructor_response TEXT,
    instructor_responded_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMIT;

-- =========================
-- FR-I19: QUIZZES, QUESTIONS & QUIZ_QUESTIONS
-- =========================
-- Quizzes linked to a Course or Module, with optional Lesson position.
CREATE TABLE quizzes (
    id                 SERIAL PRIMARY KEY,
    course_id          INTEGER       NOT NULL,
    module_id          INTEGER,
    lesson_id          INTEGER,
    created_by         INTEGER,
    title              VARCHAR(200)  NOT NULL,
    description        TEXT,
    instructions       TEXT,
    status             VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    total_marks         NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
    passing_marks       NUMERIC(8,2),
    time_limit_minutes  INTEGER,
    number_of_questions INTEGER,
    max_attempts        INTEGER       NOT NULL DEFAULT 1,
    available_from      TIMESTAMP,
    available_until     TIMESTAMP,
    created_at          TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quizzes_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quizzes_module
        FOREIGN KEY (module_id)
        REFERENCES course_modules(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_quizzes_lesson
        FOREIGN KEY (lesson_id)
        REFERENCES lessons(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_quizzes_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_quiz_status
        CHECK (status IN ('DRAFT', 'PUBLISHED')),

    CONSTRAINT chk_quiz_max_attempts
        CHECK (max_attempts >= 1),

    CONSTRAINT chk_quiz_availability_window
        CHECK (available_from IS NULL OR available_until IS NULL OR available_from < available_until)
);

-- Reusable Questions table (extensible for FR-I21 question types, FR-I22 marking schemes, FR-I24 question bank)
CREATE TABLE questions (
    id                      SERIAL PRIMARY KEY,
    course_id               INTEGER,
    created_by              INTEGER,
    question_text           TEXT          NOT NULL,
    question_type           VARCHAR(30)   NOT NULL DEFAULT 'MULTIPLE_CHOICE',
    options_json            TEXT,
    correct_answer          TEXT,
    default_marks           NUMERIC(8,2)  NOT NULL DEFAULT 1.00,
    negative_marks          NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
    numerical_tolerance     NUMERIC(8,2)  DEFAULT 0.00,
    grading_rubric          TEXT,
    requires_manual_grading BOOLEAN       NOT NULL DEFAULT FALSE,
    topic                   VARCHAR(100),
    difficulty              VARCHAR(30),
    explanation             TEXT,
    is_bank_question        BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_questions_course
        FOREIGN KEY (course_id)
        REFERENCES courses(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_questions_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_question_type
        CHECK (question_type IN ('MULTIPLE_CHOICE', 'MULTIPLE_SELECT', 'TRUE_FALSE', 'NUMERICAL', 'DESCRIPTIVE', 'SHORT_ANSWER'))
);

-- Ordered mapping between Quizzes and Questions (FR-I19)
CREATE TABLE quiz_questions (
    id             SERIAL PRIMARY KEY,
    quiz_id        INTEGER       NOT NULL,
    question_id    INTEGER       NOT NULL,
    order_index    INTEGER       NOT NULL DEFAULT 1,
    marks          NUMERIC(8,2),
    negative_marks NUMERIC(8,2),
    is_required    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP     NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quiz_questions_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quiz_questions_question
        FOREIGN KEY (question_id)
        REFERENCES questions(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_quiz_questions_quiz_question
        UNIQUE (quiz_id, question_id)
);

-- Persistent server-side quiz attempt tracking for FR-I20 / DR-ED06 / S19, S22, S24
CREATE TABLE quiz_attempts (
    id             SERIAL PRIMARY KEY,
    quiz_id        INTEGER      NOT NULL,
    student_id     INTEGER      NOT NULL,
    attempt_number INTEGER      NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'IN_PROGRESS',
    started_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMP,
    submitted_at   TIMESTAMP,
    obtained_marks NUMERIC(8,2),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_quiz_attempts_quiz
        FOREIGN KEY (quiz_id)
        REFERENCES quizzes(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_quiz_attempts_student
        FOREIGN KEY (student_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_quiz_attempts_quiz_student_num
        UNIQUE (quiz_id, student_id, attempt_number),

    CONSTRAINT chk_quiz_attempt_status
        CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'EXPIRED')),

    CONSTRAINT chk_quiz_attempt_number
        CHECK (attempt_number >= 1)
);

-- Step 13 / FR-I13: Chunked and Resumable Video Uploads
CREATE TABLE IF NOT EXISTS video_uploads (
    id VARCHAR(64) PRIMARY KEY,
    lesson_id INT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    uploader_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    total_bytes BIGINT NOT NULL,
    received_bytes BIGINT NOT NULL DEFAULT 0,
    chunk_size BIGINT NOT NULL,
    total_chunks INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    storage_path VARCHAR(500),
    hls_url VARCHAR(500),
    dash_url VARCHAR(500),
    failure_reason TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

