# YOUR TASK — Database Engineer (Pranamya)

## What You Own

You are the **sole owner** of the entire database layer for Sprint 1. No other team member creates tables, writes migrations, or modifies the schema. They consume your database through their backend code.

Your deliverables:
1. A complete SQL schema file that creates all tables
2. An indexes file
3. A seed data file with realistic test data
4. A Docker Compose file so anyone can spin up the DB locally
5. A credentials sheet to share with the team

---

## Project Structure

Create the following files inside the project root:

```
Online Learning Platform/
├── db/
│   ├── 01-schema.sql          ← Table definitions
│   ├── 02-indexes.sql         ← All indexes
│   ├── 03-seed.sql            ← Test data
│   └── drop-all.sql           ← Nuclear reset script
├── docker-compose.yml         ← One-command local DB
└── DB-README.md               ← Setup instructions for the team
```

---

## File 1: `db/01-schema.sql`

This is the master schema. Every column, constraint, default, and relationship is defined here. Copy this exactly.

```sql
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
```

### Design Decisions Explained

| Decision | Reasoning |
|---|---|
| `SERIAL` instead of `UUID` | Simpler for Sprint 1. UUIDs are better for distributed systems, but we're on a single Postgres instance. SERIAL gives us short, readable IDs for debugging. |
| `VARCHAR(20)` with `CHECK` instead of `ENUM` | Easier to add new roles/statuses in later sprints without `ALTER TYPE`. |
| `ON DELETE CASCADE` everywhere | Keeps things simple during development. If you delete a test user, their courses, lectures, and enrollments all clean up automatically. |
| Separate `video_id` column on `lectures` | Avoids parsing the YouTube URL every time the frontend needs a thumbnail or embed. The backend extracts it once at insert time. |
| `order_index` as `INTEGER` not `SERIAL` | The instructor controls the order. It's not auto-incrementing across all lectures, just within a course. |
| `NOT NULL DEFAULT NOW()` on timestamps | Every row gets a timestamp automatically. The `NOT NULL` prevents anyone from explicitly inserting NULL. |

---

## File 2: `db/02-indexes.sql`

Indexes speed up the queries the backend will run most often. Without these, every query does a full table scan.

```sql
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
```

### Why These Specific Indexes

| Index | Query It Speeds Up | Who Uses It |
|---|---|---|
| `idx_courses_instructor_id` | `SELECT * FROM courses WHERE instructor_id = ?` | Person 3 (Instructor Backend) |
| `idx_courses_status` | `SELECT * FROM courses WHERE status = 'published'` | Person 4 (Student Backend) |
| `idx_lectures_course_id_order` | `SELECT * FROM lectures WHERE course_id = ? ORDER BY order_index` | Person 3, Person 4 |
| `idx_enrollments_student_id` | `SELECT * FROM enrollments WHERE student_id = ?` | Person 4 (list enrolled courses) |
| `idx_enrollments_course_id` | `SELECT COUNT(*) FROM enrollments WHERE course_id = ?` | Person 4 (enrollment count) |

---

## File 3: `db/03-seed.sql`

Realistic test data. Every backend and frontend developer needs this to develop and test without creating data manually every time.

**Important:** The password hashes below are real bcrypt hashes. The plaintext passwords are listed in comments so the team can log in during testing.

```sql
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
```

### Why This Specific Seed Data

| Scenario | What It Tests |
|---|---|
| 3 published courses, 1 draft | Catalog should show only 3. Draft should be invisible to students but visible to its instructor (James). |
| 2 instructors, each with 2 courses | Tests that instructor dashboard only shows their own courses (Sarah sees 1 and 2, James sees 3 and 4). |
| Course 4 has no lectures | Tests empty state on the manage course page. |
| Alice enrolled in 2 courses, not in 1 | Tests the "Enroll" vs "Go to Lectures" button logic. |
| Bob enrolled in only 1 course | Tests that enrollment check blocks access to unenrolled courses. |
| Charlie enrolled in course 2 | Additional variation. |

---

## File 4: `db/drop-all.sql`

A nuclear reset script. Use it when you need to completely wipe and rebuild.

```sql
-- ============================================================
-- DANGER: Drops everything. Use only during development.
-- ============================================================

DROP TABLE IF EXISTS enrollments CASCADE;
DROP TABLE IF EXISTS lectures CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS users CASCADE;
```

