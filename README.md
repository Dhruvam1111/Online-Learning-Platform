# Online Learning Platform

A full-stack online learning platform built for speed, simplicity, and performance. 

## 🚀 Sprint 1 Objective
The goal of Sprint 1 is to build the core end-to-end functionality of the platform, enabling two distinct user roles: **Instructors** and **Students**.

### Key Features (Sprint 1)
- **Role-based Authentication:** Secure JWT-based registration and login for Students and Instructors, with Google OAuth2 social sign-in support.
- **Instructor Dashboard:** Instructors can create courses, add YouTube-based lectures, manage course status (draft/published), and organize their curriculum.
- **Student Experience:** Students can browse published courses, enroll instantly (free for Sprint 1), and consume lecture content through an integrated YouTube player.
- **Premium UI:** A custom-designed frontend using React (Vite) and Vanilla CSS, adhering to strict design guidelines (solid colors, Inter font, responsive layouts).

## 🛠️ Tech Stack
- **Frontend:** React 18, Vite, Vanilla CSS
- **Backend:** Java 17 / 21, Spring Boot 3, Spring Security 6 (JWT & Google OAuth2)
- **Database:** PostgreSQL 16 (with Docker Compose)
- **Deployment:** Render (Web Service & Managed Postgres)

## 📂 Project Structure Overview
- `/backend` — Spring Boot 3 RESTful authorization service (JWT, Google OAuth, PostgreSQL JPA).
- `/db` — PostgreSQL database schema, indexes, and seed scripts.
- `/Documentation` — Project requirements, user stories, and guides.
- `docker-compose.yml` — One-click local database setup.

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
