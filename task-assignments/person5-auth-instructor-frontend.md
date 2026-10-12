# PERSON 5 — Frontend: Auth Pages + Instructor Pages

## What You Own

You are the **sole owner** of the authentication UI (Login, Register) and all instructor-facing UI pages (Dashboard, Create Course, Manage Course with Add Lecture).

You also own the **auth state management** — the logic that stores the JWT token, tracks whether the user is logged in, and provides auth context to the entire app.

**Nobody else writes auth UI or instructor UI code.**

---

## What You Depend On

| Dependency | Provider | What You Consume |
|---|---|---|
| Auth API endpoints | Person 2 (Auth Backend) | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me` |
| Instructor API endpoints | Person 3 (Instructor Backend) | `GET /api/instructor/courses`, `POST /api/instructor/courses`, `GET /api/instructor/courses/:id`, `POST /api/instructor/courses/:id/lectures` |
| Shared layout and navbar | Person 6 (Student Frontend) | Person 6 builds the navbar and shared layout. You build pages that render inside their layout. |

### How to Work Before the Backend Is Ready

You don't need Person 2 or Person 3's backend running to start building your pages. Use mock data:

```javascript
// Mock responses you can use during development
const MOCK_LOGIN_RESPONSE = {
  message: "Login successful",
  token: "fake-jwt-token-for-development",
  user: { id: 1, name: "Dr. Sarah Chen", email: "sarah@instructor.com", role: "instructor" }
};

const MOCK_COURSES = {
  courses: [
    { id: 1, title: "Web Development Fundamentals", description: "Learn HTML, CSS, JS", status: "published", lectureCount: 3, createdAt: "2026-09-25T10:00:00" },
    { id: 2, title: "Introduction to Databases", description: "SQL and PostgreSQL", status: "published", lectureCount: 2, createdAt: "2026-09-24T14:30:00" },
    { id: 3, title: "Advanced ML", description: "Neural networks", status: "draft", lectureCount: 0, createdAt: "2026-09-23T09:00:00" }
  ]
};

const MOCK_COURSE_DETAIL = {
  course: {
    id: 1, title: "Web Development Fundamentals",
    description: "Learn the core building blocks of the web...",
    status: "published", createdAt: "2026-09-25T10:00:00",
    lectures: [
      { id: 1, title: "What is the Web?", youtubeUrl: "https://www.youtube.com/watch?v=hJHvdBlSxug", videoId: "hJHvdBlSxug", embedUrl: "https://www.youtube.com/embed/hJHvdBlSxug", thumbnailUrl: "https://img.youtube.com/vi/hJHvdBlSxug/mqdefault.jpg", orderIndex: 1 },
      { id: 2, title: "HTML Crash Course", youtubeUrl: "https://www.youtube.com/watch?v=UB1O30fR-EE", videoId: "UB1O30fR-EE", embedUrl: "https://www.youtube.com/embed/UB1O30fR-EE", thumbnailUrl: "https://img.youtube.com/vi/UB1O30fR-EE/mqdefault.jpg", orderIndex: 2 }
    ]
  }
};
```

Switch from mock data to real API calls once Person 2 and Person 3 push their code.

---

## Tech Stack

- **React 18** (created with Vite)
- **React Router v6** for client-side routing
- **Vanilla CSS** (no Tailwind, no CSS-in-JS)
- **fetch API** for HTTP requests (no axios needed for Sprint 1)

---

## Project Structure

All your code goes under `frontend/src/`. Create exactly these files:

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── main.jsx                           ← Entry point (exists from Vite)
│   ├── App.jsx                            ← Route definitions
│   ├── App.css                            ← Minimal app-level styles
│   ├── index.css                          ← Global styles, CSS variables, reset
│   │
│   ├── context/
│   │   └── AuthContext.jsx                ← Auth state management (token, user, login/logout)
│   │
│   ├── services/
│   │   └── api.js                         ← Centralized API client (fetch wrapper)
│   │
│   ├── pages/
│   │   ├── Login.jsx                      ← Login page
│   │   ├── Login.css
│   │   ├── Register.jsx                   ← Register page
│   │   ├── Register.css
│   │   ├── InstructorDashboard.jsx        ← Instructor's "My Courses" list
│   │   ├── InstructorDashboard.css
│   │   ├── CreateCourse.jsx               ← Create course form
│   │   ├── CreateCourse.css
│   │   ├── ManageCourse.jsx               ← Course detail + lecture list + add lecture form
│   │   └── ManageCourse.css
│   │
│   └── components/
│       ├── ProtectedRoute.jsx             ← Redirects to login if not authenticated
│       └── RoleRoute.jsx                  ← Redirects if wrong role
│
├── package.json
└── vite.config.js
```

---

## FRONTEND DESIGN GUIDELINES — MANDATORY

These rules apply to every page you build. Violation of any rule will be rejected in code review.

### Colors (use ONLY these)

