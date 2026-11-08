# Database Setup

## Prerequisites
- Docker and Docker Compose installed

## Quick Start
1. Clone the repo
2. Run: `docker-compose up -d`
3. Database is now running at `localhost:5432`

## Connection Details
| Property | Value |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | online_learning |
| Username | postgres |
| Password | postgres |
| JDBC URL | `jdbc:postgresql://localhost:5432/online_learning` |

## Test Accounts
All accounts use the password: **password123**

| Email | Role | User ID |
|---|---|---|
| sarah@instructor.com | instructor | 1 |
| james@instructor.com | instructor | 2 |
| alice@student.com | student | 3 |
| bob@student.com | student | 4 |
| charlie@student.com | student | 5 |

## Resetting the Database
To wipe all data and re-seed from scratch:
```bash
docker-compose down -v
docker-compose up -d
```

## Schema Diagram
```
users (1) ───< courses (instructor_id)
courses (1) ───< lectures (course_id)
users (1) ───< enrollments >─── (1) courses
```

## Tables
- **users** — Students and instructors (distinguished by `role` column)
- **courses** — Created by instructors, status is 'draft' or 'published'
- **lectures** — YouTube links belonging to a course, ordered by `order_index`
- **enrollments** — Join table: which student is enrolled in which course
