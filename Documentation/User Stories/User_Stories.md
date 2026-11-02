# User Stories
> Extracted directly and exactly from `Online_Learning_Platform_User_Stories (1).xlsx` without modifications.

---
## Online Learning Platform – Student User Stories

### US-S01

**Front of Card — User Story:**
> As a student, I want to register and create an account on the portal, so that I can start using it to learn.

**Back of Card — Acceptance Criteria:**
- A student can provide the required registration information and create an account.
- The system confirms successful registration or clearly reports validation errors.
- The new account can subsequently be used for secure login.

* **Source FR(s):** FR-S01
* **Relevant NFR(s):** NFR-13, NFR-14, NFR-20, NFR-30

---

### US-S02

**Front of Card — User Story:**
> As a student, I want to log in, log out, and reset my password, so that I can access my account securely anytime.

**Back of Card — Acceptance Criteria:**
- A registered student can log in with valid credentials.
- The student can log out successfully.
- The student can initiate password reset and complete the configured recovery flow.
- Authentication data is protected using the platform's secure authentication controls.

* **Source FR(s):** FR-S02
* **Relevant NFR(s):** NFR-13, NFR-14, NFR-20

---

### US-S03

**Front of Card — User Story:**
> As a student, I want to view and update my profile information, so that my details stay up to date.

**Back of Card — Acceptance Criteria:**
- The student can view the profile fields available to them.
- The student can edit permitted profile information.
- Saved changes are displayed the next time the profile is opened.
- Unauthorized profile fields or another user's data cannot be modified.

* **Source FR(s):** FR-S03
* **Relevant NFR(s):** NFR-17, NFR-20, NFR-30

---

### US-S04

**Front of Card — User Story:**
> As a student, I want to browse and search for courses, so that I can find ones that interest me.

**Back of Card — Acceptance Criteria:**
- The student can open the course catalog.
- The student can search using course-related text.
- Matching available courses are returned.
- Search results are displayed without exposing unavailable/private course data.

* **Source FR(s):** FR-S05
* **Relevant NFR(s):** NFR-02, NFR-20, NFR-24

---

### US-S05

**Front of Card — User Story:**
> As a student, I want to filter courses by domain, duration, rating, and subject, so that I can quickly find relevant options.

**Back of Card — Acceptance Criteria:**
- The student can apply the listed filters individually or together.
- The results update according to the selected criteria.
- The student can clear or change filters.

* **Source FR(s):** FR-S06
* **Relevant NFR(s):** NFR-02, NFR-20, NFR-24

---

### US-S06

**Front of Card — User Story:**
> As a student, I want to view detailed course information (description, syllabus, instructor, prerequisites, duration, outcomes), so that I can decide if it is the right course for me.

**Back of Card — Acceptance Criteria:**
- The course page shows description, syllabus, instructor, prerequisites, duration, and learning outcomes when configured.
- The information corresponds to the selected course.
- The student can return to the catalog or proceed to enrollment from the course page.

* **Source FR(s):** FR-S07
* **Relevant NFR(s):** NFR-20, NFR-21, NFR-24

---

### US-S07

**Front of Card — User Story:**
> As a student, I want to enroll in a course, so that I can avail and start learning its content.

**Back of Card — Acceptance Criteria:**
- The student can enroll when the course is available and the student is eligible.
- For paid courses, enrollment is confirmed only after successful payment.
- A successful enrollment appears in the student's enrolled-course list.
- Failed payment does not create a ghost/partial enrollment.

* **Source FR(s):** FR-S08
* **Relevant NFR(s):** NFR-12, NFR-22, NFR-23, NFR-17

---

### US-S08

**Front of Card — User Story:**
> As a student, I want to view all my enrolled courses, so that I can keep track of what I'm learning.

**Back of Card — Acceptance Criteria:**
- The student can see all courses currently enrolled in.
- The list reflects successful enrollments only.
- Selecting a course opens its learning area.

* **Source FR(s):** FR-S09
* **Relevant NFR(s):** NFR-17, NFR-22

---

### US-S09

**Front of Card — User Story:**
> As a student, I want to bookmark courses, so that I can come back to them later.

**Back of Card — Acceptance Criteria:**
- A student can bookmark an available course.
- Bookmarked courses remain available in the student's saved list.
- The student can remove a bookmark.

* **Source FR(s):** FR-S10
* **Relevant NFR(s):** NFR-20, NFR-30

---

### US-S10

**Front of Card — User Story:**
> As a student, I want to download and view my earned certificates, so that I can showcase credibility.

**Back of Card — Acceptance Criteria:**
- Only certificates earned by the student are available.
- The student can view the certificate online.
- The student can download the certificate when download is supported.
- Certificate access is restricted to the appropriate student account.
- Note: The source lists this requirement without an FR ID on page 1; FR-S78/FR-S79 cover the related certificate functions.

* **Source FR(s):** FR (certificate download/view requirement on p.1; FR-S78, FR-S79)
* **Relevant NFR(s):** NFR-17, NFR-22, NFR-30
* **Notes / Traceability:** The source lists this requirement without an FR ID on page 1; FR-S78/FR-S79 cover the related certificate functions.

---

### US-S11

**Front of Card — User Story:**
> As a student, I want to access the learning content of my enrolled courses, so that I can study whenever I want.

**Back of Card — Acceptance Criteria:**
- An enrolled student can open permitted course content.
- A student cannot access restricted course content without the required enrollment/permission.
- Available content is organized under the correct course/module.

* **Source FR(s):** FR-S11
* **Relevant NFR(s):** NFR-17, NFR-20, NFR-21

---

### US-S12

**Front of Card — User Story:**
> As a student, I want to watch, pause, resume, and seek through video lectures, so that I can learn at my own pace.

**Back of Card — Acceptance Criteria:**
- The student can play, pause, resume, and seek within an available lecture.
- Playback controls operate without changing the course content.
- Video playback begins within the configured performance target under normal network conditions.

* **Source FR(s):** FR-S12
* **Relevant NFR(s):** NFR-01, NFR-06, NFR-20, NFR-24

---

### US-S13

**Front of Card — User Story:**
> As a student, I want the platform to remember where I stopped a video, so that I can resume without searching again.

**Back of Card — Acceptance Criteria:**
- The system stores the last playback position for a student and lecture.
- Reopening the lecture offers/resumes from the stored position.
- The saved position belongs to the correct student and lecture.

* **Source FR(s):** FR-S13
* **Relevant NFR(s):** NFR-06, NFR-17

---

### US-S14

**Front of Card — User Story:**
> As a student, I want to access and download permitted materials (notes, PDFs, slides), so that I can study offline.

**Back of Card — Acceptance Criteria:**
- The student can open permitted notes, PDFs, presentations, and other resources.
- Download is available only when the instructor has marked the material as downloadable.
- View-only resources cannot be downloaded through the normal interface.

* **Source FR(s):** FR-S14
* **Relevant NFR(s):** FR-I17, NFR-17, NFR-20, NFR-21

---

### US-S15

**Front of Card — User Story:**
> As a student, I want to mark content as completed, so that I can track what I've finished.

**Back of Card — Acceptance Criteria:**
- A student can mark permitted learning content as completed.
- Completed state is saved to the student's course progress.
- The completion state can be displayed when the student revisits the course.

* **Source FR(s):** FR-S15
* **Relevant NFR(s):** NFR-17

---

### US-S16

**Front of Card — User Story:**
> As a student, I want to see my progress in each course, so that I know how much is left.

**Back of Card — Acceptance Criteria:**
- The course displays the student's current progress.
- Progress reflects completed learning content/activities supported by the course.
- The progress view updates after completion changes.

* **Source FR(s):** FR-S16
* **Relevant NFR(s):** NFR-05, NFR-20

---

### US-S17

**Front of Card — User Story:**
> As a student, I want to bookmark important lectures and resources, so that I can find them again easily.

**Back of Card — Acceptance Criteria:**
- The student can bookmark permitted lectures/resources.
- Bookmarks can be viewed later from the student's saved items.
- The student can remove a bookmark.

* **Source FR(s):** FR-S17
* **Relevant NFR(s):** NFR-20

---

### US-S18

**Front of Card — User Story:**
> As a student, I want to search for a keyword within video lectures, so that I can jump straight to the relevant part.

**Back of Card — Acceptance Criteria:**
- The student can enter a permitted topic/keyword for a lecture.
- Matching occurrences are identified when available.
- Selecting a relevant occurrence takes the student to the corresponding lecture portion.

* **Source FR(s):** FR-S18
* **Relevant NFR(s):** NFR-01, NFR-20, NFR-24

---

### US-S19

**Front of Card — User Story:**
> As a student, I want to view and attempt quizzes for my enrolled courses, so that I can check my understanding.

**Back of Card — Acceptance Criteria:**
- Only quizzes associated with the student's enrolled courses are available.
- The student can open an available quiz and begin an attempt.
- Quiz access respects configured availability/attempt rules.

* **Source FR(s):** FR-S18 (Quiz section)
* **Relevant NFR(s):** NFR-17, NFR-22

---

### US-S20

**Front of Card — User Story:**
> As a student, I want to see the quiz's instructions, duration, question count, and marks before starting, so that I know what to expect.

**Back of Card — Acceptance Criteria:**
- The quiz displays instructions, duration, number of questions, and maximum marks before the attempt begins.
- Displayed settings match the quiz configuration.

* **Source FR(s):** FR-S19
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-S21

**Front of Card — User Story:**
> As a student, I want to answer different types of questions (MCQ, multi-select, true/false, numerical, descriptive), so that my learning is tested in varied ways.

**Back of Card — Acceptance Criteria:**
- The quiz supports the question types configured by the instructor.
- The student can enter/select an answer using the appropriate input for the question type.
- The response format is preserved for grading/review.

* **Source FR(s):** FR-S20
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-S22

**Front of Card — User Story:**
> As a student, I want my answer saved when I click "save and next," so that I don't lose my work.

**Back of Card — Acceptance Criteria:**
- Clicking Save and Next stores the current response.
- The next question is displayed after a successful save.
- The saved response remains associated with the correct question and attempt.
- The response is persisted within the configured performance target.

