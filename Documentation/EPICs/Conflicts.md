# Conflicts Between EPICs & Resolution Strategies

During architectural analysis and backlog grooming, several cross-cutting dependencies and conflicting requirements emerged between Epics. This document outlines those conflicts and documents their engineering resolutions.

---

## 1. Epic 18 (RAG AI Assistant) vs Epic 6 (Content Upload & Management)
* **Conflict:** The RAG assistant in Epic 18 requires dense, accurate text representation to perform vector search and ground its responses. However, in Epic 6, instructors initially only embed YouTube URLs without providing transcripts or accompanying document notes, leaving the vector store empty.
* **Resolution:** Introduced **Feature 0: Transcript & Subtitle Capture** in Sprint 3. The platform allows instructors to upload `.srt`, `.vtt`, or raw text transcripts alongside any embedded video lecture. In addition, uploaded PDF slides and notes are prioritized for semantic chunking, ensuring the RAG pipeline always has a text corpus before queries are accepted.

---

## 2. Epic 11 (Anti-Cheating Monitoring) vs Domain Rule DR-06/DR-07 (Student Privacy & Fair Review)
* **Conflict:** High-stakes exam proctoring requires tracking window blurs, tab switches, and fullscreen exits. However, automated disqualification algorithms frequently produce false positives caused by operating system popups, connectivity drops, or accidental cursor movements, violating fair evaluation policies (DR-07).
* **Resolution:** Strictly decoupled incident detection from penalty enforcement. The client logs incidents asynchronously to `exam_monitoring_logs` without locking the student out. Instructors and administrators review chronological incident timelines after submission. An attempt can only be invalidated through explicit human review and recorded in `admin_audit_logs`.

---

## 3. Epic 18 (AI Quiz Generation) vs Epic 9 (Question Bank Integrity)
* **Conflict:** Instructors want rapid AI generation of quiz questions from course notes. However, generative LLMs can occasionally draft hallucinatory questions, ambiguous distractors, or incorrect answer keys, which would compromise assessment fairness.
* **Resolution:** Enforced the strict rule: *"No AI-generated assessment content is published without human instructor approval."* The AI quiz generator writes questions to `question_bank` with `is_draft = TRUE` and `is_ai_generated = TRUE`. Questions cannot be added to a live quiz until the instructor explicitly reviews, edits, and saves them.

---

## 4. Epic 20 (Payments & Checkout) vs Epic 4 (Enrollment & Access)
* **Conflict:** Client-side redirect callbacks from payment gateways (e.g., frontend redirect to `/payment-success`) can be spoofed by malicious users to obtain free course access without paying.
* **Resolution:** Enforced zero-trust enrollment activation. Frontend redirect pages only show a pending status indicator. Course enrollment records are created strictly inside an asynchronous webhook handler after verifying the payment gateway's cryptographic `HMAC-SHA256` signature.

---

## 5. Epic 13 (Question Disputes) vs Epic 12 (Grading & Result Immutability)
* **Conflict:** Once exam grades are published, students expect immutable grade records. If a student later reports an erroneous question and an instructor awards bonus marks, altering the grade could violate audit requirements (DR-ED04).
* **Resolution:** Every grade change resulting from an approved question dispute creates a versioned grade history record storing the previous score, adjusted score, dispute ID reference, timestamp, and resolving instructor ID.