---

## File 5: `docker-compose.yml` (project root)

This lets any team member run `docker-compose up` and get a fully seeded database in seconds.

```yaml
version: "3.8"

services:
  db:
    image: postgres:16-alpine
    container_name: olp_postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: online_learning
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      # These files run automatically in alphabetical order on first startup
      - ./db/01-schema.sql:/docker-entrypoint-initdb.d/01-schema.sql
      - ./db/02-indexes.sql:/docker-entrypoint-initdb.d/02-indexes.sql
      - ./db/03-seed.sql:/docker-entrypoint-initdb.d/03-seed.sql
      # Persist data between container restarts
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

### How to use

```bash
# Start the database (first time: creates tables + inserts seed data)
docker-compose up -d

# Check it's running
docker-compose ps

# Connect with psql to verify
docker exec -it olp_postgres psql -U postgres -d online_learning

# Inside psql, run some checks:
#   \dt                          — list all tables
#   SELECT * FROM users;         — see seed users
#   SELECT * FROM courses;       — see seed courses
#   SELECT * FROM lectures;      — see seed lectures
#   SELECT * FROM enrollments;   — see seed enrollments

# To completely reset (wipe data + re-seed):
docker-compose down -v
docker-compose up -d

# Stop the database
docker-compose down
```

---

## File 6: `DB-README.md` (project root)

Share this with the team so they can set up their local database.

````markdown
# Database Setup

## Prerequisites
- Docker and Docker Compose installed

## Quick Start
1. Clone the repo
2. Run: `docker-compose up -d`
3. Database is now running at `localhost:5432`

## Connection Details
| Property | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | online_learning |
| Username | postgres |
| Password | postgres |
| JDBC URL | `jdbc:postgresql://localhost:5432/online_learning` |

## Test Accounts
All accounts use the password: **password123**

| Email | Role | User ID |
|---|---|---|
| sarah@instructor.com | instructor | 1 |
| james@instructor.com | instructor | 2 |
| alice@student.com | student | 3 |
| bob@student.com | student | 4 |
| charlie@student.com | student | 5 |

## Resetting the Database
To wipe all data and re-seed from scratch:
```bash
docker-compose down -v
docker-compose up -d
```

## Schema Diagram
```
users (1) ───< courses (instructor_id)
courses (1) ───< lectures (course_id)
users (1) ───< enrollments >─── (1) courses
```

## Tables
- **users** — Students and instructors (distinguished by `role` column)
- **courses** — Created by instructors, status is 'draft' or 'published'
- **lectures** — YouTube links belonging to a course, ordered by `order_index`
- **enrollments** — Join table: which student is enrolled in which course
````

---

## Step-by-Step Execution Order

Follow this exact order:

### Step 1: Create the folder structure
```
mkdir db
```

### Step 2: Create `db/01-schema.sql`
Copy the schema SQL from the "File 1" section above.

### Step 3: Create `db/02-indexes.sql`
Copy the indexes SQL from the "File 2" section above.

### Step 4: Generate real bcrypt hashes
The seed data uses a placeholder hash. Before using it, generate a real bcrypt hash for "password123":

**Option A — Using Node.js:**
```bash
node -e "const bcrypt = require('bcrypt'); bcrypt.hash('password123', 10).then(h => console.log(h));"
```

**Option B — Using Python:**
```bash
python -c "import bcrypt; print(bcrypt.hashpw(b'password123', bcrypt.gensalt(10)).decode())"
```

**Option C — Using an online tool:**
Go to https://bcrypt-generator.com/, enter "password123", rounds = 10, and copy the hash.

Replace every occurrence of `$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy` in `03-seed.sql` with your generated hash.

### Step 5: Create `db/03-seed.sql`
Copy the seed SQL from the "File 3" section above (with the updated hash).

### Step 6: Create `db/drop-all.sql`
Copy from the "File 4" section.

### Step 7: Create `docker-compose.yml` in the project root
Copy from the "File 5" section.

### Step 8: Create `DB-README.md` in the project root
Copy from the "File 6" section.

### Step 9: Start the database
```bash
docker-compose up -d
```

