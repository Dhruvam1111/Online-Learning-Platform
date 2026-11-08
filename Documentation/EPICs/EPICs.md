# Distribution of Requirements into EPICs

This document presents the distribution of system requirements into high-level Epics and their allocation across sprints for the **Online Learning Platform using RAG**.

---

## Epic 1: Account & Authentication
**Goal:** Every user type can register, log in, log out, and recover access securely, with administrative MFA.
* **Stories:** `US-S01`, `US-S02`, `US-I01`, `US-I02`, `US-A01`, `US-A05`
* **FRs:** FR-01, FR-02, FR-04
* **NFRs / DRs:** NFR-13, NFR-14, NFR-17; DR-04
* **Sprint Allocation:**
  - Sprint 1: Student and Instructor registration, login, logout, password reset flow.
  - Sprint 1B: Admin elevated login with MFA challenge and user recovery overrides.

---

## Epic 2: Profile Management
**Goal:** Students and instructors can keep their personal and professional details current.
* **Stories:** `US-S03`, `US-I03`, `US-I04`
* **FRs:** FR-03
* **NFRs:** NFR-17, NFR-20, NFR-30
* **Sprint Allocation:** Sprint 1

---

## Epic 3: Course Discovery, Search & Ratings
**Goal:** Students can find the right course and judge it before enrolling.
* **Stories:** `US-S04`, `US-S05`, `US-S06`, `US-S-REV`
* **FRs:** FR-11, FR-14
* **NFRs:** NFR-02, NFR-20, NFR-22
* **Sprint Allocation:** Sprint 1

---

## Epic 4: Enrollment & My Learning
**Goal:** Students can enroll in a course, return to their enrolled courses, and save courses for later.
* **Stories:** `US-S07`, `US-S08`, `US-S09`
* **FRs:** FR-12
* **NFRs:** NFR-12, NFR-20, NFR-22
* **Sprint Allocation:** Sprint 1 (Free Enrollment & Wishlist); Sprint 5A (Paid Checkout & Enrollment Gates).

---

## Epic 5: Course Creation & Management
**Goal:** Instructors can create, structure, publish, and maintain courses.
* **Stories:** `US-I05`, `US-I06`, `US-I07`, `US-I08`, `US-I09`, `US-I10`, `US-I11`, `US-I12`, `US-I15`
* **FRs:** FR-05, FR-06
* **NFRs:** NFR-03, NFR-12, NFR-17, NFR-20, NFR-27
* **Sprint Allocation:**
  - Sprint 1: Basic course creation, draft/publish toggle, instructor dashboard.
  - Sprint 1B: Admin review submission queue (`pending_review`).
  - Sprint 2: Hierarchical modules/sections organization.
  - Sprint 5A: Pricing, discounts, prerequisites, and archiving.

---

## Epic 6: Content Upload & Management
**Goal:** Instructors can add, organize, and control learning content, and enrolled students can stream it.
* **Stories:** `US-I16`, `US-I17`, `US-I18`, `US-I19`, `US-I20`, `US-I21`, `US-I22`, `US-I23`
* **FRs:** FR-07, FR-09, FR-10
* **NFRs:** NFR-01, NFR-06, NFR-17, NFR-26; DR-08, DR-09
* **Sprint Allocation:**
  - Sprint 1: Add/reorder/delete video lectures, stream playlist.
  - Sprint 2: Supplementary materials (PDF notes, slides, bookmarks).
  - Sprint 3: Lecture transcript & subtitle capture for RAG indexing.
  - Sprint 5A: Scheduled content release gates.

---

## Epic 7: Learning Experience & Resume
**Goal:** Enrolled students can study course content smoothly and resume where they left off.
* **Stories:** `US-S11`, `US-S12`, `US-S13`, `US-S14`, `US-S17`, `US-S18`
* **FRs:** FR-07, FR-08, FR-09, FR-23
* **NFRs:** NFR-01, NFR-03, NFR-20, NFR-21
* **Sprint Allocation:**
  - Sprint 1: Watch lectures and video player controls.
  - Sprint 2: Resume playback position from last stopped second.
  - Sprint 3: In-video semantic transcript search.

