# Person 2 – Content, Question Bank, Exams & Corrections

Online Learning Platform – Instructor Module · **24 FRs**

Step numbers (1–69) are the position in the **common global order** shared by all three people, so gaps in your numbering mean another person builds that step. Always do your steps in the order below.

**Your order:** 13 (FR-I13) → 14 (FR-I64) → 15 (FR-I14) → 16 (FR-I17) → 17 (FR-I67) → 18 (FR-I15) → 19 (FR-I16) → 20 (FR-I18) → 25 (FR-I65) → 26 (FR-I24) → 27 (FR-I23) → 28 (FR-I68) → 38 (FR-I25) → 39 (FR-I27) → 40 (FR-I26) → 41 (FR-I66) → 49 (FR-I74) → 50 (FR-I33) → 51 (FR-I34) → 52 (FR-I35) → 53 (FR-I36) → 54 (FR-I37) → 55 (FR-I29) → 56 (FR-I69)

---

## Step 13 · FR-I13 – Upload Video Lectures

- Build a chunked/resumable upload endpoint that checks on the server that the instructor is associated with the target course.
- Store the original in object storage and queue a transcoding job to produce HLS/DASH renditions (NFR-26).
- Attach the video to a lesson (from FR-I06) and return upload status/progress to the UI.

## Step 14 · FR-I64 – Content Ownership Declaration

- Add a mandatory declaration at upload ("I own or license this content") with licence type and source.
- Save the declaration with user and timestamp, and block the upload without it.
- Support takedown handling: when admin flags content (FR-A11/A12), the instructor can respond, replace or remove it.

## Step 15 · FR-I14 – Upload Supplementary Materials

- Upload PDFs, documents and presentations with type/size validation and a virus-scan hook.
- Attach files to lessons or modules and store metadata (name, type, size).
- Default every file to view-only until FR-I17 changes the setting.

## Step 16 · FR-I17 – Downloadable vs View-Only

- Add a per-file downloadable / view-only flag, editable at any time.
- The student download endpoint must check both the flag and enrollment (DR-09).
- Serve view-only files through an embedded viewer that does not expose a download URL.

## Step 17 · FR-I67 – Captions and Transcripts

- Upload caption files (VTT/SRT) and transcript text per video and language.
- Validate format and timestamps, then attach them to the video player as tracks.
- Store the transcript as searchable text: it powers student keyword search inside videos (FR-S18) and accessibility (NFR-21).

## Step 18 · FR-I15 – Video Processing Status

- Track job states: uploaded, processing, ready, failed (with error reason).
- Update the state from the transcoder's webhook or polling, and notify the instructor through the notification service when it is ready or failed.
- Show a status badge per video and allow re-upload after failure.

## Step 19 · FR-I16 – Reorder, Replace, Remove Content

- Implement drag-and-drop reordering that updates the order index of lessons/content.
- Replace a file while keeping the lesson and student progress references intact.
- Remove with confirmation; use soft-delete if students already have progress, and warn if the item is part of the completion criteria.

## Step 20 · FR-I18 – Schedule Content Release

- Add a release date-time per content item, stored in UTC and shown in the instructor's time zone (default IST) with the zone label (DR-ED08).
- Run a scheduled job that publishes the content at the set time; students must not see it earlier.
- Raise a "new content available" event for the notification feature (FR-I41, Person 3).

## Step 25 · FR-I65 – Marking Rules (Negative, Partial, Tolerance)

- Add per-quiz/question configuration for negative marks, partial marks in multiple-select, and numerical tolerance.
- Extend the auto-grader from FR-I22 (Person 1) so rules are applied exactly as configured, and show the rules on the quiz start page before the attempt (DR-ED03).
- Validate that total marks equal the sum of question marks and write test cases for every rule combination.

## Step 26 · FR-I24 – Question Bank

- Create a question bank with tags (course, topic, difficulty, type); quizzes and exams can reuse questions by reference or copy.
- Add search/filter and editing with versioning so already-published quizzes do not break.
- Topic tags are essential: they feed analytics (FR-I46) and student weak-topic detection (FR-S49/S50).

## Step 27 · FR-I23 – Answer Review and Feedback Toggle

