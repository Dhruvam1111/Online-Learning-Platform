# Sprint 1 — Task Assignments

## General Instructions for Everyone

**Repo:** https://github.com/Dhruvam1111/Online-Learning-Platform.git

### Step 1: Clone the repo (one time only)
```bash
git clone https://github.com/Dhruvam1111/Online-Learning-Platform.git
cd Online-Learning-Platform
```

### Step 2: Create your own branch
Each person works on their own branch. **Never push directly to `main`.**
```bash
git checkout -b <your-branch-name>
```

Use the branch name from the Team Roster table below. For example:
```bash
git checkout -b feature/auth-backend          # Person 2
git checkout -b feature/instructor-backend     # Person 3
git checkout -b feature/student-backend        # Person 4
git checkout -b feature/auth-instructor-frontend  # Person 5
git checkout -b feature/student-frontend       # Person 6
```

### Step 3: Do your work
Read your assigned task file (linked in the table below). Write all your code on your branch.

### Step 4: Commit and push your branch
```bash
git add .
git commit -m "feat: <short description of what you built>"
git push origin <your-branch-name>
```

### Step 5: Open a Pull Request on GitHub
1. Go to https://github.com/Dhruvam1111/Online-Learning-Platform
2. You'll see a banner saying your branch was recently pushed. Click **"Compare & pull request"**.
3. Set the base branch to `main`.
4. Write a brief description of what you built.
5. Tag at least one teammate as a reviewer.
6. **Do NOT merge your own PR.** Wait for at least one review.

### Rules
- **One person per branch.** Do not work on someone else's branch.
- **Pull before you start each day:** `git pull origin main` to get the latest changes.
- **If you need someone else's code before they merge:** fetch their branch locally with `git fetch origin` then `git merge origin/<their-branch-name>` into yours.
- **Do not force push** (`git push --force`). If you get a push error, pull first.
- **Commit often.** Small, frequent commits are easier to review and revert than one massive commit at the end.

---

## Team Roster

| Person | Role | File | Git Branch |
|---|---|---|---|
| **Pranamya** | Database Engineer | [your-task-database.md](./your-task-database.md) | `main` (merge first) |
| **Person 2** | Auth Backend | [person2-auth-backend.md](./person2-auth-backend.md) | `feature/auth-backend` |
| **Person 3** | Instructor Backend | [person3-instructor-backend.md](./person3-instructor-backend.md) | `feature/instructor-backend` |
| **Person 4** | Student Backend + Deploy | [person4-student-backend-deploy.md](./person4-student-backend-deploy.md) | `feature/student-backend` |
| **Person 5** | Auth + Instructor Frontend | [person5-auth-instructor-frontend.md](./person5-auth-instructor-frontend.md) | `feature/auth-instructor-frontend` |
| **Person 6** | Student + Layout Frontend | [person6-student-layout-frontend.md](./person6-student-layout-frontend.md) | `feature/student-frontend` |

## Merge Order

```
1. Pranamya (DB)
2. Person 2 (Auth Backend)
3. Person 3 (Instructor Backend)
4. Person 4 (Student Backend + Deploy)
5. Person 5 (Frontend: Auth + Instructor)
6. Person 6 (Frontend: Student + Navbar)
```

## Shared Contracts

| Contract | Owner | Consumers |
|---|---|---|
| Database schema (table structures) | Pranamya | Person 2, 3, 4 |
| `User` entity + `UserRepository` | Person 2 | Person 3, 4 |
| Auth middleware (`@PreAuthorize`) | Person 2 | Person 3, 4 |
| `Course` + `Lecture` entities | Person 3 | Person 4 |
| `CourseRepository` + `LectureRepository` | Person 3 | Person 4 |
| `AuthContext` (useAuth hook) | Person 5 | Person 6 |
| `api.js` (apiRequest function) | Person 5 | Person 6 |
| `index.css` (design system) | Person 5 | Person 6 |
| API JSON contracts | Person 2, 3, 4 | Person 5, 6 |

## Quick Links

- [Database Schema Spec](../01-database-schema.md)
- [Auth Module Spec](../02-auth-module.md)
- [Instructor Module Spec](../03-instructor-module.md)
- [Student Module Spec](../04-student-module.md)
- [Deployment Spec](../05-deployment.md)
