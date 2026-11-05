import React, { useState } from 'react';
import api from '../services/api';

/**
 * OtpAuthModal Component
 * Maintained by Person 2 / Developer B: OTP Verification & Forgot Password Mechanism.
 *
 * Props:
 * - mode: 'verify-signup' | 'forgot-password'
 * - email: string
 * - onSuccessSession: (token, user) => void
 * - onCancel: () => void
 * - setAlertMessage: (msg: { type: string, text: string } | null) => void
 * - startCooldown: (seconds?: number) => void
 * - resendCooldown: number
 */
export default function OtpAuthModal({
  mode = 'verify-signup',
  email = '',
  onSuccessSession,
  onCancel,
  setAlertMessage,
  startCooldown,
  resendCooldown = 0,
}) {
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleOtpChange = (index, value) => {
    if (value.length > 1) {
      value = value.slice(-1);
    }
    const updated = [...otp];
    updated[index] = value;
    setOtp(updated);

    // Auto-advance to next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`otp-input-${index + 1}`);
      if (nextInput) nextInput.focus();
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      const prevInput = document.getElementById(`otp-input-${index - 1}`);
      if (prevInput) prevInput.focus();
    }
  };

  const handleResend = async () => {
    if (resendCooldown > 0) return;
    try {
      if (mode === 'verify-signup') {
        if (api.resendSignupOtp) {
          await api.resendSignupOtp(email);
        } else {
          await api.post('/auth/otp/resend', { email });
        }
      } else {
        if (api.sendPasswordResetOtp) {
          await api.sendPasswordResetOtp(email);
        } else {
          await api.post('/auth/password-reset/request', { email });
        }
      }
      if (startCooldown) startCooldown(30);
      if (setAlertMessage) {
        setAlertMessage({ type: 'success', text: `New 6-digit code sent to ${email}` });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to resend code';
      if (setAlertMessage) setAlertMessage({ type: 'error', text: msg });
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const fullOtp = otp.join('');
    if (fullOtp.length < 6) {
      if (setAlertMessage) {
        setAlertMessage({ type: 'error', text: 'Please enter all 6 digits of the verification code.' });
      }
      return;
    }

    if (mode === 'forgot-password') {
      if (!newPassword || newPassword.length < 8) {
        if (setAlertMessage) {
          setAlertMessage({ type: 'error', text: 'New password must be at least 8 characters long.' });
        }
        return;
      }
      if (newPassword !== confirmPassword) {
        if (setAlertMessage) {
          setAlertMessage({ type: 'error', text: 'Passwords do not match.' });
        }
        return;
      }
    }

    setIsSubmitting(true);
    if (setAlertMessage) setAlertMessage(null);

    try {
      if (mode === 'verify-signup') {
        const res = api.verifySignupOtp
          ? await api.verifySignupOtp(email, fullOtp)
          : await api.post('/auth/otp/verify', { email, otp: fullOtp });
        const data = res.data || res;
        if (onSuccessSession && data.token && data.user) {
          onSuccessSession(data.token, data.user);
        }
      } else {
        const res = api.resetPasswordWithOtp
          ? await api.resetPasswordWithOtp(email, fullOtp, newPassword)
          : await api.post('/auth/password-reset/confirm', { email, otp: fullOtp, newPassword });
        const data = res.data || res;
        if (onSuccessSession && data.token && data.user) {
          onSuccessSession(data.token, data.user);
        } else {
          if (setAlertMessage) {
            setAlertMessage({ type: 'success', text: 'Password reset successful! You can now log in.' });
          }
          if (onCancel) onCancel();
        }
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      if (setAlertMessage) setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4 animate-fadeIn">
      <div className="flex flex-col gap-2">
        <label className="text-xs font-semibold uppercase tracking-wider text-neutral-600">
          Enter 6-digit Code
        </label>
        <div className="flex justify-between gap-2">
          {otp.map((digit, idx) => (
            <input
              key={idx}
              id={`otp-input-${idx}`}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleOtpChange(idx, e.target.value)}
              onKeyDown={(e) => handleKeyDown(idx, e.key)}
              className="w-11 h-12 text-center text-lg font-bold rounded-md border border-neutral-300 bg-white text-neutral-900 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 transition-all"
            />
          ))}
        </div>
      </div>

      {mode === 'forgot-password' && (
        <div className="flex flex-col gap-3 pt-1">
          <input
            type="password"
            required
            minLength={8}
            placeholder="New Password (min 8 characters)"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
          />
          <input
            type="password"
            required
            minLength={8}
            placeholder="Confirm New Password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
          />
        </div>
      )}

      <div className="flex items-center justify-between text-xs text-neutral-600 pt-1">
        <span>Didn't receive code?</span>
        <button
          type="button"
          onClick={handleResend}
          disabled={resendCooldown > 0}
          className="text-[#0066cc] hover:underline font-medium cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend Code'}
        </button>
      </div>

      <div className="flex gap-2 pt-2">
        <button
          type="button"
          onClick={onCancel}
          className="w-1/3 py-2.5 px-3 rounded-md border border-neutral-300 hover:bg-neutral-50 text-neutral-700 font-medium text-xs sm:text-sm transition-colors cursor-pointer"
        >
          Back
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-2/3 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-xs sm:text-sm shadow-xs transition-all cursor-pointer disabled:opacity-75 flex items-center justify-center gap-2"
        >
          {isSubmitting ? (
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <span>{mode === 'forgot-password' ? 'Reset Password' : 'Verify & Continue'}</span>
          )}
        </button>
      </div>
    </form>
  );
}
