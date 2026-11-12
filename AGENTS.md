# Online Learning Platform (OLP) — AI Agent Instructions & Guardrails

This document defines mandatory operational rules and safety constraints for all AI coding assistants (Google Antigravity, Claude, ChatGPT, GitHub Copilot, Cursor) interacting with this repository.

---

## 1. Protected Files (NEVER Commit, Upload, or Delete)

The following files and directories must **NEVER** be committed to Git, pushed to GitHub, or deleted from the local machine:

### A. Credentials & Configuration (Local Only)
* **`backend/src/main/resources/application.properties`**: Contains live Neon Serverless PostgreSQL credentials and JWT secrets. Never track, stage, or upload this file. Only commit changes to `backend/src/main/resources/application.properties.example` using placeholder values.
* **`.env` and `**/.env*`**: Any local environment variable files.

### B. Personal Notes & Working Drafts (Local Only)
* **`Notes - personal/`**: Contains private developer notes, scratch decisions, and brainstorms. Never stage, commit, push, or delete this folder.
* **`Documentation/00-sprint1-overview.md` through `06-additional-features.md`**: Local sprint specifications maintained offline.
* **`Documentation/Person2_Content_Exam_Corrections.md`**: Person 2 assignment spec (kept locally).
* **`Documentation/person6-student-layout-frontend.md`**: Person 6 layout spec (kept locally).

### C. IDE Configurations & Build Artifacts (Local Only)
* **`.idea/`**, **`.vscode/`**: Personal IDE settings.
* **`target/`**, **`build/`**, **`dist/`**, **`node_modules/`**: Build outputs and dependency trees.

---

## 2. Git & Version Control Protocol

* **No Blind Staging:** NEVER run `git add .` or `git add -A`. Always stage specific, explicit file paths (e.g. `git add src/...`).
* **Branch Isolation:**
  * All development must happen on feature branches (e.g., `pranamya`, `feature/<name>`).
  * Never commit directly to `main` without review.
* **Preserve Untracked Files:** Before running `git clean`, `git reset`, or `git checkout`, verify that no local untracked files or `.gitignore` entries are destroyed.
* **Line Endings:** Follow `.gitattributes` (`* text=auto eol=lf`). Use LF line endings for all source code (`.java`, `.js`, `.jsx`, `.sql`, `.yml`, `.md`).

---

## 3. Architecture & Code Hygiene

* **Zero Duplicate Directories:**
  * The active frontend is located **exclusively** at [`frontend/focotech-react/`](frontend/focotech-react).
  * Never create or restore root-level copies like `focotech-react/` or loose prototype dumps.
* **Minimal, Targeted Edits:**
  * Change only files directly related to the user's explicit prompt.
  * Preserve existing comments, docstrings, and unrelated configuration blocks.
* **Pre-Verification Before Reporting Completion:**
  * If frontend files were modified, run `npm run build` in `frontend/focotech-react/` to verify zero build errors.
  * If backend files were modified, run `mvn test -B` in `backend/` to verify zero test regressions.

---

## 4. Backend & Database Constraints

* **Tech Stack:** Spring Boot 3.3.4, Java 21, Maven, Eclipse Temurin.
* **Database Engine:** Neon Serverless PostgreSQL (`neondb`).
* **Schema Governance:**
  * `spring.jpa.hibernate.ddl-auto=validate` is strictly enforced.
  * Hibernate must **NEVER** auto-create or alter tables at runtime.
  * All table, index, and seed changes must be written as explicit SQL in [`db/01-schema.sql`](db/01-schema.sql), [`db/02-indexes.sql`](db/02-indexes.sql), and [`db/03-seed.sql`](db/03-seed.sql).
* **Spring Data JPA Relationships:**
  * When navigating entity relationships (e.g., `Lecture.course.id`), avoid relying on Spring Data derived method names that fail on property traversals. Use explicit `@Query("SELECT ...")` JPQL annotations instead.
* **Docker Multi-Stage Integrity:**
  * Ensure `backend/Dockerfile` and `frontend/focotech-react/Dockerfile` build cleanly without depending on local host paths.

---

## 5. Security & Secret Protection

* **Secret Detection:** The repository enforces automated **Gitleaks** secret scanning in CI. Any commit containing API keys, private keys, database passwords, or base64-encoded JWT secrets will be blocked.
* **JWT Secret:** Use only high-entropy, 256-bit base64 strings passed via environment variables (`JWT_SECRET`), never hardcoded in source code.

