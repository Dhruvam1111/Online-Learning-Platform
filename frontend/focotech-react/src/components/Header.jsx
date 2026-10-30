import React, { useState, useEffect } from 'react';

export default function Header({
  user,
  isAuthenticated,
  onLogout,
  onNavigateSignIn,
  onNavigateSignUp,
  onNavigateDashboard
}) {
  const [scrolled, setScrolled] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      // Trigger shrink smoothly after scrolling past 80px
      // with a slight hysteresis at 40px to prevent rapid toggling
      if (window.scrollY > 80) {
        setScrolled(true);
      } else if (window.scrollY < 40) {
        setScrolled(false);
      }
    };
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="fixed top-0 inset-x-0 z-40 flex justify-center transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] pointer-events-none px-4 sm:px-6">
      <header
        id="main-header"
        className={`pointer-events-auto w-full transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] flex items-center justify-between gap-6 ${
          scrolled
            ? 'mt-3 sm:mt-4 max-w-5xl py-2.5 px-6 sm:px-8 rounded-full bg-white/80 backdrop-blur-2xl backdrop-saturate-150 shadow-[0_12px_36px_rgba(28,25,23,0.08),inset_0_1px_1px_rgba(255,255,255,0.9)] border border-white/80 ring-1 ring-outline-variant/15'
            : 'mt-0 max-w-7xl py-5 px-6 lg:px-12 rounded-none bg-surface/75 backdrop-blur-xl border-b border-outline-variant/15 shadow-[0_1px_12px_rgba(28,25,23,0.03)]'
        }`}
      >
        {/* Logo */}
        <a href="#" className="flex items-center gap-2.5 group cursor-pointer shrink-0">
          <div
            className={`rounded-xl bg-primary-container flex items-center justify-center text-on-primary font-bold shadow-sm transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] group-hover:scale-105 ${
              scrolled ? 'w-8 h-8' : 'w-9 h-9'
            }`}
          >
            <span
              className={`material-symbols-outlined transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] ${
                scrolled ? 'text-[18px]' : 'text-[21px]'
              }`}
            >
              school
            </span>
          </div>
          <span
            className={`font-headline-md text-on-surface tracking-tight font-bold group-hover:text-primary transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] ${
              scrolled ? 'text-lg sm:text-xl' : 'text-xl sm:text-2xl'
            }`}
          >
            Focotech
          </span>
        </a>

        {/* Desktop Navigation Links */}
        <nav
          className={`hidden lg:flex items-center transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] ${
            scrolled ? 'gap-6' : 'gap-8'
          }`}
        >
          <a
            href="#featured-courses"
            className="font-label-lg text-label-lg text-on-surface-variant hover:text-primary hover:-translate-y-0.5 transition-all duration-200"
          >
            Courses
          </a>
          <a
            href="#features"
            className="font-label-lg text-label-lg text-on-surface-variant hover:text-primary hover:-translate-y-0.5 transition-all duration-200"
          >
            Paths
          </a>
          <a
            href="#testimonials"
            className="font-label-lg text-label-lg text-on-surface-variant hover:text-primary hover:-translate-y-0.5 transition-all duration-200"
          >
            Mentorship
          </a>
          <a
            href="#pricing"
            className="font-label-lg text-label-lg text-on-surface-variant hover:text-primary hover:-translate-y-0.5 transition-all duration-200"
          >
            Pricing
          </a>
          <a
            href="#dashboard"
            onClick={(e) => {
              e.preventDefault();
              if (onNavigateDashboard) onNavigateDashboard();
            }}
            className="font-label-lg text-label-lg text-primary font-bold hover:text-primary-container hover:-translate-y-0.5 transition-all duration-200 flex items-center gap-1"
          >
            <span className="material-symbols-outlined text-[17px]">dashboard</span>
            <span>Dashboard</span>
          </a>
        </nav>

        {/* Actions */}
        <div className="flex items-center gap-3 sm:gap-4 shrink-0">
          {isAuthenticated ? (
            <>
              {/* Authenticated User Pill */}
              <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-surface-container-high/60 border border-outline-variant/20 text-xs font-semibold text-on-surface">
                <div className="w-5 h-5 rounded-full bg-primary-container text-white flex items-center justify-center font-bold text-[10px]">
                  {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                </div>
                <span className="max-w-[110px] truncate">{user?.name || 'User'}</span>
                <span className="text-[9px] uppercase font-bold px-1.5 py-0.5 rounded bg-primary-container/15 text-primary">
                  {user?.role || 'student'}
                </span>
              </div>

              {/* Dashboard Action Button */}
              <button
                type="button"
                onClick={() => {
                  if (onNavigateDashboard) onNavigateDashboard();
                }}
                className={`group inline-flex items-center justify-center rounded-full bg-primary-container text-on-primary font-label-lg shadow-[0_4px_16px_rgba(255,90,54,0.28)] hover:shadow-[0_8px_24px_rgba(255,90,54,0.45)] hover:bg-secondary-container hover:scale-105 active:scale-95 transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] whitespace-nowrap cursor-pointer ${
                  scrolled
                    ? 'px-4 py-1.5 text-xs font-semibold'
                    : 'px-5 py-2 text-xs sm:text-sm font-semibold'
                }`}
              >
                <span>
                  {user?.role === 'instructor'
                    ? 'Instructor Portal'
                    : user?.role === 'admin'
                    ? 'Admin Portal'
                    : 'Dashboard'}
                </span>
              </button>

              {/* Logout Button */}
              <button
                type="button"
                onClick={() => {
                  if (onLogout) onLogout();
                }}
                className="hidden sm:inline-block font-label-lg text-xs text-on-surface-variant hover:text-rose-600 transition-colors cursor-pointer"
                title="Sign Out"
              >
                Sign Out
              </button>
            </>
          ) : (
            <>
              <a
                href="#signin"
                onClick={(e) => {
                  e.preventDefault();
                  if (onNavigateSignIn) onNavigateSignIn();
                }}
                className="hidden sm:inline-block font-label-lg text-label-lg text-on-surface-variant hover:text-on-surface hover:-translate-y-0.5 transition-all duration-200"
              >
                Sign In
              </a>
              <a
                href="#get-started"
                onClick={(e) => {
                  e.preventDefault();
                  if (onNavigateSignUp) onNavigateSignUp();
                }}
                className={`group inline-flex items-center justify-center rounded-full bg-primary-container text-on-primary font-label-lg shadow-[0_4px_16px_rgba(255,90,54,0.28)] hover:shadow-[0_8px_24px_rgba(255,90,54,0.45)] hover:bg-secondary-container hover:scale-105 active:scale-95 transition-all duration-700 ease-[cubic-bezier(0.25,1,0.5,1)] whitespace-nowrap ${
                  scrolled
                    ? 'px-5 py-2 text-xs sm:text-sm font-semibold'
                    : 'px-6 py-2.5 text-label-lg'
                }`}
              >
                <span>Get Started Free</span>
                <span
                  className={`material-symbols-outlined ml-1.5 group-hover:translate-x-1 transition-transform ${
                    scrolled ? 'text-[15px]' : 'text-[17px]'
                  }`}
                >
                  arrow_forward
                </span>
              </a>
            </>
          )}

          {/* Mobile Menu Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="lg:hidden p-1.5 text-on-surface hover:text-primary transition-colors focus:outline-none"
            aria-label="Toggle mobile menu"
          >
            <span className="material-symbols-outlined text-2xl">
              {mobileMenuOpen ? 'close' : 'menu'}
            </span>
          </button>
        </div>

        {/* Mobile Menu Dropdown */}
        {mobileMenuOpen && (
          <div className="lg:hidden absolute top-full left-0 right-0 mt-3 bg-white/90 backdrop-blur-2xl rounded-2xl border border-white/80 p-5 flex flex-col gap-4 shadow-2xl ring-1 ring-black/5 animate-fadeIn">
            <a
              href="#featured-courses"
              onClick={() => setMobileMenuOpen(false)}
              className="font-label-lg text-on-surface hover:text-primary py-1"
            >
              Courses
            </a>
            <a
              href="#features"
              onClick={() => setMobileMenuOpen(false)}
              className="font-label-lg text-on-surface hover:text-primary py-1"
            >
              Paths
            </a>
            <a
              href="#testimonials"
              onClick={() => setMobileMenuOpen(false)}
              className="font-label-lg text-on-surface hover:text-primary py-1"
            >
              Mentorship
            </a>
            <a
              href="#pricing"
              onClick={() => setMobileMenuOpen(false)}
              className="font-label-lg text-on-surface hover:text-primary py-1"
            >
              Pricing
            </a>
            <a
              href="#dashboard"
              onClick={(e) => {
                e.preventDefault();
                setMobileMenuOpen(false);
                if (onNavigateDashboard) onNavigateDashboard();
              }}
              className="font-label-lg text-primary font-bold hover:text-primary-container py-1 flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-[17px]">dashboard</span>
              <span>Student Dashboard</span>
            </a>
            <div className="pt-2 border-t border-surface-container flex items-center justify-between">
              {isAuthenticated ? (
                <>
                  <div className="flex items-center gap-2">
                    <div className="w-6 h-6 rounded-full bg-primary-container text-white flex items-center justify-center font-bold text-xs">
                      {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
                    </div>
                    <span className="font-semibold text-xs text-on-surface truncate max-w-[150px]">
                      {user?.name || 'Student'}
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setMobileMenuOpen(false);
                      if (onLogout) onLogout();
                    }}
                    className="font-label-lg text-xs text-rose-600 font-semibold"
                  >
                    Sign Out
                  </button>
                </>
              ) : (
                <a
                  href="#signin"
                  onClick={(e) => {
                    e.preventDefault();
                    setMobileMenuOpen(false);
                    if (onNavigateSignIn) onNavigateSignIn();
                  }}
                  className="font-label-lg text-on-surface-variant hover:text-primary"
                >
                  Sign In
                </a>
              )}
            </div>
          </div>
        )}
      </header>
    </div>
  );
}