* **Source FR(s):** FR-S21
* **Relevant NFR(s):** NFR-03, NFR-09, NFR-23

---

### US-S23

**Front of Card — User Story:**
> As a student, I want to see the time remaining in a timed quiz, so that I can pace myself.

**Back of Card — Acceptance Criteria:**
- The remaining quiz time is visibly displayed during the attempt.
- The timer counts down according to the configured duration.
- The timer reaches zero at the configured end of the attempt.

* **Source FR(s):** FR-S22
* **Relevant NFR(s):** NFR-20

---

### US-S24

**Front of Card — User Story:**
> As a student, I want to move between questions and submit when I'm ready, so that I control my own attempt.

**Back of Card — Acceptance Criteria:**
- The student can navigate among questions according to quiz settings.
- Previously saved responses remain available when navigating.
- The student can manually submit the attempt.

* **Source FR(s):** FR-S23
* **Relevant NFR(s):** NFR-03, NFR-20

---

### US-S25

**Front of Card — User Story:**
> As a student, I want the quiz to auto-submit when time runs out, so that my attempt isn't lost.

**Back of Card — Acceptance Criteria:**
- When the configured quiz time expires, the system automatically submits the current attempt.
- Responses successfully saved before expiry are included.
- The student receives a clear submission outcome.

* **Source FR(s):** FR-S24
* **Relevant NFR(s):** NFR-03, NFR-09, NFR-23

---

### US-S26

**Front of Card — User Story:**
> As a student, I want to view my marks and results after submitting, so that I know how I did.

**Back of Card — Acceptance Criteria:**
- After the configured release point, the student can view the quiz result.
- Displayed marks correspond to the evaluated submission.
- Results are not shown before release when withholding is configured.

* **Source FR(s):** FR-S25
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S27

**Front of Card — User Story:**
> As a student, I want to review my answers and feedback when enabled, so that I can learn from my mistakes.

**Back of Card — Acceptance Criteria:**
- When review is enabled, the student can see submitted answers.
- Configured feedback is displayed for the relevant questions.
- When disabled, restricted answers/feedback remain hidden.

* **Source FR(s):** FR-S26
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S28

**Front of Card — User Story:**
> As a student, I want to see my upcoming, ongoing, and completed exams, so that I can plan ahead.

**Back of Card — Acceptance Criteria:**
- The student can distinguish upcoming, ongoing, and completed examinations.
- Only examinations relevant to the student are shown.
- The status reflects the configured examination schedule.

* **Source FR(s):** FR-S33
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S29

**Front of Card — User Story:**
> As a student, I want to view exam instructions, schedule, duration, marks, and eligibility, so that I'm well prepared.

**Back of Card — Acceptance Criteria:**
- The exam details page displays the configured instructions, schedule, duration, maximum marks, and eligibility criteria.
- The information corresponds to the selected examination.

* **Source FR(s):** FR-S34
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-S30

**Front of Card — User Story:**
> As a student, I want to start my exam during the permitted window, so that I can take it on time.

**Back of Card — Acceptance Criteria:**
- An eligible student can start only during the permitted examination period.
- Ineligible students are prevented from starting.
- The examination attempt is linked to the correct student and exam.

* **Source FR(s):** FR-S35
* **Relevant NFR(s):** NFR-10, NFR-17

---

### US-S31

**Front of Card — User Story:**
> As a student, I want my exam responses saved automatically, so that I don't lose progress if something goes wrong.

**Back of Card — Acceptance Criteria:**
- Responses are automatically saved during an active examination.
- The latest successfully saved response is retained for the correct attempt.
- Auto-save meets the configured persistence target.

* **Source FR(s):** FR-S36
* **Relevant NFR(s):** NFR-03, NFR-07, NFR-10, NFR-11

---

### US-S32

**Front of Card — User Story:**
> As a student, I want to see the remaining exam time and have it auto-submit at expiry, so that I never miss the deadline.

**Back of Card — Acceptance Criteria:**
- The remaining examination time is shown during the attempt.
- At expiry, the system automatically submits the attempt.
- The auto-submit occurs without timing failure during supported concurrent load.

* **Source FR(s):** FR-S37
* **Relevant NFR(s):** NFR-07, NFR-10, NFR-11

---

### US-S33

**Front of Card — User Story:**
> As a student, I want to submit my exam manually when done, so that I decide when to finish.

**Back of Card — Acceptance Criteria:**
- The student can manually submit an active examination.
- The system confirms the submission outcome.
- After successful submission, the student cannot continue editing that attempt unless the configured policy permits it.

* **Source FR(s):** FR-S38
* **Relevant NFR(s):** NFR-10, NFR-23

---

### US-S34

**Front of Card — User Story:**
> As a student, I want to view my exam results once published, so that I know my performance.

**Back of Card — Acceptance Criteria:**
- Once results are published, the student can open the examination result.
- The displayed result corresponds to the student's attempt.
- Unpublished results remain hidden.

* **Source FR(s):** FR-S39
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S35

**Front of Card — User Story:**
> As a student, I want to see question-wise marks and feedback when permitted, so that I understand where I gained or lost marks.

**Back of Card — Acceptance Criteria:**
- When permitted, question-wise marks are shown.
- Configured instructor feedback is shown alongside the relevant question.
- Restricted details remain hidden when not permitted.

* **Source FR(s):** FR-S40
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S36

**Front of Card — User Story:**
> As a student, I want to report a question that seems ambiguous or incorrect, so that it can be reviewed and fixed.

**Back of Card — Acceptance Criteria:**
- The student can report an eligible quiz/exam question.
- The report is associated with the correct question and assessment attempt.
- The report is submitted successfully or a clear error is shown.

* **Source FR(s):** FR-S41
* **Relevant NFR(s):** NFR-23

---

### US-S37

**Front of Card — User Story:**
> As a student, I want to pick a reason and explain my concern when reporting, so that instructors understand the issue clearly.

**Back of Card — Acceptance Criteria:**
- The reporting flow provides configured reasons.
- The student can enter an explanation.
- The selected reason and explanation are stored with the report.

* **Source FR(s):** FR-S42
* **Relevant NFR(s):** NFR-20

---

### US-S38

**Front of Card — User Story:**
> As a student, I want to check the status of my reports, so that I know if they're being looked at.

**Back of Card — Acceptance Criteria:**
- The student can view submitted reports.
- Each report shows its current status when available.
- A report is associated with the student's account and relevant assessment.

* **Source FR(s):** FR-S43
* **Relevant NFR(s):** NFR-17

---

### US-S39

**Front of Card — User Story:**
> As a student, I want to be notified once my report is resolved, so that I know the outcome.

**Back of Card — Acceptance Criteria:**
- The student receives a notification when the report resolution is published.
- The notification identifies the relevant report/question.
- The resolution status is also viewable in the report history.

* **Source FR(s):** FR-S44
* **Relevant NFR(s):** NFR-20

---

### US-S40

**Front of Card — User Story:**
> As a student, I want approved corrections or question invalidations to be applied to my assessment results, so that my marks accurately reflect a fair evaluation.

**Back of Card — Acceptance Criteria:**
- Approved corrections/invalidations are applied according to the configured assessment policy.
- Affected student results are recalculated/updated when required by policy.
- The updated outcome is reflected in the student's results.

* **Source FR(s):** FR-S45
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-S41

**Front of Card — User Story:**
> As a student, I want to view my marks from quizzes, assignments, and exams, so that I can track my performance.

**Back of Card — Acceptance Criteria:**
- Marks are available for supported assessment types after release.
- The student can distinguish results by assessment.
- Only the student's own results are displayed.

* **Source FR(s):** FR-S46
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S42

**Front of Card — User Story:**
> As a student, I want to see my overall performance and completion percentage, so that I understand my standing in a course.

**Back of Card — Acceptance Criteria:**
- The system calculates/displays overall performance using the configured course rules.
- Course completion percentage is displayed.
- The values update when supported learning/assessment data changes.

* **Source FR(s):** FR-S47
* **Relevant NFR(s):** NFR-05, NFR-20

---

### US-S43

**Front of Card — User Story:**
> As a student, I want to view a history of my past results, so that I can look back at my progress.

**Back of Card — Acceptance Criteria:**
- The student can open a historical list of assessment results.
- Historical entries identify the corresponding course/assessment.
- The history belongs only to the logged-in student.

* **Source FR(s):** FR-S48
* **Relevant NFR(s):** NFR-17, NFR-30

---

### US-S44

**Front of Card — User Story:**
> As a student, I want to see topic-wise performance where available, so that I know my strengths and weak spots.

**Back of Card — Acceptance Criteria:**
- Topic-wise performance is shown where the course/assessment supports it.
- The performance relates to the student's own results.
- The view identifies stronger and weaker topics without exposing another student's data.

* **Source FR(s):** FR-S49
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-S45

**Front of Card — User Story:**
> As a student, I want the system to point out topics I need to practice more, so that I can effectively allocate my time.

**Back of Card — Acceptance Criteria:**
- The system identifies topics requiring additional practice based on supported performance data.
- The identified topics are visible to the student.
- Recommendations are based on the student's permitted learning/assessment data.

* **Source FR(s):** FR-S50
* **Relevant NFR(s):** NFR-04, NFR-17

---

### US-S46

**Front of Card — User Story:**
> As a student, I want to track my performance over time, so that I can see how I'm improving.

**Back of Card — Acceptance Criteria:**
- The student can view performance data across time.
- Historical points/results are associated with the correct student.
- The view remains accessible when historical data is available.

* **Source FR(s):** FR-S51
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-S47

**Front of Card — User Story:**
> As a student, I want a personalized study schedule based on my courses and deadlines, so that I can manage my time better.

**Back of Card — Acceptance Criteria:**
- The schedule uses the student's courses, deadlines, and learning progress when those data are available.
- The resulting schedule is visible to the student.
- Changes in relevant deadlines/progress can be reflected when the feature is recalculated.

* **Source FR(s):** FR-S52
* **Relevant NFR(s):** NFR-04, NFR-17

