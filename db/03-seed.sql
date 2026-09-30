-- ============================================================
-- Online Learning Platform — Sprint 1 Seed Data
-- Run AFTER 01-schema.sql and 02-indexes.sql
-- ============================================================

-- Clear existing data (safe to re-run)
DELETE FROM enrollments;
DELETE FROM lectures;
DELETE FROM courses;
DELETE FROM users;

-- Reset sequences so IDs start from 1
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE courses_id_seq RESTART WITH 1;
ALTER SEQUENCE lectures_id_seq RESTART WITH 1;
ALTER SEQUENCE enrollments_id_seq RESTART WITH 1;

-- =========================
-- USERS
-- =========================
-- All passwords below are: "password123"
-- Bcrypt hash generated with cost factor 10
-- Hash for "password123": $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
--
-- NOTE TO TEAM: Use "password123" to log in as any of these users during testing.

INSERT INTO users (name, email, password_hash, role) VALUES
    ('Dr. Sarah Chen',    'sarah@instructor.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'instructor'),
    ('Prof. James Wilson', 'james@instructor.com',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'instructor'),
    ('Alice Johnson',      'alice@student.com',      '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student'),
    ('Bob Martinez',       'bob@student.com',        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student'),
    ('Charlie Park',       'charlie@student.com',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student');

-- User IDs after insert:
--   1 = Dr. Sarah Chen (instructor)
--   2 = Prof. James Wilson (instructor)
--   3 = Alice Johnson (student)
--   4 = Bob Martinez (student)
--   5 = Charlie Park (student)


-- =========================
-- COURSES
-- =========================

INSERT INTO courses (instructor_id, title, description, status) VALUES
    (1, 'Web Development Fundamentals',
        'Learn the core building blocks of the web: HTML for structure, CSS for styling, and JavaScript for interactivity. This course takes you from zero to building your first responsive website.',
        'published'),

    (1, 'Introduction to Databases',
        'Understand how data is stored, queried, and managed. Covers relational database concepts, SQL syntax, table design, and normalization. Uses PostgreSQL for hands-on exercises.',
        'published'),

    (2, 'Python for Beginners',
        'A gentle introduction to programming using Python. Covers variables, loops, functions, and basic data structures. No prior coding experience required.',
        'published'),

    (2, 'Advanced Machine Learning',
        'Deep dive into neural networks, transformers, and reinforcement learning. Prerequisites: Python proficiency and basic statistics.',
        'draft');

-- Course IDs after insert:
--   1 = Web Development Fundamentals (published, by Sarah)
--   2 = Introduction to Databases (published, by Sarah)
--   3 = Python for Beginners (published, by James)
--   4 = Advanced Machine Learning (draft, by James) — should NOT appear in catalog


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