### Step 10: Verify everything works
```bash
docker exec -it olp_postgres psql -U postgres -d online_learning
```

Then run each of these inside psql and confirm the output:

```sql
-- Should show 4 tables
\dt

-- Should show 5 users (2 instructors, 3 students)
SELECT id, name, email, role FROM users;

-- Should show 4 courses (3 published, 1 draft)
SELECT id, title, status, instructor_id FROM courses;

-- Should show 8 lectures across 3 courses (course 4 has none)
SELECT id, course_id, title, order_index FROM lectures ORDER BY course_id, order_index;

-- Should show 4 enrollments
SELECT e.id, u.name, c.title
FROM enrollments e
JOIN users u ON e.student_id = u.id
JOIN courses c ON e.course_id = c.id;
```

### Step 11: Share with the team
- Push all files to the `main` branch (this is foundational, everyone depends on it)
- Send the team the `DB-README.md` content (or just tell them to read it in the repo)
- Confirm the shared password ("password123") and connection details

---

## Verification Checklist

Run through this before marking your task as done:

- [ ] `docker-compose up -d` starts the database without errors
- [ ] `\dt` shows exactly 4 tables: users, courses, lectures, enrollments
- [ ] `SELECT * FROM users` returns 5 rows with correct roles
- [ ] `SELECT * FROM courses` returns 4 rows; only 3 have status = 'published'
- [ ] `SELECT * FROM courses WHERE status = 'published'` returns exactly 3 rows
- [ ] `SELECT * FROM lectures ORDER BY course_id, order_index` returns 8 rows in correct order
- [ ] Course 4 (draft) has zero lectures
- [ ] `SELECT * FROM enrollments` returns 4 rows with correct student/course pairs
- [ ] Inserting a duplicate enrollment `INSERT INTO enrollments (student_id, course_id) VALUES (3, 1)` returns a unique constraint violation error
- [ ] Inserting a user with role 'admin' fails with a check constraint violation
- [ ] Inserting a course with status 'archived' fails with a check constraint violation
- [ ] Deleting user 1 (Sarah) cascades: her courses (1, 2) and their lectures are also deleted
- [ ] After cascade delete, run `docker-compose down -v && docker-compose up -d` to restore seed data
- [ ] `DB-README.md` accurately reflects the connection details and test accounts
- [ ] The bcrypt hash actually works: Person 2 can verify "password123" against it using `bcrypt.compare`

---

## Common Pitfalls to Avoid

| Pitfall | How to Avoid |
|---|---|
| Init scripts don't re-run on `docker-compose up` | PostgreSQL only runs `/docker-entrypoint-initdb.d/` scripts on first initialization. If you change SQL files, you must run `docker-compose down -v` (the `-v` deletes the volume) then `docker-compose up -d` to re-initialize. |
| Sequence IDs don't restart | After deleting and re-inserting seed data without resetting sequences, IDs will continue from where they left off (e.g., 6, 7, 8 instead of 1, 2, 3). The seed script handles this with `ALTER SEQUENCE ... RESTART WITH 1`. |
| Bcrypt hash is invalid | If you copy-paste the hash and it gets corrupted (missing characters, wrong quoting), login will silently fail. Always test the hash with `bcrypt.compare` before sharing. |
| Port 5432 already in use | If you have a local Postgres installation, it may already be using port 5432. Either stop the local Postgres or change the Docker port mapping to `"5433:5432"` (and tell the team to use port 5433). |
| Windows line endings in SQL files | If you're on Windows, make sure your SQL files use LF line endings (not CRLF). Some editors insert CRLF by default, which can cause issues with the Docker init scripts. In VS Code: click "CRLF" in the status bar and change to "LF". |

---

## What the Backend Developers Need from You

After you're done, each backend person needs:

| Person | What They Need from Your DB |
|---|---|
| **Person 2 (Auth)** | The `users` table schema, the bcrypt hash for testing, and confirmation that email has a UNIQUE constraint |
| **Person 3 (Instructor)** | The `courses` and `lectures` table schemas, the `video_id` column spec, and confirmation about CASCADE behavior |
| **Person 4 (Student)** | The `enrollments` table schema, the UNIQUE constraint on (student_id, course_id), and the seed enrollment data for testing |

Share the `DB-README.md` and confirm they can connect. That's your handoff.
