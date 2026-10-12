# Sprint 1 — Additional Features

These features expand the original Sprint 1 scope (register/login, create course, add lecture, browse, enroll, watch) to make the deliverable more substantial and closer to a real product.

> Status legend: 🔲 Not started · 🟡 In progress · ✅ Done

---

## Feature 1: Profile Management (Student + Instructor)
**FRs:** FR-S03, FR-I03, FR-I04
**Effort:** Low
**Status:** 🔲

**Description:**
Students and instructors can view and update their profile information. Instructors can additionally set qualifications, expertise areas, and a bio.

**What to build:**
- `GET /api/users/me` — return logged-in user's profile
- `PUT /api/users/me` — update name, bio, expertise (instructor), profile picture URL
- Frontend: profile view/edit page accessible from the navbar

**DB changes:** Add columns to `users`:
```sql
ALTER TABLE users ADD COLUMN bio TEXT;
ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(500);
ALTER TABLE users ADD COLUMN expertise VARCHAR(255); -- instructor only
ALTER TABLE users ADD COLUMN qualifications VARCHAR(255); -- instructor only
```

**Notes:** Backend DTOs (`UpdateProfileRequest`, `UserResponse`) already exist from the merged `origin/main` code. Extend them as needed.

---

## Feature 2: My Enrolled Courses Dashboard (Student)
**FRs:** FR-S09
**Effort:** Low
**Status:** 🔲

**Description:**
Students see a "My Courses" page listing all courses they are currently enrolled in, with course title, instructor name, and enrollment date.

**What to build:**
- `GET /api/students/my-courses` — returns all courses the logged-in student is enrolled in
- Frontend: "My Courses" page linked from the student navbar/dashboard

**DB changes:** None — uses existing `enrollments` + `courses` + `users` join.

**Query pattern:**
```sql
SELECT c.id, c.title, c.description, u.name AS instructor_name, e.enrolled_at
FROM enrollments e
JOIN courses c ON e.course_id = c.id
JOIN users u ON c.instructor_id = u.id
WHERE e.student_id = :studentId
ORDER BY e.enrolled_at DESC;
```

---

## Feature 3: Instructor Course Dashboard (My Courses + Edit)
**FRs:** FR-I60, FR-I09
**Effort:** Low
**Status:** 🔲

**Description:**
Instructors see a dashboard listing all their own courses with status (draft/published). They can edit course title/description and toggle between draft and published.

**What to build:**
- `GET /api/instructor/courses` — list courses owned by the logged-in instructor
- `PUT /api/instructor/courses/:id` — update title, description, status
- Frontend: instructor dashboard with course cards showing status badges and edit functionality

**DB changes:** None — uses existing `courses` table.

---

## Feature 4: Course Search & Filter
**FRs:** FR-S06
**Effort:** Low
**Status:** 🔲

**Description:**
Students can search courses by keyword (matches title/description) and filter by category/domain.

**What to build:**
- `GET /api/courses?search=keyword&category=programming` — search/filter endpoint
- Frontend: search bar + category dropdown on the course catalog page

**DB changes:** Add `category` column to `courses`:
```sql
ALTER TABLE courses ADD COLUMN category VARCHAR(100);
```

**Query pattern:**
```sql
SELECT c.*, u.name AS instructor_name
FROM courses c
JOIN users u ON c.instructor_id = u.id
WHERE c.status = 'published'
  AND (c.title ILIKE '%keyword%' OR c.description ILIKE '%keyword%')
  AND (c.category = :category OR :category IS NULL)
ORDER BY c.created_at DESC;
```

---

## Feature 5: Course Progress Tracking
**FRs:** FR-S15, FR-S16
**Effort:** Medium
**Status:** 🔲

**Description:**
Students can mark individual lectures as completed. The system displays a progress bar showing completion percentage (e.g., "3/7 lectures — 43%").

**What to build:**
- `POST /api/courses/:courseId/lectures/:lectureId/complete` — mark a lecture as completed
- `DELETE /api/courses/:courseId/lectures/:lectureId/complete` — unmark
- `GET /api/courses/:courseId/progress` — returns completed count / total count
- Frontend: checkbox per lecture on the learn page + progress bar on My Courses and course detail

**DB changes:** New table:
```sql
CREATE TABLE lecture_progress (
    id           SERIAL PRIMARY KEY,
    student_id   INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lecture_id   INTEGER   NOT NULL REFERENCES lectures(id) ON DELETE CASCADE,
    completed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, lecture_id)
);
```

---

