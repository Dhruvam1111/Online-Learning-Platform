# PERSON 6 — Frontend: Student Pages + Shared Layout (Navbar)

## What You Own

You are the **sole owner** of:
1. **The shared Navbar** — appears on every page for every user (students, instructors, logged out visitors)
2. **All student-facing UI pages** — Course Catalog, Course Detail, and the Watch/Learn page
3. **The Vite project setup** — you initialize the React project that everyone's code lives in

**Nobody else writes the Navbar or student UI code.**

---

## What You Depend On

| Dependency | Provider | What You Consume |
|---|---|---|
| Student API endpoints | Person 4 (Student Backend) | `GET /api/courses`, `GET /api/courses/:id`, `POST /api/courses/:id/enroll`, `GET /api/courses/:id/lectures` |
| `AuthContext` (useAuth hook) | Person 5 (Auth Frontend) | `user`, `isAuthenticated`, `isInstructor`, `isStudent`, `logout`, `loading` |
| `api.js` (apiRequest function) | Person 5 (Auth Frontend) | You add your student API functions to this file |
| `index.css` (design system) | Person 5 (Auth Frontend) | All CSS variables, component classes (`.btn`, `.card`, `.form-input`, etc.) |
| `App.jsx` (route definitions) | Person 5 (Auth Frontend) | You add your routes and the `<Navbar />` component to this file |
| `ProtectedRoute`, `RoleRoute` | Person 5 (Auth Frontend) | You wrap your routes with these |

### Critical Rule: Do NOT Recreate Person 5's Work

Person 5 creates `AuthContext.jsx`, `api.js`, `index.css`, `ProtectedRoute.jsx`, `RoleRoute.jsx`, and `App.jsx`. You **import and extend** these files — you do NOT create your own versions.

Specifically:
- Add your student API functions to Person 5's `api.js`
- Add your routes to Person 5's `App.jsx`
- Add the `<Navbar />` to Person 5's `App.jsx`
- Use Person 5's CSS variables from `index.css`

### How to Work Before the Backend Is Ready

Use mock data until Person 4's backend is available:

```javascript
const MOCK_CATALOG = {
  courses: [
    { id: 1, title: "Web Development Fundamentals", description: "Learn the core building blocks of the web: HTML for structure, CSS for styling, and JavaScript for interactivity.", instructorName: "Dr. Sarah Chen", lectureCount: 3, enrollmentCount: 2, createdAt: "2026-09-25T10:00:00" },
    { id: 2, title: "Introduction to Databases", description: "Understand how data is stored, queried, and managed. Covers relational database concepts, SQL syntax, and normalization.", instructorName: "Dr. Sarah Chen", lectureCount: 2, enrollmentCount: 1, createdAt: "2026-09-25T09:00:00" },
    { id: 3, title: "Python for Beginners", description: "A gentle introduction to programming using Python. Covers variables, loops, functions, and basic data structures.", instructorName: "Prof. James Wilson", lectureCount: 3, enrollmentCount: 1, createdAt: "2026-09-24T14:30:00" }
  ]
};

const MOCK_COURSE_DETAIL = {
  course: {
    id: 1, title: "Web Development Fundamentals",
    description: "Learn the core building blocks of the web: HTML for structure, CSS for styling, and JavaScript for interactivity. This course takes you from zero to building your first responsive website.",
    instructorName: "Dr. Sarah Chen", lectureCount: 3, enrollmentCount: 2,
    isEnrolled: false, createdAt: "2026-09-25T10:00:00"
  }
};

const MOCK_LECTURES = {
  lectures: [
    { id: 1, title: "What is the Web?", videoId: "hJHvdBlSxug", embedUrl: "https://www.youtube.com/embed/hJHvdBlSxug", thumbnailUrl: "https://img.youtube.com/vi/hJHvdBlSxug/mqdefault.jpg", orderIndex: 1 },
    { id: 2, title: "HTML Crash Course", videoId: "UB1O30fR-EE", embedUrl: "https://www.youtube.com/embed/UB1O30fR-EE", thumbnailUrl: "https://img.youtube.com/vi/UB1O30fR-EE/mqdefault.jpg", orderIndex: 2 },
    { id: 3, title: "CSS in 100 Seconds", videoId: "OEV8gMkCHXQ", embedUrl: "https://www.youtube.com/embed/OEV8gMkCHXQ", thumbnailUrl: "https://img.youtube.com/vi/OEV8gMkCHXQ/mqdefault.jpg", orderIndex: 3 }
  ]
};
```

---

## Your Deliverables

