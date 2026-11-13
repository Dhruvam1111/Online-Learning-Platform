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

// ─── Auth API ───────────────────────────────────────────────────────────────
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  sendRegistrationOtp: (data) => api.post('/auth/register/send-otp', data),
  verifyRegistrationOtp: (data) => api.post('/auth/register/verify-otp', data),
  resendRegistrationOtp: (data) => api.post('/auth/register/resend-otp', data),
  login: (data) => api.post('/auth/login', data),
  loginWithGoogle: (data) => api.post('/auth/google', data),
  sendPasswordResetOtp: (data) => api.post('/auth/password-reset/send-otp', data),
  resetPasswordWithOtp: (data) => api.post('/auth/password-reset/reset', data),
  logout: () => api.post('/auth/logout'),
  me: () => api.get('/auth/me'),
  verifyEmail: (token) => api.get(`/auth/verify-email?token=${token}`),
};

// ─── Convenience Helpers on api Object ──────────────────────────────────────
api.login = async (email, password) => {
  const res = await authAPI.login({ email, password });
  return res.data;
};

api.sendRegistrationOtp = async (name, email, password, role = 'student') => {
  const res = await authAPI.sendRegistrationOtp({ name, email, password, role });
  return res.data;
};

api.verifyRegistrationOtp = async (email, otpCode) => {
  const res = await authAPI.verifyRegistrationOtp({ email, otpCode });
  return res.data;
};

api.resendRegistrationOtp = async (email) => {
  const res = await authAPI.resendRegistrationOtp({ email });
  return res.data;
};

api.loginWithGoogle = async (email, name = '', idToken = '') => {
  const res = await authAPI.loginWithGoogle({ email, name, idToken });
  return res.data;
};

api.sendPasswordResetOtp = async (email) => {
  const res = await authAPI.sendPasswordResetOtp({ email });
  return res.data;
};

api.resetPasswordWithOtp = async (email, otpCode, newPassword, confirmPassword) => {
  const res = await authAPI.resetPasswordWithOtp({
    email,
    otpCode,
    newPassword,
    confirmPassword: confirmPassword || newPassword,
  });
  return res.data;
};

export default api;

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

// ─── Notification Centre API (Step 5 / FR-I70) ──────────────────────────────
export const notificationAPI = {
  getAll: (unreadOnly = false) =>
    api.get('/notifications', { params: { unreadOnly } }),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markAsRead: (id) => api.patch(`/notifications/${id}/read`),
  markAllAsRead: () => api.patch('/notifications/read-all'),
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
  getEarnings: () => api.get('/payments/instructor/earnings'),
  withdrawEarnings: (bank) => api.post('/payments/instructor/payouts/withdraw-all', bank),
  getPayoutHistory: () => api.get('/payments/instructor/payouts/history'),
  getNotifications: (unreadOnly = false) =>
    api.get('/instructor/notifications', { params: { unreadOnly } }),
  getUnreadNotificationCount: () => api.get('/instructor/notifications/unread-count'),
  markNotificationRead: (id) => api.patch(`/instructor/notifications/${id}/read`),
  markAllNotificationsRead: () => api.patch('/instructor/notifications/read-all'),
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
  getAllTransactions: () => api.get('/payments/admin/all'),
  getAllRefunds: () => api.get('/payments/refunds/all'),
  getAllInstructorPayouts: () => api.get('/payments/instructor/admin/all'),
  settlePayout: (earningId) => api.post(`/payments/instructor/payouts/${earningId}/settle`),
  haltPayout: (earningId, reason) => api.post(`/payments/instructor/payouts/${earningId}/halt`, { reason }),
  unhaltPayout: (earningId, reason) => api.post(`/payments/instructor/payouts/${earningId}/unhalt`, { reason }),
  getAllCoupons: () => api.get('/payments/coupons/admin/all'),
  createCoupon: (coupon) => api.post('/payments/coupons/create', coupon),
  toggleCoupon: (couponId) => api.patch(`/payments/coupons/${couponId}/toggle`),
  processExceptionalRefund: (paymentId, reason) =>
    api.post('/payments/refunds/exceptions', { paymentId, reason }),
  getRefundExceptions: () => api.get('/payments/refunds/exceptions'),
  decideRefundException: (id, decision, reason) => api.post(`/payments/refunds/exceptions/${id}/decision`, {decision, reason}),
  getReconciliation: () => api.get('/payments/reconciliation/report'),
};

// ─── Payment Gateway API (QR Code Flow) ────────────────────────────────────
export const paymentAPI = {
  requestException: (paymentId, reason) => api.post('/payments/refunds/exceptions', {paymentId, reason}),
  createOrder: (courseId, amount, currency = 'INR', couponCode = null, useWallet = false, paymentMethod = null, policyAcceptedVersion = null, settlementCurrency = 'INR') =>
    api.post('/payments/create-order', { courseId, amount, currency, couponCode, useWallet, paymentMethod, policyAcceptedVersion, settlementCurrency }),
  confirmPayment: (orderId, paymentMethod) =>
    api.post(`/payments/confirm/${orderId}`, paymentMethod ? { paymentMethod } : {}),
  failPayment: (orderId) => api.post(`/payments/fail/${orderId}`),
  getUpiQr: (orderId) => api.get(`/payments/upi-qr/${orderId}`),
  getOrderStatus: (orderId) => api.get(`/payments/status/${orderId}`),
  simulatePay: (orderId) => api.post(`/payments/confirm/${orderId}`),
  verify: (orderId, paymentId, signature, paymentMethod) =>
    api.post('/payments/verify', { orderId, paymentId, signature, paymentMethod }),
  sandboxSign: (orderId, paymentId) =>
    Promise.resolve({ data: { orderId, paymentId, signature: 'sig_ok' } }),
  applyCoupon: (code, orderAmount) =>
    api.post('/payments/coupons/apply', { code, orderAmount }),
  getWallet: () =>
    api.get('/payments/wallet'),
  getMyTransactions: () => api.get('/payments/my-transactions'),
  getInvoice: (orderId) => api.get(`/payments/invoices/${orderId}`),
  requestRefund: (paymentId, reason = 'Student requested course refund', amount = null) =>
    api.post('/payments/refunds/initiate', { paymentId, reason, amount }),
  createStripeSession: (courseId, amount) => api.post('/payments/stripe/create-session', { courseId, amount }),
  verifyStripeSession: (sessionId) => api.post('/payments/stripe/verify-session', { sessionId }),
};

export const paymentSimulationAPI = {
  methods: () => api.get('/payments/methods'),
  saveMethod: (method, consent) => api.post('/payments/methods', {method, consent}),
  revokeMethod: token => api.delete(`/payments/methods/${token}`),
  pay: (token, orderId) => api.post(`/payments/methods/${token}/pay`, {orderId}),
  requestRefund: (paymentId, amount, reason) => api.post('/payments/refunds/request', {paymentId, amount, reason}),
  event: body => api.post('/payments/simulator/events', body),
};
