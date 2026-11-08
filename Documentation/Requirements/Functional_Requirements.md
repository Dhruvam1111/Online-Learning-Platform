# Functional Requirements

> Extracted directly and exactly from `FRs, NFRs and Domain Requirements.pdf` without modifications.

---



## (A)Student Functional Requirement


### 1. Account and Profile Management


* **FR-S01**: The system shall allow students to register and create an account on the Online Learning Portal.

* **FR-S02**: The system shall allow registered students to securely log in, log out, and reset their password.

* **FR-S03**: The system shall allow students to view and update their profile information.

### 2. Course Discovery and Enrollment


* **FR-S05**: The system shall allow students to browse and search for available courses.

* **FR-S06**: The system shall allow students to filter courses based on criteria such as domain, duration, rating, and subject.

* **FR-S07**: The system shall allow students to view detailed course information, including course description, syllabus, instructor, prerequisites, duration, and learning outcomes.

* **FR-S08**: The system shall allow students to enroll in courses available to them.

* **FR-S09**: The system shall allow students to view their enrolled courses.

* **FR-S10**: The system shall allow students to bookmark courses for future reference. FR The system shall allow registered students to download and view their earned certificate.

### 3. Learning and Course Content


* **FR-S11**: The system shall allow enrolled students to access the learning content of their courses.

* **FR-S12**: The system shall allow students to watch, pause, resume, and seek through video lectures.

* **FR-S13**: The system shall remember the student's video playback position and allow them to resume from where they stopped.

* **FR-S14**: The system shall allow students to access and download permitted learning materials such as notes, PDFs, presentations, and other resources.

* **FR-S15**: The system shall allow students to mark learning content as completed.

* **FR-S16**: The system shall track and display the student's progress within each enrolled course.

* **FR-S17**: The system shall allow students to bookmark important lectures and learning resources.

* **FR-S18 **: The system shall allow students to search for specific topics or keywords within permitted video lectures and access the relevant portion of the lecture.

### 4. Quiz Management


* **FR-S18**: The system shall allow students to view and attempt quizzes associated with their enrolled courses.

* **FR-S19**: The system shall display quiz instructions, duration, number of questions, and maximum marks.

* **FR-S20**: The system shall support different types of quiz questions, including multiple-choice, multiple-select, true/false, numerical, and descriptive questions where configured.

* **FR-S21**: The system shall save the user's response after it clicks the save and next option.

* **FR-S22**: The system shall display the remaining time for timed quizzes.

* **FR-S23**: The system shall allow students to navigate between questions and submit the quiz.

* **FR-S24**: The system shall automatically submit the quiz when the allotted time expires.

* **FR-S25**: The system shall allow students to view their quiz marks and results after submission, according to the configured assessment settings.

* **FR-S26**: The system shall allow students to review their answers and feedback when enabled.

### 6. Examination Management


* **FR-S33**: The system shall allow students to view upcoming, ongoing, and completed examinations.

* **FR-S34**: The system shall display examination instructions, schedule, duration, maximum marks, and eligibility requirements.

* **FR-S35**: The system shall allow eligible students to start an examination during the permitted examination period.

* **FR-S36**: The system shall automatically save examination responses during an active examination.

* **FR-S37**: The system shall display the remaining examination time and automatically submit the examination when the allotted time expires.

* **FR-S38**: The system shall allow students to submit their examination manually.

* **FR-S39**: The system shall allow students to view their examination results and marks after evaluation and publication.

* **FR-S40**: The system shall allow students to view question-wise marks and feedback when permitted.

### 7. Question Reporting and Ambiguity


* **FR-S41**: The system shall allow students to report ambiguous, incorrect, incomplete, or otherwise problematic questions in quizzes and examinations.

* **FR-S42**: The system shall allow students to select a reason for reporting a question and provide an explanation.

* **FR-S43**: The system shall allow students to view the status of their submitted question reports.

* **FR-S44**: The system shall notify students about the resolution of their reported questions.

