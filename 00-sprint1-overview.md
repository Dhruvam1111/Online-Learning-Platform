# Sprint 1 — Overview & Scope

## Goal
Get a thin, end-to-end slice of the platform working: a student can log in and watch a lecture that an instructor uploaded, where the lecture itself is just a YouTube link/embed (no real video hosting/transcoding yet).

## In scope this sprint
- Student registration & login
- Instructor registration & login
- Instructor: create a course, add a lecture to it (title + YouTube URL)
- Student: browse/view available courses, open a course, watch its lecture(s) via embedded YouTube player
- Minimal enrollment (student clicks "Enroll", free — no payment)

## Explicitly out of scope this sprint (later sprints)
- Payments / Payment Gateway (FR-PG*, DR-PY*)
- Admin course approval workflow (FR-A06–A10) — courses publish directly
- Quizzes, exams, monitoring/anti-cheat (FR-S18–S61)
- Gamification, AI assistant, certificates, discussion forums
- Real video upload/transcoding — lectures are YouTube links only
- Password reset flow (just register/login/logout)

## Mapped requirements
| Feature | Requirement IDs |
|---|---|
| Student register/login | FR-S01, FR-S02 (US-S01, US-S02) |
| Instructor register/login | FR-I01, FR-I02 (US-I01, US-I02) — skip admin-approval gate this sprint |
| Instructor creates course | FR-I05 (US-I05) |
| Instructor adds lecture | FR-I13 (US-I13) — simplified to "paste YouTube link" instead of file upload |
| Student browses courses | FR-S05, FR-S07 (US-S05, US-S07) |
| Student enrolls | FR-S08 (US-S08) — no payment path |
| Student watches lecture | FR-S11, FR-S12 (US-S11, US-S12) — native YouTube player controls stand in for FR-S12/S13 |

## Suggested stack (adjust to what your team already knows)
- Backend: Node.js + Express (or Django/Flask if your team prefers Python)
- DB: PostgreSQL (or MongoDB if you want schema flexibility — schema below is written relationally but maps 1:1 to collections)
- Frontend: React (plain HTML/JS is fine too for Sprint 1)
- Auth: JWT stored in an httpOnly cookie, or simple session-based auth — either is fine for a class project

## Task breakdown / suggested order
1. Set up repo, DB, base Express/Django app skeleton
2. Build `users` table + register/login/logout (see `02-auth-module.md`)
3. Build `courses` + `lectures` tables (see `01-database-schema.md`)
4. Instructor: "Create Course" form + "Add Lecture" form (see `03-instructor-module.md`)
5. Student: course catalog page + course detail page + embedded YouTube player (see `04-student-module.md`)
6. Wire up enrollment (simple join table, no payment)
7. Manual end-to-end test: register instructor → create course → add lecture → register student → enroll → watch lecture

## Definition of Done for Sprint 1
- A fresh user can register as either a student or instructor
- An instructor can create a course and attach at least one YouTube-linked lecture
- A student can see that course in a catalog, enroll, and watch the embedded lecture
- All passwords are hashed (never stored in plain text)
- Basic role check: only instructors see "Create Course"; only enrolled students can view lecture content