```css
:root {
    --color-primary: #2563EB;           /* Blue — buttons, links, active states */
    --color-primary-hover: #1D4ED8;     /* Darker blue — button hover */
    --color-primary-border: #1E40AF;    /* Even darker — button border */

    --color-success: #16A34A;           /* Green — success messages, published badge */
    --color-warning: #D97706;           /* Amber — draft badge, warnings */
    --color-danger: #DC2626;            /* Red — error messages, delete actions */

    --color-bg-page: #F9FAFB;          /* Very light gray — page background */
    --color-bg-card: #FFFFFF;          /* White — card backgrounds */
    --color-bg-input: #FFFFFF;         /* White — input backgrounds */
    --color-bg-hover: #F3F4F6;         /* Light gray — hover state on rows */
    --color-bg-active: #EFF6FF;        /* Very light blue — selected/active items */

    --color-border: #D1D5DB;           /* Medium gray — borders on cards, inputs */
    --color-border-light: #E5E7EB;     /* Light gray — dividers, subtle borders */

    --color-text-primary: #111827;     /* Near-black — headings, primary text */
    --color-text-secondary: #6B7280;   /* Medium gray — secondary text, labels */
    --color-text-muted: #9CA3AF;       /* Light gray — placeholder text, timestamps */
    --color-text-inverse: #FFFFFF;     /* White — text on colored backgrounds */
}
```

**DO NOT invent new colors.** If something needs a color, use one from this list.

### Typography

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    font-size: 14px;
    line-height: 1.5;
    color: var(--color-text-primary);
}
```

| Element | Size | Weight |
|---|---|---|
| Page title (h1) | 24px | 700 (bold) |
| Section heading (h2) | 18px | 600 (semibold) |
| Card title | 16px | 600 (semibold) |
| Body text | 14px | 400 (regular) |
| Small text / labels | 13px | 500 (medium) |
| Tiny text / timestamps | 12px | 400 (regular) |

### Layout Rules

| Rule | Value |
|---|---|
| Max content width | 1100px, centered with `margin: 0 auto` |
| Page padding | 32px horizontal, 24px vertical |
| Card padding | 20px |
| Space between cards | 16px |
| Space between form fields | 16px |
| Space between sections | 32px |

### Component Styles

**Cards:**
```css
.card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border-light);
    border-radius: 6px;
    padding: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}
```

**Buttons (Primary):**
```css
.btn-primary {
    background: var(--color-primary);
    color: var(--color-text-inverse);
    border: 2px solid var(--color-primary-border);
    border-radius: 8px;
    padding: 10px 20px;
    font-size: 14px;
    font-weight: 600;
    font-family: 'Inter', sans-serif;
    cursor: pointer;
    transition: background 150ms;
}
.btn-primary:hover {
    background: var(--color-primary-hover);
}
```

**Buttons (Secondary/Outline):**
```css
.btn-secondary {
    background: var(--color-bg-card);
    color: var(--color-text-primary);
    border: 2px solid var(--color-border);
    border-radius: 8px;
    padding: 10px 20px;
    font-size: 14px;
    font-weight: 600;
    font-family: 'Inter', sans-serif;
    cursor: pointer;
    transition: background 150ms;
}
.btn-secondary:hover {
    background: var(--color-bg-hover);
}
```

**Form Inputs:**
```css
.form-input {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid var(--color-border);
    border-radius: 6px;
    font-size: 14px;
    font-family: 'Inter', sans-serif;
    background: var(--color-bg-input);
    color: var(--color-text-primary);
    transition: border-color 150ms;
}
.form-input:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
```

**Form Labels:**
```css
.form-label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-secondary);
    margin-bottom: 6px;
}
```

**Error Messages:**
```css
.error-message {
    color: var(--color-danger);
    font-size: 13px;
    margin-top: 6px;
}
.error-banner {
    background: #FEF2F2;
    border: 1px solid #FECACA;
    color: var(--color-danger);
    padding: 12px 16px;
    border-radius: 6px;
    font-size: 14px;
    margin-bottom: 16px;
}
```

**Success Messages:**
```css
.success-banner {
    background: #F0FDF4;
    border: 1px solid #BBF7D0;
    color: var(--color-success);
    padding: 12px 16px;
    border-radius: 6px;
    font-size: 14px;
    margin-bottom: 16px;
}
```

**Status Badges:**
```css
.badge {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}
.badge-published {
    background: #F0FDF4;
    color: var(--color-success);
    border: 1px solid #BBF7D0;
}
.badge-draft {
    background: #FFFBEB;
    color: var(--color-warning);
    border: 1px solid #FDE68A;
}
```

### What NOT to Do

| Banned | Why |
|---|---|
| Gradient backgrounds (`linear-gradient`, `radial-gradient`) | Looks AI-generated. Use flat solid colors only. |
| Decorative dashes or lines (`---`, `<hr>` styled with dashes) | Looks AI-generated. Use whitespace or solid `1px` borders. |
| Pill-shaped buttons (`border-radius: 999px`) | Use `8px` for buttons, `6px` for cards/inputs. |
| Drop shadows larger than `0 1px 3px rgba(0,0,0,0.08)` | Heavy shadows look cheap. Keep them barely visible. |
| Slide-in or bounce animations | Use only `opacity` and subtle `transform` transitions, max 200ms. |
| System fonts without specifying Inter | Always load Inter from Google Fonts. |
| Colored backgrounds on the page body | Page background is `#F9FAFB` (very light gray). Not white, not blue, not any color. |
| Centered form layouts | Left-align everything. Labels above inputs, not beside them. |
| Icons without text labels | If you use an icon (e.g., a "+" icon), always pair it with text ("Create New Course"). |
| ALL CAPS headings | Only badges use uppercase. All other text uses normal casing. |