* **FR-S45**: The system shall apply instructor-approved corrections or question invalidations to affected assessment results according to the configured assessment policy.

### 8. Marks and Performance Tracking


* **FR-S46**: The system shall allow students to view marks obtained in quizzes, assignments, and examinations.

* **FR-S47**: The system shall display the student's overall performance and course completion percentage.

* **FR-S48**: The system shall maintain a history of the student's assessment results.

* **FR-S49**: The system shall provide topic-wise performance information where supported.

* **FR-S50**: The system shall identify topics in which students require additional practice based on their performance.

* **FR-S51**: The system shall allow students to track their academic performance over time.

* **FR-S52**: The system shall allow students to create or receive a personalized study schedule based on their courses, deadlines, and learning progress.

### 9. Examination Monitoring and Anti-Cheating


* **FR-S52**: The system shall maintain an activity log for each monitored examination attempt.

* **FR-S53**: The system shall detect and record when a student leaves the examination browser tab or window during a monitored examination.

* **FR-S54**: The system shall record the timestamp and duration of each detected focus-loss or tab-change event.

* **FR-S55**: The system shall notify students when configured examination-monitoring rules are triggered.

* **FR-S56**: The system shall support randomized question and answer-option ordering during examinations.

* **FR-S57**: The system shall support generating different examination sets from a configured question bank.

* **FR-S58**: The system shall support fullscreen examination mode where supported by the browser.

* **FR-S59**: The system shall request explicit permission from students before accessing screen-sharing functionality.

* **FR-S60**: The system shall record when screen sharing starts, stops, or is unexpectedly terminated during a monitored examination.

* **FR-S61**: The system shall associate examination-monitoring events with the corresponding student's examination attempt.

### 10. Communication and Interaction


* **FR-S62**: The system shall allow students to view course announcements.

* **FR-S63**: The system shall allow students to ask questions related to course content.

* **FR-S64**: The system shall allow students to participate in course discussions and view instructor responses.

* **FR-S65**: The system shall notify students about important course events, deadlines, newly available content, examinations, and announcements.

### 11. Gamification and Engagement


* **FR-S66**: The system shall award points to students for configured learning activities.

* **FR-S67**: The system shall track student learning streaks.

* **FR-S68**: The system shall award badges for predefined achievements.

* **FR-S69**: The system shall allow students to view their achievements, badges, and points.

* **FR-S70**: The system shall provide optional leaderboards and learning challenges where enabled.

### 12. AI-Based Learning Assistance


* **FR-S71**: The system shall provide an AI-based learning assistant for course-related queries.

* **FR-S72**: The system shall allow students to ask questions about permitted course content through the AI learning assistant.

* **FR-S73**: The system shall generate summaries of permitted course videos and learning materials.

* **FR-S74**: The system shall provide explanations for selected assessment questions where enabled.

* **FR-S75**: The system shall identify weak topics based on the student's learning and assessment performance.

* **FR-S76**: The system shall recommend relevant learning resources and practice questions based on the student's learning activity and performance.

### 13. Course Completion and Certificates


* **FR-S77**: The system shall determine whether a student has satisfied the configured completion requirements of a course.

* **FR-S78**: The system shall allow eligible students to view and obtain their course completion certificate.

* **FR-S79**: The system shall maintain a record of certificates earned by the student.


---



## (B) Course Instructor Functional Requirements


### 1. Account and Profile Management


* **FR-I01**: The system shall allow instructors to register and create an account on the Online Learning Portal, subject to admin verification/approval.

* **FR-I02**: The system shall allow registered instructors to securely log in, log out, and reset their password.

* **FR-I03**: The system shall allow instructors to view and update their profile information, including qualifications, expertise areas, and bio.

* **FR-I04**: The system shall allow instructors to upload and update a profile picture and supporting credentials/documents.

### 2. Course Creation and Management


* **FR-I05**: The system shall allow instructors to create a new course with title, description, category, prerequisites, and learning outcomes.