---

### US-S48

**Front of Card — User Story:**
> As a student, I want my exam activity to be logged, so that my attempt is recorded fairly.

**Back of Card — Acceptance Criteria:**
- A monitored examination maintains an activity log for the attempt.
- Recorded activity is associated with the correct student attempt.
- Access to monitoring logs is restricted to authorized roles.

* **Source FR(s):** FR-S52
* **Relevant NFR(s):** NFR-18, NFR-19, NFR-32

---

### US-S49

**Front of Card — User Story:**
> As a student, I want the system to detect if I leave the exam tab or window, so that the exam stays fair for everyone.

**Back of Card — Acceptance Criteria:**
- Focus-loss/tab-change events are detected during monitored examinations.
- The event is recorded against the correct attempt.
- Configured rules determine whether the event triggers a notification/flag.

* **Source FR(s):** FR-S53
* **Relevant NFR(s):** NFR-18, NFR-32

---

### US-S50

**Front of Card — User Story:**
> As a student, I want any focus-loss events to be timestamped accurately, so that flags on my exam are based on correct evidence.

**Back of Card — Acceptance Criteria:**
- Each detected focus-loss/tab-change event records its timestamp.
- The event duration is recorded where applicable.
- The record is linked to the correct examination attempt.

* **Source FR(s):** FR-S54
* **Relevant NFR(s):** NFR-18, NFR-32

---

### US-S51

**Front of Card — User Story:**
> As a student, I want to be notified if a monitoring rule is triggered during my exam, so that I'm aware something was flagged.

**Back of Card — Acceptance Criteria:**
- The student receives a notification when a configured monitoring rule is triggered.
- The notification identifies the relevant rule/event at an understandable level.
- The monitoring process continues according to the configured policy.

* **Source FR(s):** FR-S55
* **Relevant NFR(s):** NFR-18, NFR-23, NFR-32

---

### US-S52

**Front of Card — User Story:**
> As a student, I want questions and options to appear in a random order, so that the exam stays secure and fair.

**Back of Card — Acceptance Criteria:**
- Configured examinations can randomize question order.
- Configured questions' answer options can also be randomized where enabled.
- The generated order remains consistent for the student's attempt.

* **Source FR(s):** FR-S56
* **Relevant NFR(s):** NFR-07, NFR-17

---

### US-S53

**Front of Card — User Story:**
> As a student, I want to receive a properly generated exam set from the question bank, so that my paper is fair compared to others.

**Back of Card — Acceptance Criteria:**
- The system can generate an exam set from the configured question bank.
- The generated set respects the examination configuration.
- The set is associated with the student's attempt.

* **Source FR(s):** FR-S57
* **Relevant NFR(s):** NFR-07, NFR-17

---

### US-S54

**Front of Card — User Story:**
> As a student, I want to take my exam in fullscreen mode, so that I stay focused without distraction.

**Back of Card — Acceptance Criteria:**
- Fullscreen mode can be requested for supported browsers.
- The exam displays an understandable warning when fullscreen is unsupported.
- The exam remains usable according to the fallback behavior.

* **Source FR(s):** FR-S58
* **Relevant NFR(s):** NFR-25, NFR-24, NFR-20

---

### US-S55

**Front of Card — User Story:**
> As a student, I want to be asked for permission before screen sharing starts, so that my privacy is respected.

**Back of Card — Acceptance Criteria:**
- The system explicitly requests permission before screen sharing starts.
- Screen sharing does not start when the student denies permission.
- The consent state is handled according to the monitoring flow.

* **Source FR(s):** FR-S59
* **Relevant NFR(s):** NFR-18, NFR-32

---

### US-S56

**Front of Card — User Story:**
> As a student, I want screen-sharing events to be recorded accurately, so that my session is properly documented.

**Back of Card — Acceptance Criteria:**
- The start of screen sharing is recorded.
- The stop/end of screen sharing is recorded.
- Unexpected termination is recorded where detected.
- The events are linked to the correct attempt and access-restricted.

* **Source FR(s):** FR-S60
* **Relevant NFR(s):** NFR-18, NFR-32

---

### US-S57

**Front of Card — User Story:**
> As a student, I want monitoring events linked to my correct exam attempt, so that the records reflect my actual session.

**Back of Card — Acceptance Criteria:**
- Each monitoring event references the corresponding examination attempt.
- Events cannot be assigned to a different student's attempt.
- Authorized staff can trace the events back to the correct attempt.

* **Source FR(s):** FR-S61
* **Relevant NFR(s):** NFR-17, NFR-18, NFR-19

---

### US-S58

**Front of Card — User Story:**
> As a student, I want to view course announcements, so that I stay updated on important information.

**Back of Card — Acceptance Criteria:**
- The student can view announcements for enrolled courses.
- Announcements are associated with the correct course.
- Students cannot edit instructor/admin announcements.

* **Source FR(s):** FR-S62
* **Relevant NFR(s):** NFR-20, NFR-17

---

### US-S59

**Front of Card — User Story:**
> As a student, I want to ask questions related to course content, so that I can clear my doubts.

**Back of Card — Acceptance Criteria:**
- The student can submit a course-related question.
- The question is associated with the correct course.
- The student can view the submitted question and response when available.

* **Source FR(s):** FR-S63
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-S60

**Front of Card — User Story:**
> As a student, I want to take part in course discussions and see instructor replies, so that I can engage more with the material.

**Back of Card — Acceptance Criteria:**
- The student can create a permitted discussion post/reply.
- The student can view instructor responses.
- The discussion is limited to the relevant course context and permissions.

* **Source FR(s):** FR-S64
* **Relevant NFR(s):** NFR-17, NFR-20, NFR-21

---

### US-S61

**Front of Card — User Story:**
> As a student, I want to be notified about deadlines, new content, exams, and announcements, so that I never miss anything important.

**Back of Card — Acceptance Criteria:**
- Notifications are generated for configured course events.
- The student can view the notification.
- Notification content identifies the relevant event and course where applicable.

* **Source FR(s):** FR-S65
* **Relevant NFR(s):** NFR-20, NFR-23

---

### US-S62

**Front of Card — User Story:**
> As a student, I want to earn points for learning activities, so that I stay motivated.

**Back of Card — Acceptance Criteria:**
- Configured learning activities award the configured number of points.
- Awarded points are associated with the correct student.
- The updated points total is visible when supported.

* **Source FR(s):** FR-S66
* **Relevant NFR(s):** NFR-17

---

### US-S63

**Front of Card — User Story:**
> As a student, I want my learning streaks tracked, so that I stay consistent with my studies.

**Back of Card — Acceptance Criteria:**
- The system tracks the student's configured streak activity.
- The current streak is updated when qualifying activity occurs.
- The streak is associated only with the student's account.

* **Source FR(s):** FR-S67
* **Relevant NFR(s):** NFR-17

---

### US-S64

**Front of Card — User Story:**
> As a student, I want to earn badges for achievements, so that my milestones feel recognized.

**Back of Card — Acceptance Criteria:**
- The system awards configured badges when their achievement conditions are met.
- The earned badge is associated with the student.
- The badge becomes visible in the student's achievements view.

* **Source FR(s):** FR-S68
* **Relevant NFR(s):** NFR-17

---

### US-S65

**Front of Card — User Story:**
> As a student, I want to view my achievements, badges, and points, so that I can see my progress at a glance.

**Back of Card — Acceptance Criteria:**
- The student can view earned achievements, badges, and accumulated points.
- The information reflects the student's current gamification state.
- Only the student's permitted achievement data is shown.

* **Source FR(s):** FR-S69
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S66

**Front of Card — User Story:**
> As a student, I want access to leaderboards and challenges, so that I feel more engaged with my learning.

**Back of Card — Acceptance Criteria:**
- Leaderboards/challenges are visible when enabled for the course.
- The student can view the supported leaderboard/challenge information.
- When disabled, these features are not presented as active.

* **Source FR(s):** FR-S70
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-S67

**Front of Card — User Story:**
> As a student, I want an AI learning assistant, so that I can get quick help with course-related queries.

**Back of Card — Acceptance Criteria:**
- The student can open the AI learning assistant.
- The assistant accepts course-related queries supported by the platform.
- A response is returned for standard queries within the configured target when service is available.

* **Source FR(s):** FR-S71
* **Relevant NFR(s):** NFR-04, NFR-20

---

### US-S68

**Front of Card — User Story:**
> As a student, I want to ask the AI assistant about permitted course content, so that I can clarify doubts anytime.

**Back of Card — Acceptance Criteria:**
- The student can submit a question about permitted course content.
- The assistant responds using permitted content/data only.
- The response is associated with the current learning context where supported.

* **Source FR(s):** FR-S72
* **Relevant NFR(s):** NFR-04, NFR-17, NFR-30

---

### US-S69

**Front of Card — User Story:**
> As a student, I want the AI to summarize videos and materials, so that I can review content faster.

**Back of Card — Acceptance Criteria:**
- The student can request a summary for a permitted video/material.
- The summary is generated from the selected permitted content.
- The summary is returned in a usable format within the configured standard-query target where applicable.

* **Source FR(s):** FR-S73
* **Relevant NFR(s):** NFR-04, NFR-21

---

### US-S70

**Front of Card — User Story:**
> As a student, I want explanations for selected assessment questions, so that I can understand the correct approach.

**Back of Card — Acceptance Criteria:**
- Explanations are available only for assessment questions configured for explanation.
- The student can request/view the explanation after the appropriate access point.
- Restricted assessment content remains protected.

* **Source FR(s):** FR-S74
* **Relevant NFR(s):** NFR-04, NFR-17

---

### US-S71

**Front of Card — User Story:**
> As a student, I want the AI to identify my weak topics, so that I know where to focus my efforts.

**Back of Card — Acceptance Criteria:**
- The system analyzes supported learning/assessment performance.
- Weak topics are shown to the student.
- The analysis uses only the student's permitted data.

* **Source FR(s):** FR-S75
* **Relevant NFR(s):** NFR-04, NFR-17

---

### US-S72