## Feature 6: Instructor — Reorder & Delete Lectures
**FRs:** FR-I16 (partial)
**Effort:** Medium
**Status:** 🔲

**Description:**
Instructors can reorder lectures within a course (up/down arrows or drag-and-drop updating `order_index`) and delete lectures.

**What to build:**
- `PUT /api/instructor/courses/:courseId/lectures/reorder` — accepts an ordered list of lecture IDs, updates `order_index`
- `DELETE /api/instructor/courses/:courseId/lectures/:lectureId` — deletes a lecture
- Frontend: up/down arrows (or drag handle) + delete button on the course management page

**DB changes:** None — uses existing `lectures.order_index`.

---

## Feature 7: Course Ratings & Reviews
**FRs:** Supports FR-S06 (filter by rating) — standard LMS feature
**Effort:** Medium
**Status:** 🔲

**Description:**
Enrolled students can rate a course (1–5 stars) and leave a short text review. The course detail page shows the average rating and a list of reviews.

**What to build:**
- `POST /api/courses/:courseId/reviews` — submit rating + comment (one per student per course)
- `GET /api/courses/:courseId/reviews` — list all reviews for a course
- `GET /api/courses` — include `avg_rating` in the catalog response
- Frontend: star rating component + review form on course detail page, average stars on catalog cards

**DB changes:** New table:
```sql
CREATE TABLE reviews (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   INTEGER  NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    rating      SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, course_id)
);
```

---

## Feature 8: Course Announcements
**FRs:** FR-I39, FR-S62
**Effort:** Medium
**Status:** 🔲

**Description:**
Instructors can post announcements for their course. Enrolled students see announcements on the course/learn page.

**What to build:**
- `POST /api/instructor/courses/:courseId/announcements` — create announcement (title + body)
- `GET /api/courses/:courseId/announcements` — list announcements for a course (newest first)
- `DELETE /api/instructor/courses/:courseId/announcements/:id` — delete an announcement
- Frontend: announcement section on course learn page + announcement form on instructor course management page