* **FR-I06**: The system shall allow instructors to organize course content into modules, sections, and lessons.

* **FR-I07**: The system shall allow instructors to save a course as a draft before publishing.

* **FR-I08**: The system shall allow instructors to submit a course for admin review and approval prior to publishing.

* **FR-I09**: The system shall allow instructors to edit, update, or unpublish an existing course.

* **FR-I10**: The system shall allow instructors to set course pricing, discounts, and enrollment limits where applicable.

* **FR-I11**: The system shall allow instructors to define prerequisites required for a course.

* **FR-I12**: The system shall allow instructors to archive or delete a course, subject to platform policy.

* **FR-I58**: The system shall allow instructors to associate themselves with (get assigned to or select) any course available on the platform, subject to admin approval where required.

* **FR-I59**: The system shall allow multiple instructors to be associated with the same course.

* **FR-I60**: The system shall allow instructors to view a list of all courses they are currently associated with.

### 3. Content Upload and Management


* **FR-I13**: The system shall allow instructors to upload video lectures for their courses.

* **FR-I14**: The system shall allow instructors to upload supplementary learning materials such as notes, PDFs, presentations, and other resources.

* **FR-I15**: The system shall notify instructors of the processing/transcoding status of uploaded video content.

* **FR-I16**: The system shall allow instructors to reorder, replace, or remove uploaded content within a course.

* **FR-I17**: The system shall allow instructors to mark specific learning materials as downloadable or view-only.

* **FR-I18**: The system shall allow instructors to schedule the release date/time of specific course content.

* **FR-I61**: The system shall allow an instructor to upload video content only to the course(s) they are associated with.

* **FR-I62**: The system shall allow students to view/stream video content uploaded by the instructor for their enrolled course.

### 4. Quiz and Assessment Creation


* **FR-I19**: The system shall allow instructors to create quizzes and associate them with specific courses or modules.

* **FR-I20**: The system shall allow instructors to configure quiz settings, including duration, number of questions, maximum marks, and number of attempts.

* **FR-I21**: The system shall allow instructors to add different types of quiz questions, including multiple-choice, multiple-select, true/false, numerical, and descriptive questions.

* **FR-I22**: The system shall allow instructors to define correct answers and marking schemes for objective questions.

* **FR-I23**: The system shall allow instructors to enable or disable answer review and feedback visibility for students after quiz submission.

* **FR-I24**: The system shall allow instructors to maintain a question bank for reuse across quizzes and examinations.

### 5. Examination Management


* **FR-I25**: The system shall allow instructors to create and schedule examinations, including start time, duration, and eligibility criteria.

* **FR-I26**: The system shall allow instructors to configure examination-monitoring rules such as tab-switch detection, fullscreen mode, and screen-sharing requirements.

* **FR-I27**: The system shall allow instructors to generate randomized examination sets from a configured question bank.

* **FR-I28**: The system shall allow instructors to publish examination results after evaluation.

* **FR-I29**: The system shall allow instructors to review examination-monitoring logs and flagged activity for their examinations.

### 6. Grading and Evaluation


* **FR-I30**: The system shall allow instructors to view and grade student submissions for descriptive/subjective questions.

* **FR-I31**: The system shall allow instructors to provide feedback and comments on graded submissions.

* **FR-I32**: The system shall allow instructors to override auto-generated grades where necessary, with a recorded justification.

* **FR-I33**: The system shall notify instructors of pending ungraded submissions.

* **FR-I34**: The system shall allow instructors to publish or withhold assessment results according to configured release settings.

### 7. Question Reporting and Ambiguity Resolution


* **FR-I35**: The system shall notify instructors when a student reports an ambiguous, incorrect, or problematic question.

* **FR-I36**: The system shall allow instructors to review reported questions and take action such as correcting, invalidating, or dismissing the report.

* **FR-I37**: The system shall allow instructors to apply approved corrections or invalidations to affected student results.

