-- ============================================================
-- Online Learning Platform — Sprint 1 Seed Data
-- Run AFTER 01-schema.sql and 02-indexes.sql
-- ============================================================

-- Clear existing data (safe to re-run)
DELETE FROM quiz_attempts;
DELETE FROM quiz_questions;
DELETE FROM questions;
DELETE FROM quizzes;
DELETE FROM course_association_requests;
DELETE FROM lessons;
DELETE FROM course_sections;
DELETE FROM course_modules;
DELETE FROM course_prerequisites;
DELETE FROM course_instructors;
DELETE FROM instructor_upload_history;
DELETE FROM instructor_credentials;
DELETE FROM enrollments;
DELETE FROM lectures;
DELETE FROM courses;
DELETE FROM users;

-- Reset sequences so IDs start from 1
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE courses_id_seq RESTART WITH 1;
ALTER SEQUENCE lectures_id_seq RESTART WITH 1;
ALTER SEQUENCE enrollments_id_seq RESTART WITH 1;
ALTER SEQUENCE course_modules_id_seq RESTART WITH 1;
ALTER SEQUENCE course_sections_id_seq RESTART WITH 1;
ALTER SEQUENCE lessons_id_seq RESTART WITH 1;
ALTER SEQUENCE course_association_requests_id_seq RESTART WITH 1;
ALTER SEQUENCE quizzes_id_seq RESTART WITH 1;
ALTER SEQUENCE questions_id_seq RESTART WITH 1;
ALTER SEQUENCE quiz_questions_id_seq RESTART WITH 1;
ALTER SEQUENCE quiz_attempts_id_seq RESTART WITH 1;

-- =========================
-- USERS
-- =========================
-- All passwords below are: "password123"
-- Bcrypt hash generated with cost factor 10
-- Valid Hash for "password123": $2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW
--
-- NOTE TO TEAM: Use "password123" to log in as any of these users during testing.