**DB changes:** New table:
```sql
CREATE TABLE announcements (
    id            SERIAL PRIMARY KEY,
    course_id     INTEGER      NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    instructor_id INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(200) NOT NULL,
    body          TEXT         NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

## Feature 9: Bookmark / Wishlist Courses
**FRs:** FR-S10
**Effort:** Low
**Status:** 🔲

**Description:**
Students can bookmark courses they're interested in (before or after enrollment). A "My Wishlist" page shows all bookmarked courses.

**What to build:**
- `POST /api/courses/:courseId/bookmark` — add bookmark
- `DELETE /api/courses/:courseId/bookmark` — remove bookmark
- `GET /api/students/bookmarks` — list bookmarked courses
- Frontend: bookmark icon (heart/star) on course cards + "My Wishlist" page

**DB changes:** New table:
```sql
CREATE TABLE bookmarks (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER   NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   INTEGER   NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, course_id)
);
```

---

## Feature 10: Basic Enrollment Stats for Instructors
**FRs:** FR-I43
**Effort:** Low
**Status:** 🔲

**Description:**
Instructor dashboard shows enrollment count per course and a simple total enrolled students metric.

**What to build:**
- `GET /api/instructor/courses/:courseId/stats` — returns enrollment count, average rating (if reviews exist)
- Include enrollment count in `GET /api/instructor/courses` response
- Frontend: enrollment count badge on instructor course cards + simple stats section on course management page

**DB changes:** None — aggregate query on `enrollments`.

**Query pattern:**
```sql
SELECT c.id, c.title, COUNT(e.id) AS enrollment_count
FROM courses c
LEFT JOIN enrollments e ON c.id = e.course_id
WHERE c.instructor_id = :instructorId
GROUP BY c.id, c.title;
```

---

## Feature 11: Password Reset Flow
**FRs:** FR-S02, FR-I02
**Effort:** Medium
**Status:** 🔲

**Description:**
Users who forget their password can request a reset link via email. They click the link, set a new password, and log in.

**What to build:**
- `POST /api/auth/forgot-password` — accepts email, generates a reset token, sends email (or logs the link for dev)
- `POST /api/auth/reset-password` — accepts token + new password, updates `password_hash`
- Frontend: "Forgot Password?" link on login page → email input form → reset form

**DB changes:** New table (or add columns to `users`):
```sql
CREATE TABLE password_reset_tokens (
    id          SERIAL PRIMARY KEY,
    user_id     INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

**Notes:** For Sprint 1 / dev mode, you can skip actual email sending — just log the reset URL to the console. Use a UUID or secure random string for the token. Token should expire after ~1 hour.

---

## Feature 12: Admin Login / Logout
**FRs:** FR-A01
**Effort:** Low
**Status:** 🔲

**Description:**
Admins can log in with elevated privileges. There is no admin registration — admin accounts are seeded directly in the database. The admin role grants access to all admin-specific endpoints and pages.

**What to build:**
- Update role CHECK constraint to allow `'admin'`
- Add `requireRole('admin')` middleware/guard for all admin routes
- `POST /api/auth/login` — existing login endpoint, but admin role now grants access to admin dashboard
- Frontend: admin dashboard page (separate from student/instructor views) with navigation to user management, course approval, etc.
- Seed a default admin account in `03-seed.sql`

**DB changes:**
```sql
-- Update role constraint
ALTER TABLE users DROP CONSTRAINT chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('student', 'instructor', 'admin'));

-- Seed admin (in 03-seed.sql)
INSERT INTO users (name, email, password_hash, role)
VALUES ('Platform Admin', 'admin@olp.com', '<bcrypt_hash_of_admin123>', 'admin');
```

**Notes:** For security (NFR-13), admin sessions should have a shorter expiry than regular users. In production, MFA would be added — skip for Sprint 1.

---

## Feature 13: User Management Dashboard (Admin)
**FRs:** FR-A02
**Effort:** Medium
**Status:** 🔲

**Description:**
Admins can view a list of all users (students and instructors), search/filter by role or name, and take actions: suspend or delete accounts.

**What to build:**
- `GET /api/admin/users?role=student&search=john&status=active` — paginated list of all users with filters
- `PUT /api/admin/users/:id/suspend` — set `account_status = 'suspended'`
- `PUT /api/admin/users/:id/activate` — set `account_status = 'active'`
- `DELETE /api/admin/users/:id` — delete a user account (cascades to their courses/enrollments)
- Frontend: admin user management page with a table, search bar, role filter, and action buttons per row

**DB changes:** Add `account_status` column to `users`:
```sql
ALTER TABLE users ADD COLUMN account_status VARCHAR(20) NOT NULL DEFAULT 'active';
ALTER TABLE users ADD CONSTRAINT chk_users_account_status
    CHECK (account_status IN ('active', 'pending', 'suspended'));
```

**Notes:** Suspended users should be blocked from logging in — check `account_status` during the login flow.

---

## Feature 14: Instructor Approval Workflow
**FRs:** FR-A03, FR-I01
**Effort:** Medium
**Status:** 🔲

**Description:**
When a new instructor registers, their `account_status` is set to `'pending'` instead of `'active'`. They cannot create courses until an admin approves them. Admins see a list of pending instructors and can approve or reject them.

**What to build:**
- Modify `POST /api/auth/register` — if `role = 'instructor'`, set `account_status = 'pending'`
- `GET /api/admin/instructors/pending` — list all instructors awaiting approval
- `PUT /api/admin/instructors/:id/approve` — set `account_status = 'active'`
- `PUT /api/admin/instructors/:id/reject` — set `account_status = 'rejected'` (or delete the account)
- Update course creation endpoint — check that `account_status = 'active'` before allowing course creation
- Frontend: pending instructors queue on admin dashboard with approve/reject buttons + "Your account is pending approval" message for unapproved instructors

**DB changes:** Uses the `account_status` column from Feature 13. Add `'rejected'` to the constraint:
```sql
ALTER TABLE users DROP CONSTRAINT chk_users_account_status;
ALTER TABLE users ADD CONSTRAINT chk_users_account_status
    CHECK (account_status IN ('active', 'pending', 'suspended', 'rejected'));
```

**Notes:** Students register with `account_status = 'active'` by default (no approval needed). Only instructors go through the approval gate.

---

## Feature 15: Course Approval Workflow (Admin)
**FRs:** FR-A06, FR-A07
**Effort:** Medium
**Status:** 🔲

**Description:**
Instead of courses going directly to `'published'`, instructors submit courses for review. The status becomes `'pending_review'`. Admins review and approve (→ `'published'`) or reject (→ `'rejected'`) with a reason.

**What to build:**
- `PUT /api/instructor/courses/:id/submit` — instructor submits course for review, status → `'pending_review'`
- `GET /api/admin/courses/pending` — list all courses awaiting approval
- `PUT /api/admin/courses/:id/approve` — status → `'published'`
- `PUT /api/admin/courses/:id/reject` — status → `'rejected'`, include a `rejection_reason`
- Frontend: "Submit for Review" button on instructor course page + pending courses queue on admin dashboard with approve/reject + reason modal

**DB changes:**
```sql
-- Update course status constraint
ALTER TABLE courses DROP CONSTRAINT chk_courses_status;
ALTER TABLE courses ADD CONSTRAINT chk_courses_status
    CHECK (status IN ('draft', 'pending_review', 'published', 'rejected'));

-- Add rejection reason column
ALTER TABLE courses ADD COLUMN rejection_reason TEXT;
```

**Notes:** Rejected courses go back to the instructor who can edit and re-submit. The instructor should be able to see the rejection reason.

---

## Feature 16: Admin — View All Courses
**FRs:** FR-A09
**Effort:** Low
**Status:** 🔲

**Description:**
Admins can see a master list of ALL courses on the platform (not just published ones), filterable by status. This is a management/oversight view, different from the student catalog.

**What to build:**
- `GET /api/admin/courses?status=published&search=java` — list all courses with status and search filters
- Response includes: course title, instructor name, status, enrollment count, created date
- Frontend: admin courses page with a table, status filter tabs (All / Draft / Pending / Published / Rejected), and search bar

**DB changes:** None — query on existing `courses` + `users` + `enrollments` tables.

**Query pattern:**
```sql
SELECT c.id, c.title, c.status, c.created_at, u.name AS instructor_name,
       COUNT(e.id) AS enrollment_count
FROM courses c
JOIN users u ON c.instructor_id = u.id
LEFT JOIN enrollments e ON c.id = e.course_id
WHERE (:status IS NULL OR c.status = :status)
  AND (:search IS NULL OR c.title ILIKE '%' || :search || '%')
GROUP BY c.id, c.title, c.status, c.created_at, u.name
ORDER BY c.created_at DESC;
```

---

## Feature 17: Admin — Unpublish / Remove Course
**FRs:** FR-A08
**Effort:** Low
**Status:** 🔲

**Description:**
Admins can unpublish a live course (moves it back to `'draft'`) or permanently remove it from the platform. This is used for policy violations or quality issues.

**What to build:**
- `PUT /api/admin/courses/:id/unpublish` — status → `'draft'`, course disappears from student catalog
- `DELETE /api/admin/courses/:id` — permanently delete the course and all its lectures/enrollments (CASCADE)
- Frontend: "Unpublish" and "Remove" action buttons on the admin courses page (Feature 16), with a confirmation modal for deletion

**DB changes:** None — uses existing `courses` table and status values.

**Notes:** Consider sending a notification (or at least storing a reason) when unpublishing — the instructor should know why their course was taken down. For Sprint 1, a simple status change is sufficient.

---

## Summary Table

| # | Feature | Stakeholder | FRs | Effort | New Tables |
|---|---------|-------------|-----|--------|------------|
| 1 | Profile Management | Student/Instructor | FR-S03, FR-I03, FR-I04 | Low | — (alter `users`) |
| 2 | My Enrolled Courses | Student | FR-S09 | Low | — |
| 3 | Instructor Course Dashboard | Instructor | FR-I60, FR-I09 | Low | — |
| 4 | Course Search & Filter | Student | FR-S06 | Low | — (alter `courses`) |
| 5 | Progress Tracking | Student | FR-S15, FR-S16 | Medium | `lecture_progress` |
| 6 | Reorder & Delete Lectures | Instructor | FR-I16 | Medium | — |
| 7 | Ratings & Reviews | Student | (supports FR-S06) | Medium | `reviews` |
| 8 | Announcements | Instructor/Student | FR-I39, FR-S62 | Medium | `announcements` |
| 9 | Bookmark / Wishlist | Student | FR-S10 | Low | `bookmarks` |
| 10 | Enrollment Stats | Instructor | FR-I43 | Low | — |
| 11 | Password Reset | All | FR-S02, FR-I02 | Medium | `password_reset_tokens` |
| 12 | Admin Login | Admin | FR-A01 | Low | — (alter `users` role) |
| 13 | User Management | Admin | FR-A02 | Medium | — (alter `users`) |
| 14 | Instructor Approval | Admin | FR-A03, FR-I01 | Medium | — |
| 15 | Course Approval Workflow | Admin/Instructor | FR-A06, FR-A07 | Medium | — (alter `courses`) |
| 16 | View All Courses (Admin) | Admin | FR-A09 | Low | — |
| 17 | Unpublish / Remove Course | Admin | FR-A08 | Low | — |

---

## More Features (To Be Added)

_Additional features to be discussed and added below._

---
