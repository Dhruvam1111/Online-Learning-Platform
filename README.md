# Online Learning Platform

A full-stack online learning platform built for speed, simplicity, and performance. 

## 🚀 Sprint 1 Objective
The goal of Sprint 1 is to build the core end-to-end functionality of the platform, enabling two distinct user roles: **Instructors** and **Students**.

### Key Features (Sprint 1)
- **Role-based Authentication:** Secure JWT-based login and registration for Students and Instructors.
- **Instructor Dashboard:** Instructors can create courses, add YouTube-based lectures, manage course status (draft/published), and organize their curriculum.
- **Student Experience:** Students can browse published courses, enroll instantly (free for Sprint 1), and consume lecture content through an integrated YouTube player.
- **Premium UI:** A custom-designed frontend using React (Vite) and Vanilla CSS, adhering to strict design guidelines (solid colors, Inter font, responsive layouts).

## 🛠️ Tech Stack
- **Frontend:** React 18, Vite, Vanilla CSS
- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT)
- **Database:** PostgreSQL (with Docker Compose)
- **Deployment:** Render (Web Service & Managed Postgres)

## 📂 Project Structure Overview
- `/db` — PostgreSQL database schema, indexes, and seed scripts.
- `/task-assignments` — Detailed implementation guides and API contracts for all 6 team members.
- `docker-compose.yml` — One-click local database setup.

## 🏁 Getting Started

### 1. Database Setup
Ensure Docker is installed and running, then spin up the seeded local database:
```bash
docker compose up -d
```
*See [DB-README.md](./DB-README.md) for test credentials and schema diagrams.*

### 2. Backend Setup
*(Instructions will be added as the backend is developed by the team)*

### 3. Frontend Setup
*(Instructions will be added as the frontend is developed by the team)*
