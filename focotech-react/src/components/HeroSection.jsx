import React from 'react';
import HeroVisual from './HeroVisual';

export default function HeroSection({ onExploreCourses }) {
  return (
    <section className="relative w-full overflow-hidden bg-surface py-12 md:py-20 lg:py-24">
      {/* Ambient Animated Gradient Blobs */}
      <div className="pointer-events-none absolute -top-24 right-1/4 h-96 w-96 rounded-full bg-secondary-fixed/40 blur-3xl animate-orb-glow" />
      <div
        className="pointer-events-none absolute top-1/2 -left-20 h-80 w-80 rounded-full bg-primary-fixed/30 blur-3xl animate-orb-glow"
        style={{ animationDelay: '-3s' }}
      />
      <div
        className="pointer-events-none absolute bottom-10 right-10 h-72 w-72 rounded-full bg-tertiary-fixed/30 blur-3xl animate-orb-glow"
        style={{ animationDelay: '-5s' }}
      />

      <div className="max-w-7xl mx-auto px-6 lg:px-12">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
          {/* Left Column: Hero Copy */}
          <div className="lg:col-span-7 flex flex-col gap-6 z-10 animate-fadeInUp">
            {/* Main Headline */}
            <h1 className="font-display text-display text-on-surface tracking-tight leading-none text-balance">
              Master In-Demand Tech &amp; Design Skills.{' '}
              <span className="text-primary-container bg-gradient-to-r from-primary to-primary-container bg-clip-text text-transparent">
                Build Your Career Faster.
              </span>
            </h1>

            {/* Subtitle */}
            <p className="font-body-lg text-body-lg text-on-surface-variant max-w-2xl leading-relaxed">
              Interactive video lessons, guided project tracks, daily learning streaks, and 1-on-1 mentor critiques designed for ambitious designers and frontend engineers.
            </p>

            {/* CTAs */}
            <div className="flex flex-wrap items-center gap-4 pt-2">
              <a
                href="#get-started"
                className="group inline-flex items-center justify-center gap-2 px-8 py-4 rounded-full bg-primary-container text-on-primary font-label-lg text-label-lg shadow-lg hover:shadow-xl hover:shadow-orange-500/25 hover:bg-secondary-container hover:scale-105 active:scale-95 transition-all duration-200"
              >
                <span>Start Learning Free</span>
                <span className="material-symbols-outlined text-[18px] group-hover:translate-x-1 transition-transform">
                  arrow_forward
                </span>
              </a>
              <button
                type="button"
                onClick={onExploreCourses}
                className="group inline-flex items-center justify-center gap-2 px-7 py-4 rounded-full bg-surface-container-lowest text-on-surface font-label-lg text-label-lg shadow-sm hover:bg-surface-container hover:scale-105 active:scale-95 border border-surface-container-highest transition-all duration-200"
              >
                <span className="material-symbols-outlined text-primary-container text-[20px] group-hover:scale-110 transition-transform">
                  play_circle
                </span>
                <span>Explore 85+ Courses</span>
              </button>
            </div>

            {/* Social Proof */}
            <div className="flex flex-wrap items-center gap-6 pt-4">
              <div className="flex -space-x-3 items-center">
                <div className="w-10 h-10 rounded-full bg-secondary-fixed flex items-center justify-center font-bold text-on-surface text-label-sm ring-2 ring-surface shadow-sm hover:scale-110 hover:z-20 transition-transform cursor-pointer">
                  BS
                </div>
                <div className="w-10 h-10 rounded-full bg-primary-fixed flex items-center justify-center font-bold text-on-surface text-label-sm ring-2 ring-surface shadow-sm hover:scale-110 hover:z-20 transition-transform cursor-pointer">
                  SW
                </div>
                <div className="w-10 h-10 rounded-full bg-tertiary-fixed flex items-center justify-center font-bold text-on-surface text-label-sm ring-2 ring-surface shadow-sm hover:scale-110 hover:z-20 transition-transform cursor-pointer">
                  AS
                </div>
                <div className="w-10 h-10 rounded-full bg-primary-container text-on-primary flex items-center justify-center font-bold text-label-sm ring-2 ring-surface shadow-sm hover:scale-110 hover:z-20 transition-transform cursor-pointer">
                  +12k
                </div>
              </div>
              <div className="flex flex-col">
                <div className="flex items-center gap-1 text-secondary-container">
                  <span className="material-symbols-outlined text-[18px] transition-transform hover:scale-125" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                  <span className="material-symbols-outlined text-[18px] transition-transform hover:scale-125" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                  <span className="material-symbols-outlined text-[18px] transition-transform hover:scale-125" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                  <span className="material-symbols-outlined text-[18px] transition-transform hover:scale-125" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star
                  </span>
                  <span className="material-symbols-outlined text-[18px] transition-transform hover:scale-125" style={{ fontVariationSettings: "'FILL' 1" }}>
                    star_half
                  </span>
                  <span className="font-headline-sm text-headline-sm text-on-surface font-bold ml-1">4.9/5</span>
                </div>
                <span className="font-body-sm text-body-sm text-on-surface-variant">
                  Over <span className="font-semibold text-on-surface">120,000</span>+ active builders enrolled
                </span>
              </div>
            </div>
          </div>

          {/* Right Column: REDESIGNED HERO SHOWCASE WITH IMAGE 2 TARGET DESIGN */}
          <div className="lg:col-span-5 relative flex justify-center items-center">
            <HeroVisual />
          </div>
        </div>
      </div>
    </section>
  );
}
