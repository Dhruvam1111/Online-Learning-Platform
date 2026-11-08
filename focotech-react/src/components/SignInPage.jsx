import React, { useState, useEffect } from 'react';
import EarthCanvas from './EarthCanvas';

export default function SignInPage({ onNavigateHome, onNavigateDashboard, initialNotice }) {
  const [isSignUp, setIsSignUp] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullname, setFullname] = useState('');
  const [role, setRole] = useState('student');
  const [expertise, setExpertise] = useState('');
  const [qualification, setQualification] = useState('');
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
      const url = isSignUp ? '/api/auth/register' : '/api/auth/login';
      const payload = isSignUp
        ? {
            name: fullname,
            email,
            password,
            role,
            ...(role === 'instructor' ? { expertise, qualification } : {})
          }
        : { email, password };

      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const data = await res.json();
      setIsLoading(false);

      if (res.ok) {
        if (isSignUp && role === 'instructor') {
          setAlertMessage({
            type: 'pending_approval',
            text: data.message || 'Registration submitted! Your instructor account is awaiting admin approval. Please check your email for the verification link.',
            verificationLink: data.verificationLink
          });
          setTimeout(() => setIsSignUp(false), 3500);
        } else {
          setAlertMessage({
            type: 'success',
            text: data.message || (isSignUp ? 'Account created successfully!' : 'Login successful!')
          });
          if (data.token) {
            localStorage.setItem('olp_token', data.token);
            if (data.user) localStorage.setItem('olp_user', JSON.stringify(data.user));
          }
          setTimeout(() => {
            setAlertMessage(null);
            if (onNavigateDashboard) onNavigateDashboard();
            else if (onNavigateHome) onNavigateHome();
          }, 1200);
        }
      } else {
        const isPending =
          data.message?.toLowerCase().includes('awaiting admin approval') ||
          data.message?.toLowerCase().includes('pending');

        setAlertMessage({
          type: isPending ? 'pending_approval' : 'error',
          text: data.message || 'Authentication failed'
        });
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
      <div className="h-1 w-full bg-gradient-to-r from-primary via-primary-container to-secondary-container shrink-0" />

      <div className="flex-1 w-full grid grid-cols-1 lg:grid-cols-12 min-h-[calc(100vh-4px)]">
        <div className="lg:col-span-6 bg-[#0c0a09] text-white relative flex flex-col justify-between p-8 sm:p-12 lg:p-16 overflow-hidden min-h-[460px] lg:min-h-full">
          <EarthCanvas />

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

            <button
              type="button"
              onClick={onNavigateHome}
              className="lg:hidden text-white/70 hover:text-white flex items-center gap-1 text-xs sm:text-sm font-semibold transition-colors"
            >
              <span>Back</span>
              <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>

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
          </div>
        </div>

        <div className="lg:col-span-6 bg-white flex flex-col justify-center px-6 sm:px-12 lg:px-20 py-12 relative overflow-y-auto">
          <div className="w-full max-w-[420px] mx-auto flex flex-col gap-6">
            <div className="flex flex-col">
              <span className="text-neutral-700 font-semibold text-lg sm:text-xl">
                {isSignUp ? (role === 'instructor' ? 'Instructor Portal' : 'Create your profile') : 'Welcome back!'}
              </span>
              <h2 className="text-neutral-900 font-bold text-2xl sm:text-3xl tracking-tight mt-0.5">
                {isSignUp
                  ? (role === 'instructor' ? 'Register as Instructor' : 'Sign up for Focotech')
                  : 'Login to your account'}
              </h2>
            </div>

            {alertMessage && (
              alertMessage.type === 'pending_approval' ? (
                <div className="p-3.5 rounded-md text-xs sm:text-sm font-medium bg-amber-50 text-amber-900 border border-amber-300 flex flex-col gap-1.5 animate-fadeIn">
                  <div className="flex items-center gap-2 font-semibold">
                    <span className="material-symbols-outlined text-[18px] text-amber-600">hourglass_top</span>
                    <span>Awaiting Admin Approval</span>
                  </div>
                  <p className="text-amber-800 text-xs font-normal leading-relaxed">{alertMessage.text}</p>
                  {alertMessage.verificationLink && (
                    <div className="mt-1 pt-1.5 border-t border-amber-200 text-[11px] text-amber-700 break-all">
                      <span className="font-semibold">Verification link sent to email. Link: </span>
                      <a href={alertMessage.verificationLink} className="underline hover:text-amber-950 font-mono" target="_blank" rel="noreferrer">
                        {alertMessage.verificationLink}
                      </a>
                    </div>
                  )}
                </div>
              ) : (
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
              )
            )}

            <form onSubmit={handleSubmit} className="flex flex-col gap-3.5">
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
                >
                  <span className="material-symbols-outlined text-[19px]">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>

              {isSignUp && role === 'instructor' && (
                <>
                  <div className="animate-fadeIn">
                    <input
                      type="text"
                      required
                      value={expertise}
                      onChange={(e) => setExpertise(e.target.value)}
                      placeholder="Primary expertise (e.g. Distributed Systems, AI, Web)"
                      className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
                    />
                  </div>
                  <div className="animate-fadeIn">
                    <input
                      type="text"
                      required
                      value={qualification}
                      onChange={(e) => setQualification(e.target.value)}
                      placeholder="Highest qualification (e.g. Ph.D. in CS, M.Tech, Lead Architect)"
                      className="w-full px-3.5 py-2.5 rounded-md border border-neutral-300 bg-white text-neutral-900 placeholder:text-neutral-400 focus:outline-none focus:border-neutral-500 focus:ring-1 focus:ring-neutral-500 text-sm transition-all"
                    />
                  </div>
                </>
              )}

              <button
                type="submit"
                disabled={isLoading}
                className="w-full mt-1 py-2.5 px-4 rounded-md bg-primary-container hover:bg-secondary-container text-white font-medium text-sm shadow-xs hover:shadow-md transition-all duration-200 active:scale-[0.99] flex items-center justify-center gap-2 cursor-pointer disabled:opacity-75"
              >
                {isLoading ? (
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                ) : (
                  <span>
                    {isSignUp
                      ? (role === 'instructor' ? 'Submit Instructor Registration' : 'Sign Up')
                      : 'Log In'}
                  </span>
                )}
              </button>

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
              </div>
            </form>

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
