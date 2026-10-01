# Online Learning Platform — Backend Service

Spring Boot 3 RESTful authentication backend service supporting **JWT Authentication** and **Google OAuth2** for Students and Instructors.

---

## 🛠️ Tech Stack
- **Framework:** Java 17 / 21, Spring Boot 3.3.4
- **Security:** Spring Security 6, JJWT (io.jsonwebtoken 0.12.6)
- **OAuth:** Google API Client & OAuth2 ID Token Verification
- **Persistence:** Spring Data JPA, Hibernate, PostgreSQL
- **Build Tool:** Apache Maven (with included `mvnw` wrapper)

---

## 🚀 Getting Started

### 1. Start PostgreSQL Database
From the project root:
```bash
docker compose up -d
```
The database will be initialized with the schema (`db/01-schema.sql`) and sample seed accounts (`db/03-seed.sql`).

### 2. Run the Backend Service
Navigate to the `backend` folder and run using the Maven wrapper:

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

The server starts at `http://localhost:8080`.

---

## 🔑 Authentication Endpoints

### 1. Health Check
- **URL:** `GET /api/health`
- **Access:** Public
- **Response (`200 OK`):**
  ```json
  {
    "status": "UP",
    "service": "online-learning-backend",
    "timestamp": "2026-10-01T10:45:00",
    "version": "1.0.0-sprint1"
  }
  ```

---

### 2. Register Account
- **URL:** `POST /api/auth/register`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "name": "Jane Doe",
    "email": "jane@example.com",
    "password": "Password123!",
    "role": "student"
  }
  ```
  *(Role can be either `"student"` or `"instructor"`)*
- **Response (`201 Created`):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "id": 6,
    "name": "Jane Doe",
    "email": "jane@example.com",
    "role": "student"
  }
  ```

---

### 3. Login
- **URL:** `POST /api/auth/login`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "email": "sarah@instructor.com",
    "password": "password123"
  }
  ```
- **Response (`200 OK`):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "id": 1,
    "name": "Dr. Sarah Chen",
    "email": "sarah@instructor.com",
    "role": "instructor"
  }
  ```

---

### 4. Google OAuth2 Login / Registration
- **URL:** `POST /api/auth/google`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "idToken": "<google_id_token_from_frontend>",
    "role": "student"
  }
  ```
  *(If the Google account doesn't exist yet, it will automatically register with the selected role; if it already exists, it signs in immediately).*
- **Response (`200 OK`):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "id": 7,
    "name": "Google User",
    "email": "user@gmail.com",
    "role": "student"
  }
  ```

---

### 5. Get Current User Profile
- **URL:** `GET /api/auth/me`
- **Access:** Authenticated (Requires `Authorization: Bearer <token>` header)
- **Response (`200 OK`):**
  ```json
  {
    "id": 1,
    "name": "Dr. Sarah Chen",
    "email": "sarah@instructor.com",
    "role": "instructor",
    "createdAt": "2026-10-01T00:40:00"
  }
  ```

---

## ⚙️ Environment Variables & Configuration

| Variable | Default Value | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `online_learning` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password |
| `GOOGLE_CLIENT_ID` | `online-learning-platform-google-client-id...` | Google OAuth Web Client ID |

---

## 🧪 Running Tests
To execute all automated unit and integration tests:
```bash
cd backend
./mvnw test
```
The test suite utilizes an in-memory H2 PostgreSQL mode profile to run without external dependencies.
