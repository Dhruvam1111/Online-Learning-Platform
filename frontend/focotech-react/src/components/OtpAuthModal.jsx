import React, { useState } from 'react';
import api from '../services/api';

/**
 * OTP Verification & Password Recovery Module.
 */
export default function OtpAuthModal({
  mode, // 'verify-signup' | 'forgot-password'
  email,
  onSuccessSession, // callback with (token, user)
  onCancel,
  setAlertMessage,
  startCooldown,
  resendCooldown,
}) {
  // Common states
  const [isLoading, setIsLoading] = useState(false);

  // Sign up verification state
  const [signupOtpCode, setSignupOtpCode] = useState('');

  // Password reset states
  const [resetEmail, setResetEmail] = useState(email || '');
  const [resetOtpSent, setResetOtpSent] = useState(false);
  const [resetOtpCode, setResetOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);

  // =========================================================================
  // SIGN UP STEP 2: VERIFY REGISTRATION OTP
  // =========================================================================
  const handleVerifySignupOtp = async (e) => {
    e.preventDefault();
    if (signupOtpCode.trim().length !== 6) {
      setAlertMessage({ type: 'error', text: 'Please enter the 6-digit verification code.' });
      return;
    }

    setIsLoading(true);
    setAlertMessage(null);

    try {
      const data = await api.verifyRegistrationOtp(email.trim(), signupOtpCode.trim());
      if (onSuccessSession) {
        onSuccessSession(data.token, data.user);
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsLoading(false);
    }
  };

  const handleResendSignupOtp = async () => {
    if (resendCooldown > 0) return;
    setIsLoading(true);
    setAlertMessage(null);

    try {
      const res = await api.resendRegistrationOtp(email.trim());
      startCooldown(15);
      setAlertMessage(null);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsLoading(false);
    }
  };

  // =========================================================================
  // FORGOT PASSWORD FLOW (EMAIL OTP RESET)
  // =========================================================================
  const handleSendResetOtp = async (e) => {
    e.preventDefault();
    const targetEmail = resetEmail.trim();
    if (!targetEmail) {
      setAlertMessage({ type: 'error', text: 'Please enter your registered email address.' });
      return;
    }

    setIsLoading(true);
    setAlertMessage(null);

    try {
      const res = await api.sendPasswordResetOtp(targetEmail);
      setResetOtpSent(true);
      startCooldown(15);
      setAlertMessage(null);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsLoading(false);
    }
  };

  const handleResetPassword = async (e) => {
    e.preventDefault();
    const targetEmail = resetEmail.trim();
    if (newPassword.length < 8) {
      setAlertMessage({ type: 'error', text: 'New password must be at least 8 characters.' });
      return;
    }

    if (newPassword !== confirmPassword) {
      setAlertMessage({ type: 'error', text: 'New password and confirmation do not match.' });
      return;
    }

    setIsLoading(true);
    setAlertMessage(null);

    try {
      const res = await api.resetPasswordWithOtp(targetEmail, resetOtpCode.trim(), newPassword, confirmPassword);
      setAlertMessage({
        type: 'success',
        text: res.message || 'Password reset successfully! You can now log in.',
      });

      setTimeout(() => {
        if (onCancel) onCancel();
      }, 1600);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsLoading(false);
    }
  };

  // ── VIEW 1: SIGN UP EMAIL OTP VERIFICATION ─────────────────────────────────
  if (mode === 'verify-signup') {
    return (
      <form onSubmit={handleVerifySignupOtp} className="flex flex-col gap-3.5 animate-fadeIn">
        <div>
          <label className="block text-xs font-medium text-neutral-600 mb-1">
            Enter 6-Digit Verification Code
          </label>
          <input
            type="text"
            maxLength="6"
            required
            autoFocus
            value={signupOtpCode}
            onChange={(e) => setSignupOtpCode(e.target.value.replace(/\D/g, ''))}
            placeholder="• • • • • •"
            className="w-full text-center tracking-[8px] font-mono font-bold text-xl px-3.5 py-3 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-[#ff5722] focus:ring-1 focus:ring-[#ff5722] transition-all"
          />
        </div>

        <div className="flex items-center justify-between text-xs pt-0.5 bg-neutral-50/80 p-2.5 rounded-md border border-neutral-200">
          <span className="text-neutral-600 font-medium">Didn't receive code?</span>
          <button
            type="button"
            onClick={handleResendSignupOtp}
            disabled={isLoading || resendCooldown > 0}
            className="text-[#ff5722] font-semibold hover:underline disabled:opacity-50 disabled:no-underline cursor-pointer flex items-center gap-1"
          >
            {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : 'Resend OTP Code'}
          </button>
        </div>

        <button
          type="submit"
          disabled={isLoading || signupOtpCode.length !== 6}
          className="w-full mt-1 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-sm shadow-xs hover:shadow-md transition-all duration-200 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-75"
        >
          {isLoading ? (
            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          ) : (
            <span>Verify &amp; Activate Account</span>
          )}
        </button>

        <div className="pt-1 text-center text-xs text-neutral-600">
          <button
            type="button"
            onClick={onCancel}
            className="text-[#0066cc] hover:underline font-normal cursor-pointer"
          >
            ← Back to profile details / edit email
          </button>
        </div>
      </form>
    );
  }

  // ── VIEW 2: FORGOT PASSWORD (EMAIL OTP AUTHENTICATION) ───────────────────────
  return (
    <div className="flex flex-col gap-3.5 animate-fadeIn">
      {!resetOtpSent ? (
        <form onSubmit={handleSendResetOtp} className="flex flex-col gap-3.5">
          <div>
            <label className="block text-xs font-medium text-neutral-600 mb-1">
              Registered Email Address
            </label>
            <input
              type="email"
              required
              autoFocus
              value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
              placeholder="e.g. student@example.com"
              className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-[#ff5722] focus:ring-1 focus:ring-[#ff5722] text-sm transition-all"
            />
          </div>

          <button
            type="submit"
            disabled={isLoading || !resetEmail.trim()}
            className="w-full mt-1 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-sm shadow-xs hover:shadow-md transition-all duration-200 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-75"
          >
            {isLoading ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <span>Send Reset Code</span>
            )}
          </button>
        </form>
      ) : (
        <form onSubmit={handleResetPassword} className="flex flex-col gap-3.5">
          <div className="text-xs text-neutral-600 bg-neutral-50 p-2.5 rounded-md border border-neutral-200">
            A 6-digit recovery code was sent to <strong className="text-neutral-900">{resetEmail}</strong>
          </div>

          <div>
            <input
              type="text"
              maxLength="6"
              required
              autoFocus
              value={resetOtpCode}
              onChange={(e) => setResetOtpCode(e.target.value.replace(/\D/g, ''))}
              placeholder="6-digit OTP code"
              className="w-full text-center tracking-[8px] font-mono font-bold text-lg px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 transition-all"
            />
          </div>

          <div className="relative">
            <input
              type={showNewPassword ? 'text' : 'password'}
              required
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="New password (min 8 characters)"
              className="w-full pl-3.5 pr-10 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
            />
            <button
              type="button"
              onClick={() => setShowNewPassword(!showNewPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-700 transition-colors focus:outline-none p-0.5 cursor-pointer"
            >
              <span className="material-symbols-outlined text-[19px]">
                {showNewPassword ? 'visibility_off' : 'visibility'}
              </span>
            </button>
          </div>

          <div>
            <input
              type="password"
              required
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm new password"
              className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
            />
          </div>

          <div className="flex items-center justify-between text-xs pt-0.5">
            <span className="text-neutral-500">Didn't get the code?</span>
            <button
              type="button"
              onClick={handleSendResetOtp}
              disabled={isLoading || resendCooldown > 0}
              className="text-[#0066cc] font-normal hover:underline disabled:opacity-50 disabled:no-underline cursor-pointer"
            >
              {resendCooldown > 0 ? `Resend OTP in ${resendCooldown}s` : 'Resend OTP'}
            </button>
          </div>

          <button
            type="submit"
            disabled={isLoading || resetOtpCode.length !== 6}
            className="w-full mt-1 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-sm shadow-xs hover:shadow-md transition-all duration-200 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-75"
          >
            {isLoading ? (
              <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : (
              <span>Set New Password</span>
            )}
          </button>
        </form>
      )}

      <div className="pt-2 text-center text-xs sm:text-sm text-neutral-600">
        <button
          type="button"
          onClick={onCancel}
          className="text-[#0066cc] hover:underline font-normal cursor-pointer"
        >
          Back to Log In
        </button>
      </div>
    </div>
  );
}
