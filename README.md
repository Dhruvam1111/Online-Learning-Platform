# Online Learning Platform (OLP)

A full-stack e-learning platform with distinct, role-based workflows for **Students**, **Instructors** and **Administrators** — covering course authoring, video delivery, assessment and grading, certification, and a complete payments and financial-governance  layer.

---

## 📑 Contents

1. [Tech Stack](#️-tech-stack)
2. [Implemented Functionality](#-implemented-functionality)
3. [Benefits](#-benefits)
4. [Architecture](#-architecture)
5. [Project Structure](#-project-structure)
6. [Getting Started](#-getting-started)
7. [Test Credentials](#-test-credentials)
8. [Testing](#-testing)
9. [Performance & Load Testing](#-performance--load-testing)
10. [CI/CD Pipeline](#-cicd-pipeline)
11. [Security](#-security)
12. [Future Scope](#-future-scope)
13. [Documentation Index](#-documentation-index)
14. [Contributing Conventions](#-contributing-conventions)

---

## 🛠️ Tech Stack

| Layer | Technology |
| --- | --- |
| **Frontend** | React 19, Vite 8, Tailwind CSS 3.4, Lucide Icons, Axios, React Context API |
| **Backend** | Spring Boot 3.3.4, Java 17, Spring Security, Spring Data JPA, Spring Mail, Spring Boot Actuator |
| **Auth** | Stateless JWT (JJWT 0.12.x), BCrypt password hashing, email OTP two-factor |
| **Database** | Neon Serverless PostgreSQL (`neondb`); local fallback via Docker Compose |
| **Payments** | Razorpay, Stripe, UPI QR (ZXing), plus a deterministic simulation gateway for testing |
| **Testing** | JUnit 5, Mockito, Spring Test (MockMvc), JaCoCo |
| **CI/CD** | GitHub Actions — Gitleaks secret scanning, Maven tests against a live PostgreSQL service, Vite build, Docker buildx |
| **Containers** | Docker & Docker Compose (multi-stage builds for both services) |

> Versions above are read from [`backend/pom.xml`](backend/pom.xml) and [`frontend/focotech-react/package.json`](frontend/focotech-react/package.json). Keep them in sync when upgrading.

---

## ✅ Implemented Functionality

Current scale: **52 REST controllers · 79 services · 66 JPA entities · 50 repositories · 158 DTOs · 93 test classes running 1,407 passing tests.**

### 1. Authentication, Identity & Access Control

- Stateless JWT authentication (`/api/auth/login`, `/api/auth/register`, `/api/auth/me`) with BCrypt-hashed credentials.
- Role-gated endpoints enforced by Spring Security across three roles: `student`, `instructor`, `admin`.
- Account lifecycle enforcement — `active`, `pending`, `suspended`, `rejected` — with suspended accounts revoked at the filter layer on their next request.
- **Email OTP two-factor authentication** with expiry, attempt throttling and single-use token invalidation.
- Password reset via expiring one-time emailed links, invalidating prior sessions.
- Token blacklisting for logout and forced revocation.

### 2. Course Authoring (Instructor)

- Full course lifecycle: `DRAFT` → `PENDING_REVIEW` → `PUBLISHED` → `ARCHIVED`, with draft saves that skip completeness validation.
- Hierarchical structure — **Course → Module → Section → Lesson** — with ordered indices and drag-reorder support at every level.
- Lecture management with ordered video lectures, YouTube video-ID extraction, and resumable chunked uploads.
- **Server-side video transcoding** driven by a webhook callback pipeline, plus caption parsing.
- Supplementary material uploads (PDFs, resources) with virus-scanning hooks and private storage.
- **Scheduled content release** so modules unlock on a date rather than all at once.
- Content declarations and compliance metadata per course.
- Multi-instructor courses via association requests, with admin approval and auto-approval for creators.
- Instructor profiles: qualifications, expertise areas, bio, profile picture and private credential documents.

### 3. Student Learning Experience

- Course catalog with search, syllabus trees, lecture counts and course previews.
- Enrollment and instant access, with prerequisite data available for enrollment checks.
- Video playback with **per-lecture progress tracking and playback-position resume**.
- Course-level progress aggregation and completion detection.
- Bookmarks at both course and lecture level.
- Reviews and star ratings, with instructor-visible aggregates.
- Student profile management and **daily streak tracking**.
- **Certificate issuance** on course completion, with instructor-side certificate management.

### 4. Assessment & Grading

- **Quiz authoring** with six question types: `MULTIPLE_CHOICE`, `MULTIPLE_SELECT`, `TRUE_FALSE`, `NUMERICAL`, `DESCRIPTIVE`, `SHORT_ANSWER`.
- A **reusable, versioned question bank** (`version` / `parent_question_id` / `is_latest`) filterable by topic, tag, difficulty, type and origin.
- Configurable **marking rules**: per-question marks, negative marking, partial marking, numeric tolerance, and per-question correction policies.
- Server-side attempt tracking with time limits, expiry, attempt caps and availability windows — timing is never trusted to the client.
- **Auto-grading** for objective types; **descriptive grading** with rubrics and a manual-grading queue.
- Configurable answer-release timing (`IMMEDIATELY` or deferred), correct-answer visibility and feedback toggles.
- **Exams** as a distinct flow: blueprints, question randomization, monitoring configuration and correction policies.
- **Assignments** with submissions and late-submission policy enforcement.
- Pending-grading alerts for instructors, and student-facing result history.

### 5. Payments & Financial Governance

The largest subsystem, built for auditability rather than just checkout.

- Course pricing, multi-currency support and coupon/discount handling with usage limits.
- Checkout through **Razorpay** and **Stripe**, plus **UPI QR** generation and a deterministic **simulation gateway** for tests and demos.
- Signature-verified payment webhooks with authenticated handlers and replay protection.
- Order expiration and scheduled payment-maintenance jobs.
- **Subscriptions** alongside one-time purchases.
- Student **wallet** with transaction ledger.
- Refunds: standard, exceptional, and royalty-aware refund policies.
- **Instructor payouts** with bank-account management.
- Invoices and financial documents; **reconciliation**, payment audit trails, payment evidence capture and fraud-detection hooks.
- Financial governance and policy services gating what each role may do.

### 6. Administration

- Platform-wide user management, instructor onboarding approval/rejection, and course status auditing.
- Payment governance, policy configuration and reconciliation oversight.

---

## 💡 Benefits

### For Students

| Benefit | How it is delivered |
| --- | --- |
| **No lost progress** | Playback position and per-lecture progress persist server-side, so learning resumes on any device |
| **Honest assessment** | Attempt timing, expiry and attempt caps are enforced on the server, so results mean something |
| **Immediate feedback** | Auto-grading returns objective results instantly, with explanations where the instructor allows it |
| **Verifiable achievement** | Certificates are issued on genuine completion, not on enrollment |
| **Payment confidence** | Wallet ledger, invoices and a clear refund path — including exceptional cases |
| **Flexible access** | One-time purchase or subscription, multi-currency, coupons, and UPI for Indian learners |
| **Motivation** | Daily streaks, bookmarks and visible progress keep sessions short and repeatable |

### For Instructors

| Benefit | How it is delivered |
| --- | --- |
| **Author once, reuse forever** | The versioned question bank means questions outlive the quiz they were written for |
| **Grading time reclaimed** | Objective questions self-grade; descriptive answers arrive in a rubric-driven queue with alerts |
| **Fine-grained assessment control** | Negative marking, partial credit, numeric tolerance and release timing are all per-quiz settings |
| **Paced course delivery** | Scheduled release prevents binge-and-abandon and keeps cohorts together |
| **Co-teaching** | Multiple instructors per course via a reviewed association flow |
| **Transparent earnings** | Payouts, royalty-aware refunds and bank-account management are first-class, not spreadsheets |
| **Content protection** | Credential and material files are stored privately with scanning hooks |

### For Administrators

| Benefit | How it is delivered |
| --- | --- |
| **Gatekeeping that scales** | Instructor approval and course review are explicit, auditable states |
| **Financial defensibility** | Reconciliation, audit trails, payment evidence and fraud hooks mean disputes can be answered with records |
| **Policy as configuration** | Refund, payment and governance policies are services, not hardcoded branches |
| **Incident containment** | Account suspension takes effect at the next request via filter-level revocation |

### Engineering Benefits

| Benefit | How it is delivered |
| --- | --- |
| **Schema is governed, not guessed** | `ddl-auto=validate` means Hibernate can never silently alter production; every change is reviewed SQL in [`db/`](db/) |
| **High regression safety** | 1,407 tests across 93 classes covering services, controllers, security filters and error handling |
| **Secrets cannot leak by accident** | Gitleaks runs in CI on every push; the JWT secret comes only from the `JWT_SECRET` environment variable |
| **Deployable anywhere** | Multi-stage Dockerfiles for both services, with no dependency on local host paths |
| **Fast CI** | Path-filtered jobs mean a frontend-only change does not pay for the backend test suite |
| **Readable contracts** | Controllers and DTOs are the API source of truth; errors are centralised in [`exception/`](backend/src/main/java/com/olp/exception/) |

---

## 🏗 Architecture

```
┌────────────────────────────┐
│  React 19 + Vite (SPA)     │   Axios client, Context-based auth,
│  frontend/focotech-react   │   token persistence, role-based routing
└─────────────┬──────────────┘
              │  HTTPS / JSON  (Vite dev proxy → :8080/api)
┌─────────────▼──────────────┐
│  Spring Boot 3.3.4 REST    │
│                            │
│  Controller  (52)  ← role-gated entry points, DTO mapping
│  Service     (79)  ← business rules, policies, grading, payments
│  Repository  (50)  ← Spring Data JPA, explicit @Query JPQL
│  Entity      (66)  ← maps to governed SQL schema
│                            │
│  Security: JWT filter → account-status revocation → RBAC
└─────────────┬──────────────┘
              │  JDBC (SSL)
┌─────────────▼──────────────┐
│  Neon Serverless Postgres  │   Schema owned by db/*.sql
│  (ddl-auto = validate)     │   Hibernate validates, never migrates
└────────────────────────────┘

External: Razorpay · Stripe · UPI QR · SMTP (OTP & resets) · video transcoder webhooks
```

**Key design rules**

- **The database schema is owned by SQL, not by Hibernate.** `spring.jpa.hibernate.ddl-auto=validate` is enforced; the application fails to start if entities and schema disagree.
- **Relationship traversals use explicit `@Query` JPQL.** Derived Spring Data method names break on property traversals such as `Lecture.course.id`.
- **Server is the source of truth for anything that can be cheated** — quiz timing, attempt counts, marks, payment state.

---

## 📂 Project Structure

```text
├── backend/                       # Spring Boot 3 REST API
│   ├── src/main/java/com/olp/     # controller, service, repository, entity, dto, security, exception, util
│   ├── src/test/java/com/olp/     # 93 test classes (JUnit 5, Mockito, MockMvc)
│   ├── src/main/resources/        # application.properties (local only) + .example
│   ├── Dockerfile                 # multi-stage build
│   └── pom.xml
│
├── frontend/focotech-react/       # React 19 + Vite SPA (the ONLY active frontend)
│   ├── src/components/            # 26 components: dashboards, course, auth, payments, landing
│   ├── src/context/               # AuthContext (global state & token persistence)
│   ├── src/services/              # Axios API client & interceptors
│   ├── Dockerfile
│   └── package.json
│
├── db/                            # PostgreSQL schema — the single source of truth
│   ├── 01-schema.sql              # tables and constraints
│   ├── 02-indexes.sql             # performance indexes
│   ├── 03-seed.sql                # seed users, courses, lectures
│   ├── 04-*.sql                   # payment & enterprise auth schema
│   ├── migrations/                # dated incremental migrations
│   └── drop-all.sql               # reset script (development only)
│
├── Documentation/                 # Specs, reviews, guides — see index below
├── .github/workflows/             # ci.yml, cd.yml
└── docker-compose.yml             # local PostgreSQL container
```

---

## 🏁 Getting Started

### Prerequisites
- **Java 17+** & **Maven**
- **Node.js 18+** & **npm**
- **PostgreSQL** (via Neon Cloud or local Docker)

---

### 1. Database Setup

#### Option A: Neon Cloud PostgreSQL (Recommended)
Set your Neon connection credentials in `backend/src/main/resources/application.properties` (or export environment variables — see [`application.properties.example`](backend/src/main/resources/application.properties.example)):

```properties
spring.datasource.url=jdbc:postgresql://<neon-host>/neondb?sslmode=require
spring.datasource.username=<neon-user>
spring.datasource.password=<neon-password>
spring.jpa.hibernate.ddl-auto=validate
```

Run `db/01-schema.sql`, `db/02-indexes.sql`, and `db/03-seed.sql` in Neon's SQL Editor (or via test runner), then apply anything in `db/migrations/` in filename order.

> `application.properties` holds live credentials and is **never** committed. Only the `.example` file is tracked.

#### Option B: Local PostgreSQL (Docker)
```bash
docker compose up -d
```
Runs PostgreSQL at `localhost:5432/online_learning` (`postgres` / `postgres`).

---

### 2. Run the Backend
```bash
cd backend
mvn spring-boot:run
```
The REST API will start on **`http://localhost:8080`**.

The `JWT_SECRET` environment variable must be set to a high-entropy 256-bit base64 string.

---

### 3. Run the Frontend
```bash
cd frontend/focotech-react
npm install
npm run dev
```
The Vite development server will start on **`http://localhost:3000`** with proxying to `http://localhost:8080/api`.

---

## 🔑 Test Credentials

All pre-seeded test accounts use the password: **`password123`**

| Role | Name | Email (Login ID) | Notes |
|---|---|---|---|
| **Admin** | Platform Admin | `admin@olp.com` | User management & course approval |
| **Instructor** | Dr. Sarah Chen | `sarah@instructor.com` | Owns Web Dev & Database courses |
| **Instructor** | Prof. James Wilson | `james@instructor.com` | Owns Python & ML courses |
| **Student** | Alice Johnson | `alice@student.com` | Enrolled student |
| **Student** | Bob Martinez | `bob@student.com` | Enrolled student |

---

## 🧪 Testing

### Automated test suite

**93 test classes · 1,407 test cases · 0 failures, 0 errors, 0 skipped** (measured 2026-11-14, `mvn clean test jacoco:report -B`, full run ≈ 65 s).

Spanning five layers:

| Layer | What is tested | Examples |
| --- | --- | --- |
| **Service (unit)** | Business rules in isolation with Mockito | `AutoGradingServiceTest`, `ExamServiceTest`, `CoursePricingServiceTest`, `DescriptiveGradingServiceTest` |
| **Controller (MockMvc)** | Request/response contracts, status codes, payload shapes | `StudentQuizControllerTest`, `QuestionControllerMockMvcTest`, `AdminControllerTest` |
| **Security** | RBAC, JWT validation, method-level security, account revocation | `RbacSecurityTest`, `JwtAuthenticationFilterTest`, `InstructorAuthorizationTest`, `PaymentControllerMethodSecurityTest` |
| **Branch coverage** | Error paths and edge branches explicitly | `ExamServiceBranchTest`, `AssignmentServiceBranchTest`, `JwtAuthenticationFilterBranchTest` |
| **Error handling** | Centralised exception → HTTP mapping | `ApiErrorHandlingMockMvcTest` |

### Running tests

```bash
# Backend — required if any backend file changed
cd backend && mvn test -B

# Backend with coverage report (HTML at target/site/jacoco/index.html)
cd backend && mvn clean test jacoco:report -B

# Backend compile only (fast sanity check)
cd backend && mvn compile -B

# Frontend — required if any frontend file changed
cd frontend/focotech-react && npm run build
```

### Coverage

JaCoCo 0.8.12 is configured in [`backend/pom.xml`](backend/pom.xml). Current measured coverage across 188 analysed classes:

| Metric | Coverage | Covered / Total |
| --- | --- | --- |
| Instruction | **59%** | 36,057 of 60,469 |
| Branch | **48%** | 3,628 of 7,509 |
| Line | **67%** | 7,907 of 11,709 |
| Method | **57%** | 1,016 of 1,768 |

*Measured 2026-11-14. Regenerate with `mvn clean test jacoco:report -B`; the HTML report lands at `backend/target/site/jacoco/index.html`.*

> **On the earlier 94% figure.** A locally maintained coverage report records a dedicated testing effort that moved instruction coverage from 13% → 94% as of 2026-11-08. That measurement was accurate for the codebase of its time (6,013 instructions). The codebase has since grown roughly tenfold to 60,469 instructions — chiefly the payments, exam and financial-governance subsystems — and the test suite has not kept pace proportionally, so the honest current figure is 59%. The older report should be read as a historical milestone, not as the present state.

**Coverage gap — where new tests are most needed:** the payments and financial-governance packages carry the largest untested surface, and branch coverage (48%) trails line coverage (67%), indicating error and edge paths are thinner than happy paths.

### Testing conventions

- Service tests must not require a database — mock the repository.
- Every new endpoint needs a controller test asserting both the success path and the authorisation failure.
- Every new policy or marking rule needs a branch test for each outcome.

---

## 📈 Performance & Load Testing

> **Status: planned, not yet implemented.** No load-testing tooling is currently present in the repository. This section defines the intended approach so it can be built as specified rather than improvised. Do not cite these numbers as measured results.

### Why it matters here

Neon is serverless, which means connection establishment and cold starts behave differently from a long-running Postgres instance. Three parts of this platform are the realistic pressure points:

1. **Quiz submission bursts** — an entire cohort submitting within the same minute, each submission writing multiple `quiz_attempt_answers` rows inside one transaction.
2. **Video progress writes** — `lecture_progress` updates fire continuously during playback across all active learners.
3. **Payment webhooks** — provider retries can arrive concurrently and must remain idempotent under duplication.

### Proposed tooling

**k6** (scriptable, CI-friendly, low footprint) with scenarios committed under `perf/`. Spring Boot Actuator is already a dependency, so `/actuator/metrics` and `/actuator/health` can be scraped during runs without new instrumentation.

### Scenario matrix

| # | Scenario | Shape | What it proves |
| --- | --- | --- | --- |
| 1 | **Catalog browse** | 200 VUs ramped over 2 min, sustained 5 min | Read-path and query-index health under normal load |
| 2 | **Quiz submission storm** | 500 VUs submitting within 60 s | Transactional write throughput; attempt-row contention |
| 3 | **Progress write flood** | 1,000 VUs, one `lecture_progress` write every 10 s for 10 min | Sustained write amplification; connection-pool saturation |
| 4 | **Auth + OTP** | 100 VUs login, 50 requesting OTP | BCrypt CPU cost and SMTP backpressure |
| 5 | **Webhook duplication** | Same payment webhook delivered 10× concurrently | Idempotency and signature-verification correctness under race |
| 6 | **Soak** | 50 VUs mixed traffic for 2 h | Connection and memory leaks; Neon reconnection behaviour |
| 7 | **Spike** | 0 → 800 VUs in 10 s | Cold-start and pool-exhaustion recovery |

### Target thresholds (to be validated, then enforced in CI)

| Metric | Target |
| --- | --- |
| Catalog read p95 | < 300 ms |
| Quiz submission p95 | < 800 ms |
| Progress write p95 | < 200 ms |
| Error rate, all scenarios | < 0.1% |
| Soak test memory growth | < 5% over 2 h |
| Webhook duplicate handling | 100% idempotent, zero double-credits |

### Correctness under load

Load testing here is not only about latency. Each scenario must also assert that **no invariant breaks under concurrency**:

- No student exceeds `max_attempts` when submitting in parallel.
- No duplicate wallet credit from a replayed webhook.
- No `quiz_attempt_answers` row orphaned by a half-committed submission.
- Marks totals remain consistent with the per-answer records.

### Also planned

- JMeter equivalents for teams that prefer a GUI.
- Database-level `EXPLAIN ANALYZE` review of the heaviest catalog and progress queries.
- A CI performance smoke job (short, threshold-gated) distinct from the full load suite.

---

## 🔄 CI/CD Pipeline

Defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml) and [`.github/workflows/cd.yml`](.github/workflows/cd.yml).

### Continuous Integration

| Job | Purpose |
| --- | --- |
| **Filter / Path Detection** | `dorny/paths-filter` decides which downstream jobs are needed, so unaffected suites are skipped |
| **Security / Gitleaks Secret Scan** | Blocks any commit containing API keys, private keys, DB passwords or base64 JWT secrets |
| **Frontend / Typecheck & Vite Build** | `npm ci`, `npm run build`, plus a non-blocking `npm audit --audit-level=high` |
| **Backend / PostgreSQL & Maven Tests** | Spins up a PostgreSQL service container and runs the full Maven test suite |
| **Containers / Docker Build Check** | Builds both Dockerfiles via buildx to catch host-path dependencies |
| **CI / Quality Gate** | Aggregates results into a single required status |

### Continuous Deployment

Publishes frontend and backend container images (buildx + metadata tagging) and dispatches a cloud rollout, component-filtered so only changed services redeploy.

---

## 🔐 Security

- **Secrets never enter Git.** `application.properties`, `.env*` and personal notes are untracked; Gitleaks enforces this in CI.
- **JWT secret from environment only** — a high-entropy 256-bit base64 value in `JWT_SECRET`, never hardcoded.
- **Passwords** are BCrypt-hashed; reset links are single-use and expiring.
- **Two-factor** via email OTP with expiry, attempt limits and invalidation on use.
- **Authorization is layered**: JWT validity → account status revocation → role check → ownership check.
- **Payment webhooks** are signature-verified before any state change.
- **Uploads** are type- and size-validated, virus-scan-hooked, and credential documents are stored privately.
- **Database access** is SSL-only (`sslmode=require`) against Neon.

---

## 🔮 Future Scope

### Near term

| Feature | Notes |
| --- | --- |
| **Adaptive AI quizzes** | Per-student question selection using weighted sub-topic tags, Elo mastery estimation, half-life retention modelling and a two-stage retrieval/ranking funnel. A full design specification is maintained locally (`Documentation/ADAPTIVE_AI_QUIZ_DESIGN.md`) |
| **Load & stress test suite** | Build the k6 scenarios described above and gate CI on the thresholds |
| **Notification service** | Centralised in-app and email notifications; currently email is sent per-feature |
| **Personalised course recommendations** | A real recommender behind the existing recommended-courses surface |
| **API contract documentation** | OpenAPI/Swagger generation, so controllers stop being the only contract |

### Medium term

| Feature | Notes |
| --- | --- |
| **AI question generation** | LLM-authored questions targeting a student's weak sub-topic combinations, instructor-approved before use |
| **AI misconception explanations** | After a wrong answer, name the likely misconception instead of only revealing the correct option |
| **Instructor analytics** | Cohort mastery heatmaps, drop-off points per lecture, question-level difficulty calibration |
| **Live sessions** | Scheduled classes, recordings and attendance |
| **Discussion forums** | Per-course and per-lecture Q&A with instructor endorsement |
| **Mobile applications** | React Native clients reusing the existing REST API |
| **SCORM / xAPI export** | The course structure was designed to permit this |

### Long term

| Feature | Notes |
| --- | --- |
| **Learning-path generation** | Multi-course personalised paths derived from the sub-topic prerequisite graph |
| **Proctoring** | Extend the existing exam-monitoring configuration into real integrity signals |
| **Accessibility certification** | Full WCAG audit and remediation |
| **Internationalisation** | UI translation on top of existing multi-currency support |
| **Marketplace features** | Instructor storefronts, bundles, affiliate attribution |

---

## 📚 Documentation Index

| Document | Contents |
| --- | --- |
| [`Documentation/DB-README.md`](Documentation/DB-README.md) | Database guide and schema relationships |
| [`Documentation/README-Payment-Simulation.md`](Documentation/README-Payment-Simulation.md) | How the deterministic payment simulation gateway works |
| [`AGENTS.md`](AGENTS.md) | Mandatory rules and guardrails for AI coding assistants |

Further specifications — the adaptive AI quiz design, test coverage report, codebase functionality review, role-based functionality matrix, architecture guide, deployment comparison and per-person sprint specs — are maintained **locally only** and are not tracked in version control. They are deliberately not linked here, because the links would not resolve for anyone cloning the repository.

---

## 🤝 Contributing Conventions

- **Branches:** develop on feature branches (`pranamya`, `feature/<name>`). Never commit directly to `main`.
- **Staging:** stage explicit file paths only. Never `git add .` or `git add -A`.
- **Schema:** all table, index and seed changes go in `db/01-schema.sql`, `db/02-indexes.sql`, `db/03-seed.sql`, plus a dated file in `db/migrations/`. Hibernate must never create or alter tables.
- **JPA:** use explicit `@Query` JPQL for relationship traversals.
- **Line endings:** LF for all source files, per [`.gitattributes`](.gitattributes).
- **Before opening a PR:** `mvn test -B` if backend changed, `npm run build` if frontend changed.
- **Never commit:** `application.properties`, `.env*`, `Notes - personal/`, or IDE/build artifacts.

---

## 📄 License

See repository settings for licensing terms.