**Front of Card — User Story:**
> As a student, I want the AI to recommend resources and practice questions, so that I can improve more efficiently.

**Back of Card — Acceptance Criteria:**
- Recommendations are based on supported learning activity/performance.
- Recommended resources/questions are relevant to identified learning needs.
- The student can open or access the recommended permitted items.

* **Source FR(s):** FR-S76
* **Relevant NFR(s):** NFR-04, NFR-17, NFR-20

---

### US-S73

**Front of Card — User Story:**
> As a student, I want the system to check if I've met a course's completion criteria, so that I know when I'm eligible for a certificate.

**Back of Card — Acceptance Criteria:**
- The system evaluates the student's progress against the configured completion criteria.
- The student can see whether the completion requirements are satisfied.
- Eligibility status updates when relevant course requirements change.

* **Source FR(s):** FR-S77
* **Relevant NFR(s):** NFR-17

---

### US-S74

**Front of Card — User Story:**
> As a student, I want to view and obtain my course completion certificate, so that I can prove my achievement.

**Back of Card — Acceptance Criteria:**
- An eligible student can view the course completion certificate.
- The certificate corresponds to the correct course and student.
- The student can obtain/download the certificate when supported.

* **Source FR(s):** FR-S78
* **Relevant NFR(s):** NFR-22, NFR-30

---

### US-S75

**Front of Card — User Story:**
> As a student, I want a record of all certificates I've earned, so that I can access them anytime I need.

**Back of Card — Acceptance Criteria:**
- The system maintains a certificate history for the student.
- The student can open certificates from the history.
- The history is restricted to the student's own certificates.

* **Source FR(s):** FR-S79
* **Relevant NFR(s):** NFR-17, NFR-30

---

### US-SWQ

**Front of Card — User Story:**
> As a student, I want to take weekly graded quizzes, so that I can regularly assess my learning and progress.

**Back of Card — Acceptance Criteria:**
- A weekly graded quiz can be made available to the relevant students.
- The quiz follows configured questions, marks, timing, submission, and result rules.
- The student's graded result is recorded with the appropriate assessment history.

* **Source FR(s):** Source note on p.1: "Weekly graded quiz" (no FR ID provided)
* **Relevant NFR(s):** NFR-03, NFR-17, NFR-23
* **Notes / Traceability:** Included because the FR document explicitly contains this note, but it does not assign an FR identifier. Assign an official FR ID in the requirements document if required by your submission.

---

## Online Learning Platform – Instructor User Stories

### US-I01

**Front of Card — User Story:**
> As a course instructor, I want to register and create an account on the portal, subject to admin verification/approval, so that I can become an authorized instructor.

**Back of Card — Acceptance Criteria:**
- The instructor can submit registration information.
- The registration remains pending until required admin verification/approval is completed.
- Unapproved instructors do not receive course-creation access.

* **Source FR(s):** FR-I01
* **Relevant NFR(s):** NFR-13, NFR-14, NFR-17

---

### US-I02

**Front of Card — User Story:**
> As a course instructor, I want to log in, log out, and reset my password, so that I can securely access and leave my instructor account.

**Back of Card — Acceptance Criteria:**
- Valid credentials allow login.
- The instructor can log out.
- Password reset follows the configured recovery process.

* **Source FR(s):** FR-I02
* **Relevant NFR(s):** NFR-13, NFR-14

---

### US-I03

**Front of Card — User Story:**
> As a course instructor, I want to view and update my profile, including qualifications, expertise areas, and bio, so that my professional information stays current.

**Back of Card — Acceptance Criteria:**
- The instructor can view permitted profile fields.
- Qualifications, expertise areas, and bio can be updated.
- Saved data is shown on subsequent profile views.

* **Source FR(s):** FR-I03
* **Relevant NFR(s):** NFR-17, NFR-30

---

### US-I04

**Front of Card — User Story:**
> As a course instructor, I want to upload and update my profile picture and supporting credentials/documents, so that my instructor profile can be verified and presented accurately.

**Back of Card — Acceptance Criteria:**
- The instructor can upload supported profile/credential files.
- Existing supported files can be updated.
- Uploaded documents are access-restricted appropriately.

* **Source FR(s):** FR-I04
* **Relevant NFR(s):** NFR-17, NFR-20, NFR-30

---

### US-I05

**Front of Card — User Story:**
> As a course instructor, I want to create a course with title, description, category, prerequisites, and learning outcomes, so that I can define the course before publishing.

**Back of Card — Acceptance Criteria:**
- All required course fields can be entered.
- The course can be saved with the entered metadata.
- The course is associated with the instructor.

* **Source FR(s):** FR-I05
* **Relevant NFR(s):** NFR-17

---

### US-I06

**Front of Card — User Story:**
> As a course instructor, I want to organize course content into modules, sections, and lessons, so that students can follow a clear learning structure.

**Back of Card — Acceptance Criteria:**
- The instructor can create/edit the defined hierarchy.
- Lessons can be associated with the appropriate section/module.
- The structure is reflected in the published course.

* **Source FR(s):** FR-I06
* **Relevant NFR(s):** NFR-20

---

### US-I07

**Front of Card — User Story:**
> As a course instructor, I want to save a course as a draft before publishing, so that I can continue preparing it before making it public.

**Back of Card — Acceptance Criteria:**
- The course can be saved in draft status.
- Draft content remains editable by authorized instructors.
- Draft courses are not treated as published courses.

* **Source FR(s):** FR-I07
* **Relevant NFR(s):** NFR-17

---

### US-I08

**Front of Card — User Story:**
> As a course instructor, I want to submit a course for admin review and approval before publishing, so that the course can enter the platform approval workflow.

**Back of Card — Acceptance Criteria:**
- A completed/draft course can be submitted for review.
- Its status changes to the configured review state.
- Admin can see the submitted course for approval.

* **Source FR(s):** FR-I08
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I09

**Front of Card — User Story:**
> As a course instructor, I want to edit, update, or unpublish an existing course, so that I can keep course information current or temporarily remove it from learners.

**Back of Card — Acceptance Criteria:**
- An authorized instructor can edit the course.
- Changes can be saved.
- The instructor can unpublish the course when permitted.
- Unauthorized users cannot modify it.

* **Source FR(s):** FR-I09
* **Relevant NFR(s):** NFR-17

---

### US-I10

**Front of Card — User Story:**
> As a course instructor, I want to set course pricing, discounts, and enrollment limits where applicable, so that I can control the commercial and capacity settings of my course.

**Back of Card — Acceptance Criteria:**
- Supported pricing/discount fields can be configured.
- Enrollment limits can be configured when applicable.
- The configured values are used by the enrollment/purchase flow.

* **Source FR(s):** FR-I10
* **Relevant NFR(s):** NFR-12, NFR-17

---

### US-I11

**Front of Card — User Story:**
> As a course instructor, I want to define prerequisites for a course, so that students understand and the platform can enforce required prior knowledge.

**Back of Card — Acceptance Criteria:**
- Prerequisites can be entered for the course.
- The configured prerequisite information is visible where appropriate.
- Enrollment eligibility can use the configured prerequisite rule when supported.

* **Source FR(s):** FR-I11
* **Relevant NFR(s):** NFR-17

---

### US-I12

**Front of Card — User Story:**
> As a course instructor, I want to archive or delete a course, subject to platform policy, so that I can retire course content that is no longer needed.

**Back of Card — Acceptance Criteria:**
- An authorized instructor can archive/delete when policy allows.
- Archived/deleted state follows platform policy.
- The action does not expose or orphan unauthorized content.

* **Source FR(s):** FR-I12
* **Relevant NFR(s):** NFR-19, NFR-34

---

### US-I13

**Front of Card — User Story:**
> As a course instructor, I want to associate myself with a course available on the platform, subject to admin approval where required, so that I can teach courses assigned to me.

**Back of Card — Acceptance Criteria:**
- The instructor can request/select an available course.
- Where approval is required, the association remains pending until approved.
- Approved associations appear in the instructor's course list.

* **Source FR(s):** FR-I58
* **Relevant NFR(s):** NFR-17

---

### US-I14

**Front of Card — User Story:**
> As a course instructor, I want to associate multiple instructors with the same course, so that co-instructors can jointly manage the course when permitted.

**Back of Card — Acceptance Criteria:**
- A course can have more than one associated instructor.
- Each approved instructor can see the course in their associated-course list.
- Access remains limited to authorized course roles.

* **Source FR(s):** FR-I59
* **Relevant NFR(s):** NFR-17

---

### US-I15

**Front of Card — User Story:**
> As a course instructor, I want to view all courses I am currently associated with, so that I can quickly access the courses I teach.

**Back of Card — Acceptance Criteria:**
- The instructor can see the list of associated courses.
- The list excludes courses they are not associated with.
- Selecting a course opens its permitted management area.

* **Source FR(s):** FR-I60
* **Relevant NFR(s):** NFR-17

---

### US-I16

**Front of Card — User Story:**
> As a course instructor, I want to upload video lectures for my courses, so that students can learn from video content.

**Back of Card — Acceptance Criteria:**
- Video can be uploaded for an associated course.
- Unsupported/failed uploads are reported clearly.
- The uploaded video is associated with the selected course.

* **Source FR(s):** FR-I13
* **Relevant NFR(s):** NFR-01, NFR-06, NFR-26, NFR-29

---

### US-I17

**Front of Card — User Story:**
> As a course instructor, I want to upload supplementary learning materials such as notes, PDFs, presentations, and other resources, so that students can access supporting material.

**Back of Card — Acceptance Criteria:**
- Supported materials can be uploaded.
- The material is associated with the selected course.
- Access/download behavior follows the configured permissions.

* **Source FR(s):** FR-I14
* **Relevant NFR(s):** NFR-20, NFR-29

---

### US-I18

**Front of Card — User Story:**
> As a course instructor, I want to see the processing/transcoding status of uploaded video content, so that I know when a video is ready to use.

**Back of Card — Acceptance Criteria:**
- The status of uploaded video processing is visible.
- The instructor can distinguish processing from successful/failed states.
- A failed processing state is communicated clearly.

