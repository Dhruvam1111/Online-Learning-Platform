# Online Learning Platform

A full-stack, high-performance online learning platform built for speed, simplicity, and modern developer experience. 

---

## 📌 Project Progress & Sprint Tracking (Notion)

We use **Notion** for agile sprint management, tracking user stories, task assignments, blockers, and PR reviews across all team members.

- 🔗 **Sprint 1 Progress Board:** [Online Learning Platform — Sprint 1 Board (Notion)](https://www.notion.so/Online-Learning-Platform-Sprint-1-Progress-Board)
- 📖 **New to Notion?** Read the step-by-step setup and usage guide in [Documentation/NOTION_GUIDE.md](./Documentation/NOTION_GUIDE.md).

> **Note for Contributors:** Keep the Notion board updated daily as you pick up tasks, submit PRs, and complete features. To update the board link, edit this section in `README.md`.

---

## 🚀 Sprint 1 Objective
The goal of Sprint 1 is to build the core end-to-end functionality of the platform, enabling two distinct user roles: **Instructors** and **Students**.

### Key Features (Sprint 1)
- **Role-based Authentication:** Secure JWT-based registration and login for Students and Instructors, with Google OAuth2 social sign-in support.
- **Instructor Dashboard:** Instructors can create courses, add YouTube-based lectures, manage course status (`draft` / `published`), and organize their curriculum.
- **Student Experience:** Students can browse published courses, enroll instantly (free for Sprint 1), and consume lecture content through an integrated YouTube player.
- **Modern UI:** A custom-designed frontend using React 18, Vite, and Vanilla CSS, adhering to clean design principles (solid colors, Inter font, responsive layouts).

---

## 🛠️ Tech Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 18, Vite, Vanilla CSS |
| **Backend** | Java 17 / 21, Spring Boot 3, Spring Security 6, JJWT (io.jsonwebtoken 0.12.6) |
| **Authentication** | JWT (Stateless Token Authentication) & Google OAuth2 ID Token Verification |
| **Database** | PostgreSQL 16 (orchestrated with Docker Compose) |
| **Project Tracking** | Notion Kanban Board & GitHub Projects |
| **Deployment** | Render (Web Service & Managed Postgres) |

---

## 📂 Project Structure Overview

```
Online-Learning-Platform/
├── .github/workflows/       # GitHub Actions CI pipelines (Database CI, Build)
├── backend/                 # Spring Boot 3 REST API & Authentication Service
│   ├── src/main/java/       # Controllers, Services, Security, Entities, DTOs
│   ├── src/test/java/       # Automated test suite (MockMvc, JWT tests)
│   ├── pom.xml              # Maven configuration
│   └── README.md            # Backend API specifications & cURL examples
├── db/                      # PostgreSQL database scripts
│   ├── 01-schema.sql        # Database schema definitions
│   ├── 02-indexes.sql       # Query performance indexes
│   ├── 03-seed.sql          # Seed data with test credentials
│   └── drop-all.sql         # Clean slate reset script
├── Documentation/           # Requirements, User Stories, and Setup Guides
│   ├── FRs, NFRs and Domain Requirements.pdf
│   ├── Online_Learning_Platform_User_Stories.xlsx
│   └── NOTION_GUIDE.md      # Comprehensive Notion onboarding & sprint tracking guide
├── docker-compose.yml       # Local PostgreSQL container definition
├── DB-README.md             # Database credentials and schema diagrams
└── README.md                # Project documentation and getting started guide
```

---

## 🏁 Getting Started

### 1. Database Setup
Ensure Docker is installed and running, then spin up the seeded local database:
```bash
docker compose up -d
```
*See [DB-README.md](./DB-README.md) for test credentials and schema diagrams.*

### 2. Backend Setup
Navigate into the `backend/` directory and run using the Maven wrapper:

**On Windows:**
```cmd
cd backend
mvnw.cmd spring-boot:run
```

**On Linux/macOS:**
```bash
cd backend
./mvnw spring-boot:run
```

The backend server starts on port `8080`.
Verify it is running:
```bash
curl http://localhost:8080/api/health
```

*See [backend/README.md](./backend/README.md) for full API endpoint documentation, Google OAuth configuration, and test suites.*

### 3. Frontend Setup
*(Instructions will be added as the frontend is developed by the team)*

---

## 🤝 Contribution Guidelines

1. **Branch Naming:**
   - Feature branches: `<name>/<feature-name>` or `<pair-names>/<feature-name>` (e.g., `dhruvam-shubh/authorization-backend`, `dhruvam/documentation`).
2. **Co-authoring Commits:**
   - When collaborating in pairs, include the co-author trailer at the end of commit messages:
     ```
     Co-authored-by: Contributor Name <contributor-email@dau.ac.in>
     ```
   - GitHub will automatically award contribution credit and display both avatars.
3. **Pull Requests:**
   - Always open a PR against `main`.
   - Link the relevant Notion task card and describe the changes made.