---

## Epic 8: Progress Tracking
**Goal:** Students can mark content complete and track overall completion percentages.
* **Stories:** `US-S15`, `US-S16`
* **FRs:** FR-13
* **NFRs:** NFR-03, NFR-20
* **Sprint Allocation:** Sprint 1

---

## Epic 9: Quizzes & Question Bank
**Goal:** Instructors can maintain question banks and build quizzes; students can attempt timed quizzes with auto-scoring.
* **Stories:** `US-S19` to `US-S27`, `US-I24` to `US-I29`
* **FRs:** FR-15, FR-16, FR-17, FR-18
* **NFRs:** NFR-03, NFR-11, NFR-17, NFR-20, NFR-22; DR-ED03, DR-ED06
* **Sprint Allocation:** Sprint 2

---

## Epic 10: Examinations Core
**Goal:** Instructors can schedule high-stakes exams, and students can take them fairly within open windows.
* **Stories:** `US-S28` to `US-S33`, `US-S52`, `US-S53`, `US-I30`, `US-I32`, `US-A17`
* **FRs:** FR-26, FR-27
* **NFRs:** NFR-03, NFR-07, NFR-10, NFR-11, NFR-24; DR-ED06, DR-ED08
* **Sprint Allocation:** Sprint 4A

---

## Epic 11: Exam Monitoring & Anti-Cheating
**Goal:** Exams are monitored with consent, logging tab blur and fullscreen exits without unfair automated penalties.
* **Stories:** `US-S48` to `US-S51`, `US-S54` to `US-S57`, `US-I31`, `US-I34`, `US-A18`, `US-A19`
* **FRs:** FR-28, FR-29
* **NFRs:** NFR-07, NFR-18, NFR-23, NFR-32; DR-06, DR-07
* **Sprint Allocation:** Sprint 4B

---

## Epic 12: Grading, Results & Academic Records
**Goal:** Descriptive answers are evaluated, results are published by instructors, and corrections are audit-logged.
* **Stories:** `US-S34`, `US-S35`, `US-I33`, `US-I35` to `US-I39`
* **FRs:** FR-30
* **NFRs:** NFR-17, NFR-19, NFR-20; DR-01, DR-ED04
* **Sprint Allocation:** Sprint 4A

---

## Epic 13: Question Reporting & Resolution
**Goal:** Students report ambiguous questions, and instructors and admins resolve them fairly with uniform score adjustments.
* **Stories:** `US-S36` to `US-S40`, `US-I40` to `US-I43`, `US-A15`, `US-A16`
* **FRs:** FR-31
* **NFRs:** NFR-17, NFR-19, NFR-23; DR-ED05
* **Sprint Allocation:** Sprint 4B

---

## Epic 14: Student Performance & Study Planning
**Goal:** Students understand their performance and receive actionable revision recommendations.
* **Stories:** `US-S41` to `US-S47`, `US-S71`, `US-S72`
* **FRs:** FR-24
* **NFRs:** NFR-04, NFR-05, NFR-17, NFR-20
* **Sprint Allocation:** Sprint 3 (AI Study Schedule & Weakness Diagnostics); Sprint 4B (Cumulative Gradebook).

---

## Epic 15: Instructor Analytics
**Goal:** Instructors can view enrollment counts, pass rates, and export gradebooks.
* **Stories:** `US-I48` (stats), `US-I49`, `FR-I45` to `FR-I47`
* **FRs:** FR-15
* **NFRs:** NFR-05, NFR-17, NFR-30
* **Sprint Allocation:** Sprint 1 (Basic Stats); Sprint 4B (Analytics & Gradebook Export).

---

## Epic 16: Announcements, Discussions & Notifications
**Goal:** Instructors and peers engage via course discussion boards and in-app notifications.
* **Stories:** `US-S58` to `US-S61`, `US-I44` to `US-I47`, `US-A27`, `US-A28`
* **FRs:** FR-16
* **NFRs:** NFR-17, NFR-20, NFR-23
* **Sprint Allocation:** Sprint 1 (Announcements); Sprint 2 (Forums & Notifications); Sprint 5B (Admin Broadcasts).