---

## File 1: `index.css` — Global Styles

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

/* ===== Reset ===== */
*, *::before, *::after {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

/* ===== CSS Variables ===== */
:root {
    --color-primary: #2563EB;
    --color-primary-hover: #1D4ED8;
    --color-primary-border: #1E40AF;
    --color-success: #16A34A;
    --color-warning: #D97706;
    --color-danger: #DC2626;
    --color-bg-page: #F9FAFB;
    --color-bg-card: #FFFFFF;
    --color-bg-input: #FFFFFF;
    --color-bg-hover: #F3F4F6;
    --color-bg-active: #EFF6FF;
    --color-border: #D1D5DB;
    --color-border-light: #E5E7EB;
    --color-text-primary: #111827;
    --color-text-secondary: #6B7280;
    --color-text-muted: #9CA3AF;
    --color-text-inverse: #FFFFFF;
}

/* ===== Base ===== */
body {
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    font-size: 14px;
    line-height: 1.5;
    color: var(--color-text-primary);
    background: var(--color-bg-page);
}

a {
    color: var(--color-primary);
    text-decoration: none;
}
a:hover {
    text-decoration: underline;
}

/* ===== Reusable Utilities ===== */
.page-container {
    max-width: 1100px;
    margin: 0 auto;
    padding: 24px 32px;
}

.page-title {
    font-size: 24px;
    font-weight: 700;
    color: var(--color-text-primary);
    margin-bottom: 24px;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
}

/* Cards */
.card {
    background: var(--color-bg-card);
    border: 1px solid var(--color-border-light);
    border-radius: 6px;
    padding: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

/* Buttons */
.btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    border-radius: 8px;
    padding: 10px 20px;
    font-size: 14px;
    font-weight: 600;
    font-family: 'Inter', sans-serif;
    cursor: pointer;
    transition: background 150ms;
    text-decoration: none;
}
.btn:hover {
    text-decoration: none;
}
.btn-primary {
    background: var(--color-primary);
    color: var(--color-text-inverse);
    border: 2px solid var(--color-primary-border);
}
.btn-primary:hover {
    background: var(--color-primary-hover);
}
.btn-secondary {
    background: var(--color-bg-card);
    color: var(--color-text-primary);
    border: 2px solid var(--color-border);
}
.btn-secondary:hover {
    background: var(--color-bg-hover);
}
.btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

/* Form Elements */
.form-group {
    margin-bottom: 16px;
}
.form-label {
    display: block;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-secondary);
    margin-bottom: 6px;
}
.form-input {
    width: 100%;
    padding: 10px 12px;
    border: 1px solid var(--color-border);
    border-radius: 6px;
    font-size: 14px;
    font-family: 'Inter', sans-serif;
    background: var(--color-bg-input);
    color: var(--color-text-primary);
    transition: border-color 150ms;
}
.form-input:focus {
    outline: none;
    border-color: var(--color-primary);
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
.form-textarea {
    min-height: 100px;
    resize: vertical;
}

/* Feedback Messages */
.error-message {
    color: var(--color-danger);
    font-size: 13px;
    margin-top: 6px;
}
.error-banner {
    background: #FEF2F2;
    border: 1px solid #FECACA;
    color: var(--color-danger);
    padding: 12px 16px;
    border-radius: 6px;
    font-size: 14px;
    margin-bottom: 16px;
}
.success-banner {
    background: #F0FDF4;
    border: 1px solid #BBF7D0;
    color: var(--color-success);
    padding: 12px 16px;
    border-radius: 6px;
    font-size: 14px;
    margin-bottom: 16px;
}

/* Badges */
.badge {
    display: inline-block;
    padding: 2px 10px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}
.badge-published {
    background: #F0FDF4;
    color: var(--color-success);
    border: 1px solid #BBF7D0;
}
.badge-draft {
    background: #FFFBEB;
    color: var(--color-warning);
    border: 1px solid #FDE68A;
}
```

---

## File 2: `services/api.js` — API Client

This is the centralized HTTP client. Every API call goes through this. It automatically attaches the JWT token to requests.

```javascript
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Get the stored JWT token.
 */
function getToken() {
    return localStorage.getItem('token');
}

/**
 * Make an authenticated API request.
 * Automatically adds the Authorization header if a token exists.
 *
 * @param {string} endpoint - e.g., '/auth/login' (will be prefixed with API_BASE_URL)
 * @param {object} options - fetch options (method, body, etc.)
 * @returns {Promise<{ok: boolean, status: number, data: object}>}
 */
export async function apiRequest(endpoint, options = {}) {
    const token = getToken();

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers,
    });

    // Parse JSON response (or empty object if no body)
    let data = {};
    try {
        data = await response.json();
    } catch (e) {
        // Response had no JSON body
    }

    return {
        ok: response.ok,
        status: response.status,
        data,
    };
}

// ===== Auth API =====

export async function registerUser(name, email, password, role) {
    return apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password, role }),
    });
}

export async function loginUser(email, password) {
    return apiRequest('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
    });
}

export async function logoutUser() {
    return apiRequest('/auth/logout', { method: 'POST' });
}

export async function getCurrentUser() {
    return apiRequest('/auth/me');
}

// ===== Instructor API =====