- Add quiz settings to show or hide correct answers and feedback, and to choose when (immediately or after the deadline).
- Make the student result API check these settings before returning answers (FR-S26).
- Default these settings to off for exams.

## Step 28 · FR-I68 – Assignments

- Create the Assignment entity: title, instructions, deadline, max marks, submission type (file/text) and late policy.
- Store student submissions and send them to the grading queue of FR-I30 (Person 1).
- Store deadlines in UTC, enforce them on the server, and raise reminder events for notifications.

## Step 38 · FR-I25 – Create and Schedule Examinations

- Create the Exam entity: start/end window, duration, eligibility rules (minimum progress, prerequisite, enrolled), max marks; store times in UTC and show the zone.
- On exam start, check on the server that the student is eligible, inside the window and within attempt limits; return a clear denial reason (DR-ED07).
- Keep timer authority on the server (DR-ED06) and link questions from the question bank.

## Step 39 · FR-I27 – Randomised Exam Sets

- Define a blueprint (number of questions per topic/difficulty) and generate multiple sets from the bank with a random seed.
- Shuffle question and option order per student and store which set was assigned to each attempt for fairness and audit.
- Avoid duplicates and keep difficulty and total marks balanced across sets.

## Step 40 · FR-I26 – Examination Monitoring Rules

- Build a configuration form for tab-switch detection, fullscreen requirement, screen-share requirement and thresholds.
- Save it as the exam's monitoring config, read by the student exam client (FR-S53–S60) after a consent screen (DR-06).
- Show a clear warning where the browser lacks fullscreen support (NFR-25).

## Step 41 · FR-I66 – Question Correction Policy

- Add a per-assessment setting for what happens when a question is invalid: award full marks to all, or drop the question and rescale.
- Save the policy at creation and lock it once attempts start, so it stays uniform (DR-ED05).
- Expose the policy to FR-I37, which applies it.

## Step 49 · FR-I74 – Result Change History

- Build an API/page listing result versions: old value, new value, reason, actor, timestamp, read from Person 1's versioning service (FR-I32).
- Add filters by student and assessment.
- Keep it strictly read-only; history can never be deleted (DR-ED04).

## Step 50 · FR-I33 – Pending Grading Alerts

- Count and list ungraded descriptive submissions per course.
- Notify the instructor when a new submission arrives and send a daily reminder digest.
- Show a pending-count badge on the dashboard.

## Step 51 · FR-I34 – Publish or Withhold Results

- Add a publish/withhold toggle per assessment (quiz, assignment and exam), blocked while ungraded items remain unless the instructor confirms.
- Publishing locks the results; later changes are possible only through versioned corrections (DR-01).
- Publishing makes marks visible and notifies students.

## Step 52 · FR-I35 – Reported-Question Alerts

- Listen for the student's "report question" event (FR-S41) and notify the instructor with a link and the reason.
- Create an item in a reported-questions inbox with a report count per question.
- Start an escalation timer so unresolved reports reach admin (FR-A15).

## Step 53 · FR-I36 – Review Reported Questions

- Show reports per assessment with reason, explanation and status filters.
- Offer actions correct, invalidate or dismiss with a comment, and save the status so students can see it (FR-S43/S44).
- Show how many attempts would be affected before the instructor approves.

## Step 54 · FR-I37 – Apply Corrections to Results

- On approval, find all affected attempts and recompute marks using the policy from FR-I66.
- Save each change through the versioning service (FR-I32) with reason, actor, timestamp, and recompute totals and certificate eligibility.
- Notify students and make the operation idempotent so it can never be applied twice.

## Step 55 · FR-I29 – Review Monitoring Logs

- Build a page listing exam attempts with an event timeline: tab switches, focus-loss time and duration, screen-share stops.
- Filter flagged attempts and show counts, severity and timestamps.
- Restrict access to that course's instructors and admins (NFR-18, DR-05).

## Step 56 · FR-I69 – Flag and Escalate Attempts

- Add a review status (pending, cleared, escalated) and a notes field to each flagged attempt.
- An Escalate button sends a recommendation to admin (FR-A19); a flag alone must never penalise a student (DR-07).
- Keep a history of every review decision.