1. **Initialize the Vite + React project** (the project shell everyone's code lives in)
2. **Navbar component** (shared across all pages)
3. **Catalog page** (`/courses`)
4. **Course Detail page** (`/courses/:id`)
5. **Watch page** (`/courses/:id/learn`)
6. **Add student API functions** to Person 5's `api.js`
7. **Add your routes and Navbar** to Person 5's `App.jsx`

---

## Project Structure

Your files within the existing frontend project:

```
frontend/src/
├── components/
│   ├── Navbar.jsx                         ← YOU CREATE THIS
│   ├── Navbar.css                         ← YOU CREATE THIS
│   ├── ProtectedRoute.jsx                 ← EXISTS (Person 5) — DO NOT TOUCH
│   └── RoleRoute.jsx                      ← EXISTS (Person 5) — DO NOT TOUCH
│
├── pages/
│   ├── Login.jsx                          ← EXISTS (Person 5) — DO NOT TOUCH
│   ├── Register.jsx                       ← EXISTS (Person 5) — DO NOT TOUCH
│   ├── InstructorDashboard.jsx            ← EXISTS (Person 5) — DO NOT TOUCH
│   ├── CreateCourse.jsx                   ← EXISTS (Person 5) — DO NOT TOUCH
│   ├── ManageCourse.jsx                   ← EXISTS (Person 5) — DO NOT TOUCH
│   ├── Catalog.jsx                        ← YOU CREATE THIS
│   ├── Catalog.css                        ← YOU CREATE THIS
│   ├── CourseDetail.jsx                   ← YOU CREATE THIS
│   ├── CourseDetail.css                   ← YOU CREATE THIS
│   ├── WatchPage.jsx                      ← YOU CREATE THIS
│   └── WatchPage.css                      ← YOU CREATE THIS
│
├── context/
│   └── AuthContext.jsx                    ← EXISTS (Person 5) — DO NOT TOUCH
│
├── services/
│   └── api.js                             ← EXISTS (Person 5) — YOU ADD FUNCTIONS TO THIS
│
├── App.jsx                                ← EXISTS (Person 5) — YOU ADD ROUTES + NAVBAR
├── App.css
├── index.css                              ← EXISTS (Person 5) — DO NOT TOUCH
└── main.jsx                               ← EXISTS (from Vite) — DO NOT TOUCH
```

---

## FRONTEND DESIGN GUIDELINES — MANDATORY

Same rules as Person 5. Review this summary and use ONLY these values.

### Quick Reference

| Element | Value |
|---|---|
| Page background | `var(--color-bg-page)` = `#F9FAFB` |
| Card background | `var(--color-bg-card)` = `#FFFFFF` |
| Card border | `1px solid var(--color-border-light)` = `1px solid #E5E7EB` |
| Card border-radius | `6px` |
| Card shadow | `0 1px 3px rgba(0, 0, 0, 0.08)` |
| Button border-radius | `8px` |
| Primary color | `var(--color-primary)` = `#2563EB` |
| Text color | `var(--color-text-primary)` = `#111827` |
| Secondary text | `var(--color-text-secondary)` = `#6B7280` |
| Muted text | `var(--color-text-muted)` = `#9CA3AF` |
| Font | Inter from Google Fonts |
| Max content width | `1100px` |
| Page padding | `24px 32px` |

### Absolute Bans

- NO gradient backgrounds (use flat solid colors)
- NO decorative dashes or `<hr>` styled with dashes (use whitespace or `1px solid` borders)
- NO pill-shaped buttons (use `border-radius: 8px`)
- NO heavy drop shadows
- NO slide-in/bounce animations (only opacity + transform, max 200ms)
- NO system fonts without Inter
- NO ALL CAPS headings (only badges use uppercase)

---

## Step 0: Initialize the Vite + React Project

You set up the project shell. Run these commands:

```bash
# Navigate to the project root
cd "d:/Project/Online Learning Platform"

# Create the frontend project
npx -y create-vite@latest frontend -- --template react

# Install dependencies
cd frontend
npm install

# Install React Router (the only extra dependency needed)
npm install react-router-dom
```

After this, the `frontend/` directory exists with a working React app. Person 5 will add their files to this structure.

### `vite.config.js`

Update the Vite config to proxy API requests to the backend (avoids CORS during development):

```javascript
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
            },
        },
    },
});
```

With this proxy, the frontend can call `/api/courses` instead of `http://localhost:8080/api/courses`, and Vite forwards it to the backend. This means the `API_BASE_URL` in `api.js` can be changed to just `/api` during development (or kept as `http://localhost:8080/api` — both work, but the proxy avoids CORS entirely).

---

## File 1: Add Student API Functions to `api.js`

Person 5 creates `api.js` with `apiRequest` and the auth/instructor functions. You add the student functions at the bottom of the same file. **Do not create a separate file.**

Add these functions to `frontend/src/services/api.js`:

```javascript
// ===== Student API (added by Person 6) =====

export async function getCatalog() {
    return apiRequest('/courses');
}

export async function getCourseDetail(courseId) {
    return apiRequest(`/courses/${courseId}`);
}

export async function enrollInCourse(courseId) {
    return apiRequest(`/courses/${courseId}/enroll`, { method: 'POST' });
}

export async function getCourseLectures(courseId) {
    return apiRequest(`/courses/${courseId}/lectures`);
}
```

---

## File 2: `components/Navbar.jsx`

```jsx
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { logoutUser } from '../services/api';
import './Navbar.css';

export default function Navbar() {
    const { user, isAuthenticated, isInstructor, logout } = useAuth();
    const navigate = useNavigate();

    async function handleLogout() {
        await logoutUser();
        logout();
        navigate('/login');
    }

    return (
        <nav className="navbar">
            <div className="navbar-inner">
                {/* Left side: App name */}
                <Link to="/" className="navbar-brand">LearnHub</Link>

                {/* Right side */}
                <div className="navbar-actions">
                    {isAuthenticated ? (
                        <>
                            {/* Show catalog link for everyone */}
                            <Link to="/courses" className="navbar-link">Browse Courses</Link>

                            {/* Instructor-only link */}
                            {isInstructor && (
                                <Link to="/instructor" className="navbar-link">My Courses</Link>
                            )}

                            {/* User info and logout */}
                            <div className="navbar-user">
                                <span className="navbar-user-name">{user.name}</span>
                                <span className="navbar-role-badge">{user.role}</span>
                                <button onClick={handleLogout} className="navbar-logout-btn">
                                    Log out
                                </button>
                            </div>
                        </>
                    ) : (
                        <>
                            <Link to="/login" className="navbar-link">Sign in</Link>
                            <Link to="/register" className="btn btn-primary navbar-register-btn">
                                Create account
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
}
```

## `Navbar.css`

```css
.navbar {
    position: sticky;
    top: 0;
    z-index: 100;
    background: var(--color-bg-card);
    border-bottom: 1px solid var(--color-border-light);
}

.navbar-inner {
    max-width: 1100px;
    margin: 0 auto;
    padding: 0 32px;
    height: 56px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.navbar-brand {
    font-size: 18px;
    font-weight: 700;
    color: var(--color-text-primary);
    text-decoration: none;
}
.navbar-brand:hover {
    text-decoration: none;
}

.navbar-actions {
    display: flex;
    align-items: center;
    gap: 20px;
}

.navbar-link {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-secondary);
    text-decoration: none;
}
.navbar-link:hover {
    color: var(--color-text-primary);
    text-decoration: none;
}

.navbar-user {
    display: flex;
    align-items: center;
    gap: 10px;
}

.navbar-user-name {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
}

.navbar-role-badge {
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: 2px 8px;
    border-radius: 6px;
    background: var(--color-bg-active);
    color: var(--color-primary);
    border: 1px solid #BFDBFE;
}

.navbar-logout-btn {
    background: none;
    border: none;
    font-size: 13px;
    font-weight: 500;
    font-family: 'Inter', sans-serif;
    color: var(--color-text-muted);
    cursor: pointer;
    padding: 4px 0;
}
.navbar-logout-btn:hover {
    color: var(--color-danger);
}

.navbar-register-btn {
    padding: 6px 16px;
    font-size: 13px;
}

/* Responsive: stack on small screens */
@media (max-width: 640px) {
    .navbar-inner {
        padding: 0 16px;
    }
    .navbar-actions {
        gap: 12px;
    }
    .navbar-user-name {
        display: none;
    }
}
```

### Navbar Behavior by User State

| State | Left Side | Right Side |
|---|---|---|
| **Logged out** | "LearnHub" (link to `/`) | "Sign in" link + "Create account" button |
| **Logged in as student** | "LearnHub" | "Browse Courses" link + name + "student" badge + "Log out" |
| **Logged in as instructor** | "LearnHub" | "Browse Courses" + "My Courses" link + name + "instructor" badge + "Log out" |

---

## File 3: Update `App.jsx`

You need to modify Person 5's `App.jsx` to add the Navbar and your routes. Here are the exact changes:

### Add these imports at the top:
```javascript
import Navbar from './components/Navbar';
import Catalog from './pages/Catalog';
import CourseDetail from './pages/CourseDetail';
import WatchPage from './pages/WatchPage';
```

### Add student routes inside `<Routes>`:
```jsx
{/* Student routes — added by Person 6 */}
<Route path="/courses" element={
    <ProtectedRoute><Catalog /></ProtectedRoute>
} />
<Route path="/courses/:id" element={
    <ProtectedRoute><CourseDetail /></ProtectedRoute>
} />
<Route path="/courses/:id/learn" element={
    <RoleRoute role="student"><WatchPage /></RoleRoute>
} />
```

### Add `<Navbar />` inside `<BrowserRouter>`:
```jsx
export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Navbar />          {/* ← ADD THIS LINE */}
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}
```

### Complete Updated `App.jsx`

For clarity, here is the full file after your changes:

```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

import Navbar from './components/Navbar';
import Login from './pages/Login';
import Register from './pages/Register';
import InstructorDashboard from './pages/InstructorDashboard';
import CreateCourse from './pages/CreateCourse';
import ManageCourse from './pages/ManageCourse';
import Catalog from './pages/Catalog';
import CourseDetail from './pages/CourseDetail';
import WatchPage from './pages/WatchPage';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';

function AppRoutes() {
    const { isAuthenticated, isInstructor } = useAuth();

    return (
        <Routes>
            {/* Public routes */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Instructor routes */}
            <Route path="/instructor" element={
                <RoleRoute role="instructor"><InstructorDashboard /></RoleRoute>
            } />
            <Route path="/instructor/courses/new" element={
                <RoleRoute role="instructor"><CreateCourse /></RoleRoute>
            } />
            <Route path="/instructor/courses/:id" element={
                <RoleRoute role="instructor"><ManageCourse /></RoleRoute>
            } />

            {/* Student routes */}
            <Route path="/courses" element={
                <ProtectedRoute><Catalog /></ProtectedRoute>
            } />
            <Route path="/courses/:id" element={
                <ProtectedRoute><CourseDetail /></ProtectedRoute>
            } />
            <Route path="/courses/:id/learn" element={
                <RoleRoute role="student"><WatchPage /></RoleRoute>
            } />

            {/* Default redirect */}
            <Route path="/" element={
                isAuthenticated
                    ? (isInstructor ? <Navigate to="/instructor" /> : <Navigate to="/courses" />)
                    : <Navigate to="/login" />
            } />

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/" />} />
        </Routes>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Navbar />
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}
```

---

## File 4: `pages/Catalog.jsx`

```jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getCatalog } from '../services/api';
import './Catalog.css';

export default function Catalog() {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchCatalog() {
            const result = await getCatalog();
            if (result.ok) {
                setCourses(result.data.courses);
            } else {
                setError('Failed to load courses. Please try again.');
            }
            setLoading(false);
        }
        fetchCatalog();
    }, []);

    if (loading) {
        return <div className="page-container">Loading courses...</div>;
    }

    return (
        <div className="page-container">
            <h1 className="page-title">Browse Courses</h1>

            {error && <div className="error-banner">{error}</div>}

            {courses.length === 0 ? (
                <div className="catalog-empty">
                    <p className="catalog-empty-text">No courses available yet. Check back soon.</p>
                </div>
            ) : (
                <div className="catalog-grid">
                    {courses.map(course => (
                        <div key={course.id} className="catalog-card card">
                            <h3 className="catalog-card-title">{course.title}</h3>

                            {course.description && (
                                <p className="catalog-card-desc">{course.description}</p>
                            )}

                            <div className="catalog-card-info">
                                <span className="catalog-card-instructor">
                                    {course.instructorName}
                                </span>
                                <div className="catalog-card-stats">
                                    <span>{course.lectureCount} {course.lectureCount === 1 ? 'lecture' : 'lectures'}</span>
                                    <span className="catalog-card-dot"></span>
                                    <span>{course.enrollmentCount} {course.enrollmentCount === 1 ? 'student' : 'students'}</span>
                                </div>
                            </div>

                            <Link
                                to={`/courses/${course.id}`}
                                className="btn btn-primary catalog-card-btn"
                            >
                                View course
                            </Link>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
```

## `Catalog.css`

```css
.catalog-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
}

.catalog-card {
    display: flex;
    flex-direction: column;
}

.catalog-card-title {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 8px;
}

.catalog-card-desc {
    color: var(--color-text-secondary);
    font-size: 14px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 14px;
    flex-grow: 1;
}

.catalog-card-info {
    margin-bottom: 16px;
}

.catalog-card-instructor {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-primary);
    margin-bottom: 4px;
}

.catalog-card-stats {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: var(--color-text-muted);
}

.catalog-card-dot {
    width: 3px;
    height: 3px;
    border-radius: 50%;
    background: var(--color-text-muted);
}

.catalog-card-btn {
    display: block;
    text-align: center;
    width: 100%;
}

.catalog-empty {
    text-align: center;
    padding: 60px 20px;
}

.catalog-empty-text {
    color: var(--color-text-secondary);
    font-size: 16px;
}

@media (max-width: 640px) {
    .catalog-grid {
        grid-template-columns: 1fr;
    }
}
```

---

## File 5: `pages/CourseDetail.jsx`

```jsx
import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getCourseDetail, enrollInCourse } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './CourseDetail.css';

export default function CourseDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const { isStudent } = useAuth();

    const [course, setCourse] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [enrolling, setEnrolling] = useState(false);

    useEffect(() => {
        async function fetchCourse() {
            const result = await getCourseDetail(id);
            if (result.ok) {
                setCourse(result.data.course);
            } else {
                setError('Course not found');
            }
            setLoading(false);
        }
        fetchCourse();
    }, [id]);

    async function handleEnroll() {
        setEnrolling(true);
        const result = await enrollInCourse(id);
        setEnrolling(false);

        if (result.ok) {
            // Update local state to reflect enrollment
            setCourse(prev => ({ ...prev, isEnrolled: true }));
        } else {
            setError(result.data.error || 'Failed to enroll. Please try again.');
        }
    }

    if (loading) {
        return <div className="page-container">Loading course details...</div>;
    }

    if (error && !course) {
        return (
            <div className="page-container">
                <div className="error-banner">{error}</div>
                <Link to="/courses" className="btn btn-secondary">Back to catalog</Link>
            </div>
        );
    }

    return (
        <div className="page-container">
            <Link to="/courses" className="detail-back-link">Back to catalog</Link>

            <div className="detail-layout">
                {/* Main content */}
                <div className="detail-main">
                    <h1 className="detail-title">{course.title}</h1>

                    <p className="detail-instructor">
                        Taught by <span className="detail-instructor-name">{course.instructorName}</span>
                    </p>

                    {course.description && (
                        <div className="detail-description">
                            <h2 className="detail-section-title">About this course</h2>
                            <p className="detail-description-text">{course.description}</p>
                        </div>
                    )}
                </div>

                {/* Sidebar */}
                <div className="detail-sidebar card">
                    <div className="detail-sidebar-stats">
                        <div className="detail-stat">
                            <span className="detail-stat-value">{course.lectureCount}</span>
                            <span className="detail-stat-label">{course.lectureCount === 1 ? 'Lecture' : 'Lectures'}</span>
                        </div>
                        <div className="detail-stat">
                            <span className="detail-stat-value">{course.enrollmentCount}</span>
                            <span className="detail-stat-label">{course.enrollmentCount === 1 ? 'Student' : 'Students'}</span>
                        </div>
                    </div>

                    {error && <div className="error-banner">{error}</div>}

                    {isStudent && (
                        course.isEnrolled ? (
                            <Link
                                to={`/courses/${id}/learn`}
                                className="btn btn-primary detail-sidebar-btn"
                            >
                                Go to lectures
                            </Link>
                        ) : (
                            <button
                                onClick={handleEnroll}
                                className="btn btn-primary detail-sidebar-btn"
                                disabled={enrolling}
                            >
                                {enrolling ? 'Enrolling...' : 'Enroll in this course'}
                            </button>
                        )
                    )}

                    {!isStudent && (
                        <p className="detail-sidebar-note">
                            Sign in as a student to enroll in this course.
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}
```

## `CourseDetail.css`

```css
.detail-back-link {
    display: inline-block;
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 20px;
}
.detail-back-link:hover {
    color: var(--color-primary);
    text-decoration: none;
}

.detail-layout {
    display: grid;
    grid-template-columns: 1fr 300px;
    gap: 32px;
    align-items: start;
}

.detail-title {
    font-size: 24px;
    font-weight: 700;
    margin-bottom: 8px;
}

.detail-instructor {
    font-size: 14px;
    color: var(--color-text-secondary);
    margin-bottom: 28px;
}

.detail-instructor-name {
    font-weight: 600;
    color: var(--color-text-primary);
}

.detail-section-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 12px;
}

.detail-description-text {
    font-size: 14px;
    line-height: 1.7;
    color: var(--color-text-secondary);
}

/* Sidebar */
.detail-sidebar {
    position: sticky;
    top: 80px;
}

.detail-sidebar-stats {
    display: flex;
    gap: 24px;
    margin-bottom: 20px;
}

.detail-stat {
    display: flex;
    flex-direction: column;
}

.detail-stat-value {
    font-size: 22px;
    font-weight: 700;
    color: var(--color-text-primary);
}

.detail-stat-label {
    font-size: 12px;
    color: var(--color-text-muted);
    margin-top: 2px;
}

.detail-sidebar-btn {
    display: block;
    text-align: center;
    width: 100%;
}

.detail-sidebar-note {
    font-size: 13px;
    color: var(--color-text-muted);
    text-align: center;
}

@media (max-width: 768px) {
    .detail-layout {
        grid-template-columns: 1fr;
    }
    .detail-sidebar {
        position: static;
    }
}
```

---

## File 6: `pages/WatchPage.jsx`

This is the most complex page. It has a two-column layout: the YouTube video player on the left and a lecture sidebar on the right.

```jsx
import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getCourseLectures, getCourseDetail } from '../services/api';
import './WatchPage.css';

export default function WatchPage() {
    const { id } = useParams();

    const [courseTitle, setCourseTitle] = useState('');
    const [lectures, setLectures] = useState([]);
    const [activeLecture, setActiveLecture] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchData() {
            // Fetch course title
            const courseResult = await getCourseDetail(id);
            if (courseResult.ok) {
                setCourseTitle(courseResult.data.course.title);
            }

            // Fetch lectures
            const lectureResult = await getCourseLectures(id);
            if (lectureResult.ok) {
                const lectureList = lectureResult.data.lectures;
                setLectures(lectureList);
                // Auto-select the first lecture
                if (lectureList.length > 0) {
                    setActiveLecture(lectureList[0]);
                }
            } else {
                if (lectureResult.status === 403) {
                    setError('You must be enrolled in this course to view lectures.');
                } else {
                    setError('Failed to load lectures.');
                }
            }
            setLoading(false);
        }
        fetchData();
    }, [id]);

    function handleSelectLecture(lecture) {
        setActiveLecture(lecture);
        // Scroll to top of video on mobile
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    if (loading) {
        return <div className="page-container">Loading lectures...</div>;
    }

    if (error) {
        return (
            <div className="page-container">
                <div className="error-banner">{error}</div>
                <Link to={`/courses/${id}`} className="btn btn-secondary">
                    Back to course
                </Link>
            </div>
        );
    }

    if (lectures.length === 0) {
        return (
            <div className="page-container">
                <h1 className="page-title">{courseTitle}</h1>
                <p className="watch-empty">This course doesn't have any lectures yet.</p>
                <Link to={`/courses/${id}`} className="btn btn-secondary">
                    Back to course
                </Link>
            </div>
        );
    }

    return (
        <div className="watch-layout">
            {/* Video Player */}
            <div className="watch-player">
                <div className="watch-player-header">
                    <Link to={`/courses/${id}`} className="watch-back-link">
                        Back to {courseTitle}
                    </Link>
                </div>

                {activeLecture && (
                    <>
                        <div className="watch-video-wrapper">
                            <iframe
                                key={activeLecture.id}
                                className="watch-video-iframe"
                                src={activeLecture.embedUrl}
                                title={activeLecture.title}
                                frameBorder="0"
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                allowFullScreen
                            ></iframe>
                        </div>
                        <h2 className="watch-lecture-title">
                            {activeLecture.orderIndex}. {activeLecture.title}
                        </h2>
                    </>
                )}
            </div>

            {/* Lecture Sidebar */}
            <aside className="watch-sidebar">
                <h3 className="watch-sidebar-title">Course content</h3>
                <p className="watch-sidebar-count">{lectures.length} {lectures.length === 1 ? 'lecture' : 'lectures'}</p>

                <div className="watch-sidebar-list">
                    {lectures.map(lecture => (
                        <button
                            key={lecture.id}
                            className={`watch-sidebar-item ${activeLecture?.id === lecture.id ? 'watch-sidebar-item-active' : ''}`}
                            onClick={() => handleSelectLecture(lecture)}
                        >
                            <span className="watch-sidebar-item-number">{lecture.orderIndex}</span>
                            <div className="watch-sidebar-item-info">
                                <span className="watch-sidebar-item-title">{lecture.title}</span>
                            </div>
                        </button>
                    ))}
                </div>
            </aside>
        </div>
    );
}
```

## `WatchPage.css`

```css
.watch-layout {
    display: grid;
    grid-template-columns: 1fr 320px;
    min-height: calc(100vh - 56px); /* Subtract navbar height */
}

/* Video Player Column */
.watch-player {
    padding: 20px 28px;
    background: var(--color-bg-page);
}

.watch-player-header {
    margin-bottom: 16px;
}

.watch-back-link {
    font-size: 13px;
    color: var(--color-text-secondary);
}
.watch-back-link:hover {
    color: var(--color-primary);
    text-decoration: none;
}

/* 16:9 responsive video container */
.watch-video-wrapper {
    position: relative;
    width: 100%;
    padding-bottom: 56.25%; /* 16:9 */
    background: #000;
    border-radius: 6px;
    overflow: hidden;
    margin-bottom: 16px;
}

.watch-video-iframe {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    border: none;
}

.watch-lecture-title {
    font-size: 18px;
    font-weight: 600;
}

.watch-empty {
    color: var(--color-text-secondary);
    font-size: 16px;
    margin-bottom: 16px;
}

/* Lecture Sidebar */
.watch-sidebar {
    background: var(--color-bg-card);
    border-left: 1px solid var(--color-border-light);
    padding: 20px 0;
    overflow-y: auto;
    max-height: calc(100vh - 56px);
    position: sticky;
    top: 56px;
}

.watch-sidebar-title {
    font-size: 16px;
    font-weight: 600;
    padding: 0 20px;
    margin-bottom: 2px;
}

.watch-sidebar-count {
    font-size: 12px;
    color: var(--color-text-muted);
    padding: 0 20px;
    margin-bottom: 12px;
}

.watch-sidebar-list {
    display: flex;
    flex-direction: column;
}

.watch-sidebar-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding: 12px 20px;
    background: none;
    border: none;
    text-align: left;
    font-family: 'Inter', sans-serif;
    cursor: pointer;
    transition: background 100ms;
}
.watch-sidebar-item:hover {
    background: var(--color-bg-hover);
}

.watch-sidebar-item-active {
    background: var(--color-bg-active);
    border-right: 3px solid var(--color-primary);
}
.watch-sidebar-item-active:hover {
    background: var(--color-bg-active);
}

.watch-sidebar-item-number {
    font-size: 13px;
    font-weight: 600;
    color: var(--color-text-muted);
    min-width: 20px;
}

.watch-sidebar-item-title {
    font-size: 14px;
    font-weight: 500;
    color: var(--color-text-primary);
}

.watch-sidebar-item-active .watch-sidebar-item-number {
    color: var(--color-primary);
}

.watch-sidebar-item-active .watch-sidebar-item-title {
    color: var(--color-primary);
}

/* Mobile: stack video above sidebar */
@media (max-width: 768px) {
    .watch-layout {
        grid-template-columns: 1fr;
    }
    .watch-sidebar {
        position: static;
        max-height: none;
        border-left: none;
        border-top: 1px solid var(--color-border-light);
    }
}
```

### Watch Page Behavior

| Action | Result |
|---|---|
| Page loads | Fetches lectures, auto-selects first lecture, plays it in the iframe |
| Click a lecture in the sidebar | Swaps the iframe `src` to that lecture's embed URL. The `key={activeLecture.id}` prop forces React to unmount and remount the iframe, which resets the YouTube player cleanly. |
| Not enrolled | Shows error banner with "You must be enrolled..." and a "Back to course" link |
| No lectures | Shows "This course doesn't have any lectures yet." with a back link |
| Active lecture | Highlighted with light blue background (`#EFF6FF`) and a 3px blue right border |
| Mobile | Video stacks on top, sidebar below |

### Why `key={activeLecture.id}` on the iframe

Without the `key` prop, React reuses the same iframe DOM element and just changes its `src`. Some browsers handle this well; others don't reload the YouTube player. By giving the iframe a unique `key` each time the active lecture changes, React destroys the old iframe and creates a new one, guaranteeing the new video loads cleanly every time.

---

## What Person 5 Needs from You

You need to coordinate with Person 5 on these shared touchpoints:

| Item | Who Does What |
|---|---|
| **`index.css`** | Person 5 creates it with all CSS variables. You use the variables. Do not modify this file. |
| **`App.jsx`** | Person 5 creates the initial version. You add your routes and the `<Navbar />` import. |
| **`api.js`** | Person 5 creates the `apiRequest` base function and auth endpoints. You add student endpoints at the bottom. |
| **`AuthContext.jsx`** | Person 5 creates it. You import `useAuth()`. Do not modify the context. |
| **CSS class prefixes** | Person 5 uses: `.register-`, `.login-`, `.course-card-`, `.manage-`, `.create-course-`. You use: `.navbar-`, `.catalog-`, `.detail-`, `.watch-`. No collisions. |

### Merge Strategy
When merging branches, `App.jsx` and `api.js` will have merge conflicts because both Person 5 and Person 6 edit them. Resolve by:
1. Merge Person 5's branch first (they create the files)
2. Then merge Person 6's branch (you add to the existing files)
3. Git should auto-merge cleanly if Person 5 left comment placeholders for your additions

---

## Testing Checklist

### Navbar
- [ ] Shows "Sign in" and "Create account" when logged out
- [ ] Shows user name, role badge, and "Log out" when logged in
- [ ] Shows "Browse Courses" link for both students and instructors
- [ ] Shows "My Courses" link only for instructors
- [ ] "Log out" clears token and redirects to login
- [ ] Sticky at the top of the page when scrolling
- [ ] Responsive: user name hides on mobile

### Catalog Page
- [ ] Shows all published courses (3 from seed data)
- [ ] Each card shows: title, description (truncated), instructor name, lecture count, student count
- [ ] "View course" button links to `/courses/:id`
- [ ] Empty state shown if no courses exist
- [ ] Grid layout: 3 columns on wide screens, 1 on mobile

### Course Detail Page
- [ ] Shows course title, instructor name, description
- [ ] Sidebar shows lecture count and student count
- [ ] "Enroll" button shown for unenrolled students
- [ ] After clicking "Enroll": button changes to "Go to lectures" without page reload
- [ ] "Go to lectures" links to `/courses/:id/learn`
- [ ] Instructors see "Sign in as a student to enroll" note
- [ ] Two-column layout on desktop, single column on mobile
- [ ] Sidebar is sticky on scroll
- [ ] 404 courses show error banner with "Back to catalog" link

### Watch Page
- [ ] Video iframe loads the first lecture automatically
- [ ] Clicking a sidebar lecture swaps the video
- [ ] Active lecture highlighted with blue background and right border
- [ ] Video container maintains 16:9 aspect ratio at all widths
- [ ] Sidebar scrolls independently if the lecture list is long
- [ ] "Back to [course name]" link works
- [ ] 403 error shown if student is not enrolled
- [ ] Empty state shown if course has no lectures
- [ ] Mobile: video stacks on top, sidebar below
- [ ] Sidebar is sticky on desktop (doesn't scroll with page)

### Design System Compliance
- [ ] No gradient colors used anywhere
- [ ] No decorative dashes
- [ ] All buttons have `border-radius: 8px`
- [ ] All cards have `border-radius: 6px`
- [ ] All text uses Inter font
- [ ] Page background is `#F9FAFB`
- [ ] Max content width is `1100px`
- [ ] All colors are from the CSS variables

---

## Common Pitfalls

| Pitfall | How to Avoid |
|---|---|
| Creating a duplicate `AuthContext.jsx` | Person 5 creates this. Import `useAuth` from their file: `import { useAuth } from '../context/AuthContext'` |
| Creating a separate `studentApi.js` file | Add your functions to Person 5's `api.js`. A separate file creates confusion about which API client to import. |
| Merge conflict on `App.jsx` | Coordinate with Person 5. Their branch merges first (creates the file), yours merges second (adds routes and Navbar). |
| YouTube iframe not reloading on lecture switch | Use `key={activeLecture.id}` on the `<iframe>`. Without the key, React reuses the DOM node and some browsers don't reload the video. |
| Navbar appears on login/register pages | This is intentional. The Navbar shows "Sign in" and "Create account" on those pages, which is useful. If the team wants to hide it on auth pages, wrap the `<Navbar />` in a conditional, but it's not required for Sprint 1. |
| Watch page sidebar not scrollable | The sidebar has `overflow-y: auto` and `max-height: calc(100vh - 56px)`. If you forget the `max-height`, the sidebar stretches infinitely and never shows a scrollbar. The `56px` is the navbar height. |
| CSS class collisions with Person 5 | Use prefixes: `.navbar-`, `.catalog-`, `.detail-`, `.watch-`. Person 5 uses `.register-`, `.login-`, `.course-card-`, `.manage-`, `.create-course-`. If you both use generic names like `.container`, styles will leak between pages. |
| `enrollInCourse` returns 403 for instructors | Instructors can view the course detail page (they see the catalog too), but they can't enroll. Show them the "Sign in as a student" note instead of the enroll button. The `isStudent` check in `CourseDetail.jsx` handles this. |
| Forgetting `frameBorder="0"` on the iframe | React uses camelCase (`frameBorder`) not lowercase (`frameborder`). Using the wrong casing causes a React warning in the console. |
| Watch page crashes if `activeLecture` is null | The render checks `{activeLecture && (...)}` before rendering the iframe. If you remove this check and the lecture list is empty, `activeLecture.embedUrl` throws a null reference error. |

---

## Git Branch

Work on branch: `feature/student-frontend`

```bash
git checkout -b feature/student-frontend
# ... do all your work ...
git add .
git commit -m "feat: add navbar, catalog, course detail, and watch pages"
git push origin feature/student-frontend
```

### Merge Order (Important)
1. **Pranamya** merges first (database — no dependencies)
2. **Person 2** merges second (auth backend — depends on DB)
3. **Person 3** merges third (instructor backend — depends on auth)
4. **Person 4** merges fourth (student backend — depends on auth + instructor entities)
5. **Person 5** merges fifth (frontend auth + instructor — depends on backend APIs)
6. **Person 6 (you)** merges last (depends on Person 5's shared files)

If Person 5 hasn't merged yet and you need to test, check out their branch locally and merge it into yours temporarily:
```bash
git fetch origin
git merge origin/feature/auth-instructor-frontend
```