export async function getInstructorCourses() {
    return apiRequest('/instructor/courses');
}

export async function createCourse(title, description, status) {
    return apiRequest('/instructor/courses', {
        method: 'POST',
        body: JSON.stringify({ title, description, status }),
    });
}

export async function getInstructorCourseDetail(courseId) {
    return apiRequest(`/instructor/courses/${courseId}`);
}

export async function addLecture(courseId, title, youtubeUrl) {
    return apiRequest(`/instructor/courses/${courseId}/lectures`, {
        method: 'POST',
        body: JSON.stringify({ title, youtubeUrl }),
    });
}
```

### Why a Centralized API Client

- Token management in one place — if the token format changes, update once
- Base URL in one place — easy to switch from `localhost:8080` to the deployed URL
- Consistent error handling — every caller gets the same `{ ok, status, data }` shape
- Person 6 imports the same file and adds their own student API functions to it

---

## File 3: `context/AuthContext.jsx` — Auth State Management

```jsx
import { createContext, useContext, useState, useEffect } from 'react';
import { getCurrentUser } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [loading, setLoading] = useState(true);

    // On mount, check if there's a stored token and validate it
    useEffect(() => {
        async function checkAuth() {
            if (!token) {
                setLoading(false);
                return;
            }

            const result = await getCurrentUser();
            if (result.ok) {
                setUser(result.data);
            } else {
                // Token is invalid or expired — clear it
                localStorage.removeItem('token');
                setToken(null);
                setUser(null);
            }
            setLoading(false);
        }

        checkAuth();
    }, [token]);

    function login(tokenValue, userData) {
        localStorage.setItem('token', tokenValue);
        setToken(tokenValue);
        setUser(userData);
    }

    function logout() {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
    }

    const value = {
        user,
        token,
        loading,
        isAuthenticated: !!user,
        isInstructor: user?.role === 'instructor',
        isStudent: user?.role === 'student',
        login,
        logout,
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
```

### How Other Components Use This

```jsx
import { useAuth } from '../context/AuthContext';

function SomeComponent() {
    const { user, isAuthenticated, isInstructor, login, logout } = useAuth();

    if (!isAuthenticated) return <p>Please log in</p>;
    if (isInstructor) return <p>Welcome, {user.name}</p>;
}
```

Person 6 will import and use `useAuth()` in the Navbar and student pages. This is a **shared contract** — do not change the shape of the `value` object without telling Person 6.

---

## File 4: `components/ProtectedRoute.jsx`

```jsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children }) {
    const { isAuthenticated, loading } = useAuth();

    if (loading) {
        return <div className="page-container">Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    return children;
}
```

## File 5: `components/RoleRoute.jsx`

```jsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function RoleRoute({ role, children }) {
    const { user, loading } = useAuth();

    if (loading) {
        return <div className="page-container">Loading...</div>;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    if (user.role !== role) {
        // Student trying to access instructor pages, or vice versa
        return <Navigate to="/" replace />;
    }

    return children;
}
```

---

## File 6: `App.jsx` — Routes

```jsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';

import Login from './pages/Login';
import Register from './pages/Register';
import InstructorDashboard from './pages/InstructorDashboard';
import CreateCourse from './pages/CreateCourse';
import ManageCourse from './pages/ManageCourse';
import ProtectedRoute from './components/ProtectedRoute';
import RoleRoute from './components/RoleRoute';

// Person 6 will add: import Catalog, CourseDetail, WatchPage
// Person 6 will also add: import Navbar from './components/Navbar';

function AppRoutes() {
    const { isAuthenticated, isInstructor, isStudent } = useAuth();

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

            {/* Student routes — Person 6 will add these */}
            {/* <Route path="/courses" element={<ProtectedRoute><Catalog /></ProtectedRoute>} /> */}
            {/* <Route path="/courses/:id" element={<ProtectedRoute><CourseDetail /></ProtectedRoute>} /> */}
            {/* <Route path="/courses/:id/learn" element={<RoleRoute role="student"><WatchPage /></RoleRoute>} /> */}

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
                {/* Person 6 will add <Navbar /> here */}
                <AppRoutes />
            </BrowserRouter>
        </AuthProvider>
    );
}
```

---

## File 7: `pages/Register.jsx`

```jsx
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerUser } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './Register.css';

