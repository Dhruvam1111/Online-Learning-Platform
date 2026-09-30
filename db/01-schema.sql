-- ============================================================
-- Online Learning Platform — Sprint 1 Schema
-- Run this FIRST. Creates all tables from scratch.
-- ============================================================

-- Clean slate (safe to re-run during development)
DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS lectures CASCADE;
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
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    -- Enforce that role is one of the two allowed values
    CONSTRAINT chk_users_role CHECK (role IN ('student', 'instructor'))
);

-- Why VARCHAR for role instead of ENUM?
-- PostgreSQL ENUMs are hard to alter later (adding 'admin' in Sprint 2
-- would require ALTER TYPE). A VARCHAR with a CHECK constraint is
-- easier to extend: just ALTER the CHECK constraint.


-- =========================
-- TABLE: courses
-- =========================
-- Each course belongs to exactly one instructor.
-- Status: 'draft' means only the instructor sees it.
--         'published' means students can see it in the catalog.
-- There is NO 'pending_review' status this sprint (no admin approval).

CREATE TABLE courses (
    id              SERIAL PRIMARY KEY,
    instructor_id   INTEGER      NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'draft',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_courses_instructor
        FOREIGN KEY (instructor_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_courses_status CHECK (status IN ('draft', 'published'))
);

-- ON DELETE CASCADE: If an instructor account is deleted, their courses
-- are also deleted. This is acceptable for Sprint 1. In production you
-- might want RESTRICT or soft-delete instead.


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
