import React, { useState, useEffect } from 'react';
import EarthCanvas from './EarthCanvas';
import OtpAuthModal from './OtpAuthModal';
import { useAuth } from '../context/AuthContext';
import api, { authAPI } from '../services/api';

// Official Google OAuth 2.0 Web Client ID from Google Cloud Console
const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ||
  '507577176126-kqjfjqqhsh8fc38gb8t5kpejielav77s.apps.googleusercontent.com';

export default function SignInPage({ onNavigateHome, onNavigateDashboard, initialNotice }) {
  const { login, register, setSession } = useAuth();

  // Navigation & view modes
  const [isSignUp, setIsSignUp] = useState(false);
  const [isVerifyingSignup, setIsVerifyingSignup] = useState(false);
  const [isForgotPassword, setIsForgotPassword] = useState(false);

  // Form inputs
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullname, setFullname] = useState('');
  const [role, setRole] = useState('student');
  const [expertise, setExpertise] = useState('');
  const [qualification, setQualification] = useState('');
  const [rememberMe, setRememberMe] = useState(true);

  // Feedback states
  const [isLoading, setIsLoading] = useState(false);
  const [alertMessage, setAlertMessage] = useState(() =>
    initialNotice ? { type: 'info', text: initialNotice } : null
  );
  const [resendCooldown, setResendCooldown] = useState(0);

  const startCooldown = (seconds = 30) => {
    setResendCooldown(seconds);
    const timer = setInterval(() => {
      setResendCooldown((prev) => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  // Check for email verification link in URL hash
  useEffect(() => {
    const hash = window.location.hash;
    if (hash && hash.includes('verify-email')) {
      const query = hash.includes('?') ? hash.split('?')[1] : '';
      const params = new URLSearchParams(query);
      const token = params.get('token');
      if (token) {
        setIsLoading(true);
        authAPI
          .verifyEmail(token)
          .then((res) => {
            setAlertMessage({
              type: 'pending_approval',
              text: res.data?.message || 'Email verified successfully! Your instructor account is awaiting administrator approval.'
            });
          })
          .catch((err) => {
            setAlertMessage({
              type: 'error',
              text: err.response?.data?.message || 'Email verification link is invalid or has expired.'
            });
          })
          .finally(() => setIsLoading(false));
      }
    }
  }, []);

  // =========================================================================
  // INITIALIZE GOOGLE IDENTITY SERVICES (OFFICIAL MODERN WEB SDK)
  // =========================================================================
  useEffect(() => {
    if (!window.google?.accounts?.id) return;

    try {
      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: async (response) => {
          if (response.credential) {
            try {
              const base64Url = response.credential.split('.')[1];
              const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
              const jsonPayload = decodeURIComponent(
                atob(base64)
                  .split('')
                  .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                  .join('')
              );
              const payload = JSON.parse(jsonPayload);
              await completeGoogleAuth(payload.email, payload.name, response.credential);
            } catch (err) {
              setAlertMessage({ type: 'error', text: 'Google authentication error: ' + err.message });
            }
          }
        },
      });
    } catch (err) {
      console.warn('Google Identity Services notice:', err);
    }
  }, [isSignUp, isForgotPassword, isVerifyingSignup]);

  // =========================================================================
  // GOOGLE SIGN-IN POPUP TRIGGER (accounts.google.com)
  // =========================================================================
  const handleGoogleSignIn = () => {
    if (!window.google?.accounts?.oauth2) {
      setAlertMessage({
        type: 'error',
        text: 'Google Sign-In SDK is loading. Please check your connection or refresh the page.',
      });
      return;
    }

    try {
      setIsLoading(true);
      setAlertMessage(null);

      const client = window.google.accounts.oauth2.initTokenClient({
        client_id: GOOGLE_CLIENT_ID,
        scope: 'openid email profile',
        callback: async (tokenResponse) => {
          if (tokenResponse.error) {
            setIsLoading(false);
            if (tokenResponse.error !== 'access_denied') {
              setAlertMessage({
                type: 'error',
                text: `Google Sign-In: ${tokenResponse.error_description || tokenResponse.error}`,
              });
            }
            return;
          }

          if (tokenResponse.access_token) {
            try {
              const userInfoRes = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
                headers: { Authorization: `Bearer ${tokenResponse.access_token}` },
              });
              const userInfo = await userInfoRes.json();
              await completeGoogleAuth(userInfo.email, userInfo.name, tokenResponse.access_token);
            } catch (err) {
              setAlertMessage({
                type: 'error',
                text: 'Failed to retrieve profile from Google: ' + err.message,
              });
            } finally {
              setIsLoading(false);
            }
          }
        },
      });

      client.requestAccessToken();
    } catch (err) {
      setIsLoading(false);
      setAlertMessage({ type: 'error', text: 'Error launching Google OAuth: ' + err.message });
    }
  };

  const completeGoogleAuth = async (googleEmail, googleName = '', idToken = '') => {
    setIsLoading(true);
    setAlertMessage(null);

    try {
      const data = await api.loginWithGoogle(googleEmail, googleName, idToken);
      localStorage.setItem('olp_token', data.token);
      localStorage.setItem('olp_user', JSON.stringify(data.user));
      if (setSession) setSession(data.token, data.user);

      setAlertMessage({
        type: 'success',
        text: `Connected with Google (${googleEmail})! Welcome, ${data.user.name}.`,
      });

      setTimeout(() => {
        setAlertMessage(null);
        if (onNavigateDashboard) {
          onNavigateDashboard();
        } else if (onNavigateHome) {
          onNavigateHome();
        }
      }, 1200);
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || err.message;
      setAlertMessage({ type: 'error', text: msg });
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setAlertMessage({
      type: 'info',
      text: isSignUp ? 'Creating your account...' : 'Logging in to your account...'
    });

    try {
      if (isSignUp) {
        const extra = role === 'instructor' ? { expertise, qualification } : {};
        const result = await register(fullname, email, password, role, extra);
        setIsLoading(false);
        if (result.success) {
          if (role === 'instructor') {
            setAlertMessage({
              type: 'pending_approval',
              text: result.message || 'Registration submitted! Your instructor account is awaiting admin approval. Please check your email for the verification link.',
              verificationLink: result.verificationLink
            });
            setTimeout(() => {
              setIsSignUp(false);
            }, 3500);
          } else {
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
          }
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
            text: result.message || 'Login successful! Redirecting...'
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

  const handleOtpSuccess = (token, user) => {
    localStorage.setItem('olp_token', token);
    localStorage.setItem('olp_user', JSON.stringify(user));
    if (setSession) setSession(token, user);

    setAlertMessage({
      type: 'success',
      text: `Welcome to Focotech, ${user.name}! Workspace ready.`,
    });

    setTimeout(() => {
      setAlertMessage(null);
      if (onNavigateDashboard) {
        onNavigateDashboard();
      } else if (onNavigateHome) {
        onNavigateHome();
      }
    }, 1400);
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
            <div className="pt-1">
              <button
                type="button"
                onClick={onNavigateHome}
                className="text-primary-container hover:text-secondary-container font-medium text-sm transition-colors hover:underline flex items-center gap-1 group cursor-pointer"
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
            RIGHT PANEL: Clean Login / Sign Up Form
            ========================================================================= */}
        <div className="lg:col-span-6 bg-white flex flex-col justify-center px-6 sm:px-12 lg:px-20 py-12 relative">
          {/* Top Return Link */}
          <div className="absolute top-8 right-8 hidden lg:flex items-center">
            <button
              type="button"
              onClick={onNavigateHome}
              className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-md hover:bg-neutral-100 text-neutral-600 hover:text-neutral-900 text-xs font-semibold transition-all duration-200 cursor-pointer"
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

            {/* Alert Banner */}
            {alertMessage && (
              <div
                className={`p-3 rounded-md text-xs sm:text-sm font-medium flex flex-col gap-1 transition-all animate-fadeIn ${
                  alertMessage.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                    : alertMessage.type === 'pending_approval'
                      ? 'bg-amber-50 text-amber-900 border border-amber-300'
                      : alertMessage.type === 'error'
                        ? 'bg-rose-50 text-rose-800 border border-rose-200'
                        : 'bg-primary-fixed/60 text-on-primary-fixed border border-primary-fixed'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px]">
                    {alertMessage.type === 'success'
                      ? 'check_circle'
                      : alertMessage.type === 'pending_approval'
                        ? 'hourglass_top'
                        : alertMessage.type === 'error'
                          ? 'error'
                          : 'info'}
                  </span>
                  <span>{alertMessage.text}</span>
                </div>
                {alertMessage.verificationLink && (
                  <div className="mt-1 pt-1 border-t border-amber-200 text-xs">
                    <span className="font-semibold">Simulated Verification Link: </span>
                    <a
                      href={alertMessage.verificationLink}
                      className="underline break-all text-amber-950 font-mono hover:text-black"
                    >
                      {alertMessage.verificationLink}
                    </a>
                  </div>
                )}
              </div>
            )}

            {/* ===================================================================
                VIEW A: CREDENTIALS LOGIN & SIGN UP FORM
                =================================================================== */}
            {!isForgotPassword && !isVerifyingSignup && (
              <>
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

                  <div>
                    <input
                      type="email"
                      required
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      placeholder="Your username or email"
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

                  {/* FR-I01: Instructor specific registration fields */}
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

                  {/* Primary Action Button */}
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

                    {!isSignUp && (
                      <button
                        type="button"
                        onClick={() => {
                          setIsForgotPassword(true);
                          setAlertMessage(null);
                        }}
                        className="text-[#0066cc] hover:underline font-normal text-xs sm:text-sm cursor-pointer"
                      >
                        Forgot password?
                      </button>
                    )}
                  </div>
                </form>

                <div className="relative flex items-center my-0.5">
                  <div className="flex-grow border-t border-neutral-200" />
                  <span className="px-3 text-xs text-neutral-400 bg-white">or</span>
                  <div className="flex-grow border-t border-neutral-200" />
                </div>

                {/* Google Sign-In Button */}
                <div>
                  <button
                    type="button"
                    onClick={handleGoogleSignIn}
                    disabled={isLoading}
                    className="w-full flex items-center justify-center gap-2.5 py-2.5 px-4 rounded-md border border-neutral-300 hover:bg-neutral-50 text-neutral-800 text-xs sm:text-sm font-medium transition-colors shadow-2xs cursor-pointer disabled:opacity-50"
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
                </div>

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
                        className="text-[#0066cc] hover:underline font-normal cursor-pointer"
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
                        className="text-[#0066cc] hover:underline font-normal cursor-pointer"
                      >
                        Sign up
                      </button>
                    </span>
                  )}
                </div>
              </>
            )}

            {/* ===================================================================
                VIEW B & C: DELEGATED TO PERSON 2 (OtpAuthModal)
                =================================================================== */}
            {isSignUp && isVerifyingSignup && (
              <OtpAuthModal
                mode="verify-signup"
                email={email}
                onSuccessSession={handleOtpSuccess}
                onCancel={() => {
                  setIsVerifyingSignup(false);
                  setAlertMessage(null);
                }}
                setAlertMessage={setAlertMessage}
                startCooldown={startCooldown}
                resendCooldown={resendCooldown}
              />
            )}

            {isForgotPassword && (
              <OtpAuthModal
                mode="forgot-password"
                email={email}
                onSuccessSession={handleOtpSuccess}
                onCancel={() => {
                  setIsForgotPassword(false);
                  setAlertMessage(null);
                }}
                setAlertMessage={setAlertMessage}
                startCooldown={startCooldown}
                resendCooldown={resendCooldown}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

