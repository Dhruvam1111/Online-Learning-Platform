import axios from 'axios';

// ─── Base Axios Instance ────────────────────────────────────────────────────
// In development Vite proxies /api → http://localhost:8080
// In production or custom setup, VITE_API_BASE_URL can override it
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// ─── Request Interceptor: attach JWT token ──────────────────────────────────
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('olp_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// ─── Response Interceptor: handle 401 globally ──────────────────────────────
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // Clear token if invalid/expired (except during login attempts)
      if (!error.config.url.includes('/auth/login') && !error.config.url.includes('/auth/register')) {
        localStorage.removeItem('olp_token');
        localStorage.removeItem('olp_user');
        if (window.location.hash !== '#signin') {
          window.location.hash = '#signin';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;

// ─── Health Check ───────────────────────────────────────────────────────────
export const checkBackendHealth = async () => {
  try {
    const res = await api.get('/courses');
    return { online: true, status: res.status };
  } catch (err) {
    if (err.response) {
      // Backend responded with status code
      return { online: true, status: err.response.status };
    }
    return { online: false, error: err.message };
  }
};

// ─── Auth API ───────────────────────────────────────────────────────────────
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
};

// ─── User / Profile API ─────────────────────────────────────────────────────
export const userAPI = {
  getProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
};

// ─── Student API ────────────────────────────────────────────────────────────
export const studentAPI = {
  getProfile: () => api.get('/student/profile'),
  updateProfile: (data) => api.put('/student/profile', data),
  getMyCourses: () => api.get('/students/my-courses'),
  getBookmarks: () => api.get('/students/bookmarks'),
  enroll: (courseId) => api.post(`/courses/${courseId}/enroll`),
};

// ─── Course API (Public & Student) ──────────────────────────────────────────
export const courseAPI = {
  getAll: (params) => api.get('/courses', { params }),
  getById: (id) => api.get(`/courses/${id}`),
  getLectures: (courseId) => api.get(`/courses/${courseId}/lectures`),
  enroll: (courseId) => api.post(`/courses/${courseId}/enroll`),
  getReviews: (courseId) => api.get(`/courses/${courseId}/reviews`),
  addReview: (courseId, data) => api.post(`/courses/${courseId}/reviews`, data),
  addBookmark: (courseId) => api.post(`/courses/${courseId}/bookmark`),
  removeBookmark: (courseId) => api.delete(`/courses/${courseId}/bookmark`),
};

// ─── Instructor API ─────────────────────────────────────────────────────────
export const instructorAPI = {
  getCourses: () => api.get('/instructor/courses'),
  createCourse: (data) => api.post('/instructor/courses', data),
  updateCourse: (courseId, data) => api.put(`/instructor/courses/${courseId}`, data),
  updateCourseStatus: (courseId, data) =>
    api.patch(`/instructor/courses/${courseId}/status`, data),
  getLectures: (courseId) => api.get(`/instructor/courses/${courseId}/lectures`),
  createLecture: (courseId, data) =>
    api.post(`/instructor/courses/${courseId}/lectures`, data),
  updateLecture: (courseId, lectureId, data) =>
    api.put(`/instructor/courses/${courseId}/lectures/${lectureId}`, data),
  deleteLecture: (courseId, lectureId) =>
    api.delete(`/instructor/courses/${courseId}/lectures/${lectureId}`),
  reorderLectures: (courseId, data) =>
    api.patch(`/instructor/courses/${courseId}/lectures/reorder`, data),
  getProfile: () => api.get('/instructor/profile'),
  updateProfile: (data) => api.put('/instructor/profile', data),
};

// ─── Admin API ──────────────────────────────────────────────────────────────
export const adminAPI = {
  getUsers: (params) => api.get('/admin/users', { params }),
  getPendingInstructors: (params) => api.get('/admin/instructors/pending', { params }),
  approveInstructor: (id) => api.put(`/admin/instructors/${id}/approve`),
  rejectInstructor: (id) => api.put(`/admin/instructors/${id}/reject`),
  suspendUser: (id) => api.put(`/admin/users/${id}/suspend`),
  activateUser: (id) => api.put(`/admin/users/${id}/activate`),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  getCourses: (params) => api.get('/admin/courses', { params }),
  getPendingCourses: (params) => api.get('/admin/courses/pending', { params }),
  approveCourse: (id) => api.put(`/admin/courses/${id}/approve`),
  rejectCourse: (id, data) => api.put(`/admin/courses/${id}/reject`, data),
  unpublishCourse: (id) => api.put(`/admin/courses/${id}/unpublish`),
  deleteCourse: (id) => api.delete(`/admin/courses/${id}`),
};
