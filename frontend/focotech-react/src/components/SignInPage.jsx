import React, { useState } from 'react';
import EarthCanvas from './EarthCanvas';
import { useAuth } from '../context/AuthContext';

export default function SignInPage({ onNavigateHome, onNavigateDashboard, initialNotice }) {
  const { login, register } = useAuth();
  const [isSignUp, setIsSignUp] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullname, setFullname] = useState('');
  const [role, setRole] = useState('student');
  const [rememberMe, setRememberMe] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState(() =>
    initialNotice ? { type: 'info', text: initialNotice } : null
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setAlertMessage({
      type: 'info',
      text: isSignUp ? 'Creating your account...' : 'Logging in to your account...'
    });

    try {
      if (isSignUp) {
        const result = await register(fullname, email, password, role);
        setIsLoading(false);
        if (result.success) {
          setAlertMessage({
            type: 'success',
            text: result.message || 'Account created successfully! Redirecting...'
          });
          setTimeout(() => {
            setAlertMessage(null);
            if (onNavigateDashboard) {
              onNavigateDashboard();
            } else if (onNavigateHome) {
              onNavigateHome();
            }
          }, 1200);
        } else {
          setAlertMessage({
            type: 'error',
            text: result.message
          });
        }
      } else {
        const result = await login(email, password);
        setIsLoading(false);
        if (result.success) {
          setAlertMessage({
            type: 'success',
            text: result.message || 'Login successful! Redirecting to student dashboard...'
          });
          setTimeout(() => {
            setAlertMessage(null);
            if (onNavigateDashboard) {
              onNavigateDashboard();
            } else if (onNavigateHome) {
              onNavigateHome();
            }
          }, 1200);
        } else {
          setAlertMessage({
            type: 'error',
            text: result.message
          });
        }
      }
    } catch {
      setIsLoading(false);
      setAlertMessage({
        type: 'error',
        text: 'An unexpected error occurred. Please check if the backend server is running.'
      });
    }
  };

  const handleSocialAuth = (provider) => {
    setAlertMessage({
      type: 'info',
      text: `Connecting with ${provider}...`
    });
    setTimeout(() => {
      setAlertMessage({
        type: 'success',
        text: `Authenticated with ${provider}! Redirecting...`
      });
      setTimeout(() => {
        setAlertMessage(null);
        if (onNavigateHome) onNavigateHome();
      }, 1200);
    }, 900);
  };

  return (
    <div className="min-h-screen w-full flex flex-col bg-white font-body-md text-on-surface antialiased selection:bg-primary-container selection:text-on-primary">
      {/* Top Accent Line matching theme */}
      <div className="h-1 w-full bg-gradient-to-r from-primary via-primary-container to-secondary-container shrink-0" />

      {/* Main Split Screen Container */}
      <div className="flex-1 w-full grid grid-cols-1 lg:grid-cols-12 min-h-[calc(100vh-4px)]">
        {/* =========================================================================
            LEFT PANEL: Dark Ambient 3D Rotating Earth matching HackerRank design
            ========================================================================= */}
        <div className="lg:col-span-6 bg-[#0c0a09] text-white relative flex flex-col justify-between p-8 sm:p-12 lg:p-16 overflow-hidden min-h-[460px] lg:min-h-full">
          {/* Moving 3D Earth Animation Canvas */}
          <EarthCanvas />

          {/* Top Brand Logo matching HackerRank's block aesthetic */}
          <div className="relative z-10 flex items-center justify-between">
            <button
              type="button"
              onClick={onNavigateHome}
              className="flex items-center gap-2 group cursor-pointer focus:outline-none"
            >
              <div className="flex items-center gap-1">
                <div className="w-5 h-7 rounded-[3px] bg-primary-container flex items-center justify-center font-bold text-white text-xs shadow-md group-hover:scale-105 transition-transform">
                  F
                </div>
                <div className="w-1.5 h-7 rounded-[3px] bg-secondary-container" />
              </div>
              <span className="font-headline-md text-xl sm:text-2xl text-white font-bold tracking-tight ml-1 group-hover:text-primary-container transition-colors">
                Focotech
              </span>
            </button>

            {/* Mobile return link */}
            <button
              type="button"
              onClick={onNavigateHome}
              className="lg:hidden text-white/70 hover:text-white flex items-center gap-1 text-xs sm:text-sm font-semibold transition-colors"
            >
              <span>Back</span>
              <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>

          {/* Community Copy (Bottom Left anchor exactly as in HackerRank) */}
          <div className="relative z-10 mt-auto pt-16 flex flex-col gap-2 max-w-md">
            <span className="text-white/80 font-normal text-base sm:text-lg">
              Welcome to
            </span>
            <h1 className="font-headline-xl text-3xl sm:text-4xl lg:text-5xl font-extrabold text-white tracking-tight leading-tight">
              Focotech Community
            </h1>
            <p className="text-white/60 text-sm sm:text-base pt-1">
              Home to 120,000+ developers &amp; designers worldwide
            </p>
            <div className="pt-1">
              <button
                type="button"
                onClick={onNavigateHome}
                className="text-primary-container hover:text-secondary-container font-medium text-sm transition-colors hover:underline flex items-center gap-1 group"
              >
                <span>Know more</span>
                <span className="material-symbols-outlined text-[14px] group-hover:translate-x-0.5 transition-transform">
                  arrow_forward
                </span>
              </button>
            </div>
          </div>
        </div>

        {/* =========================================================================
            RIGHT PANEL: Clean Login Form directly structured like HackerRank
            ========================================================================= */}
        <div className="lg:col-span-6 bg-white flex flex-col justify-center px-6 sm:px-12 lg:px-20 py-12 relative">
          {/* Top Return Link */}
          <div className="absolute top-8 right-8 hidden lg:flex items-center">
            <button
              type="button"
              onClick={onNavigateHome}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md hover:bg-neutral-100 text-neutral-600 hover:text-neutral-900 text-xs font-semibold transition-all duration-200"
            >
              <span className="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>Back to Campus</span>
            </button>
          </div>

          <div className="w-full max-w-[420px] mx-auto flex flex-col gap-6">
            {/* Form Headers */}
            <div className="flex flex-col">
              <span className="text-neutral-700 font-semibold text-lg sm:text-xl">
                {isSignUp ? 'Create your profile' : 'Welcome back!'}
              </span>
              <h2 className="text-neutral-900 font-bold text-2xl sm:text-3xl tracking-tight mt-0.5">
                {isSignUp ? 'Sign up for Focotech' : 'Login to your account'}
              </h2>
              <p className="text-neutral-500 text-xs sm:text-sm mt-1.5">
                {isSignUp
                  ? 'Join 120,000+ builders mastering real-world software.'
                  : "It's nice to see you again. Ready to code?"}
              </p>
            </div>

            {/* Alert / Notification Feedback */}
            {alertMessage && (
              <div
                className={`p-3 rounded-md text-xs sm:text-sm font-medium flex items-center gap-2 transition-all animate-fadeIn ${
                  alertMessage.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                    : alertMessage.type === 'error'
                    ? 'bg-rose-50 text-rose-800 border border-rose-200'
                    : 'bg-primary-fixed/60 text-on-primary-fixed border border-primary-fixed'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">
                  {alertMessage.type === 'success'
                    ? 'check_circle'
                    : alertMessage.type === 'error'
                    ? 'error'
                    : 'info'}
                </span>
                <span>{alertMessage.text}</span>
              </div>
            )}

            {/* Form Inputs & Submit */}
            <form onSubmit={handleSubmit} className="flex flex-col gap-3.5">
              {/* Role selection when signing up */}
              {isSignUp && (
                <div className="flex flex-col gap-1.5 animate-fadeIn">
                  <label className="text-xs font-semibold text-neutral-600">Register as:</label>
                  <div className="grid grid-cols-2 gap-2 p-1 bg-neutral-100 rounded-lg">
                    <button
                      type="button"
                      onClick={() => setRole('student')}
                      className={`py-1.5 px-3 rounded-md text-xs font-semibold transition-all cursor-pointer ${
                        role === 'student'
                          ? 'bg-white text-neutral-900 shadow-xs'
                          : 'text-neutral-500 hover:text-neutral-800'
                      }`}
                    >
                      🎓 Student
                    </button>
                    <button
                      type="button"
                      onClick={() => setRole('instructor')}
                      className={`py-1.5 px-3 rounded-md text-xs font-semibold transition-all cursor-pointer ${
                        role === 'instructor'
                          ? 'bg-white text-neutral-900 shadow-xs'
                          : 'text-neutral-500 hover:text-neutral-800'
                      }`}
                    >
                      👨‍🏫 Instructor
                    </button>
                  </div>
                </div>
              )}

              {/* Optional Name for Sign Up */}
              {isSignUp && (
                <div className="animate-fadeIn">
                  <input
                    type="text"
                    required
                    value={fullname}
                    onChange={(e) => setFullname(e.target.value)}
                    placeholder="Your full name"
                    className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
                  />
                </div>
              )}

              {/* Username or Email Input */}
              <div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="Your email address"
                  className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
                />
              </div>

              {/* Password Input with show/hide toggle */}
              <div className="relative">
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  minLength={8}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder={isSignUp ? "Password (min 8 characters)" : "Your password"}
                  className="w-full pl-3.5 pr-10 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-700 transition-colors focus:outline-none p-0.5"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  <span className="material-symbols-outlined text-[19px]">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>

              {/* Primary Action Button */}
              <button
                type="submit"
                disabled={isLoading}
                className="w-full mt-1 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-sm shadow-xs hover:shadow-md transition-all duration-200 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-75"
              >
                {isLoading ? (
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <span>{isSignUp ? 'Sign Up' : 'Log In'}</span>
                )}
              </button>

              {/* Remember Me & Forgot Password Row */}
              <div className="flex items-center justify-between text-xs sm:text-sm pt-1">
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={(e) => setRememberMe(e.target.checked)}
                    className="w-3.5 h-3.5 rounded border-neutral-300 text-neutral-900 focus:ring-neutral-900 accent-neutral-900 cursor-pointer"
                  />
                  <span className="text-neutral-700">Remember me</span>
                </label>

                {!isSignUp && (
                  <button
                    type="button"
                    onClick={() =>
                      setAlertMessage({
                        type: 'info',
                        text: 'Password recovery email dispatched to your inbox.'
                      })
                    }
                    className="text-[#0066cc] hover:underline font-normal text-xs sm:text-sm"
                  >
                    Forgot password?
                  </button>
                )}
              </div>
            </form>

            {/* Divider: or */}
            <div className="relative flex items-center my-0.5">
              <div className="flex-grow border-t border-neutral-200" />
              <span className="px-3 text-xs text-neutral-400 bg-white">
                or
              </span>
              <div className="flex-grow border-t border-neutral-200" />
            </div>

            {/* Social Logins */}
            <div className="flex flex-col gap-2.5">
              {/* Google Button */}
              <button
                type="button"
                onClick={() => handleSocialAuth('Google')}
                className="w-full flex items-center justify-center gap-2.5 py-2 px-4 rounded-md border border-neutral-300 hover:bg-neutral-50 text-neutral-800 text-xs sm:text-sm font-medium transition-colors shadow-2xs"
              >
                <svg className="w-4 h-4 shrink-0" viewBox="0 0 24 24">
                  <path
                    d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.8-2.4 3.66v3.05h3.88c2.27-2.09 3.66-5.17 3.66-9.15z"
                    fill="#4285F4"
                  />
                  <path
                    d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.33 24 12 24z"
                    fill="#34A853"
                  />
                  <path
                    d="M5.28 14.27c-.25-.72-.38-1.49-.38-2.27s.13-1.55.38-2.27V6.58H1.25C.45 8.18 0 9.99 0 12s.45 3.82 1.25 5.42l4.03-3.15z"
                    fill="#FBBC05"
                  />
                  <path
                    d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.33 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"
                    fill="#EA4335"
                  />
                </svg>
                <span>Continue with Google</span>
              </button>

              {/* LinkedIn & GitHub Buttons Row */}
              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => handleSocialAuth('LinkedIn')}
                  className="flex items-center justify-center gap-2 py-2 px-3 rounded-md border border-neutral-300 hover:bg-neutral-50 text-neutral-800 text-xs sm:text-sm font-medium transition-colors shadow-2xs"
                >
                  <svg className="w-4 h-4 fill-[#0A66C2] shrink-0" viewBox="0 0 24 24">
                    <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.88 8.56a1.68 1.68 0 0 0 1.68-1.68c0-.93-.75-1.69-1.68-1.69a1.69 1.69 0 0 0-1.69 1.69c0 .93.76 1.68 1.69 1.68m1.39 9.94v-8.37H5.5v8.37h2.77z" />
                  </svg>
                  <span>LinkedIn</span>
                </button>

                <button
                  type="button"
                  onClick={() => handleSocialAuth('GitHub')}
                  className="flex items-center justify-center gap-2 py-2 px-3 rounded-md border border-neutral-300 hover:bg-neutral-50 text-neutral-800 text-xs sm:text-sm font-medium transition-colors shadow-2xs"
                >
                  <svg className="w-4 h-4 fill-[#24292F] shrink-0" viewBox="0 0 24 24">
                    <path
                      fillRule="evenodd"
                      clipRule="evenodd"
                      d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"
                    />
                  </svg>
                  <span>GitHub</span>
                </button>
              </div>
            </div>

            {/* Bottom Sign up switch */}
            <div className="pt-2 text-center text-xs sm:text-sm text-neutral-600">
              {isSignUp ? (
                <span>
                  Already have an account?{' '}
                  <button
                    type="button"
                    onClick={() => {
                      setIsSignUp(false);
                      setAlertMessage(null);
                    }}
                    className="text-[#0066cc] hover:underline font-normal"
                  >
                    Log In
                  </button>
                </span>
              ) : (
                <span>
                  Don't have an account?{' '}
                  <button
                    type="button"
                    onClick={() => {
                      setIsSignUp(true);
                      setAlertMessage(null);
                    }}
                    className="text-[#0066cc] hover:underline font-normal"
                  >
                    Sign up
                  </button>
                </span>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
