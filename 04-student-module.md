# Sprint 1 — Student Module (Browse, Enroll, Watch)

Covers FR-S05, FR-S07, FR-S08, FR-S11, FR-S12 (US-S05, US-S07, US-S08, US-S11, US-S12) — simplified to the essentials.

## Pages / routes needed
- `GET /courses` — catalog: list all `published` courses (open to any logged-in student; search/filter can wait for a later sprint)
- `GET /courses/:id` — course detail page: title, description, instructor name, an "Enroll" button (or "Go to Lectures" if already enrolled)
- `POST /courses/:id/enroll` — creates an `enrollments` row for the logged-in student
- `GET /courses/:id/learn` — the actual lecture-watching page; requires an existing enrollment row, otherwise redirect back to the detail page with a message

## Catalog page (`/courses`)
- Query: `SELECT * FROM courses WHERE status = 'published'`
- Show title, short description, instructor name (join `users` on `instructor_id`) for each

## Course detail page (`/courses/:id`)
- Show full course info
- Check if an `enrollments` row exists for `(student_id, course_id)`:
  - If not enrolled → show "Enroll" button → `POST /courses/:id/enroll`
  - If enrolled → show "Go to Lectures" link → `/courses/:id/learn`

## Enroll action
- Insert into `enrollments (student_id, course_id)`.
- Since there's no payment in Sprint 1, this is a direct insert — no gateway call, no "confirmed success" gate (that's FR-PG04, later sprint).
- Handle the case where the student is already enrolled gracefully (don't error, just redirect to `/learn`).

## Watch page (`/courses/:id/learn`)
- Server check: does an `enrollments` row exist for this student + course? If not, deny access (FR-S11: "Non-enrolled students are denied access").
- Fetch the course's lectures (ordered by `order_index`).
- Render each lecture as an embedded YouTube iframe:
  ```html
  <iframe
    width="640" height="360"
    src="https://www.youtube.com/embed/{video_id}"
    frameborder="0"
    allow="accelerated-video; encrypted-media; picture-in-picture"
    allowfullscreen>
  </iframe>
  ```
- YouTube's embedded player already gives you play/pause/seek/resume-position-within-session for free (covers the spirit of FR-S12). True cross-session "resume where I left off" (FR-S13) would need you to track playback position yourself via the YouTube IFrame API — worth flagging as a stretch goal, not required for Sprint 1's Definition of Done.

## Out of scope this sprint
- Search/filter by domain, duration, rating, subject (FR-S06)
- Bookmarking courses (FR-S10)
- Progress tracking / "mark as completed" (FR-S15, FR-S16)
- Downloadable materials, notes, PDFs (FR-S14)
- In-video keyword search (FR-S18)