* **Source FR(s):** FR-I15
* **Relevant NFR(s):** NFR-01, NFR-26, NFR-23

---

### US-I19

**Front of Card — User Story:**
> As a course instructor, I want to reorder, replace, or remove uploaded content within a course, so that I can maintain the course content structure.

**Back of Card — Acceptance Criteria:**
- Authorized course content can be reordered.
- Existing supported content can be replaced.
- Content can be removed when permitted.
- The updated structure is reflected to authorized viewers.

* **Source FR(s):** FR-I16
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I20

**Front of Card — User Story:**
> As a course instructor, I want to mark specific learning materials as downloadable or view-only, so that I can control how students access each resource.

**Back of Card — Acceptance Criteria:**
- The instructor can choose downloadable or view-only for supported material.
- Student access follows the selected setting.
- Unauthorized users cannot change the setting.

* **Source FR(s):** FR-I17
* **Relevant NFR(s):** NFR-17

---

### US-I21

**Front of Card — User Story:**
> As a course instructor, I want to schedule the release date/time of specific course content, so that I can release learning material according to my teaching plan.

**Back of Card — Acceptance Criteria:**
- A release date/time can be configured.
- Content remains unavailable before its configured release point where applicable.
- The content becomes available according to the schedule.

* **Source FR(s):** FR-I18
* **Relevant NFR(s):** NFR-20, NFR-24

---

### US-I22

**Front of Card — User Story:**
> As a course instructor, I want to upload video content only to courses I am associated with, so that course ownership and content access remain secure.

**Back of Card — Acceptance Criteria:**
- The upload target list contains only associated courses.
- A direct attempt to upload to an unauthorized course is blocked.
- The system enforces the association server-side.

* **Source FR(s):** FR-I61
* **Relevant NFR(s):** NFR-17

---

### US-I23

**Front of Card — User Story:**
> As a course instructor, I want to let students stream the video content I upload for their enrolled course, so that students can learn from my published video lectures.

**Back of Card — Acceptance Criteria:**
- Students enrolled in the relevant course can stream published videos.
- A non-enrolled student cannot access restricted course video.
- Video playback uses the supported streaming mechanism.

* **Source FR(s):** FR-I62
* **Relevant NFR(s):** NFR-01, NFR-06, NFR-17, NFR-26

---

### US-I24

**Front of Card — User Story:**
> As a course instructor, I want to create quizzes and associate them with specific courses or modules, so that I can assess students in the relevant learning context.

**Back of Card — Acceptance Criteria:**
- A quiz can be created.
- The quiz can be linked to a course/module.
- Only the intended course audience can access it when published.

* **Source FR(s):** FR-I19
* **Relevant NFR(s):** NFR-17

---

### US-I25

**Front of Card — User Story:**
> As a course instructor, I want to configure quiz duration, question count, maximum marks, and number of attempts, so that I can control how the quiz is administered.

**Back of Card — Acceptance Criteria:**
- Each supported setting can be configured.
- The configuration is shown/used by the student attempt.
- Invalid configurations are prevented or clearly reported.

* **Source FR(s):** FR-I20
* **Relevant NFR(s):** NFR-03, NFR-20

---

### US-I26

**Front of Card — User Story:**
> As a course instructor, I want to add multiple-choice, multiple-select, true/false, numerical, and descriptive quiz questions, so that I can assess students using varied question formats.

**Back of Card — Acceptance Criteria:**
- Supported question types can be added.
- Each question stores the appropriate response format.
- Students receive the configured question type during the quiz.

* **Source FR(s):** FR-I21
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-I27

**Front of Card — User Story:**
> As a course instructor, I want to define correct answers and marking schemes for objective questions, so that objective questions can be graded consistently.

**Back of Card — Acceptance Criteria:**
- Correct answers can be configured.
- The marking scheme is saved with the question/quiz.
- Released results use the configured scheme.

* **Source FR(s):** FR-I22
* **Relevant NFR(s):** NFR-17

---

### US-I28

**Front of Card — User Story:**
> As a course instructor, I want to enable or disable answer review and feedback visibility after quiz submission, so that I can control what students can review after an attempt.

**Back of Card — Acceptance Criteria:**
- The review/feedback setting can be enabled or disabled.
- Student visibility follows the configured setting.
- The setting applies to the intended quiz.

* **Source FR(s):** FR-I23
* **Relevant NFR(s):** NFR-17

---

### US-I29

**Front of Card — User Story:**
> As a course instructor, I want to maintain a reusable question bank for quizzes and examinations, so that I can reuse quality-assured questions efficiently.

**Back of Card — Acceptance Criteria:**
- Questions can be added to the bank.
- Stored questions can be reused in supported assessments.
- Only authorized instructors can modify their permitted question bank.

* **Source FR(s):** FR-I24
* **Relevant NFR(s):** NFR-17, NFR-28

---

### US-I30

**Front of Card — User Story:**
> As a course instructor, I want to create and schedule examinations with start time, duration, and eligibility criteria, so that I can conduct assessments at the intended time for eligible students.

**Back of Card — Acceptance Criteria:**
- An examination can be created with start time and duration.
- Eligibility criteria can be configured.
- The schedule is available to relevant students.

* **Source FR(s):** FR-I25
* **Relevant NFR(s):** NFR-07, NFR-10, NFR-17

---

### US-I31

**Front of Card — User Story:**
> As a course instructor, I want to configure examination-monitoring rules such as tab-switch detection, fullscreen mode, and screen-sharing requirements, so that I can apply the required monitoring policy for an examination.

**Back of Card — Acceptance Criteria:**
- Supported monitoring rules can be configured.
- The rules are applied to student attempts of the intended examination.
- Student consent is requested where required for screen sharing.

* **Source FR(s):** FR-I26
* **Relevant NFR(s):** NFR-07, NFR-18, NFR-25, NFR-32

---

### US-I32

**Front of Card — User Story:**
> As a course instructor, I want to generate randomized examination sets from a configured question bank, so that students can receive controlled but varied examination sets.

**Back of Card — Acceptance Criteria:**
- The system can generate an exam set from the configured bank.
- The generated set follows configured rules.
- The generated attempt is associated with the correct exam/student.

* **Source FR(s):** FR-I27
* **Relevant NFR(s):** NFR-07, NFR-17

---

### US-I33

**Front of Card — User Story:**
> As a course instructor, I want to publish examination results after evaluation, so that students can access their results at the intended release point.

**Back of Card — Acceptance Criteria:**
- Results can be published after evaluation.
- Before publication, results remain withheld when configured.
- Published results become available to eligible students.

* **Source FR(s):** FR-I28
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I34

**Front of Card — User Story:**
> As a course instructor, I want to review examination-monitoring logs and flagged activity for my examinations, so that I can investigate flagged attempts using authorized evidence.

**Back of Card — Acceptance Criteria:**
- The instructor can view monitoring logs for their examinations.
- Flagged activity can be identified.
- Access is restricted to authorized instructors/admins.

* **Source FR(s):** FR-I29
* **Relevant NFR(s):** NFR-18, NFR-19, NFR-32

---

### US-I35

**Front of Card — User Story:**
> As a course instructor, I want to view and grade student submissions for descriptive/subjective questions, so that I can complete manual evaluation where automated grading is insufficient.

**Back of Card — Acceptance Criteria:**
- Pending descriptive submissions are visible.
- The instructor can enter a grade.
- The grade is saved against the correct student/assessment.

* **Source FR(s):** FR-I30
* **Relevant NFR(s):** NFR-17

---

### US-I36

**Front of Card — User Story:**
> As a course instructor, I want to provide feedback and comments on graded submissions, so that students can understand their evaluation.

**Back of Card — Acceptance Criteria:**
- The instructor can add feedback/comments to a graded submission.
- Saved feedback is linked to the correct student and assessment.
- Students can view it according to release settings.

* **Source FR(s):** FR-I31
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I37

**Front of Card — User Story:**
> As a course instructor, I want to override an auto-generated grade when necessary, with a recorded justification, so that I can correct an automated result while preserving accountability.

**Back of Card — Acceptance Criteria:**
- An authorized instructor can override an auto-generated grade.
- A justification is required/recorded.
- The final grade reflects the authorized override.

* **Source FR(s):** FR-I32
* **Relevant NFR(s):** NFR-19

---

### US-I38

**Front of Card — User Story:**
> As a course instructor, I want to receive notifications about pending ungraded submissions, so that I can complete grading on time.

**Back of Card — Acceptance Criteria:**
- Pending ungraded work can be identified.
- A notification is generated according to the platform's notification rules.
- The notification points to the relevant assessment/submissions.

* **Source FR(s):** FR-I33
* **Relevant NFR(s):** NFR-20

---

### US-I39

**Front of Card — User Story:**
> As a course instructor, I want to publish or withhold assessment results according to configured release settings, so that I can control when students see results.

**Back of Card — Acceptance Criteria:**
- Release settings can be applied to supported assessments.
- Results remain hidden while withheld.
- Published results become visible to eligible students.

* **Source FR(s):** FR-I34
* **Relevant NFR(s):** NFR-17

---

### US-I40

**Front of Card — User Story:**
> As a course instructor, I want to receive notifications when a student reports an ambiguous, incorrect, or problematic question, so that I can respond to issues raised by students.

**Back of Card — Acceptance Criteria:**
- A submitted report reaches the relevant instructor.
- The notification identifies the question/assessment.
- The report remains traceable to the student's report.

* **Source FR(s):** FR-I35
* **Relevant NFR(s):** NFR-20

---

### US-I41

**Front of Card — User Story:**
> As a course instructor, I want to review reported questions and correct, invalidate, or dismiss the report, so that I can resolve question-quality issues.

**Back of Card — Acceptance Criteria:**
- Reported questions are reviewable.
- The instructor can select a supported resolution action.
- The resolution is recorded and linked to the report.

* **Source FR(s):** FR-I36
* **Relevant NFR(s):** NFR-19, NFR-30

---

### US-I42

**Front of Card — User Story:**
> As a course instructor, I want to apply approved corrections or invalidations to affected student results, so that student results reflect the approved resolution.

