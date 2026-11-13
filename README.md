# Online Learning Platform (OLP)

A modern full-stack e-learning platform providing distinct, role-based workflows for **Students**, **Instructors**, and **Administrators**.

---

## 🛠️ Tech Stack

- **Frontend:** React 19, Vite, Tailwind CSS, Lucide Icons, Axios, React Context API
- **Backend:** Java 25, Spring Boot 3.5.16, Spring Security, Spring Data JPA, JWT (JJWT 0.12.5)
- **Database:** PostgreSQL (Cloud deployment on **Neon Serverless PostgreSQL**; local fallback via Docker Compose)
- **Containerization:** Docker & Docker Compose

---

## 🚀 Key Modules & Architecture

### 1. Authentication & Role-Based Access Control (RBAC)
- Stateless JWT-based authentication (`/api/auth/login`, `/api/auth/register`, `/api/auth/me`).
- Role-gated endpoints with Spring Security (`student`, `instructor`, `admin`).
- Account lifecycle enforcement (`active`, `pending`, `suspended`, `rejected`).

### 2. Student Experience
- **Course Catalog & Search:** Browse and search published courses with syllabus trees and lecture counts.
- **Enrollment & Learning:** Instant course enrollment, video playback via embedded YouTube player, and lecture progress tracking (`/api/progress`).
- **Profile & Engagement:** Student profile management, bookmarking courses, reviews/ratings, and daily streak tracking.

### 3. Instructor Portal
- **Course Management:** Create, edit, and organize draft/published courses.
- **Lecture Management:** Add, update, and reorder video lectures (`order_index`) with video ID extraction.
- **Instructor Profile:** Qualifications, bio, and expertise areas.

### 4. Admin Governance
- Oversee platform users, handle instructor onboarding approvals, and audit course status changes.

---

## 📂 Project Structure

```text
├── backend/                       # Spring Boot 3 REST API
│   ├── src/main/java/com/olp/     # Controllers, Services, Repositories, Entities, Security
│   ├── src/main/resources/        # application.properties
│   └── pom.xml                    # Maven configuration
│
├── frontend/focotech-react/       # React 19 + Vite frontend
│   ├── src/components/            # Dashboard, Course, Auth, and Landing UI components
│   ├── src/context/               # AuthContext (global state & token persistence)
│   ├── src/services/              # Axios API client & interceptors
│   └── package.json
│
├── db/                            # PostgreSQL SQL migrations
│   ├── 01-schema.sql              # Table definitions and constraints
│   ├── 02-indexes.sql             # Performance indexes
│   ├── 03-seed.sql                # Seed data (users, courses, lectures)
│   └── drop-all.sql               # Reset script (development only)
│
├── Documentation/                 # Requirements, sprint specs, and design documents
│   └── DB-README.md               # Detailed database guide & schema relationships
│
├── Images - reference - frontend/ # UI mockups and architectural wireframe references
│
└── docker-compose.yml             # Local PostgreSQL container configuration
```

---

## 🏁 Getting Started

### Prerequisites
- **Java 25+** & **Maven**
- **Node.js 18+** & **npm**
- **PostgreSQL** (via Neon Cloud or local Docker)

---

### 1. Database Setup

#### Option A: Neon Cloud PostgreSQL (Recommended)
Set your Neon connection credentials in `backend/src/main/resources/application.properties` (or export environment variables):

```properties
spring.datasource.url=jdbc:postgresql://<neon-host>/neondb?sslmode=require
spring.datasource.username=<neon-user>
spring.datasource.password=<neon-password>
spring.jpa.hibernate.ddl-auto=validate
```

Run `db/01-schema.sql`, `db/02-indexes.sql`, and `db/03-seed.sql` in Neon's SQL Editor (or via test runner).

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