* **FR-I38**: The system shall notify students about the resolution status of their reported questions.

### 8. Student Interaction and Communication


* **FR-I39**: The system shall allow instructors to post announcements for their enrolled students.

* **FR-I40**: The system shall allow instructors to respond to student questions and participate in course discussion forums.

* **FR-I41**: The system shall allow instructors to send notifications regarding deadlines, newly available content, and examinations.

* **FR-I42**: The system shall allow instructors to view and moderate discussion posts within their own courses.

### 9. Student Performance and Analytics


* **FR-I43**: The system shall allow instructors to view enrollment statistics for their courses.

* **FR-I44**: The system shall allow instructors to view aggregate and individual student performance in quizzes and examinations.

* **FR-I45**: The system shall allow instructors to view course completion rates and engagement metrics.

* **FR-I46**: The system shall allow instructors to identify commonly missed topics or questions across their student cohort.

* **FR-I47**: The system shall allow instructors to export student performance reports.

### 10. Earnings and Payouts


* **FR-I48**: The system shall allow instructors to view their earnings summary based on enrollments and course sales.

* **FR-I49**: The system shall allow instructors to view their payout history and status.

* **FR-I50**: The system shall allow instructors to configure or update their payout account details.

### 11. Gamification Configuration


* **FR-I51**: The system shall allow instructors to configure gamification rules such as points, badges, and streak criteria for their courses.

* **FR-I52**: The system shall allow instructors to enable or disable leaderboards and learning challenges for their courses.

### 12. AI-Based Assistance for Instructors


* **FR-I53**: The system shall provide instructors with AI-generated suggestions for quiz questions based on course content.

* **FR-I54**: The system shall provide instructors with AI-generated summaries of student performance trends.

* **FR-I55**: The system shall allow instructors to review and edit AI-generated content before publishing it to students.

### 13. Certification


* **FR-I56**: The system shall allow instructors to define the completion criteria required for students to earn a course certificate.

* **FR-I57**: The system shall allow instructors to view a record of certificates issued for their courses.

Admin & Payment Gateway — Functional Requirements


---



## (C) Admin Functional Requirements


### 1. Account and Access Management


* **FR-A01**: The system shall allow admins to securely log in and log out with elevated access privileges.

* **FR-A02**: The system shall allow admins to create, update, suspend, or delete student and instructor accounts.

* **FR-A03**: The system shall allow admins to verify and approve new instructor registrations before granting course-creation access.

* **FR-A04**: The system shall allow admins to assign or revoke admin/sub-admin roles and permissions.

* **FR-A05**: The system shall allow admins to reset user passwords or trigger account recovery on behalf of users when required.

### 2. Course Approval and Management


* **FR-A06**: The system shall allow admins to review courses submitted by instructors before publishing.

* **FR-A07**: The system shall allow admins to approve, reject, or request changes to a submitted course, with reasons provided to the instructor.

* **FR-A08**: The system shall allow admins to unpublish, archive, or remove a live course from the platform.

* **FR-A09**: The system shall allow admins to view a list of all courses on the platform, filterable by status (draft, pending review, published, archived).

* **FR-A10**: The system shall allow admins to assign or reassign instructor(s) to a course where necessary.

### 3. Content Moderation


* **FR-A11**: The system shall allow admins to review flagged or reported course content, discussion posts, and comments.

* **FR-A12**: The system shall allow admins to remove or edit content that violates platform policy.

* **FR-A13**: The system shall allow admins to view a queue of pending moderation actions prioritized by report severity/age.

* **FR-A14**: The system shall notify the relevant instructor or student when moderation action is taken on their content.

### 4. Question Reporting Oversight


* **FR-A15**: The system shall allow admins to view escalated question reports that instructors have not resolved within a configured timeframe.

* **FR-A16**: The system shall allow admins to override or finalize a decision on a disputed question report.

### 5. Examination Oversight


* **FR-A17**: The system shall allow admins to view platform-wide examination schedules and monitoring logs.