export default function Register() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [role, setRole] = useState('student');
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [submitting, setSubmitting] = useState(false);

    function validate() {
        const errors = {};
        if (!name.trim()) errors.name = 'Name is required';
        if (!email.trim()) errors.email = 'Email is required';
        else if (!/\S+@\S+\.\S+/.test(email)) errors.email = 'Enter a valid email';
        if (!password) errors.password = 'Password is required';
        else if (password.length < 8) errors.password = 'Password must be at least 8 characters';
        if (password !== confirmPassword) errors.confirmPassword = 'Passwords do not match';
        return errors;
    }

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');
        setFieldErrors({});

        const errors = validate();
        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            return;
        }

        setSubmitting(true);
        const result = await registerUser(name.trim(), email.trim().toLowerCase(), password, role);
        setSubmitting(false);

        if (result.ok) {
            login(result.data.token, result.data.user);
            if (result.data.user.role === 'instructor') {
                navigate('/instructor');
            } else {
                navigate('/courses');
            }
        } else {
            setError(result.data.error || 'Registration failed. Please try again.');
        }
    }

    return (
        <div className="register-page">
            <div className="register-container">
                <h1 className="register-title">Create your account</h1>
                <p className="register-subtitle">Start learning or teaching today</p>

                {error && <div className="error-banner">{error}</div>}

                <form onSubmit={handleSubmit} noValidate>
                    <div className="form-group">
                        <label className="form-label" htmlFor="name">Full name</label>
                        <input
                            id="name"
                            type="text"
                            className={`form-input ${fieldErrors.name ? 'input-error' : ''}`}
                            value={name}
                            onChange={e => setName(e.target.value)}
                            placeholder="Jane Doe"
                        />
                        {fieldErrors.name && <p className="error-message">{fieldErrors.name}</p>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="email">Email address</label>
                        <input
                            id="email"
                            type="email"
                            className={`form-input ${fieldErrors.email ? 'input-error' : ''}`}
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            placeholder="jane@example.com"
                        />
                        {fieldErrors.email && <p className="error-message">{fieldErrors.email}</p>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            className={`form-input ${fieldErrors.password ? 'input-error' : ''}`}
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            placeholder="At least 8 characters"
                        />
                        {fieldErrors.password && <p className="error-message">{fieldErrors.password}</p>}
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="confirmPassword">Confirm password</label>
                        <input
                            id="confirmPassword"
                            type="password"
                            className={`form-input ${fieldErrors.confirmPassword ? 'input-error' : ''}`}
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            placeholder="Re-enter your password"
                        />
                        {fieldErrors.confirmPassword && <p className="error-message">{fieldErrors.confirmPassword}</p>}
                    </div>

                    <div className="form-group">
                        <label className="form-label">I want to</label>
                        <div className="role-selector">
                            <button
                                type="button"
                                className={`role-option ${role === 'student' ? 'role-active' : ''}`}
                                onClick={() => setRole('student')}
                            >
                                Learn courses
                            </button>
                            <button
                                type="button"
                                className={`role-option ${role === 'instructor' ? 'role-active' : ''}`}
                                onClick={() => setRole('instructor')}
                            >
                                Teach courses
                            </button>
                        </div>
                    </div>

                    <button type="submit" className="btn btn-primary register-submit" disabled={submitting}>
                        {submitting ? 'Creating account...' : 'Create account'}
                    </button>
                </form>

                <p className="register-footer">
                    Already have an account? <Link to="/login">Sign in</Link>
                </p>
            </div>
        </div>
    );
}
```

## `Register.css`

```css
.register-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
}

.register-container {
    width: 100%;
    max-width: 420px;
}

.register-title {
    font-size: 24px;
    font-weight: 700;
    margin-bottom: 4px;
}

.register-subtitle {
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-bottom: 28px;
}

.register-submit {
    width: 100%;
    margin-top: 8px;
}

.register-footer {
    text-align: center;
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-top: 20px;
}

/* Role selector: two side-by-side buttons */
.role-selector {
    display: flex;
    gap: 12px;
}

.role-option {
    flex: 1;
    padding: 10px;
    border: 2px solid var(--color-border);
    border-radius: 8px;
    background: var(--color-bg-card);
    font-size: 14px;
    font-weight: 500;
    font-family: 'Inter', sans-serif;
    color: var(--color-text-secondary);
    cursor: pointer;
    transition: border-color 150ms, color 150ms;
}

.role-option:hover {
    border-color: var(--color-primary);
}

.role-active {
    border-color: var(--color-primary);
    background: var(--color-bg-active);
    color: var(--color-primary);
}

.input-error {
    border-color: var(--color-danger);
}
```

---

## File 8: `pages/Login.jsx`

```jsx
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { loginUser } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './Login.css';

export default function Login() {
    const navigate = useNavigate();
    const { login } = useAuth();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');

        if (!email.trim() || !password) {
            setError('Please enter your email and password');
            return;
        }

        setSubmitting(true);
        const result = await loginUser(email.trim().toLowerCase(), password);
        setSubmitting(false);

        if (result.ok) {
            login(result.data.token, result.data.user);
            if (result.data.user.role === 'instructor') {
                navigate('/instructor');
            } else {
                navigate('/courses');
            }
        } else {
            setError(result.data.error || 'Invalid email or password');
        }
    }

    return (
        <div className="login-page">
            <div className="login-container">
                <h1 className="login-title">Welcome back</h1>
                <p className="login-subtitle">Sign in to your account</p>

                {error && <div className="error-banner">{error}</div>}

                <form onSubmit={handleSubmit} noValidate>
                    <div className="form-group">
                        <label className="form-label" htmlFor="login-email">Email address</label>
                        <input
                            id="login-email"
                            type="email"
                            className="form-input"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            placeholder="you@example.com"
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="login-password">Password</label>
                        <input
                            id="login-password"
                            type="password"
                            className="form-input"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            placeholder="Your password"
                        />
                    </div>

                    <button type="submit" className="btn btn-primary login-submit" disabled={submitting}>
                        {submitting ? 'Signing in...' : 'Sign in'}
                    </button>
                </form>

                <p className="login-footer">
                    Don't have an account? <Link to="/register">Create one</Link>
                </p>
            </div>
        </div>
    );
}
```

## `Login.css`

```css
.login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
}

.login-container {
    width: 100%;
    max-width: 400px;
}

