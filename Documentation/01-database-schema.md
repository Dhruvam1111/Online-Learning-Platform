# Sprint 1 — Database Schema

Minimal relational schema covering only what Sprint 1 needs. Extra columns referenced in the FR/NFR doc (pricing, modules/sections, certificates, etc.) are deliberately left out — add them in later sprints.

## `users`
| Column | Type | Notes |
|---|---|---|
| id | UUID / SERIAL PK | |
| name | VARCHAR(100) | |
| email | VARCHAR(255) UNIQUE NOT NULL | |
| password_hash | VARCHAR(255) NOT NULL | bcrypt/argon2 — never store plaintext (NFR-13) |
| role | ENUM('student','instructor') NOT NULL | admin role not needed yet |
| created_at | TIMESTAMP DEFAULT now() | |

## `courses`
| Column | Type | Notes |
|---|---|---|
| id | UUID / SERIAL PK | |
| instructor_id | FK → users.id NOT NULL | must be a user with role='instructor' |
| title | VARCHAR(200) NOT NULL | |
| description | TEXT | |
| status | ENUM('draft','published') DEFAULT 'draft' | skip 'pending_review' — no admin approval this sprint |
| created_at | TIMESTAMP DEFAULT now() | |

## `lectures`
| Column | Type | Notes |
|---|---|---|
| id | UUID / SERIAL PK | |
| course_id | FK → courses.id NOT NULL | |
| title | VARCHAR(200) NOT NULL | |
| youtube_url | VARCHAR(500) NOT NULL | validate it's a real YouTube URL before saving |
| order_index | INTEGER DEFAULT 0 | so lectures display in a set order |
| created_at | TIMESTAMP DEFAULT now() | |

## `enrollments`
| Column | Type | Notes |
|---|---|---|
| id | UUID / SERIAL PK | |
| student_id | FK → users.id NOT NULL | must be role='student' |
| course_id | FK → courses.id NOT NULL | |
| enrolled_at | TIMESTAMP DEFAULT now() | |
| UNIQUE(student_id, course_id) | | prevents double-enrolling |

## Relationships
```
users (1) ───< courses (instructor_id)
courses (1) ───< lectures (course_id)
users (1) ───< enrollments >─── (1) courses
```

## Access rules to enforce in the API layer (not just the DB)
- Only the instructor who owns a course can add/edit its lectures (maps to DR-05 / NFR-17 in spirit, simplified).
- A student can only watch a course's lecture list if they have a row in `enrollments` for that course (FR-S11: "non-enrolled students are denied access").
- `youtube_url` should be converted to an embeddable format server-side (e.g. extract the video ID and build `https://www.youtube.com/embed/{id}`) rather than trusting the raw pasted URL as-is.

## To extend in a later sprint
- Add `modules` / `lessons` tables if you need the structured module→section→lesson hierarchy (FR-I06)
- Add `price`, `enrollment_limit`, `prerequisites` to `courses` (FR-I10, FR-I11)
- Split `status` into the full draft → pending_review → published → archived lifecycle once admin approval (FR-A06) is in scope