**Back of Card — Acceptance Criteria:**
- Approved changes can be applied to affected results.
- Only authorized corrections are applied.
- Updated results are stored and become visible according to release rules.

* **Source FR(s):** FR-I37
* **Relevant NFR(s):** NFR-19

---

### US-I43

**Front of Card — User Story:**
> As a course instructor, I want to notify students about the resolution status of their reported questions, so that students know what happened to their reports.

**Back of Card — Acceptance Criteria:**
- The report resolution status is communicated to the reporting student.
- The message identifies the relevant report/question.
- The updated status is visible in the report history.

* **Source FR(s):** FR-I38
* **Relevant NFR(s):** NFR-20

---

### US-I44

**Front of Card — User Story:**
> As a course instructor, I want to post announcements for my enrolled students, so that I can communicate important course information.

**Back of Card — Acceptance Criteria:**
- An instructor can create an announcement for their course.
- Only the intended enrolled audience receives/see it.
- The announcement is associated with the correct course.

* **Source FR(s):** FR-I39
* **Relevant NFR(s):** NFR-20, NFR-17

---

### US-I45

**Front of Card — User Story:**
> As a course instructor, I want to respond to student questions and participate in course discussion forums, so that I can support students academically.

**Back of Card — Acceptance Criteria:**
- The instructor can view relevant student questions/posts.
- The instructor can reply.
- Replies appear in the appropriate course discussion.

* **Source FR(s):** FR-I40
* **Relevant NFR(s):** NFR-20, NFR-21

---

### US-I46

**Front of Card — User Story:**
> As a course instructor, I want to send notifications about deadlines, newly available content, and examinations, so that students stay informed about important course events.

**Back of Card — Acceptance Criteria:**
- The instructor can trigger/send configured course notifications.
- Notifications identify the relevant event.
- Only the intended audience receives them.

* **Source FR(s):** FR-I41
* **Relevant NFR(s):** NFR-20

---

### US-I47

**Front of Card — User Story:**
> As a course instructor, I want to view and moderate discussion posts within my own courses, so that I can maintain a constructive course discussion space.

**Back of Card — Acceptance Criteria:**
- The instructor can review posts in their courses.
- The instructor can perform the supported moderation action.
- The instructor cannot moderate unrelated courses without permission.

* **Source FR(s):** FR-I42
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I48

**Front of Card — User Story:**
> As a course instructor, I want to view enrollment statistics for my courses, so that I can understand course participation.

**Back of Card — Acceptance Criteria:**
- Enrollment counts/statistics are available for associated courses.
- The data correspond to the selected course.
- Unauthorized course data are not shown.

* **Source FR(s):** FR-I43
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-I49

**Front of Card — User Story:**
> As a course instructor, I want to view aggregate and individual student performance in quizzes and examinations, so that I can assess student outcomes and identify learning needs.

**Back of Card — Acceptance Criteria:**
- Aggregate performance is available.
- Individual performance is available to authorized instructors.
- The data are associated with the correct course/assessment.

* **Source FR(s):** FR-I44
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-I50

**Front of Card — User Story:**
> As a course instructor, I want to view course completion rates and engagement metrics, so that I can understand how learners are progressing through the course.

**Back of Card — Acceptance Criteria:**
- Completion rates are displayed.
- Supported engagement metrics are displayed.
- The dashboard reflects the selected course.

* **Source FR(s):** FR-I45
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-I51

**Front of Card — User Story:**
> As a course instructor, I want to identify commonly missed topics or questions across my student cohort, so that I can focus teaching on common learning difficulties.

**Back of Card — Acceptance Criteria:**
- The system identifies commonly missed topics/questions from supported performance data.
- The insights are shown for the instructor's permitted courses.
- No unauthorized student data is exposed.

* **Source FR(s):** FR-I46
* **Relevant NFR(s):** NFR-05, NFR-17, NFR-30

---

### US-I52

**Front of Card — User Story:**
> As a course instructor, I want to export student performance reports, so that I can analyze or share course performance data in the supported format.

**Back of Card — Acceptance Criteria:**
- The instructor can generate/export a supported performance report.
- The report contains the selected course/student performance data.
- Export access is restricted to authorized users.

* **Source FR(s):** FR-I47
* **Relevant NFR(s):** NFR-05, NFR-17, NFR-30

---

### US-I53

**Front of Card — User Story:**
> As a course instructor, I want to view my earnings summary based on enrollments and course sales, so that I can understand my course-related earnings.

**Back of Card — Acceptance Criteria:**
- An earnings summary is available.
- The summary reflects the instructor's eligible sales/enrollments.
- The instructor cannot view another instructor's earnings.

* **Source FR(s):** FR-I48
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-I54

**Front of Card — User Story:**
> As a course instructor, I want to view my payout history and status, so that I can track money paid or pending to me.

**Back of Card — Acceptance Criteria:**
- Payout history is displayed.
- Each payout shows its current status where supported.
- The history belongs to the logged-in instructor.

* **Source FR(s):** FR-I49
* **Relevant NFR(s):** NFR-17, NFR-19, NFR-33

---

### US-I55

**Front of Card — User Story:**
> As a course instructor, I want to configure or update my payout account details, so that my future payouts can be routed to the correct account.

**Back of Card — Acceptance Criteria:**
- The instructor can enter/update supported payout account details.
- Sensitive account information is protected.
- Changes are saved to the correct instructor account.

* **Source FR(s):** FR-I50
* **Relevant NFR(s):** NFR-15, NFR-17, NFR-19

---

### US-I56

**Front of Card — User Story:**
> As a course instructor, I want to configure gamification rules such as points, badges, and streak criteria for my courses, so that I can tailor engagement rules to the course.

**Back of Card — Acceptance Criteria:**
- Supported points, badge, and streak rules can be configured.
- Rules apply to the intended course.
- Only authorized instructors can modify course rules.

* **Source FR(s):** FR-I51
* **Relevant NFR(s):** NFR-17

---

### US-I57

**Front of Card — User Story:**
> As a course instructor, I want to enable or disable leaderboards and learning challenges for my courses, so that I can control whether these engagement features are used.

**Back of Card — Acceptance Criteria:**
- Leaderboards/challenges can be enabled or disabled.
- The setting affects the intended course.
- Students see the feature only when enabled.

* **Source FR(s):** FR-I52
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-I58

**Front of Card — User Story:**
> As a course instructor, I want to receive AI-generated suggestions for quiz questions based on course content, so that I can accelerate quiz authoring while using my course material.

**Back of Card — Acceptance Criteria:**
- The instructor can request AI-generated question suggestions from permitted course content.
- Suggestions are clearly presented as generated suggestions.
- The instructor can review them before use.

* **Source FR(s):** FR-I53
* **Relevant NFR(s):** NFR-04, NFR-29

---

### US-I59

**Front of Card — User Story:**
> As a course instructor, I want to receive AI-generated summaries of student performance trends, so that I can quickly understand important performance patterns.

**Back of Card — Acceptance Criteria:**
- A summary can be generated from supported student-performance data.
- The summary corresponds to the selected course/period.
- The generated output is available to the instructor for review.

* **Source FR(s):** FR-I54
* **Relevant NFR(s):** NFR-04, NFR-05, NFR-30

---

### US-I60

**Front of Card — User Story:**
> As a course instructor, I want to review and edit AI-generated content before publishing it to students, so that I can verify AI output before learners see it.

**Back of Card — Acceptance Criteria:**
- AI-generated content can be reviewed before publication.
- The instructor can edit the content.
- Publishing is a separate authorized action.

* **Source FR(s):** FR-I55
* **Relevant NFR(s):** NFR-04, NFR-29

---

### US-I61

**Front of Card — User Story:**
> As a course instructor, I want to define the completion criteria required for students to earn a course certificate, so that certificate eligibility reflects my course completion policy.

**Back of Card — Acceptance Criteria:**
- The instructor can define supported completion conditions.
- The configured conditions are used for certificate eligibility.
- Only authorized course instructors can edit them.

* **Source FR(s):** FR-I56
* **Relevant NFR(s):** NFR-17

---

### US-I62

**Front of Card — User Story:**
> As a course instructor, I want to view a record of certificates issued for my courses, so that I can track certificates earned by learners.

**Back of Card — Acceptance Criteria:**
- A certificate issuance record is available for the instructor's course(s).
- The record identifies the relevant course/certificate event.
- Access is restricted to authorized course instructors/admins.

* **Source FR(s):** FR-I57
* **Relevant NFR(s):** NFR-17, NFR-30

---

## Online Learning Platform – Admin User Stories

### US-A01

**Front of Card — User Story:**
> As an administrator, I want to log in and log out with elevated privileges, so that I can securely perform authorized administrative tasks.

**Back of Card — Acceptance Criteria:**
- Valid admin credentials allow login.
- The admin can log out.
- Elevated access is available only to authorized admins.

* **Source FR(s):** FR-A01
* **Relevant NFR(s):** NFR-13, NFR-17

---

### US-A02

**Front of Card — User Story:**
> As an administrator, I want to create, update, suspend, or delete student and instructor accounts, so that I can manage platform user access.

**Back of Card — Acceptance Criteria:**
- Admin can perform the supported account actions.
- Changes affect the intended user only.
- Each action is restricted to admin privileges and auditable.

* **Source FR(s):** FR-A02
* **Relevant NFR(s):** NFR-17, NFR-19, NFR-30

---

### US-A03

**Front of Card — User Story:**
> As an administrator, I want to verify and approve new instructor registrations before granting course-creation access, so that only approved instructors can create/manage courses.

**Back of Card — Acceptance Criteria:**
- Pending instructor registrations can be reviewed.
- Admin can approve or reject according to the workflow.
- Course-creation access is granted only after required approval.

* **Source FR(s):** FR-A03
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-A04

**Front of Card — User Story:**
> As an administrator, I want to assign or revoke admin/sub-admin roles and permissions, so that I can manage privileged access.

**Back of Card — Acceptance Criteria:**
- Admin can assign/revoke configured roles.
- Permissions reflect the selected role.
- Unauthorized users cannot grant themselves elevated access.

