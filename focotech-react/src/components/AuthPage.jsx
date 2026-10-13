import React, { useState, useEffect } from 'react';
import StudentCardPreview from './StudentCardPreview';
import {
  DISCIPLINES,
  UNIVERSITIES,
  ACADEMIC_YEARS,
  STUDY_GOALS,
  DEMO_STUDENTS,
  LIVE_CAMPUS_FEED
} from '../data/studentDisciplines';

export default function AuthPage({ initialMode = 'signin', onBackToHome, onLoginSuccess }) {
  const [mode, setMode] = useState(initialMode); // 'signin' or 'signup'
  
  // Registration Form State
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [university, setUniversity] = useState(UNIVERSITIES[0].name);
  const [customMajor, setCustomMajor] = useState('');
  const [selectedDiscipline, setSelectedDiscipline] = useState('business');
  const [academicYear, setAcademicYear] = useState('sophomore');
  const [studyGoal, setStudyGoal] = useState('exams');
  const [studentId, setStudentId] = useState('STU-2026-8941');
  const [agreeTerms, setAgreeTerms] = useState(true);

  // Sign In Form State
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(true);

  // UI States
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isEduVerified, setIsEduVerified] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successToast, setSuccessToast] = useState('');
  const [feedIndex, setFeedIndex] = useState(0);

  // Live rotating campus feed
  useEffect(() => {
    const timer = setInterval(() => {
      setFeedIndex((prev) => (prev + 1) % LIVE_CAMPUS_FEED.length);
    }, 4500);
    return () => clearInterval(timer);
  }, []);

  // Detect .edu domain or partner university domain
  useEffect(() => {
    const activeEmail = mode === 'signup' ? email : loginEmail;
    if (!activeEmail || !activeEmail.includes('@')) {
      setIsEduVerified(false);
      return;
    }
    const domain = activeEmail.split('@')[1]?.toLowerCase() || '';
    const isEdu = domain.endsWith('.edu') || domain.endsWith('.ac.uk') || domain.endsWith('.edu.sg') || domain.includes('.ac.');
    setIsEduVerified(isEdu);

    // Auto match university if domain matches
    if (mode === 'signup' && isEdu) {
      const matchedUni = UNIVERSITIES.find(
        (u) => u.domain && domain.includes(u.domain.split('.')[0])
      );
      if (matchedUni) {
        setUniversity(matchedUni.name);
      }
    }
  }, [email, loginEmail, mode]);

  // Generate deterministic student ID preview as name changes
  useEffect(() => {
    if (fullName.trim()) {
      const code = Math.abs(
        fullName.split('').reduce((acc, char) => acc * 31 + char.charCodeAt(0), 7)
      )
        .toString()
        .slice(0, 4);
      const uniPrefix = university.split(' ')[0].toUpperCase().slice(0, 4);
      setStudentId(`${uniPrefix}-2026-${code}`);
    }
  }, [fullName, university]);

  // Password Strength calculation
  const getPasswordStrength = (pass) => {
    if (!pass) return { score: 0, label: 'Enter password', color: 'bg-surface-variant' };
    let score = 0;
    if (pass.length >= 8) score += 1;
    if (/[A-Z]/.test(pass)) score += 1;
    if (/[0-9]/.test(pass)) score += 1;
    if (/[^A-Za-z0-9]/.test(pass)) score += 1;

    switch (score) {
      case 1:
        return { score: 1, label: 'Dorm Wi-Fi vulnerable 🚨', color: 'bg-error' };
      case 2:
        return { score: 2, label: 'Basic campus security 🔓', color: 'bg-amber-500' };
      case 3:
        return { score: 3, label: 'Library study room safe 🔒', color: 'bg-emerald-500' };
      case 4:
      default:
        return { score: 4, label: 'Campus Vault Secured 🛡️', color: 'bg-primary-container' };
    }
  };

  const passStrength = getPasswordStrength(password);

  // Load Demo Student Credentials
  const handleSelectDemo = (demo) => {
    if (mode === 'signin') {
      setLoginEmail(demo.email);
      setLoginPassword(demo.password);
      setSuccessToast(`Filled credentials for ${demo.name} (${demo.majorName})`);
    } else {
      setFullName(demo.name);
      setEmail(demo.email);
      setPassword(demo.password);
      setConfirmPassword(demo.password);
      setUniversity(demo.university);
      setSelectedDiscipline(demo.disciplineId);
      setCustomMajor(demo.majorName);
      setAcademicYear(demo.year);
      setStudyGoal(demo.goal);
      setSuccessToast(`Pre-filled profile for ${demo.name}!`);
    }
    setTimeout(() => setSuccessToast(''), 3500);
  };

  // Submit Sign In
  const handleSignInSubmit = (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!loginEmail || !loginPassword) {
      setErrorMsg('Please enter both your university email and password.');
      return;
    }

    // Check if matched demo user or generic scholar
    const matchedDemo = DEMO_STUDENTS.find(
      (d) => d.email.toLowerCase() === loginEmail.toLowerCase()
    );

    const loggedInUser = matchedDemo || {
      id: 'student-' + Date.now(),
      name: loginEmail.split('@')[0].replace(/[._]/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase()),
      email: loginEmail,
      university: loginEmail.includes('stanford')
        ? 'Stanford University'
        : loginEmail.includes('ox')
        ? 'University of Oxford'
        : 'State University',
      disciplineId: 'business',
      majorName: 'Interdisciplinary Studies',
      year: 'sophomore',
      studentId: 'STU-2026-9041',
      streakDays: 3,
      avatarInitials: loginEmail.slice(0, 2).toUpperCase(),
      goal: 'exams'
    };

    onLoginSuccess(loggedInUser);
  };

  // Submit Sign Up
  const handleSignUpSubmit = (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!fullName.trim()) {
      setErrorMsg('Please enter your full student name.');
      return;
    }
    if (!email.trim() || !email.includes('@')) {
      setErrorMsg('Please provide a valid university or student email.');
      return;
    }
    if (password.length < 6) {
      setErrorMsg('Password should be at least 6 characters long.');
      return;
    }
    if (password !== confirmPassword) {
      setErrorMsg('Passwords do not match. Please verify your password.');
      return;
    }
    if (!agreeTerms) {
      setErrorMsg('Please agree to the Campus Academic Honor Code.');
      return;
    }

    const currentDiscipline = DISCIPLINES.find((d) => d.id === selectedDiscipline) || DISCIPLINES[0];

    const newStudent = {
      id: 'student-' + Date.now(),
      name: fullName.trim(),
      email: email.trim(),
      university: university,
      disciplineId: selectedDiscipline,
      majorName: customMajor.trim() || currentDiscipline.name,
      year: academicYear,
      studentId: studentId,
      streakDays: 1,
      avatarInitials: fullName
        .split(' ')
        .map((n) => n[0])
        .join('')
        .toUpperCase()
        .slice(0, 2) || 'ST',
      goal: studyGoal,
      isEduVerified: isEduVerified
    };

    onLoginSuccess(newStudent);
  };

  const currentActiveDiscipline = DISCIPLINES.find((d) => d.id === selectedDiscipline) || DISCIPLINES[0];

  return (
    <div className="min-h-screen w-full bg-surface text-on-surface flex flex-col justify-between selection:bg-primary-container selection:text-on-primary relative overflow-x-hidden">
      {/* Background Ambient University Aura Blobs */}
      <div className="pointer-events-none fixed -top-40 -left-40 w-[550px] h-[550px] rounded-full bg-primary-fixed/25 blur-[120px] -z-10" />
      <div className="pointer-events-none fixed top-1/3 -right-40 w-[600px] h-[600px] rounded-full bg-secondary-fixed/20 blur-[130px] -z-10" />
      <div className="pointer-events-none fixed -bottom-40 left-1/3 w-[500px] h-[500px] rounded-full bg-tertiary-fixed/20 blur-[120px] -z-10" />

      {/* Top Navbar Header */}
      <header className="w-full max-w-7xl mx-auto px-6 py-5 flex items-center justify-between z-20">
        {/* Brand Logo & Back Home */}
        <button
          type="button"
          onClick={onBackToHome}
          className="flex items-center gap-3 group cursor-pointer text-left focus:outline-none"
        >
          <div className="w-10 h-10 rounded-xl bg-primary-container flex items-center justify-center text-on-primary font-bold shadow-md group-hover:scale-105 group-hover:shadow-orange-500/30 transition-all duration-300">
            <span className="material-symbols-outlined text-[22px]">school</span>
          </div>
          <div className="flex flex-col">
            <span className="font-headline-md text-on-surface tracking-tight font-bold group-hover:text-primary transition-colors flex items-center gap-1.5">
              Focotech <span className="text-xs px-2 py-0.5 rounded-full bg-primary/10 text-primary font-semibold">Campus</span>
            </span>
            <span className="text-[11px] text-on-surface-variant font-medium">
              Multi-Disciplinary University Network
            </span>
          </div>
        </button>

        {/* Back to Home Link */}
        <button
          type="button"
          onClick={onBackToHome}
          className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-semibold text-on-surface-variant hover:text-primary transition-colors px-3 py-1.5 rounded-full hover:bg-surface-container border border-transparent hover:border-outline-variant/30"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          <span>Back to Campus Hub</span>
        </button>
      </header>

      {/* Live Campus Broadcast Ticker */}
      <div className="w-full bg-surface-container-low border-y border-outline-variant/20 py-2 px-4 z-10">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex items-center gap-2 overflow-hidden">
            <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-ping shrink-0" />
            <span className="font-bold text-primary shrink-0 uppercase tracking-wider text-[10px]">
              Live Campus Pulse:
            </span>
            <div className="flex items-center gap-1.5 text-on-surface-variant truncate">
              <span className="font-bold text-on-surface">{LIVE_CAMPUS_FEED[feedIndex].name}</span>
              <span className="text-primary font-medium">({LIVE_CAMPUS_FEED[feedIndex].university} • {LIVE_CAMPUS_FEED[feedIndex].major})</span>
              <span>{LIVE_CAMPUS_FEED[feedIndex].action}</span>
              <span className="text-[10px] text-on-surface-variant/70 italic">[{LIVE_CAMPUS_FEED[feedIndex].time}]</span>
            </div>
          </div>
          <div className="hidden md:flex items-center gap-4 text-on-surface-variant text-[11px]">
            <span className="flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px] text-primary">groups</span>
              <b>14,830+</b> College Students Studying Now
            </span>
            <span>•</span>
            <span className="flex items-center gap-1">
              <span className="material-symbols-outlined text-[14px] text-emerald-600">verified</span>
              <b>340+</b> Universities Globally
            </span>
          </div>
        </div>
      </div>

      {/* Main Authentication Arena */}
      <main className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-12 flex-1 flex flex-col justify-center">
        {/* Toast Alert */}
        {successToast && (
          <div className="mb-6 max-w-lg mx-auto w-full bg-emerald-50 border border-emerald-300 text-emerald-900 px-4 py-3 rounded-2xl shadow-lg flex items-center gap-3 animate-fadeIn">
            <span className="material-symbols-outlined text-emerald-600">check_circle</span>
            <span className="text-xs sm:text-sm font-medium">{successToast}</span>
          </div>
        )}

        {/* Error Alert */}
        {errorMsg && (
          <div className="mb-6 max-w-lg mx-auto w-full bg-error-container/70 border border-error/30 text-on-error-container px-4 py-3 rounded-2xl shadow-lg flex items-center gap-3 animate-fadeIn">
            <span className="material-symbols-outlined text-error">error</span>
            <span className="text-xs sm:text-sm font-medium">{errorMsg}</span>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 lg:gap-12 items-start">
          
          {/* ================= LEFT SIDE: Student Pass + Multi-Major Vision ================= */}
          <div className="lg:col-span-5 flex flex-col gap-6 order-2 lg:order-1">
            {/* Live 3D Student Holographic Pass */}
            <div className="bg-white/70 backdrop-blur-xl p-6 rounded-3xl border border-white/80 shadow-[0_16px_36px_rgba(28,25,23,0.06)] flex flex-col items-center">
              <StudentCardPreview
                studentName={mode === 'signup' ? fullName : loginEmail.split('@')[0]}
                universityName={university}
                disciplineId={selectedDiscipline}
                customMajor={customMajor}
                yearId={academicYear}
                email={mode === 'signup' ? email : loginEmail}
                studentId={studentId}
                isEduVerified={isEduVerified}
              />

              {/* Dynamic Discipline Campus Impact Box */}
              <div className="mt-5 w-full bg-surface-container-low rounded-2xl p-4 border border-outline-variant/20 flex flex-col gap-2">
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-on-surface flex items-center gap-1.5">
                    <span>{currentActiveDiscipline.emoji}</span>
                    <span>{currentActiveDiscipline.name}</span>
                  </span>
                  <span className="text-[10px] font-bold text-primary bg-primary-fixed/40 px-2 py-0.5 rounded-full">
                    Active Cohort
                  </span>
                </div>
                <p className="text-xs text-on-surface-variant leading-relaxed">
                  {currentActiveDiscipline.tagline}. Current popular study tracks include{' '}
                  <span className="font-semibold text-on-surface">{currentActiveDiscipline.popularInDiscipline}</span>.
                </p>
                <div className="flex items-center gap-1.5 text-[11px] font-bold text-tertiary pt-1 border-t border-outline-variant/15">
                  <span className="material-symbols-outlined text-[14px]">trending_up</span>
                  <span>{currentActiveDiscipline.stat}</span>
                </div>
              </div>
            </div>

            {/* Fast-Track 1-Click Demo Profiles */}
            <div className="bg-surface-container-low/80 backdrop-blur-md rounded-2xl p-5 border border-outline-variant/20 flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-on-surface flex items-center gap-1.5">
                  <span className="material-symbols-outlined text-primary text-[16px]">bolt</span>
                  Instant Test Drive (1-Click Demo Scholars)
                </span>
                <span className="text-[10px] text-on-surface-variant">Click to fill</span>
              </div>
              <p className="text-[11px] text-on-surface-variant">
                Experience the portal immediately across different college majors (Medical, Business, Law, Architecture):
              </p>
              <div className="grid grid-cols-2 gap-2">
                {DEMO_STUDENTS.map((demo) => {
                  const disc = DISCIPLINES.find((d) => d.id === demo.disciplineId);
                  return (
                    <button
                      key={demo.id}
                      type="button"
                      onClick={() => handleSelectDemo(demo)}
                      className="group flex flex-col text-left p-2.5 rounded-xl bg-white hover:bg-surface-container-highest border border-outline-variant/30 hover:border-primary/40 transition-all shadow-xs hover:shadow-sm"
                    >
                      <div className="flex items-center justify-between gap-1 w-full">
                        <span className="text-xs font-bold text-on-surface group-hover:text-primary transition-colors">
                          {demo.name}
                        </span>
                        <span className="text-xs">{disc?.emoji}</span>
                      </div>
                      <span className="text-[10px] text-on-surface-variant truncate">
                        {demo.majorName}
                      </span>
                      <span className="text-[9px] text-primary/80 font-medium">
                        {demo.university.split(' ')[0]}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Campus Perks Pill Grid */}
            <div className="grid grid-cols-2 gap-3 text-xs text-on-surface-variant">
              <div className="flex items-center gap-2 p-3 bg-white/60 rounded-xl border border-outline-variant/20">
                <span className="material-symbols-outlined text-primary text-[18px]">verified_user</span>
                <div>
                  <div className="font-bold text-on-surface">100% Free Scholar Tier</div>
                  <div className="text-[10px]">Verified college students</div>
                </div>
              </div>
              <div className="flex items-center gap-2 p-3 bg-white/60 rounded-xl border border-outline-variant/20">
                <span className="material-symbols-outlined text-primary text-[18px]">forum</span>
                <div>
                  <div className="font-bold text-on-surface">Campus Study Pods</div>
                  <div className="text-[10px]">Cross-university peers</div>
                </div>
              </div>
            </div>
          </div>

          {/* ================= RIGHT SIDE: Auth Control Hub ================= */}
          <div className="lg:col-span-7 flex flex-col gap-6 order-1 lg:order-2">
            <div className="bg-white/80 backdrop-blur-2xl rounded-3xl p-6 sm:p-8 lg:p-10 border border-white/90 shadow-[0_20px_50px_rgba(28,25,23,0.08)]">
              
              {/* Header Mode Pill Switcher */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div>
                  <h1 className="font-headline-xl text-2xl sm:text-3xl font-bold text-on-surface tracking-tight">
                    {mode === 'signup' ? 'Join the Student Network' : 'Welcome Back, Scholar'}
                  </h1>
                  <p className="font-body-md text-xs sm:text-sm text-on-surface-variant mt-1">
                    {mode === 'signup'
                      ? 'Create your multi-disciplinary college account and claim your digital pass.'
                      : 'Access your coursework, study streaks, and interdisciplinary campus pods.'}
                  </p>
                </div>

                {/* Sliding Toggle */}
                <div className="inline-flex p-1 rounded-full bg-surface-container border border-outline-variant/30 shrink-0 self-start sm:self-center">
                  <button
                    type="button"
                    onClick={() => {
                      setMode('signin');
                      setErrorMsg('');
                    }}
                    className={`px-4 py-2 rounded-full text-xs font-bold transition-all duration-300 ${
                      mode === 'signin'
                        ? 'bg-primary-container text-on-primary shadow-sm'
                        : 'text-on-surface-variant hover:text-on-surface'
                    }`}
                  >
                    Sign In
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setMode('signup');
                      setErrorMsg('');
                    }}
                    className={`px-4 py-2 rounded-full text-xs font-bold transition-all duration-300 ${
                      mode === 'signup'
                        ? 'bg-primary-container text-on-primary shadow-sm'
                        : 'text-on-surface-variant hover:text-on-surface'
                    }`}
                  >
                    Activate Account
                  </button>
                </div>
              </div>

              {/* Single Sign-On (SSO) Options */}
              <div className="flex flex-col sm:flex-row gap-3 mb-6">
                <button
                  type="button"
                  onClick={() => handleSelectDemo(DEMO_STUDENTS[0])}
                  className="flex-1 flex items-center justify-center gap-2.5 py-3 px-4 rounded-xl bg-surface-container-lowest hover:bg-surface-container-high border border-outline-variant/30 text-on-surface text-xs font-bold transition-all shadow-xs hover:shadow-sm"
                >
                  <span className="material-symbols-outlined text-[18px] text-primary">account_balance</span>
                  <span>University Portal (Canvas / SSO)</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleSelectDemo(DEMO_STUDENTS[1])}
                  className="flex-1 flex items-center justify-center gap-2.5 py-3 px-4 rounded-xl bg-surface-container-lowest hover:bg-surface-container-high border border-outline-variant/30 text-on-surface text-xs font-bold transition-all shadow-xs hover:shadow-sm"
                >
                  <svg className="w-4 h-4" viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                  <span>Google Student (.edu)</span>
                </button>
              </div>

              {/* Divider */}
              <div className="relative flex items-center justify-center my-6">
                <div className="border-t border-outline-variant/30 w-full" />
                <span className="bg-white px-3 text-[11px] text-on-surface-variant uppercase tracking-wider font-semibold absolute">
                  Or continue with student credentials
                </span>
              </div>

              {/* ================= SIGN IN FORM ================= */}
              {mode === 'signin' && (
                <form onSubmit={handleSignInSubmit} className="flex flex-col gap-4">
                  {/* Email */}
                  <div className="flex flex-col gap-1.5">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-bold text-on-surface">University / Student Email</label>
                      {isEduVerified && (
                        <span className="text-[10px] font-bold text-emerald-600 flex items-center gap-1">
                          <span className="material-symbols-outlined text-[13px]">verified</span>
                          Campus Email Detected
                        </span>
                      )}
                    </div>
                    <div className="relative">
                      <input
                        type="email"
                        value={loginEmail}
                        onChange={(e) => setLoginEmail(e.target.value)}
                        placeholder="e.g. maya.lin@stanford.edu or student@college.edu"
                        required
                        className="w-full pl-10 pr-4 py-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-sm text-on-surface transition-all outline-none"
                      />
                      <span className="material-symbols-outlined absolute left-3 top-3 text-on-surface-variant text-[20px]">
                        mail
                      </span>
                    </div>
                  </div>

                  {/* Password */}
                  <div className="flex flex-col gap-1.5">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-bold text-on-surface">Password</label>
                      <button
                        type="button"
                        onClick={() =>
                          alert('Campus Password Reset instructions have been dispatched to your university email.')
                        }
                        className="text-[11px] text-primary hover:underline font-semibold"
                      >
                        Forgot password?
                      </button>
                    </div>
                    <div className="relative">
                      <input
                        type={showPassword ? 'text' : 'password'}
                        value={loginPassword}
                        onChange={(e) => setLoginPassword(e.target.value)}
                        placeholder="••••••••••••"
                        required
                        className="w-full pl-10 pr-10 py-3 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-sm text-on-surface transition-all outline-none"
                      />
                      <span className="material-symbols-outlined absolute left-3 top-3 text-on-surface-variant text-[20px]">
                        lock
                      </span>
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="absolute right-3 top-3 text-on-surface-variant hover:text-on-surface focus:outline-none"
                      >
                        <span className="material-symbols-outlined text-[20px]">
                          {showPassword ? 'visibility_off' : 'visibility'}
                        </span>
                      </button>
                    </div>
                  </div>

                  {/* Remember Me */}
                  <div className="flex items-center justify-between text-xs pt-1">
                    <label className="flex items-center gap-2 cursor-pointer select-none text-on-surface-variant">
                      <input
                        type="checkbox"
                        checked={rememberMe}
                        onChange={(e) => setRememberMe(e.target.checked)}
                        className="w-4 h-4 rounded text-primary focus:ring-primary accent-primary"
                      />
                      <span>Remember this student device</span>
                    </label>
                    <span className="text-[11px] text-tertiary font-semibold flex items-center gap-1">
                      <span className="material-symbols-outlined text-[13px]">wifi_protected_setup</span>
                      Dorm Safe
                    </span>
                  </div>

                  {/* Submit CTA */}
                  <button
                    type="submit"
                    className="w-full mt-2 py-3.5 px-6 rounded-full bg-primary-container text-on-primary font-bold text-sm shadow-md hover:shadow-lg hover:shadow-orange-500/25 hover:bg-secondary-container hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center gap-2 group"
                  >
                    <span>Sign In to Student Campus</span>
                    <span className="material-symbols-outlined text-[18px] group-hover:translate-x-1 transition-transform">
                      arrow_forward
                    </span>
                  </button>

                  {/* Footer link to switch */}
                  <div className="text-center text-xs text-on-surface-variant pt-3">
                    New college student?{' '}
                    <button
                      type="button"
                      onClick={() => setMode('signup')}
                      className="text-primary font-bold hover:underline"
                    >
                      Activate your account &amp; get Campus Pass
                    </button>
                  </div>
                </form>
              )}

              {/* ================= SIGN UP FORM ================= */}
              {mode === 'signup' && (
                <form onSubmit={handleSignUpSubmit} className="flex flex-col gap-4">
                  {/* Step 1: Academic Identity */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* Full Name */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-on-surface">Full Name</label>
                      <div className="relative">
                        <input
                          type="text"
                          value={fullName}
                          onChange={(e) => setFullName(e.target.value)}
                          placeholder="e.g. Maya Lin"
                          required
                          className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none"
                        />
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          badge
                        </span>
                      </div>
                    </div>

                    {/* Email */}
                    <div className="flex flex-col gap-1.5">
                      <div className="flex items-center justify-between">
                        <label className="text-xs font-bold text-on-surface">University Email</label>
                        {isEduVerified && (
                          <span className="text-[10px] font-bold text-emerald-600 flex items-center gap-0.5">
                            <span className="material-symbols-outlined text-[12px]">verified</span>
                            .EDU Unlocks $0
                          </span>
                        )}
                      </div>
                      <div className="relative">
                        <input
                          type="email"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          placeholder="student@stanford.edu"
                          required
                          className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none"
                        />
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          mail
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* University & Academic Year */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {/* University Picker */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-on-surface">College / University</label>
                      <div className="relative">
                        <select
                          value={university}
                          onChange={(e) => setUniversity(e.target.value)}
                          className="w-full pl-9 pr-8 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none appearance-none cursor-pointer"
                        >
                          {UNIVERSITIES.map((u) => (
                            <option key={u.name} value={u.name}>
                              {u.name}
                            </option>
                          ))}
                        </select>
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          school
                        </span>
                        <span className="material-symbols-outlined absolute right-2.5 top-2.5 text-on-surface-variant text-[18px] pointer-events-none">
                          expand_more
                        </span>
                      </div>
                    </div>

                    {/* Academic Year */}
                    <div className="flex flex-col gap-1.5">
                      <label className="text-xs font-bold text-on-surface">Current Standing / Year</label>
                      <div className="relative">
                        <select
                          value={academicYear}
                          onChange={(e) => setAcademicYear(e.target.value)}
                          className="w-full pl-9 pr-8 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none appearance-none cursor-pointer"
                        >
                          {ACADEMIC_YEARS.map((y) => (
                            <option key={y.id} value={y.id}>
                              {y.label}
                            </option>
                          ))}
                        </select>
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          calendar_month
                        </span>
                        <span className="material-symbols-outlined absolute right-2.5 top-2.5 text-on-surface-variant text-[18px] pointer-events-none">
                          expand_more
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* ================= INNOVATION: Multi-Disciplinary Major Picker ================= */}
                  <div className="flex flex-col gap-2 pt-1">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-bold text-on-surface flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-primary text-[16px]">category</span>
                        <span>Academic Field / Major Discipline</span>
                        <span className="text-[10px] text-primary font-semibold bg-primary-fixed/40 px-2 py-0.5 rounded-full">
                          All Majors Welcome
                        </span>
                      </label>
                      <span className="text-[10px] text-on-surface-variant">Select your domain</span>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                      {DISCIPLINES.map((d) => {
                        const isSelected = selectedDiscipline === d.id;
                        return (
                          <button
                            key={d.id}
                            type="button"
                            onClick={() => {
                              setSelectedDiscipline(d.id);
                              if (!customMajor) setCustomMajor(d.name);
                            }}
                            className={`flex items-center gap-2 p-2 rounded-xl text-left transition-all border ${
                              isSelected
                                ? 'bg-primary-container text-on-primary shadow-sm border-primary-container scale-[1.02]'
                                : 'bg-surface-container-lowest hover:bg-surface-container-high text-on-surface border-outline-variant/30'
                            }`}
                          >
                            <span className="text-base">{d.emoji}</span>
                            <div className="flex flex-col min-w-0">
                              <span className="text-xs font-bold truncate leading-tight">
                                {d.shortName}
                              </span>
                              <span
                                className={`text-[9px] truncate ${
                                  isSelected ? 'text-white/80' : 'text-on-surface-variant'
                                }`}
                              >
                                {d.tagline.split(',')[0]}
                              </span>
                            </div>
                          </button>
                        );
                      })}
                    </div>

                    {/* Specific Major Name (Optional input) */}
                    <div className="mt-1">
                      <input
                        type="text"
                        value={customMajor}
                        onChange={(e) => setCustomMajor(e.target.value)}
                        placeholder={`Specific Major/Concentration (e.g. ${currentActiveDiscipline.popularInDiscipline.split(',')[0]})`}
                        className="w-full px-3 py-1.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 text-xs text-on-surface outline-none focus:border-primary"
                      />
                    </div>
                  </div>

                  {/* Study Goal / Campus Vibe Mode */}
                  <div className="flex flex-col gap-1.5 pt-1">
                    <label className="text-xs font-bold text-on-surface flex items-center gap-1">
                      <span className="material-symbols-outlined text-[15px] text-primary">target</span>
                      <span>Primary Campus Learning Goal</span>
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {STUDY_GOALS.map((g) => (
                        <button
                          key={g.id}
                          type="button"
                          onClick={() => setStudyGoal(g.id)}
                          className={`flex items-center gap-2 p-2 rounded-xl text-left text-xs transition-all border ${
                            studyGoal === g.id
                              ? 'bg-secondary text-white font-bold border-secondary shadow-xs'
                              : 'bg-surface-container-lowest text-on-surface-variant hover:text-on-surface border-outline-variant/30'
                          }`}
                        >
                          <span>{g.emoji}</span>
                          <span className="truncate">{g.label}</span>
                        </button>
                      ))}
                    </div>
                  </div>

                  {/* Password & Confirm Password */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                    {/* Password */}
                    <div className="flex flex-col gap-1">
                      <label className="text-xs font-bold text-on-surface">Create Password</label>
                      <div className="relative">
                        <input
                          type={showPassword ? 'text' : 'password'}
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="Min. 8 characters"
                          required
                          className="w-full pl-9 pr-8 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none"
                        />
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          lock
                        </span>
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-2.5 top-2.5 text-on-surface-variant hover:text-on-surface"
                        >
                          <span className="material-symbols-outlined text-[18px]">
                            {showPassword ? 'visibility_off' : 'visibility'}
                          </span>
                        </button>
                      </div>
                    </div>

                    {/* Confirm Password */}
                    <div className="flex flex-col gap-1">
                      <label className="text-xs font-bold text-on-surface">Confirm Password</label>
                      <div className="relative">
                        <input
                          type={showConfirmPassword ? 'text' : 'password'}
                          value={confirmPassword}
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder="Re-enter password"
                          required
                          className="w-full pl-9 pr-8 py-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/40 focus:border-primary focus:ring-2 focus:ring-primary/20 text-xs sm:text-sm text-on-surface outline-none"
                        />
                        <span className="material-symbols-outlined absolute left-2.5 top-2.5 text-on-surface-variant text-[18px]">
                          check_circle
                        </span>
                        <button
                          type="button"
                          onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                          className="absolute right-2.5 top-2.5 text-on-surface-variant hover:text-on-surface"
                        >
                          <span className="material-symbols-outlined text-[18px]">
                            {showConfirmPassword ? 'visibility_off' : 'visibility'}
                          </span>
                        </button>
                      </div>
                    </div>
                  </div>

                  {/* Password Strength Meter */}
                  {password && (
                    <div className="flex flex-col gap-1">
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-on-surface-variant font-medium">Security Meter:</span>
                        <span className="font-bold text-on-surface">{passStrength.label}</span>
                      </div>
                      <div className="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden flex gap-1">
                        {[1, 2, 3, 4].map((step) => (
                          <div
                            key={step}
                            className={`h-full flex-1 transition-all duration-300 ${
                              passStrength.score >= step ? passStrength.color : 'bg-surface-variant/40'
                            }`}
                          />
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Honor Code & Terms */}
                  <label className="flex items-start gap-2.5 cursor-pointer text-xs text-on-surface-variant pt-1 select-none">
                    <input
                      type="checkbox"
                      checked={agreeTerms}
                      onChange={(e) => setAgreeTerms(e.target.checked)}
                      className="mt-0.5 w-4 h-4 rounded text-primary focus:ring-primary accent-primary"
                    />
                    <span>
                      I agree to the <b className="text-on-surface">Campus Academic Honor Code</b> and verify that I will respect collaborative learning guidelines.
                    </span>
                  </label>

                  {/* Submit CTA */}
                  <button
                    type="submit"
                    className="w-full mt-2 py-3.5 px-6 rounded-full bg-primary-container text-on-primary font-bold text-sm shadow-md hover:shadow-lg hover:shadow-orange-500/25 hover:bg-secondary-container hover:scale-[1.01] active:scale-[0.99] transition-all flex items-center justify-center gap-2 group"
                  >
                    <span>Activate Account &amp; Unlock Campus Pass</span>
                    <span className="material-symbols-outlined text-[18px] group-hover:translate-x-1 transition-transform">
                      military_tech
                    </span>
                  </button>

                  {/* Footer link to switch */}
                  <div className="text-center text-xs text-on-surface-variant pt-2">
                    Already have a student account?{' '}
                    <button
                      type="button"
                      onClick={() => setMode('signin')}
                      className="text-primary font-bold hover:underline"
                    >
                      Sign In here
                    </button>
                  </div>
                </form>
              )}

            </div>
          </div>

        </div>
      </main>

      {/* Footer bar */}
      <footer className="w-full py-5 border-t border-outline-variant/20 bg-surface-container-lowest/50 text-center text-xs text-on-surface-variant">
        <div className="max-w-7xl mx-auto px-6 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>&copy; 2026 Focotech Campus Learning Network. All rights reserved.</span>
          <div className="flex items-center gap-4">
            <a href="#" className="hover:text-primary transition-colors">Academic Privacy</a>
            <a href="#" className="hover:text-primary transition-colors">Student Honor Code</a>
            <a href="#" className="hover:text-primary transition-colors">University Partnerships</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
