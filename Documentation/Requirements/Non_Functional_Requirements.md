# Non-Functional Requirements
> Extracted directly and exactly from `FRs, NFRs and Domain Requirements.pdf` without modifications.

---


## (E) Non-Functional Requirements (NFRs)


### 1. Performance


* **NFR-01**: Video lectures shall begin playback within 3 seconds of a student clicking play, under normal network conditions (supports FR-S12, FR-I13, FR-I62).

* **NFR-02**: The course catalog search/filter (FR-S05, FR-S06) shall return results within 2 seconds for a catalog of up to 10,000 courses.

* **NFR-03**: Quiz and examination auto-save (FR-S21, FR-S36) shall persist a response within 1 second of the student's action to minimize data loss risk.

* **NFR-04**: The AI learning assistant (FR-S71–FR-S76, FR-I53–FR-I55) shall return a response to a student/instructor query within 5 seconds for standard queries.

* **NFR-05**: Dashboard analytics for instructors and admins (FR-I43–FR-I47, FR-A20–FR-A22) shall load within 4 seconds for datasets up to 100,000 records.

### 2. Scalability


* **NFR-06**: The system shall support at least 5,000 concurrent video streams without noticeable degradation in playback quality.

* **NFR-07**: The examination module shall support at least 2,000 students taking a monitored examination simultaneously (FR-S52–FR-S61, FR-I26, FR-A17–FR-A19) without timing or auto-submit failures.

* **NFR-08**: The system architecture shall support horizontal scaling of application servers during peak enrollment periods (e.g., course launch, exam windows) without manual reconfiguration downtime.

### 3. Availability and Reliability


* **NFR-09**: The platform shall maintain an uptime of at least 99.5%, measured monthly, excluding scheduled maintenance windows.

* **NFR-10**: The examination module shall

guarantee 99.9% availability during any active, scheduled examination window, given the criticality of FR-S35–FR-S38 and FR-I25.

* **NFR-11**: In case of a student's connectivity loss during an active examination, the system shall preserve the last auto-saved response (FR-S36) and allow resumption within the remaining time window.

* **NFR-12**: Payment transaction processing (FR-P01, FR-PG01–FR-PG04) shall have a documented failover path if the payment gateway is temporarily unreachable, preventing partial/ghost enrollments.

### 4. Security


* **NFR-13**: All user authentication (FR-S02, FR-I02, FR-A01) shall use secure password hashing (e.g., bcrypt/Argon2) and support multi-factor authentication for admin accounts.

* **NFR-14**: All data in transit between client and server shall be encrypted using TLS 1.2 or higher.

* **NFR-15**: Sensitive payment data (card numbers, CVV) shall never be stored on platform servers; all such data shall be handled exclusively by the PCI-DSS-compliant payment gateway (FR-PG09).

* **NFR-16**: Incoming payment gateway webhooks (FR-PG15) shall be authenticated via signature verification before being processed, to prevent spoofed payment/refund events.

* **NFR-17**: Role-based access control (RBAC) shall enforce that students, instructors, and admins can only access functionality and data permitted for their role (e.g., FR-I61 — instructors upload only to associated courses).

* **NFR-18**: Screen-sharing and tab-monitoring data captured during examinations (FR-S53–FR-S60) shall be stored securely and access-restricted to authorized instructors/admins only.

* **NFR-19**: The system shall maintain an immutable audit log of all admin actions (FR-A31) and critical financial transactions (FR-P07, FR-PG12) for at least 1 year, or as required by regulation.

### 5. Usability and Accessibility


* **NFR-20**: The web interface shall be responsive and fully functional across desktop, tablet, and mobile screen sizes.

* **NFR-21**: The platform shall comply with WCAG 2.1 Level AA accessibility standards to support differently-abled learners (screen-reader compatibility, keyboard navigation, captions/transcripts for video content).

* **NFR-22**: Core student workflows (enrollment, taking a quiz/exam, viewing certificates) shall be completable within 3 clicks/screens from the relevant landing page.

* **NFR-23**: Error messages (e.g., failed payment, quiz submission failure) shall be presented in clear, non-technical language with a suggested next action.

### 6. Compatibility


* **NFR-24**: The platform shall function correctly on the latest two major versions of Chrome, Firefox, Safari, and Edge.

* **NFR-25**: Fullscreen examination mode (FR-S58) shall degrade gracefully with a clear warning on browsers that do not support the Fullscreen API.

* **NFR-26**: Video content shall be encoded to support adaptive bitrate streaming (HLS/DASH) for compatibility across varying network speeds and devices.

### 7. Maintainability


* **NFR-27**: The system shall be built using a modular architecture (e.g., separate services for course management, assessments, payments, notifications) to allow independent updates without full-system downtime.

* **NFR-28**: The codebase shall maintain a minimum of 70% automated test coverage for critical modules (authentication, payments, examination submission).

* **NFR-29**: All third-party integrations (payment gateway, CDN, AI services — FR-A32) shall be abstracted behind internal service interfaces to allow provider replacement with minimal code change.

### 8. Data Privacy and Compliance


* **NFR-30**: The system shall comply with applicable data protection regulations (e.g., India's DPDP Act, GDPR for international users) regarding collection, storage, and deletion of personal data.

* **NFR-31**: Students shall be able to request export or deletion of their personal data, with the system fulfilling such requests within a regulator-defined timeframe (typically 30 days).

* **NFR-32**: Examination-monitoring data (webcam/screen-share, tab-switch logs) shall only be collected with explicit student consent (FR-S59) and retained only as long as necessary for dispute resolution.

* **NFR-33**: Financial transaction logs (FR-P07, FR-PG12) shall be retained in compliance with local financial audit regulations (commonly 7 years).

### 9. Backup and Disaster Recovery


* **NFR-34**: The system shall perform automated daily backups of course content, user data, and transaction records, with a recovery point objective (RPO) of no more than 24 hours.

* **NFR-35**: In the event of a system failure, the platform shall have a recovery time objective (RTO) of no more than 4 hours for core services (login, enrollment, video playback).
