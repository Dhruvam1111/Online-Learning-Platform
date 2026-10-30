import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authAPI } from '../services/api';

// ─── Context ────────────────────────────────────────────────────────────────
const AuthContext = createContext(null);

// ─── Provider ───────────────────────────────────────────────────────────────
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('olp_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });
  const [token, setToken] = useState(() => localStorage.getItem('olp_token'));
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Keep localStorage in sync whenever user/token change
  useEffect(() => {
    if (token) {
      localStorage.setItem('olp_token', token);
    } else {
      localStorage.removeItem('olp_token');
    }
  }, [token]);

  useEffect(() => {
    if (user) {
      localStorage.setItem('olp_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('olp_user');
    }
  }, [user]);

  // ── Verify token on mount ─────────────────────────────────────────────────
  useEffect(() => {
    if (token && !user) {
      setLoading(true);
      authAPI
        .me()
        .then((res) => {
          setUser(res.data);
        })
        .catch(() => {
          // Token is invalid/expired
          setToken(null);
          setUser(null);
        })
        .finally(() => setLoading(false));
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // ── Login ─────────────────────────────────────────────────────────────────
  const login = useCallback(async (email, password) => {
    setLoading(true);
    setError(null);
    try {
      const res = await authAPI.login({ email, password });
      const { token: jwt, user: userData, message } = res.data;
      setToken(jwt);
      setUser(userData);
      return { success: true, message, user: userData };
    } catch (err) {
      const msg =
        err.response?.data?.message || err.response?.data?.error || 'Login failed. Please try again.';
      setError(msg);
      return { success: false, message: msg };
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Register ──────────────────────────────────────────────────────────────
  const register = useCallback(async (name, email, password, role = 'student') => {
    setLoading(true);
    setError(null);
    try {
      const res = await authAPI.register({ name, email, password, role });
      const { token: jwt, user: userData, message } = res.data;
      setToken(jwt);
      setUser(userData);
      return { success: true, message, user: userData };
    } catch (err) {
      const msg =
        err.response?.data?.message || err.response?.data?.error || 'Registration failed. Please try again.';
      setError(msg);
      return { success: false, message: msg };
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Logout ────────────────────────────────────────────────────────────────
  const logout = useCallback(async () => {
    try {
      await authAPI.logout();
    } catch {
      // Ignore errors — we're clearing local state anyway
    }
    setToken(null);
    setUser(null);
    setError(null);
  }, []);

  // ── Refresh user data ─────────────────────────────────────────────────────
  const refreshUser = useCallback(async () => {
    if (!token) return;
    try {
      const res = await authAPI.me();
      setUser(res.data);
    } catch {
      // Token may have expired
      setToken(null);
      setUser(null);
    }
  }, [token]);

  const isAuthenticated = !!token && !!user;
  const isStudent = user?.role === 'student';
  const isInstructor = user?.role === 'instructor';
  const isAdmin = user?.role === 'admin';

  const value = {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    isStudent,
    isInstructor,
    isAdmin,
    login,
    register,
    logout,
    refreshUser,
    setError,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// ─── Hook ───────────────────────────────────────────────────────────────────
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