---

## Epic 17: Gamification & Engagement
**Goal:** Students stay motivated through study streaks, points, badges, and optional leaderboards.
* **Stories:** `US-S62` to `US-S66`, `US-I51`, `US-I52`, `US-I56`, `US-I57`
* **FRs:** FR-35
* **NFRs:** NFR-03, NFR-17, NFR-20
* **Sprint Allocation:** Sprint 5B

---

## Epic 18: Core RAG AI Learning Assistance
**Goal:** Course-grounded AI tutor answering student queries with citations, automated lecture summaries, and draft quiz generation.
* **Stories:** `US-S67` to `US-S72`, `US-I58` to `US-I60`
* **FRs:** FR-19, FR-20, FR-21, FR-22, FR-24, FR-25
* **NFRs:** NFR-04, NFR-05, NFR-17, NFR-29
* **Sprint Allocation:** Sprint 3

---

## Epic 19: Certificates & Course Completion
**Goal:** Students who meet completion criteria receive a digitally verifiable certificate with a public QR code.
* **Stories:** `US-S10`, `US-S73` to `US-S75`, `US-I61`, `US-I62`, `US-A29`
* **FRs:** FR-34
* **NFRs:** NFR-17, NFR-22, NFR-34; DR-01, DR-02, DR-ED01
* **Sprint Allocation:** Sprint 5B

---

## Epic 20: Payments & Checkout
**Goal:** Secure paid checkout with cryptographic signature verification and enrollment unlocking.
* **Stories:** `US-PG01` to `US-PG04`, `US-PG09` to `US-PG11`, `US-I10`
* **FRs:** FR-32
* **NFRs:** NFR-12, NFR-15, NFR-16; DR-PY01 to DR-PY03
* **Sprint Allocation:** Sprint 5A

---

## Epic 21: Refunds, Payouts & Financial Operations
**Goal:** Money movement is accurate, idempotent, and auditable.
* **Stories:** `US-PG05` to `US-PG08`, `US-PG12` to `US-PG15`, `US-I53` to `US-I55`, `US-A23` to `US-A26`
* **FRs:** FR-33
* **NFRs:** NFR-15, NFR-16, NFR-19, NFR-33; DR-PY04 to DR-PY09
* **Sprint Allocation:** Sprint 5A

---

## Epic 22: Admin User, Instructor & Course Governance
**Goal:** Administrators control who can teach and which courses go live.
* **Stories:** `US-A02`, `US-A03`, `US-A04`, `US-A06` to `US-A10`
* **FRs:** FR-36
* **NFRs:** NFR-02, NFR-05, NFR-17, NFR-19
* **Sprint Allocation:** Sprint 1B

---

## Epic 23: Content Moderation
**Goal:** Policy-violating forum posts are reviewed in a moderation queue and removed.
* **Stories:** `US-A11` to `US-A14`, `US-I42`
* **FRs:** FR-36
* **NFRs:** NFR-05, NFR-17, NFR-19, NFR-23; DR-08
* **Sprint Allocation:** Sprint 5B

---

## Epic 24: Platform Analytics & Compliance
**Goal:** Admins monitor platform health, user volumes, and audit logs.
* **Stories:** `US-A20` to `US-A22`, `US-A30` to `US-A32`
* **FRs:** FR-36
* **NFRs:** NFR-05, NFR-19, NFR-30 to NFR-33; DR-15
* **Sprint Allocation:** Sprint 1B (Audit Logs); Sprint 5B (Analytics & Privacy).

---

## Epic 25: Platform Quality & Hardening
**Goal:** System-wide load testing for 2,000 concurrent students, DR recovery drill, WCAG accessibility, and security compliance.
* **Stories:** Cross-cutting NFRs
* **Sprint Allocation:** Sprint 6