* **Source FR(s):** FR-A04
* **Relevant NFR(s):** NFR-13, NFR-17, NFR-19

---

### US-A05

**Front of Card — User Story:**
> As an administrator, I want to reset user passwords or trigger account recovery on behalf of users when required, so that I can help restore legitimate account access.

**Back of Card — Acceptance Criteria:**
- Admin can initiate the supported recovery/reset flow.
- The affected account receives the configured recovery path.
- The action is recorded for accountability.

* **Source FR(s):** FR-A05
* **Relevant NFR(s):** NFR-13, NFR-19

---

### US-A06

**Front of Card — User Story:**
> As an administrator, I want to review courses submitted by instructors before publishing, so that I can control which courses become live.

**Back of Card — Acceptance Criteria:**
- Pending courses are visible to admins.
- The admin can open course metadata/content needed for review.
- Only courses in the relevant review state are treated as pending.

* **Source FR(s):** FR-A06
* **Relevant NFR(s):** NFR-17, NFR-20

---

### US-A07

**Front of Card — User Story:**
> As an administrator, I want to approve, reject, or request changes to a submitted course with reasons, so that instructors receive a clear review outcome.

**Back of Card — Acceptance Criteria:**
- The admin can select approve/reject/request changes.
- A reason can be recorded when required.
- The course status changes to the selected outcome.
- The instructor receives the configured outcome.

* **Source FR(s):** FR-A07
* **Relevant NFR(s):** NFR-19, NFR-20, NFR-23

---

### US-A08

**Front of Card — User Story:**
> As an administrator, I want to unpublish, archive, or remove a live course from the platform, so that I can manage courses that must no longer remain live.

**Back of Card — Acceptance Criteria:**
- The admin can perform the permitted course action.
- The course changes to the appropriate platform state.
- The action is reflected for learners according to platform policy.

* **Source FR(s):** FR-A08
* **Relevant NFR(s):** NFR-17, NFR-19, NFR-34

---

### US-A09

**Front of Card — User Story:**
> As an administrator, I want to view all courses and filter them by status, so that I can oversee the full course catalog.

**Back of Card — Acceptance Criteria:**
- All permitted course records are available in the admin view.
- The admin can filter by draft, pending review, published, and archived.
- Filtering returns the correct status subset.

* **Source FR(s):** FR-A09
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-A10

**Front of Card — User Story:**
> As an administrator, I want to assign or reassign instructors to a course where necessary, so that course ownership/teaching assignments remain manageable.

**Back of Card — Acceptance Criteria:**
- Admin can choose an instructor for a course.
- A reassignment changes the course association.
- Only authorized instructors are added as course instructors.

* **Source FR(s):** FR-A10
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-A11

**Front of Card — User Story:**
> As an administrator, I want to review flagged or reported course content, discussion posts, and comments, so that I can moderate policy-sensitive content.

**Back of Card — Acceptance Criteria:**
- Flagged/report items are presented in the moderation view.
- The admin can open the relevant content context.
- Only authorized admins can access moderation tools.

* **Source FR(s):** FR-A11
* **Relevant NFR(s):** NFR-17, NFR-20, NFR-30

---

### US-A12

**Front of Card — User Story:**
> As an administrator, I want to remove or edit content that violates platform policy, so that I can enforce platform content rules.

**Back of Card — Acceptance Criteria:**
- The admin can apply a supported moderation action.
- The changed/removed content is no longer available contrary to the moderation decision.
- The moderation action is logged.

* **Source FR(s):** FR-A12
* **Relevant NFR(s):** NFR-19, NFR-30

---

### US-A13

**Front of Card — User Story:**
> As an administrator, I want to view a moderation queue prioritized by report severity/age, so that I can address higher-priority moderation work first.

**Back of Card — Acceptance Criteria:**
- Pending moderation actions appear in a queue.
- The queue reflects configured severity/age prioritization.
- Completed/resolved items are no longer treated as pending.

* **Source FR(s):** FR-A13
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-A14

**Front of Card — User Story:**
> As an administrator, I want to notify the relevant instructor or student when moderation action is taken, so that affected users are informed of moderation outcomes.

**Back of Card — Acceptance Criteria:**
- A notification can be generated for the affected user.
- The notification references the relevant content/action.
- Only the intended recipient receives the notification.

* **Source FR(s):** FR-A14
* **Relevant NFR(s):** NFR-20, NFR-23

---

### US-A15

**Front of Card — User Story:**
> As an administrator, I want to view escalated question reports that instructors have not resolved within a configured timeframe, so that I can oversee unresolved assessment issues.

**Back of Card — Acceptance Criteria:**
- Reports that exceed the configured timeframe can be identified.
- The admin can inspect the report context.
- The report remains traceable to the original assessment/question.

* **Source FR(s):** FR-A15
* **Relevant NFR(s):** NFR-19, NFR-30

---

### US-A16

**Front of Card — User Story:**
> As an administrator, I want to override or finalize a decision on a disputed question report, so that I can resolve escalated assessment disputes.

**Back of Card — Acceptance Criteria:**
- The admin can make the supported final decision.
- The decision is recorded.
- Affected results/notifications can follow the final decision workflow.

* **Source FR(s):** FR-A16
* **Relevant NFR(s):** NFR-19

---

### US-A17

**Front of Card — User Story:**
> As an administrator, I want to view platform-wide examination schedules and monitoring logs, so that I can oversee examinations across the platform.

**Back of Card — Acceptance Criteria:**
- Exam schedules across permitted courses are visible.
- Monitoring logs can be reviewed with the authorized admin access.
- The data are protected from unauthorized roles.

* **Source FR(s):** FR-A17
* **Relevant NFR(s):** NFR-05, NFR-18, NFR-19

---

### US-A18

**Front of Card — User Story:**
> As an administrator, I want to review flagged examination-monitoring incidents across all courses, so that I can investigate platform-wide monitoring incidents.

**Back of Card — Acceptance Criteria:**
- Flagged tab-switch/screen-share incidents are searchable/viewable.
- The incident identifies the relevant examination attempt.
- Access is restricted to authorized admins/instructors.

* **Source FR(s):** FR-A18
* **Relevant NFR(s):** NFR-18, NFR-19, NFR-32

---

### US-A19

**Front of Card — User Story:**
> As an administrator, I want to invalidate or nullify an examination attempt in case of confirmed malpractice, so that the platform can apply the appropriate final outcome.

**Back of Card — Acceptance Criteria:**
- The admin can select an eligible examination attempt.
- The invalidation/nullification decision is recorded.
- The resulting student outcome follows the configured assessment policy.

* **Source FR(s):** FR-A19
* **Relevant NFR(s):** NFR-19, NFR-32

---

### US-A20

**Front of Card — User Story:**
> As an administrator, I want to view platform-wide statistics for users, active courses, enrollments, and revenue, so that I can monitor the overall state of the platform.

**Back of Card — Acceptance Criteria:**
- The dashboard shows the configured platform-wide metrics.
- The figures correspond to the supported reporting data.
- The dashboard is restricted to admin access.

* **Source FR(s):** FR-A20
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-A21

**Front of Card — User Story:**
> As an administrator, I want to generate and export platform usage, engagement, and growth reports, so that I can analyze platform trends outside the dashboard.

**Back of Card — Acceptance Criteria:**
- The admin can select supported reporting views.
- Reports can be generated/exported.
- Exported data reflect the selected reporting scope.

* **Source FR(s):** FR-A21
* **Relevant NFR(s):** NFR-05, NFR-17, NFR-30

---

### US-A22

**Front of Card — User Story:**
> As an administrator, I want to view instructor performance metrics across their courses, so that I can monitor instructor activity and outcomes.

**Back of Card — Acceptance Criteria:**
- Instructor metrics are available across permitted courses.
- The metrics are attributable to the relevant instructor/course.
- Only authorized admin users can access them.

* **Source FR(s):** FR-A22
* **Relevant NFR(s):** NFR-05, NFR-17

---

### US-A23

**Front of Card — User Story:**
> As an administrator, I want to view transaction records including payments, refunds, and instructor payouts, so that I can oversee platform financial activity.

**Back of Card — Acceptance Criteria:**
- Payment, refund, and payout transactions can be viewed.
- Records contain sufficient information for supported reconciliation.
- Access is restricted to authorized admins.

* **Source FR(s):** FR-A23
* **Relevant NFR(s):** NFR-19, NFR-33

---

### US-A24

**Front of Card — User Story:**
> As an administrator, I want to approve or reject refund requests outside automated policy rules, so that I can resolve exceptional refund cases.

**Back of Card — Acceptance Criteria:**
- Exceptional refund requests are visible.
- Admin can approve/reject the request.
- The decision is recorded and sent to the supported refund workflow.

* **Source FR(s):** FR-A24
* **Relevant NFR(s):** NFR-19, NFR-23

---

### US-A25

**Front of Card — User Story:**
> As an administrator, I want to configure platform-wide pricing rules, commission percentages, and discount policies, so that the commercial rules remain centrally manageable.

**Back of Card — Acceptance Criteria:**
- Supported pricing/commission/discount settings can be configured.
- New settings are stored and used by supported transactions.
- Only authorized admins can modify them.

* **Source FR(s):** FR-A25
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-A26

**Front of Card — User Story:**
> As an administrator, I want to initiate or halt instructor payouts in case of disputes or policy violations, so that I can control exceptional payout situations.

**Back of Card — Acceptance Criteria:**
- An authorized admin can initiate or halt a payout under the supported policy.
- The payout state reflects the decision.
- The action is recorded for audit/reconciliation.

* **Source FR(s):** FR-A26
* **Relevant NFR(s):** NFR-19, NFR-33

---

### US-A27

**Front of Card — User Story:**
> As an administrator, I want to broadcast platform-wide announcements to all users or specific user segments, so that I can communicate important platform information.

**Back of Card — Acceptance Criteria:**
- The admin can select all users or a supported segment.
- The announcement is delivered to the intended audience.
- The broadcast action is recorded.

* **Source FR(s):** FR-A27
* **Relevant NFR(s):** NFR-20, NFR-19