INSERT INTO users (name, email, password_hash, role, account_status) VALUES
    ('Platform Admin',     'admin@olp.com',          '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'admin',      'active'),
    ('Dr. Sarah Chen',     'sarah@instructor.com',   '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'instructor', 'active'),
    ('Prof. James Wilson', 'james@instructor.com',   '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'instructor', 'active'),
    ('Alice Johnson',      'alice@student.com',      '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'student',    'active'),
    ('Bob Martinez',       'bob@student.com',        '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'student',    'active'),
    ('Charlie Park',       'charlie@student.com',    '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'student',    'active'),
    ('Pending Instructor', 'pending@instructor.com', '$2a$10$h1etqUgZ1ZJs7/mhkvvd3eTR1DPRhpLUW4DmQxQiYpxVVCDMzL8tW', 'instructor', 'pending');

-- User IDs after insert:
--   1 = Dr. Sarah Chen (instructor)
--   2 = Prof. James Wilson (instructor)
--   3 = Alice Johnson (student)
--   4 = Bob Martinez (student)
--   5 = Charlie Park (student)


-- =========================
-- COURSES
-- =========================

INSERT INTO courses (instructor_id, title, description, category, status) VALUES
    (1, 'Web Development Fundamentals',
        'Learn the core building blocks of the web: HTML for structure, CSS for styling, and JavaScript for interactivity. This course takes you from zero to building your first responsive website.',
        'Web Development',
        'published'),

    (1, 'Introduction to Databases',
        'Understand how data is stored, queried, and managed. Covers relational database concepts, SQL syntax, table design, and normalization. Uses PostgreSQL for hands-on exercises.',
        'Databases',
        'published'),

    (2, 'Python for Beginners',
        'A gentle introduction to programming using Python. Covers variables, loops, functions, and basic data structures. No prior coding experience required.',
        'Programming',
        'published'),

    (2, 'Advanced Machine Learning',
        'Deep dive into neural networks, transformers, and reinforcement learning. Prerequisites: Python proficiency and basic statistics.',
        'Data Science',
        'draft'),

    (2, 'Spring Boot 3 Masterclass',
        'Comprehensive guide to building production-ready REST APIs with Spring Boot 3.',
        'Backend Development',
        'pending_review');

-- Link initial course creators as 'owner' in course_instructors (FR-I05, FR-I59)
INSERT INTO course_instructors (course_id, instructor_id, role) VALUES
    (1, 1, 'owner'),
    (1, 3, 'co-instructor'),
    (2, 1, 'owner'),
    (3, 2, 'owner'),
    (4, 2, 'owner'),
    (5, 2, 'owner');

-- Course IDs after insert:
--   1 = Web Development Fundamentals (published, by Sarah)
--   2 = Introduction to Databases (published, by Sarah)
--   3 = Python for Beginners (published, by James)
--   4 = Advanced Machine Learning (draft, by James) — should NOT appear in catalog
--   5 = Spring Boot 3 Masterclass (pending_review, by James) — awaiting admin approval


-- =========================
-- LECTURES
-- =========================
-- Using real, publicly available YouTube educational videos.

INSERT INTO lectures (course_id, title, youtube_url, video_id, order_index) VALUES
    -- Course 1: Web Development Fundamentals
    (1, 'What is the Web?',
        'https://www.youtube.com/watch?v=hJHvdBlSxug',
        'hJHvdBlSxug', 1),

    (1, 'HTML Crash Course',
        'https://www.youtube.com/watch?v=UB1O30fR-EE',
        'UB1O30fR-EE', 2),

    (1, 'CSS in 100 Seconds',
        'https://www.youtube.com/watch?v=OEV8gMkCHXQ',
        'OEV8gMkCHXQ', 3),

    -- Course 2: Introduction to Databases
    (2, 'What is a Database?',
        'https://www.youtube.com/watch?v=FR4QIeZaPeM',
        'FR4QIeZaPeM', 1),

    (2, 'SQL Tutorial for Beginners',
        'https://www.youtube.com/watch?v=HXV3zeQKqGY',
        'HXV3zeQKqGY', 2),

    -- Course 3: Python for Beginners
    (3, 'Python in 100 Seconds',
        'https://www.youtube.com/watch?v=x7X9w_GIm1s',
        'x7X9w_GIm1s', 1),

    (3, 'Variables and Data Types',
        'https://www.youtube.com/watch?v=kqtD5dpn9C8',
        'kqtD5dpn9C8', 2),

    (3, 'Loops and Conditionals',
        'https://www.youtube.com/watch?v=6iF8Xb7Z3wQ',
        '6iF8Xb7Z3wQ', 3);

-- Lecture IDs after insert:
--   1, 2, 3 = Web Dev lectures
--   4, 5    = Database lectures
--   6, 7, 8 = Python lectures
-- Course 4 (draft) has NO lectures — this is intentional for testing


-- =========================
-- ENROLLMENTS
-- =========================

INSERT INTO enrollments (student_id, course_id) VALUES
    (3, 1),   -- Alice enrolled in Web Development
    (3, 3),   -- Alice enrolled in Python
    (4, 1),   -- Bob enrolled in Web Development
    (5, 2);   -- Charlie enrolled in Databases

-- Enrollment test cases this creates:
--   Alice (3):   enrolled in course 1 and 3, NOT in 2
--   Bob (4):     enrolled in course 1 only, NOT in 2 or 3
--   Charlie (5): enrolled in course 2 only, NOT in 1 or 3
--   Nobody is enrolled in course 4 (which is draft anyway)


-- =========================
-- COURSE STRUCTURE (FR-I06)
-- Modules → Sections → Lessons
-- =========================

INSERT INTO course_modules (course_id, title, description, order_index) VALUES
    (1, 'Module 1: Getting Started with HTML', 'Introduction to web markup and structure', 1),
    (1, 'Module 2: Styling with Modern CSS', 'Cascading Style Sheets, flexbox and grid layouts', 2);

INSERT INTO course_sections (module_id, title, description, order_index) VALUES
    (1, 'Section 1.1: HTML Basics', 'Core elements and page anatomy', 1),
    (1, 'Section 1.2: Semantic HTML', 'Accessible and semantic tag usage', 2),
    (2, 'Section 2.1: CSS Fundamentals', 'Selectors, box model, and cascade', 1);

INSERT INTO lessons (section_id, title, lesson_type, content, content_url, duration_minutes, order_index) VALUES
    (1, 'Introduction to HTML Tags', 'VIDEO', 'Overview of basic HTML syntax and elements', 'https://www.youtube.com/watch?v=UB1O30fR-EE', 15, 1),
    (1, 'HTML Document Skeleton', 'MATERIAL', 'Reference guide and starter template code', NULL, 10, 2),
    (1, 'HTML Quick Check', 'QUIZ', 'Knowledge check on tags and attributes', NULL, 5, 3),
    (2, 'Semantic Markup Best Practices', 'MATERIAL', 'In-depth reading on landmark elements', NULL, 20, 1),
    (2, 'Building an Accessible Blog Page', 'ASSIGNMENT', 'Create a semantic HTML portfolio page', NULL, 45, 2),
    (3, 'Mastering the CSS Box Model', 'VIDEO', 'Detailed visual walkthrough of margin, border, padding', 'https://www.youtube.com/watch?v=rIO5326FgPE', 18, 1);

-- Step 14 / FR-I64: Content Ownership Declarations Seed
INSERT INTO content_declarations (course_id, lesson_id, instructor_id, declaration_text, license_type, source_attribution, takedown_status)
VALUES
    (1, 1, 2, 'I confirm that I own all rights to this video lecture or possess the necessary licenses.', 'ORIGINAL_CREATOR', 'Created by Dr. Sarah Chen for Web Development Fundamentals', 'NONE'),
    (1, 2, 2, 'I confirm that I own all rights to this video lecture or possess the necessary licenses.', 'ORIGINAL_CREATOR', 'Created by Dr. Sarah Chen for Web Development Fundamentals', 'NONE'),
    (2, NULL, 2, 'I confirm that I hold full copyright or appropriate educational licenses for all materials in this course.', 'ORIGINAL_CREATOR', 'Database Systems Curriculum, Dr. Sarah Chen', 'NONE')
ON CONFLICT DO NOTHING;

-- =========================
-- QUIZZES & QUESTIONS (FR-I19, FR-I20)
-- =========================
INSERT INTO quizzes (course_id, module_id, lesson_id, created_by, title, description, instructions, status, total_marks, passing_marks, time_limit_minutes, number_of_questions, max_attempts, available_from, available_until) VALUES
    (1, 1, 3, 2, 'HTML Fundamentals Quick Check', 'Assess understanding of basic HTML tags and document structure', 'Answer all questions within the allotted time.', 'DRAFT', 10.00, 6.00, 15, 2, 2, NULL, NULL);

INSERT INTO questions (course_id, created_by, question_text, question_type, options_json, correct_answer, default_marks, negative_marks, topic, difficulty, is_bank_question) VALUES
    (1, 2, 'Which HTML element defines the root of an HTML document?', 'MULTIPLE_CHOICE', '["<html>","<head>","<body>","<!DOCTYPE>"]', '<html>', 5.00, 0.00, 'HTML Basics', 'EASY', TRUE),
    (1, 2, 'The <head> element contains metadata that is directly rendered in the main browser viewport.', 'TRUE_FALSE', '["TRUE","FALSE"]', 'FALSE', 5.00, 0.00, 'HTML Basics', 'EASY', TRUE);

INSERT INTO quiz_questions (quiz_id, question_id, order_index, marks, negative_marks, is_required) VALUES
    (1, 1, 1, 5.00, 0.00, TRUE),
    (1, 2, 2, 5.00, 0.00, TRUE);

-- Step 13 / FR-I13: Video Uploads Seed
INSERT INTO video_uploads (id, lesson_id, uploader_id, filename, file_type, total_bytes, received_bytes, chunk_size, total_chunks, status, storage_path, hls_url, dash_url)
VALUES
    ('seed-upload-uuid-001', 1, 2, 'intro_to_web_dev.mp4', 'video/mp4', 52428800, 52428800, 5242880, 10, 'READY', '/uploads/videos/seed-upload-uuid-001.mp4', '/uploads/videos/seed-upload-uuid-001/master.m3u8', '/uploads/videos/seed-upload-uuid-001/manifest.mpd')
ON CONFLICT (id) DO NOTHING;