.login-title {
    font-size: 24px;
    font-weight: 700;
    margin-bottom: 4px;
}

.login-subtitle {
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-bottom: 28px;
}

.login-submit {
    width: 100%;
    margin-top: 8px;
}

.login-footer {
    text-align: center;
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-top: 20px;
}
```

---

## File 9: `pages/InstructorDashboard.jsx`

```jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getInstructorCourses } from '../services/api';
import './InstructorDashboard.css';

export default function InstructorDashboard() {
    const [courses, setCourses] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        async function fetchCourses() {
            const result = await getInstructorCourses();
            if (result.ok) {
                setCourses(result.data.courses);
            } else {
                setError('Failed to load courses');
            }
            setLoading(false);
        }
        fetchCourses();
    }, []);

    if (loading) {
        return <div className="page-container">Loading your courses...</div>;
    }

    return (
        <div className="page-container">
            <div className="page-header">
                <h1 className="page-title">My Courses</h1>
                <Link to="/instructor/courses/new" className="btn btn-primary">
                    Create new course
                </Link>
            </div>

            {error && <div className="error-banner">{error}</div>}

            {courses.length === 0 ? (
                <div className="empty-state">
                    <p className="empty-state-text">You haven't created any courses yet.</p>
                    <Link to="/instructor/courses/new" className="btn btn-primary">
                        Create your first course
                    </Link>
                </div>
            ) : (
                <div className="course-grid">
                    {courses.map(course => (
                        <Link
                            key={course.id}
                            to={`/instructor/courses/${course.id}`}
                            className="course-card card"
                        >
                            <div className="course-card-header">
                                <h3 className="course-card-title">{course.title}</h3>
                                <span className={`badge badge-${course.status}`}>
                                    {course.status}
                                </span>
                            </div>
                            {course.description && (
                                <p className="course-card-desc">{course.description}</p>
                            )}
                            <div className="course-card-meta">
                                <span>{course.lectureCount} {course.lectureCount === 1 ? 'lecture' : 'lectures'}</span>
                                <span>{new Date(course.createdAt).toLocaleDateString()}</span>
                            </div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );
}
```

## `InstructorDashboard.css`

```css
.course-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
}

.course-card {
    display: block;
    color: inherit;
    text-decoration: none;
    transition: box-shadow 150ms;
}
.course-card:hover {
    text-decoration: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.course-card-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 8px;
}

.course-card-title {
    font-size: 16px;
    font-weight: 600;
}

.course-card-desc {
    color: var(--color-text-secondary);
    font-size: 14px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 12px;
}

.course-card-meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: var(--color-text-muted);
}

.empty-state {
    text-align: center;
    padding: 60px 20px;
}

.empty-state-text {
    color: var(--color-text-secondary);
    font-size: 16px;
    margin-bottom: 16px;
}
```

---

## File 10: `pages/CreateCourse.jsx`

```jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createCourse } from '../services/api';
import './CreateCourse.css';

export default function CreateCourse() {
    const navigate = useNavigate();

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [publishNow, setPublishNow] = useState(true);
    const [error, setError] = useState('');
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError('');

        if (!title.trim()) {
            setError('Course title is required');
            return;
        }

        setSubmitting(true);
        const status = publishNow ? 'published' : 'draft';
        const result = await createCourse(title.trim(), description.trim(), status);
        setSubmitting(false);

        if (result.ok) {
            navigate(`/instructor/courses/${result.data.course.id}`);
        } else {
            setError(result.data.error || 'Failed to create course');
        }
    }

    return (
        <div className="page-container">
            <h1 className="page-title">Create a new course</h1>

            <div className="create-course-form card">
                {error && <div className="error-banner">{error}</div>}

                <form onSubmit={handleSubmit} noValidate>
                    <div className="form-group">
                        <label className="form-label" htmlFor="course-title">Course title</label>
                        <input
                            id="course-title"
                            type="text"
                            className="form-input"
                            value={title}
                            onChange={e => setTitle(e.target.value)}
                            placeholder="e.g. Introduction to Web Development"
                        />
                    </div>

                    <div className="form-group">
                        <label className="form-label" htmlFor="course-desc">Description</label>
                        <textarea
                            id="course-desc"
                            className="form-input form-textarea"
                            value={description}
                            onChange={e => setDescription(e.target.value)}
                            placeholder="What will students learn in this course?"
                            rows={4}
                        />
                    </div>

                    <div className="form-group">
                        <label className="checkbox-label">
                            <input
                                type="checkbox"
                                checked={publishNow}
                                onChange={e => setPublishNow(e.target.checked)}
                            />
                            <span>Publish immediately (students can see this course right away)</span>
                        </label>
                    </div>

                    <div className="form-actions">
                        <button type="submit" className="btn btn-primary" disabled={submitting}>
                            {submitting ? 'Creating...' : 'Create course'}
                        </button>
                        <button
                            type="button"
                            className="btn btn-secondary"
                            onClick={() => navigate('/instructor')}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
```

## `CreateCourse.css`

```css
.create-course-form {
    max-width: 600px;
}

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: var(--color-text-primary);
    cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
    width: 16px;
    height: 16px;
    accent-color: var(--color-primary);
}

.form-actions {
    display: flex;
    gap: 12px;
    margin-top: 24px;
}
```

---

## File 11: `pages/ManageCourse.jsx`

```jsx
import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getInstructorCourseDetail, addLecture } from '../services/api';
import './ManageCourse.css';