---

### US-A28

**Front of Card — User Story:**
> As an administrator, I want to configure automated notification templates, so that platform notifications remain standardized and manageable.

**Back of Card — Acceptance Criteria:**
- Supported notification templates can be created/updated.
- Configured templates are used by the associated notification event.
- Only authorized admins can change templates.

* **Source FR(s):** FR-A28
* **Relevant NFR(s):** NFR-20, NFR-19

---

### US-A29

**Front of Card — User Story:**
> As an administrator, I want to configure platform-wide settings such as certificate templates, gamification rules, and AI-assistant availability, so that I can manage shared platform behavior.

**Back of Card — Acceptance Criteria:**
- Supported platform settings can be configured.
- Changes apply to the intended platform features.
- Only authorized admin users can modify them.

* **Source FR(s):** FR-A29
* **Relevant NFR(s):** NFR-17, NFR-19

---

### US-A30

**Front of Card — User Story:**
> As an administrator, I want to manage data-retention and privacy-compliance settings, so that the platform follows applicable privacy requirements.

**Back of Card — Acceptance Criteria:**
- Retention/privacy settings available to admins can be configured.
- The settings are applied to the supported data workflows.
- Changes are auditable.

* **Source FR(s):** FR-A30
* **Relevant NFR(s):** NFR-30, NFR-31, NFR-32, NFR-33, NFR-19

---

### US-A31

**Front of Card — User Story:**
> As an administrator, I want to maintain an audit log of admin actions, so that administrative activity is traceable for accountability and compliance.

**Back of Card — Acceptance Criteria:**
- Admin actions create audit log records.
- The records are immutable according to the platform design.
- Logs remain available for the required retention period.

* **Source FR(s):** FR-A31
* **Relevant NFR(s):** NFR-19

---

### US-A32

**Front of Card — User Story:**
> As an administrator, I want to configure and manage third-party integrations such as payment gateway, CDN, and AI services, so that the platform can manage its external service dependencies.

**Back of Card — Acceptance Criteria:**
- Supported integrations can be configured by an authorized admin.
- Integration status/configuration is stored securely.
- Changing providers does not require changing the user-facing workflows beyond the supported abstraction.

* **Source FR(s):** FR-A32
* **Relevant NFR(s):** NFR-29

---

## Online Learning Platform – Payment Gateway User Stories

### US-PG01

**Front of Card — User Story:**
> As a payment gateway provider, I want to process student payments for course purchases and subscriptions in supported currencies, so that successful purchases can be completed.

**Back of Card — Acceptance Criteria:**
- The gateway accepts supported payment requests from the platform.
- The transaction is processed in a supported currency.
- The gateway returns a transaction outcome to the platform.

* **Source FR(s):** FR-PG01
* **Relevant NFR(s):** NFR-12, NFR-15, NFR-16

---

### US-PG02

**Front of Card — User Story:**
> As a payment gateway provider, I want to support credit/debit cards, UPI, net banking, and digital wallets, so that students have multiple payment choices.

**Back of Card — Acceptance Criteria:**
- The gateway exposes the configured supported payment methods.
- A selected supported method can be used for a valid transaction.
- Unsupported methods are not presented as available.

* **Source FR(s):** FR-PG02
* **Relevant NFR(s):** NFR-15, NFR-20

---

### US-PG03

**Front of Card — User Story:**
> As a payment gateway provider, I want to return real-time transaction status (success, failure, pending), so that the platform can update the transaction state correctly.

**Back of Card — Acceptance Criteria:**
- The gateway returns one of the supported transaction states.
- The response identifies the corresponding transaction.
- The platform can use the status to determine the next enrollment/payment action.

* **Source FR(s):** FR-PG03
* **Relevant NFR(s):** NFR-12, NFR-16

---

### US-PG04

**Front of Card — User Story:**
> As a payment gateway integration, I want the platform to update enrollment only after a confirmed successful payment, so that students are not enrolled through failed or unconfirmed transactions.

**Back of Card — Acceptance Criteria:**
- A success response is required before the platform marks the enrollment successful.
- Failed/pending transactions do not create a successful enrollment.
- A temporarily unavailable gateway follows the documented failover path.

* **Source FR(s):** FR-PG04
* **Relevant NFR(s):** NFR-12, NFR-23

---

### US-PG05

**Front of Card — User Story:**
> As a payment gateway provider, I want to process full or partial refunds initiated by the platform, so that eligible refunds can be completed.

**Back of Card — Acceptance Criteria:**
- The gateway accepts supported refund requests.
- Full and partial refund amounts are validated.
- The refund result is returned to the platform.

* **Source FR(s):** FR-PG05
* **Relevant NFR(s):** NFR-15, NFR-16, NFR-33

---

### US-PG06

**Front of Card — User Story:**
> As a payment gateway provider, I want to notify the platform of refund status through callback/webhook, so that refund records stay synchronized.

**Back of Card — Acceptance Criteria:**
- The gateway sends the configured refund status.
- The callback/webhook identifies the relevant transaction/refund.
- The platform can distinguish processed, failed, and pending states.

* **Source FR(s):** FR-PG06
* **Relevant NFR(s):** NFR-16

---

### US-PG07

**Front of Card — User Story:**
> As a payment gateway provider, I want to disburse instructor payouts to linked bank accounts or wallets, so that instructors can receive eligible earnings.

**Back of Card — Acceptance Criteria:**
- A valid payout request can be processed for a linked destination.
- The gateway returns a payout status.
- Payout data is handled using protected financial channels.

* **Source FR(s):** FR-PG07
* **Relevant NFR(s):** NFR-15, NFR-19, NFR-33

---

### US-PG08

**Front of Card — User Story:**
> As a payment gateway provider, I want to provide payout status updates, so that the platform can keep payout records current.

**Back of Card — Acceptance Criteria:**
- The gateway reports supported payout states such as successful, failed, and delayed.
- The update identifies the correct payout.
- The platform can reconcile the payout state.

* **Source FR(s):** FR-PG08
* **Relevant NFR(s):** NFR-16, NFR-33

---

### US-PG09

**Front of Card — User Story:**
> As a payment gateway provider, I want to handle and store sensitive payment data under PCI-DSS controls without exposing raw card data to the platform, so that payment information remains protected.

**Back of Card — Acceptance Criteria:**
- Raw card numbers/CVV are handled by the gateway rather than stored on platform servers.
- The gateway follows the required PCI-DSS handling for sensitive payment data.
- The platform receives only the supported non-sensitive transaction information.

* **Source FR(s):** FR-PG09
* **Relevant NFR(s):** NFR-15

---

### US-PG10

**Front of Card — User Story:**
> As a payment gateway provider, I want to support tokenization of payment methods for repeat/subscription billing, so that authorized repeat payments can be processed without exposing raw payment data.

**Back of Card — Acceptance Criteria:**
- The gateway can create/use payment tokens for supported repeat billing.
- Tokens do not expose raw card data to the platform.
- Repeat/subscription billing can use the configured token.

* **Source FR(s):** FR-PG10
* **Relevant NFR(s):** NFR-15, NFR-29

---

### US-PG11

**Front of Card — User Story:**
> As a payment gateway provider, I want to provide fraud-detection signals or risk scores for flagged transactions where available, so that the platform can identify potentially risky payments.

**Back of Card — Acceptance Criteria:**
- Supported fraud/risk information can be returned for applicable transactions.
- The signal is associated with the relevant transaction.
- The platform can consume the signal through the integration.

* **Source FR(s):** FR-PG11
* **Relevant NFR(s):** NFR-15, NFR-16

---

### US-PG12

**Front of Card — User Story:**
> As a payment gateway provider, I want to provide transaction logs/reports accessible to the platform, so that payments can be reconciled.

**Back of Card — Acceptance Criteria:**
- Supported transaction reports/logs are available.
- Records can be associated with the relevant transactions.
- The platform can use the records for reconciliation.

* **Source FR(s):** FR-PG12
* **Relevant NFR(s):** NFR-19, NFR-33

---

### US-PG13

**Front of Card — User Story:**
> As a payment gateway provider, I want to notify the platform about chargebacks or disputes raised by a student's bank/card issuer, so that the platform can take appropriate action.

**Back of Card — Acceptance Criteria:**
- Chargeback/dispute events are communicated to the platform.
- The event identifies the relevant transaction.
- The platform can record and act on the event.

* **Source FR(s):** FR-PG13
* **Relevant NFR(s):** NFR-16, NFR-19, NFR-33

---

### US-PG14

**Front of Card — User Story:**
> As a payment gateway provider, I want to send webhooks for key payment events, so that the platform can receive timely transaction updates.

**Back of Card — Acceptance Criteria:**
- Webhooks can be sent for payment success, failure, refund, payout, and chargeback events.
- Each webhook identifies the relevant event and transaction.
- The platform can receive the event through the configured integration.

* **Source FR(s):** FR-PG14
* **Relevant NFR(s):** NFR-16

---

### US-PG15

**Front of Card — User Story:**
> As a payment gateway integration, I want incoming gateway webhooks to be verified for authenticity before processing, so that spoofed payment/refund events are rejected.

**Back of Card — Acceptance Criteria:**
- Incoming webhook authenticity/signature is verified.
- Invalid or unverifiable webhooks are rejected or not applied.
- Only authenticated events can update payment state.

* **Source FR(s):** FR-PG15
* **Relevant NFR(s):** NFR-16, NFR-15, NFR-29

---

## User Story Card Guide / Traceability Legend

| Field | Meaning |
| :--- | :--- |
| **Front of Card – User Story** | The classic As a… I want… so that… statement. |
| **Back of Card – Acceptance Criteria** | Testable conditions that define when the story is satisfied. |
| **Source FR(s)** | Functional requirement(s) from the supplied FR/NFR PDF. Student story wording is preserved from the supplied User Stories PDF where possible. |
| **Relevant NFR(s)** | Non-functional requirements that materially support/constrain the story. |
| **Notes / Traceability** | Clarifies source notes, normalized IDs, or any special requirement-traceability issue. |
