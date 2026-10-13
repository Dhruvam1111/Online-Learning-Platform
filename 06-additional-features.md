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

## Summary Table

| # | Feature | FRs | Effort | New Tables |
|---|---------|-----|--------|------------|
| 1 | Profile Management | FR-S03, FR-I03, FR-I04 | Low | — (alter `users`) |
| 2 | My Enrolled Courses | FR-S09 | Low | — |
| 3 | Instructor Course Dashboard | FR-I60, FR-I09 | Low | — |
| 4 | Course Search & Filter | FR-S06 | Low | — (alter `courses`) |
| 5 | Progress Tracking | FR-S15, FR-S16 | Medium | `lecture_progress` |
| 6 | Reorder & Delete Lectures | FR-I16 | Medium | — |
| 7 | Ratings & Reviews | (supports FR-S06) | Medium | `reviews` |
| 8 | Announcements | FR-I39, FR-S62 | Medium | `announcements` |
| 9 | Bookmark / Wishlist | FR-S10 | Low | `bookmarks` |
| 10 | Enrollment Stats | FR-I43 | Low | — |
| 11 | Password Reset | FR-S02, FR-I02 | Medium | `password_reset_tokens` |

---

## More Features (To Be Added)

_Additional features to be discussed and added below._

---