export default function ManageCourse() {
    const { id } = useParams();

    const [course, setCourse] = useState(null);
    const [lectures, setLectures] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Add lecture form state
    const [lectureTitle, setLectureTitle] = useState('');
    const [youtubeUrl, setYoutubeUrl] = useState('');
    const [lectureError, setLectureError] = useState('');
    const [lectureSuccess, setLectureSuccess] = useState('');
    const [addingLecture, setAddingLecture] = useState(false);

    useEffect(() => {
        async function fetchCourse() {
            const result = await getInstructorCourseDetail(id);
            if (result.ok) {
                setCourse(result.data.course);
                setLectures(result.data.course.lectures || []);
            } else {
                setError('Course not found');
            }
            setLoading(false);
        }
        fetchCourse();
    }, [id]);

    async function handleAddLecture(e) {
        e.preventDefault();
        setLectureError('');
        setLectureSuccess('');

        if (!lectureTitle.trim()) {
            setLectureError('Lecture title is required');
            return;
        }
        if (!youtubeUrl.trim()) {
            setLectureError('YouTube URL is required');
            return;
        }

        setAddingLecture(true);
        const result = await addLecture(id, lectureTitle.trim(), youtubeUrl.trim());
        setAddingLecture(false);

        if (result.ok) {
            // Add the new lecture to the list without reloading
            setLectures(prev => [...prev, result.data.lecture]);
            setLectureTitle('');
            setYoutubeUrl('');
            setLectureSuccess('Lecture added');
            // Clear success message after 3 seconds
            setTimeout(() => setLectureSuccess(''), 3000);
        } else {
            setLectureError(result.data.error || 'Failed to add lecture');
        }
    }

    if (loading) {
        return <div className="page-container">Loading course...</div>;
    }

    if (error) {
        return (
            <div className="page-container">
                <div className="error-banner">{error}</div>
                <Link to="/instructor" className="btn btn-secondary">Back to My Courses</Link>
            </div>
        );
    }

    return (
        <div className="page-container">
            {/* Course Header */}
            <div className="manage-header">
                <Link to="/instructor" className="back-link">Back to My Courses</Link>
                <div className="manage-title-row">
                    <h1 className="page-title">{course.title}</h1>
                    <span className={`badge badge-${course.status}`}>{course.status}</span>
                </div>
                {course.description && (
                    <p className="manage-description">{course.description}</p>
                )}
            </div>

            {/* Lecture List */}
            <section className="manage-section">
                <h2 className="section-title">
                    Lectures ({lectures.length})
                </h2>

                {lectures.length === 0 ? (
                    <p className="empty-lectures">No lectures yet. Add one below.</p>
                ) : (
                    <div className="lecture-list">
                        {lectures.map((lecture, index) => (
                            <div key={lecture.id} className="lecture-item card">
                                <div className="lecture-item-content">
                                    <img
                                        src={lecture.thumbnailUrl}
                                        alt=""
                                        className="lecture-thumb"
                                    />
                                    <div className="lecture-info">
                                        <span className="lecture-number">{lecture.orderIndex}.</span>
                                        <span className="lecture-name">{lecture.title}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* Add Lecture Form */}
            <section className="manage-section">
                <h2 className="section-title">Add a lecture</h2>

                <div className="add-lecture-form card">
                    {lectureError && <div className="error-banner">{lectureError}</div>}
                    {lectureSuccess && <div className="success-banner">{lectureSuccess}</div>}

                    <form onSubmit={handleAddLecture} noValidate>
                        <div className="form-group">
                            <label className="form-label" htmlFor="lecture-title">Lecture title</label>
                            <input
                                id="lecture-title"
                                type="text"
                                className="form-input"
                                value={lectureTitle}
                                onChange={e => setLectureTitle(e.target.value)}
                                placeholder="e.g. Introduction to Variables"
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label" htmlFor="youtube-url">YouTube URL</label>
                            <input
                                id="youtube-url"
                                type="url"
                                className="form-input"
                                value={youtubeUrl}
                                onChange={e => setYoutubeUrl(e.target.value)}
                                placeholder="https://www.youtube.com/watch?v=..."
                            />
                        </div>

                        <button type="submit" className="btn btn-primary" disabled={addingLecture}>
                            {addingLecture ? 'Adding...' : 'Add lecture'}
                        </button>
                    </form>
                </div>
            </section>
        </div>
    );
}
```

## `ManageCourse.css`

```css
.manage-header {
    margin-bottom: 32px;
}

.back-link {
    display: inline-block;
    font-size: 13px;
    color: var(--color-text-secondary);
    margin-bottom: 12px;
}
.back-link:hover {
    color: var(--color-primary);
}

.manage-title-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.manage-title-row .page-title {
    margin-bottom: 0;
}

.manage-description {
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-top: 8px;
    max-width: 600px;
}

.manage-section {
    margin-bottom: 32px;
}

.section-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 16px;
}

.empty-lectures {
    color: var(--color-text-muted);
    font-size: 14px;
    padding: 20px 0;
}

/* Lecture list */
.lecture-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.lecture-item {
    padding: 12px 16px;
}

.lecture-item-content {
    display: flex;
    align-items: center;
    gap: 14px;
}

.lecture-thumb {
    width: 120px;
    height: 68px;
    object-fit: cover;
    border-radius: 4px;
    background: var(--color-bg-hover);
}

.lecture-info {
    display: flex;
    align-items: center;
    gap: 8px;
}

.lecture-number {
    font-size: 14px;
    font-weight: 600;
    color: var(--color-text-muted);
    min-width: 24px;
}

.lecture-name {
    font-size: 14px;
    font-weight: 500;
}

/* Add Lecture form */
.add-lecture-form {
    max-width: 600px;
}
```

---

## Integration Contract with Person 6

Person 6 builds the Navbar and student pages. You need to agree on these shared elements:

### Shared Context
Person 6 imports `useAuth()` from your `AuthContext`. The contract:

```javascript
const {
    user,              // { id, name, email, role } or null
    token,             // JWT string or null
    loading,           // boolean — true while checking stored token on mount
    isAuthenticated,   // boolean
    isInstructor,      // boolean
    isStudent,         // boolean
    login,             // function(token, user) — call after successful login/register
    logout,            // function() — clears token and user
} = useAuth();
```

### Shared API Client
Person 6 adds their student API functions to `services/api.js`. They import `apiRequest` and create new functions like:

```javascript
// Person 6 adds these to api.js
export async function getCatalog() {
    return apiRequest('/courses');
}
export async function getCourseDetail(courseId) {
    return apiRequest(`/courses/${courseId}`);
}
// etc.
```

### Shared CSS Variables
Person 6 imports `index.css` (which you create). All your CSS variables are available to their components.

### App.jsx Integration
Person 6 adds their routes to `App.jsx` (you've left commented-out placeholders) and adds the `<Navbar />` component.

---

## Verification Checklist

- [ ] `npm run dev` starts the frontend without errors
- [ ] Register page: can register as student, redirects to `/courses`
- [ ] Register page: can register as instructor, redirects to `/instructor`
- [ ] Register page: duplicate email shows error banner
- [ ] Register page: short password shows inline error
- [ ] Register page: mismatched passwords shows inline error
- [ ] Login page: correct credentials redirect to the right dashboard
- [ ] Login page: wrong credentials show error banner
- [ ] Login page: "Create one" link goes to register
- [ ] Instructor Dashboard: shows the instructor's courses
- [ ] Instructor Dashboard: empty state shows "Create your first course" button
- [ ] Instructor Dashboard: clicking a course card goes to manage page
- [ ] Create Course: creates a course, redirects to manage page
- [ ] Create Course: blank title shows error
- [ ] Create Course: cancel button goes back to dashboard
- [ ] Manage Course: shows course title, status badge, description
- [ ] Manage Course: shows lecture list with thumbnails
- [ ] Manage Course: add lecture form works, new lecture appears without reload
- [ ] Manage Course: invalid YouTube URL shows error from backend
- [ ] Manage Course: "Back to My Courses" link works
- [ ] Auth state persists across page refreshes (token in localStorage)
- [ ] Logging out clears token and redirects to login
- [ ] Visiting `/instructor` while not logged in redirects to `/login`
- [ ] Visiting `/instructor` as a student redirects to `/`
- [ ] All pages use the design system colors (no gradients, no dashes)
- [ ] All text uses Inter font
- [ ] All buttons have visible 2px borders

---

## Common Pitfalls

| Pitfall | How to Avoid |
|---|---|
| Token not sent on requests | The `apiRequest` function reads from `localStorage.getItem('token')` on every call. Make sure `login()` stores the token with `localStorage.setItem('token', ...)` before making authenticated requests. |
| `useAuth()` returns null on first render | The `AuthProvider` starts with `loading: true` while it validates the stored token. Always check `loading` before rendering protected content. `ProtectedRoute` handles this for page-level protection. |
| CORS errors in the browser | The backend must have `http://localhost:5173` (Vite's default port) in its CORS config. If you see CORS errors, tell Person 2 to add your port to `SecurityConfig.java`. |
| React Router `<Navigate>` infinite loop | The `<Route path="/">` redirect checks `isAuthenticated` and `isInstructor`. If the AuthContext is still loading, this can loop. The `loading` state prevents this — the route renders "Loading..." until auth is resolved. |
| Storing the entire user object in localStorage | Don't. Store only the token. The user object is fetched from `/api/auth/me` on page load. If you store the user object, it can get stale (e.g., name change). |
| Using `axios` instead of `fetch` | The project uses `fetch` to avoid an extra dependency. Don't add `axios` unless the team agrees. |
| Forgetting `noValidate` on `<form>` | Without `noValidate`, the browser shows its own ugly validation tooltips alongside your custom error messages. Always add `noValidate` to forms. |
| CSS class naming collisions with Person 6 | Prefix your page-specific classes with the page name (e.g., `.register-page`, `.login-container`, `.manage-header`). Person 6 uses `.catalog-`, `.detail-`, `.watch-` prefixes. |

---

## Git Branch

Work on branch: `feature/auth-instructor-frontend`

```bash
git checkout -b feature/auth-instructor-frontend
# ... do all your work ...
git add .
git commit -m "feat: add auth UI (login, register) and instructor pages (dashboard, create, manage)"
git push origin feature/auth-instructor-frontend
```
