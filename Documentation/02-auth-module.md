# Sprint 1 — Auth Module (Login / Register)

Covers FR-S01/S02 (student) and FR-I01/I02 (instructor), simplified: no admin-verification gate for instructors yet, no password reset yet.

## Pages / routes needed
- `GET /register` — form: name, email, password, role (student/instructor radio or two separate pages)
- `POST /register` — creates the user
- `GET /login` — form: email, password
- `POST /login` — authenticates, starts session
- `POST /logout` — ends session

## Registration flow
1. User submits name, email, password, role.
2. Server validates: email format, password length (≥8 chars is a reasonable minimum), email not already used.
3. Hash the password (bcrypt, cost factor ~10-12, or Argon2).
4. Insert into `users` with the chosen `role`.
5. Reject duplicate email with a clear error message (US-S01 acceptance criteria: "Duplicate or invalid registrations are rejected with a clear message").

## Login flow
1. User submits email + password.
2. Look up user by email.
3. Compare password against `password_hash` (bcrypt.compare / argon2.verify).
4. On success: create a session (session cookie) or issue a JWT stored in an httpOnly cookie.
5. On failure: generic "invalid email or password" message — don't reveal which field was wrong.

## Session / role checking
- Store `user_id` and `role` in the session/JWT payload.
- Middleware: `requireAuth` (any logged-in user), `requireRole('instructor')`, `requireRole('student')`.
- Use these middlewares to gate the instructor "Create Course" routes and the student "Enroll" / "Watch Lecture" routes.

## Security checklist for this module (NFR-13, NFR-14)
- [ ] Passwords hashed, never logged or returned in any API response
- [ ] Login/register forms served over HTTPS (or localhost for dev)
- [ ] Session cookie marked `httpOnly` and `secure` (secure only in production over HTTPS)
- [ ] Basic rate limiting or at least no infinite retry lockout issue (nice-to-have, not blocking for Sprint 1)

## Out of scope this sprint
- Password reset / forgot-password flow
- Multi-factor authentication (NFR-13 says MFA for admins — no admin role yet)
- Admin verification of instructor accounts (FR-I01's "subject to admin verification/approval")
