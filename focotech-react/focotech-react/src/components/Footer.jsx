import React from 'react';

export default function Footer() {
  return (
    <footer className="w-full bg-surface-container-low mt-20 pt-16 pb-12 shadow-[0_1px_8px_rgba(28,25,23,0.02)]">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-space-xl pb-16">
        {/* Brand Bio */}
        <div className="lg:col-span-2 flex flex-col gap-space-md">
          <div className="flex items-center gap-space-sm group cursor-pointer">
            <div className="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center text-on-primary font-bold shadow-sm transition-transform group-hover:scale-105">
              <span className="material-symbols-outlined text-[20px]">school</span>
            </div>
            <span className="font-headline-md text-headline-md text-on-surface tracking-tight font-bold group-hover:text-primary transition-colors">
              Focotech
            </span>
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant max-w-sm">
            The premier interactive campus for modern designers and frontend engineers. Master production-grade design systems, micro-interactions, and high-performance web architecture.
          </p>
          <div className="flex items-center gap-space-sm">
            <div className="h-2 w-2 rounded-full bg-primary-container animate-pulse" />
            <span className="font-label-sm text-label-sm text-tertiary">
              42,000+ Active Builders Across 110 Countries
            </span>
          </div>
        </div>

        {/* Platform Links */}
        <div className="flex flex-col gap-space-sm">
          <span className="font-label-md text-label-md text-on-surface font-bold uppercase tracking-wider mb-2">
            Platform
          </span>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">All Courses</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#features">Structured Paths</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#testimonials">1-on-1 Mentorship</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#pricing">Tuition &amp; Plans</a>
        </div>

        {/* Learning Tracks */}
        <div className="flex flex-col gap-space-sm">
          <span className="font-label-md text-label-md text-on-surface font-bold uppercase tracking-wider mb-2">
            Learning Tracks
          </span>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">Design Systems &amp; Tokens</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">Modern Tailwind CSS</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">Interaction &amp; Motion</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">Full-Stack Architecture</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#featured-courses">Spatial &amp; WebGL UI</a>
        </div>

        {/* Community */}
        <div className="flex flex-col gap-space-sm">
          <span className="font-label-md text-label-md text-on-surface font-bold uppercase tracking-wider mb-2">
            Community
          </span>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#">Discord Community</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#">Live Design Crits</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#">Student Showcase</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#">Career Board</a>
          <a className="font-body-sm text-body-sm text-on-surface-variant hover:text-primary hover:translate-x-1 transition-all" href="#">Instructor Network</a>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-6 lg:px-12 pt-8 border-t border-surface-container flex flex-col md:flex-row items-center justify-between gap-space-md text-on-surface-variant font-body-sm text-body-sm">
        <p>© 2025 Focotech Inc. Designed for intentional craftsmanship.</p>
        <div className="flex items-center gap-space-lg">
          <a className="hover:text-on-surface transition-colors" href="#">Privacy Policy</a>
          <a className="hover:text-on-surface transition-colors" href="#">Terms of Service</a>
          <a className="hover:text-on-surface transition-colors" href="#">Security &amp; Trust</a>
        </div>
      </div>
    </footer>
  );
}
