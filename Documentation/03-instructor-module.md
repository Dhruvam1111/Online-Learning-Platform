# Sprint 1 — Instructor Module (Create Course + Add Lecture)

Covers FR-I05 (US-I05) and a simplified FR-I13 (US-I13), where "upload video" = "paste a YouTube link" instead of a real file upload.

## Pages / routes needed
- `GET /instructor/courses` — list of courses this instructor owns (FR-I60, simplified)
- `GET /instructor/courses/new` — "Create Course" form
- `POST /instructor/courses` — creates the course
- `GET /instructor/courses/:id` — course management page, shows its lectures + "Add Lecture" form
- `POST /instructor/courses/:id/lectures` — adds a lecture to the course

All of these require `requireRole('instructor')` and, for the last two, a check that `course.instructor_id === logged_in_user.id`.

## Create Course form
Fields (Sprint 1 minimum — matches `courses` table):
- Title (required)
- Description (optional, textarea)
- Publish status: for Sprint 1, just default new courses to `status = 'published'` so they show up immediately for students — skip the draft/review toggle unless your team wants the extra form control.

On submit: insert a row into `courses` with `instructor_id = logged_in_user.id`.

## Add Lecture form
Fields (matches `lectures` table):
- Lecture title (required)
- YouTube URL (required)

Validation:
- Confirm the URL matches a YouTube pattern (`youtube.com/watch?v=...` or `youtu.be/...`).
- Extract the video ID server-side and store it (or store the raw URL and extract on render — either works, but extracting once at save time is simpler for the student-side player).
- `order_index`: either let the instructor set a number, or just default to "append at the end" (count existing lectures for that course + 1).

On submit: insert a row into `lectures` linked to `course_id`.

## What to show on the course management page
- Course title/description
- A list of its lectures (title + thumbnail, if you want a nice touch — YouTube gives you `https://img.youtube.com/vi/{video_id}/0.jpg` for free)
- The "Add Lecture" form below the list

## Out of scope this sprint
- Real video file upload/transcoding (FR-I13's literal "upload video files")
- Course pricing, discounts, enrollment limits (FR-I10)
- Modules/sections/lessons structure (FR-I06) — lectures are a flat list per course for now
- Draft → pending review → admin approval lifecycle (FR-I07, FR-I08, FR-A06)
- Multiple instructors per course (FR-I59)