* **FR-A18**: The system shall allow admins to review flagged examination-monitoring incidents (tab-switch, screen-share violations) across all courses.

* **FR-A19**: The system shall allow admins to invalidate or nullify an examination attempt in case of confirmed malpractice.

### 6. User and Platform Analytics


* **FR-A20**: The system shall allow admins to view platform-wide statistics, including total users, active courses, enrollments, and revenue.

* **FR-A21**: The system shall allow admins to generate and export reports on platform usage, engagement, and growth trends.

* **FR-A22**: The system shall allow admins to view instructor performance metrics across all their courses.

### 7. Financial Oversight


* **FR-A23**: The system shall allow admins to view all transaction records, including payments, refunds, and instructor payouts.

* **FR-A24**: The system shall allow admins to approve or reject refund requests that fall outside automated policy rules.

* **FR-A25**: The system shall allow admins to configure platform-wide pricing rules, commission percentages, and discount policies.

* **FR-A26**: The system shall allow admins to initiate or halt instructor payouts in case of disputes or policy violations.

### 8. Communication and Notifications


* **FR-A27**: The system shall allow admins to broadcast platform-wide announcements to all users or specific user segments.

* **FR-A28**: The system shall allow admins to configure automated notification templates (e.g., enrollment confirmation, certificate issuance).

### 9. System Configuration and Compliance


* **FR-A29**: The system shall allow admins to configure platform-wide settings such as certificate templates, gamification rules, and AI-assistant availability.

* **FR-A30**: The system shall allow admins to manage data-retention and privacy-compliance settings in accordance with applicable regulations.

* **FR-A31**: The system shall maintain an audit log of all admin actions for accountability and compliance purposes.

* **FR-A32**: The system shall allow admins to configure and manage third-party integrations (payment gateway, CDN, AI services).


---



## (D) Payment Gateway (Third-Party) Functional Requirements These define what the platform requires from and exchanges with the integrated payment gateway provider(s) (e.g., Razorpay, Stripe, PayPal).


### 1. Transaction Processing


* **FR-PG01**: The payment gateway shall process student payments for course purchases and subscriptions in supported currencies.

* **FR-PG02**: The payment gateway shall support multiple payment methods, including credit/debit cards, UPI, net banking, and digital wallets.

* **FR-PG03**: The payment gateway shall return a real-time transaction status (success, failure, pending) to the platform.

* **FR-PG04**: The system shall update enrollment status only after receiving a confirmed success response from the payment gateway.

### 2. Refunds and Reversals


* **FR-PG05**: The payment gateway shall support processing of refunds initiated by the platform, either fully or partially.

* **FR-PG06**: The payment gateway shall notify the platform of refund status (processed, failed, pending) via callback or webhook.

### 3. Payouts


* **FR-PG07**: The payment gateway shall support disbursement of instructor payouts to linked bank accounts or wallets.

* **FR-PG08**: The payment gateway shall provide payout status updates (successful, failed, delayed) to the platform.

### 4. Security and Compliance


* **FR-PG09**: The payment gateway shall handle and store sensitive payment data (card numbers, CVV) in compliance with PCI-DSS standards, without exposing raw data to the platform.

* **FR-PG10**: The payment gateway shall support tokenization of payment methods for repeat/subscription billing.

* **FR-PG11**: The payment gateway shall provide fraud-detection signals or risk scores for flagged transactions where available.

### 5. Reconciliation and Reporting


* **FR-PG12**: The payment gateway shall provide transaction logs/reports accessible to the platform for reconciliation purposes.

* **FR-PG13**: The payment gateway shall notify the platform of any chargeback or dispute raised by a student's bank/card issuer.

### 6. Webhooks and Integration


* **FR-PG14**: The payment gateway shall send webhook notifications to the platform for key events (payment success, failure, refund, payout, chargeback).

* **FR-PG15**: The system shall verify the authenticity of incoming payment gateway webhooks (e.g., via signature validation) before processing them.
