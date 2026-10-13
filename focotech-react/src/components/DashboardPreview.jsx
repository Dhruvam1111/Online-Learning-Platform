import React from 'react';

export default function DashboardPreview({ onOpenCourse }) {
  return (
    <section className="w-full bg-surface-container-low py-20 lg:py-24">
      <div className="max-w-7xl mx-auto px-6 lg:px-12 flex flex-col gap-12">
        {/* Section Heading */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-6">
          <div className="flex flex-col gap-3 max-w-2xl">
            <span className="font-label-sm text-label-sm text-primary uppercase tracking-widest font-bold">
              Unified Learner Workspace
            </span>
            <h2 className="font-headline-xl text-headline-xl text-on-surface">
              An EdTech experience designed to keep you motivated.
            </h2>
            <p className="font-body-md text-body-md text-on-surface-variant">
              Built following the exact aesthetic in your daily learning app: clean progress analytics, live course carousels, and persistent streak counters.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface-container-lowest text-primary font-label-md text-label-md shadow-sm border border-primary-fixed/50 hover:bg-primary hover:text-on-primary transition-colors cursor-pointer">
              <span className="material-symbols-outlined text-[16px] animate-spin" style={{ animationDuration: '4s' }}>
                bolt
              </span>{' '}
              Live Sync
            </span>
            <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-surface-container text-on-surface font-label-md text-label-md hover:bg-surface-container-high transition-colors cursor-pointer">
              ⌘K Command Palette
            </span>
          </div>
        </div>

        {/* Dashboard Mockup Frame */}
        <div className="w-full bg-surface-container-lowest rounded-3xl p-4 md:p-8 shadow-xl flex flex-col gap-8 border border-outline-variant/30 hover:shadow-2xl transition-shadow duration-300">
          {/* Top Workspace Bar */}
          <div className="flex flex-wrap items-center justify-between gap-4 pb-6 border-b border-surface-container">
            <div className="flex items-center gap-3">
              <span className="font-headline-md text-headline-md text-on-surface font-bold">
                Student Dashboard
              </span>
              <span className="px-2.5 py-0.5 rounded-full bg-primary-fixed text-primary font-label-sm text-label-sm flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-primary animate-ping" /> Active Session
              </span>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2 bg-surface-container-low px-4 py-2 rounded-full text-on-surface-variant font-body-sm text-body-sm focus-within:ring-2 focus-within:ring-primary/40 focus-within:bg-surface transition-all">
                <span className="material-symbols-outlined text-[18px]">search</span>
                <input
                  className="bg-transparent border-none outline-none text-body-sm text-on-surface placeholder:text-on-surface-variant w-44 sm:w-56 focus:outline-none focus:ring-0 p-0"
                  placeholder="Search components, lessons..."
                  type="text"
                />
                <kbd className="px-1.5 py-0.5 bg-surface-container-lowest rounded text-label-sm shadow-xs font-mono">
                  ⌘K
                </kbd>
              </div>
              <div className="w-9 h-9 rounded-full bg-secondary-fixed flex items-center justify-center font-bold text-on-secondary-fixed text-label-sm hover:scale-110 transition-transform cursor-pointer">
                BS
              </div>
            </div>
          </div>

          {/* 3-Column Dashboard Body */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left Content Area (8 Cols) */}
            <div className="lg:col-span-8 flex flex-col gap-6">
              {/* Continue Learning */}
              <div className="flex flex-col gap-3">
                <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                  Continue Learning
                </h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Card 1 */}
                  <div
                    onClick={() => onOpenCourse('advance-ui-ux')}
                    className="group bg-surface-container-low p-4 rounded-2xl flex flex-col gap-3 hover:bg-surface-container hover:shadow-md hover:-translate-y-1 transition-all duration-300 cursor-pointer border border-transparent hover:border-primary-fixed relative overflow-hidden"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-primary-container text-on-primary flex items-center justify-center group-hover:scale-110 transition-transform">
                        <span className="material-symbols-outlined text-[20px]">brush</span>
                      </div>
                      <div className="flex flex-col min-w-0">
                        <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold truncate group-hover:text-primary transition-colors">
                          Advance UI/UX Design
                        </h4>
                        <span className="font-label-sm text-label-sm text-primary uppercase font-bold">
                          Design
                        </span>
                      </div>
                    </div>
                    <div className="w-full bg-surface-container-high h-2 rounded-full overflow-hidden">
                      <div className="bg-primary-container h-full rounded-full transition-all duration-1000 ease-out w-[48%]" />
                    </div>
                    <div className="flex items-center justify-between font-label-sm text-label-sm text-on-surface-variant">
                      <span>18/40 Lessons</span>
                      <span>2 hours left</span>
                    </div>
                    <div className="pt-1 flex items-center justify-between">
                      <span className="text-primary font-bold text-label-sm flex items-center gap-1 group-hover:underline group-hover:translate-x-0.5 transition-all">
                        Resume Course{' '}
                        <span className="material-symbols-outlined text-[14px]">
                          arrow_circle_right
                        </span>
                      </span>
                      <span className="opacity-0 group-hover:opacity-100 transition-opacity bg-primary-container/10 text-primary px-2 py-0.5 rounded-full font-label-sm text-label-sm font-semibold">
                        Quick Preview
                      </span>
                    </div>
                  </div>

                  {/* Card 2 */}
                  <div
                    onClick={() => onOpenCourse('modern-fullstack')}
                    className="group bg-surface-container-low p-4 rounded-2xl flex flex-col gap-3 hover:bg-surface-container hover:shadow-md hover:-translate-y-1 transition-all duration-300 cursor-pointer border border-transparent hover:border-tertiary-fixed relative overflow-hidden"
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-tertiary text-on-tertiary flex items-center justify-center group-hover:scale-110 transition-transform">
                        <span className="material-symbols-outlined text-[20px]">code</span>
                      </div>
                      <div className="flex flex-col min-w-0">
                        <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold truncate group-hover:text-tertiary transition-colors">
                          Basic Web Development
                        </h4>
                        <span className="font-label-sm text-label-sm text-tertiary uppercase font-bold">
                          Development
                        </span>
                      </div>
                    </div>
                    <div className="w-full bg-surface-container-high h-2 rounded-full overflow-hidden">
                      <div className="bg-secondary-container h-full rounded-full transition-all duration-1000 ease-out w-[35%]" />
                    </div>
                    <div className="flex items-center justify-between font-label-sm text-label-sm text-on-surface-variant">
                      <span>14/40 Lessons</span>
                      <span>3.5 hours left</span>
                    </div>
                    <div className="pt-1 flex items-center justify-between">
                      <span className="text-secondary-container font-bold text-label-sm flex items-center gap-1 group-hover:underline group-hover:translate-x-0.5 transition-all">
                        Resume Course{' '}
                        <span className="material-symbols-outlined text-[14px]">
                          arrow_circle_right
                        </span>
                      </span>
                      <span className="opacity-0 group-hover:opacity-100 transition-opacity bg-secondary-container/10 text-secondary-container px-2 py-0.5 rounded-full font-label-sm text-label-sm font-semibold">
                        Quick Preview
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Featured Video Tutorial */}
              <div className="flex flex-col gap-3">
                <h3 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                  Featured Video Tutorial
                </h3>
                <div
                  onClick={() => onOpenCourse('webflow-tutorial')}
                  className="group bg-surface-container-low rounded-2xl p-4 flex flex-col sm:flex-row gap-4 items-center hover:bg-surface-container hover:shadow-md transition-all duration-300 cursor-pointer border border-transparent hover:border-primary-fixed/40"
                >
                  <div className="relative w-full sm:w-48 aspect-video rounded-xl bg-neutral-900 overflow-hidden flex items-center justify-center text-white shrink-0 group-hover:shadow-lg transition-all">
                    <div className="absolute inset-0 bg-black/20 group-hover:bg-black/10 transition-colors" />
                    <div className="absolute top-2 left-2 px-2 py-0.5 rounded-full bg-primary-container text-white font-label-sm text-label-sm font-semibold">
                      New
                    </div>
                    <div className="relative flex items-center justify-center">
                      <span className="absolute w-12 h-12 rounded-full bg-primary-container/40 animate-ping" />
                      <span
                        className="material-symbols-outlined text-4xl text-white group-hover:scale-125 transition-transform duration-300 z-10"
                        style={{ fontVariationSettings: "'FILL' 1" }}
                      >
                        play_circle
                      </span>
                    </div>
                    <div className="absolute bottom-1 right-2 font-label-sm text-label-sm text-white/90 bg-black/50 px-1.5 py-0.5 rounded">
                      9:32
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 w-full">
                    <div className="flex items-center justify-between">
                      <span className="font-label-sm text-label-sm text-primary font-bold">
                        Webflow Mastery
                      </span>
                      <span className="text-xs bg-surface-container-high px-2 py-0.5 rounded text-on-surface-variant flex items-center gap-1 group-hover:bg-primary-container group-hover:text-on-primary transition-colors">
                        <span className="material-symbols-outlined text-[13px]">visibility</span> Click to Preview
                      </span>
                    </div>
                    <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold group-hover:text-primary transition-colors">
                      Webflow Tutorial: Build Your First Portfolio Website In a Minute
                    </h4>
                    <p className="font-body-sm text-body-sm text-on-surface-variant">
                      Taught by Adam Smith • 4.7 Rating (32 reviews)
                    </p>
                    <div className="flex items-center justify-between pt-1">
                      <span className="font-headline-sm text-headline-sm text-on-surface font-bold">
                        ₹999
                      </span>
                      <button
                        type="button"
                        className="px-4 py-1.5 rounded-full bg-primary-container text-on-primary font-label-sm text-label-sm shadow-sm hover:bg-secondary-container hover:scale-105 active:scale-95 transition-all"
                      >
                        Preview Details
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Metric Rail (4 Cols) */}
            <div className="lg:col-span-4 flex flex-col gap-4">
              {/* Weekly Streak */}
              <div className="bg-surface-container-low p-5 rounded-2xl flex flex-col gap-4 border border-outline-variant/20 hover:border-outline-variant/50 transition-colors">
                <div className="flex items-center justify-between">
                  <span className="font-headline-sm text-headline-sm text-on-surface font-bold flex items-center gap-1.5">
                    Weekly Streak <span className="animate-flame">🔥</span>
                  </span>
                  <span className="font-label-sm text-label-sm text-on-surface-variant bg-surface-container px-2 py-0.5 rounded-md font-semibold">
                    May 2025
                  </span>
                </div>
                <div className="grid grid-cols-7 gap-1 text-center font-label-sm text-label-sm">
                  {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, idx) => {
                    const days = [29, 30, 31, 1, 2, 3, 4];
                    const isStreak = idx < 3;
                    return (
                      <div key={day} className="flex flex-col items-center gap-1 group cursor-pointer">
                        <span className="text-on-surface-variant">{day}</span>
                        <span
                          className={`w-8 h-8 rounded-full font-bold flex items-center justify-center transition-all ${
                            isStreak
                              ? 'bg-primary-container text-on-primary shadow-xs group-hover:scale-110 group-hover:ring-2 ring-primary/40'
                              : 'bg-surface-container-high text-on-surface group-hover:bg-primary-fixed group-hover:text-primary'
                          }`}
                        >
                          {days[idx]}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* In Progress vs Completed Count */}
              <div className="grid grid-cols-2 gap-3">
                <div className="bg-surface-container-low p-4 rounded-2xl flex flex-col gap-1 hover:bg-surface-container transition-all cursor-pointer group hover:-translate-y-1">
                  <span className="material-symbols-outlined text-primary text-[22px] group-hover:scale-110 transition-transform">
                    auto_stories
                  </span>
                  <span className="font-headline-md text-headline-md text-on-surface font-bold mt-1">
                    3 Courses
                  </span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant">In Progress</span>
                </div>
                <div className="bg-surface-container-low p-4 rounded-2xl flex flex-col gap-1 hover:bg-surface-container transition-all cursor-pointer group hover:-translate-y-1">
                  <span className="material-symbols-outlined text-tertiary text-[22px] group-hover:scale-110 transition-transform">
                    workspace_premium
                  </span>
                  <span className="font-headline-md text-headline-md text-on-surface font-bold mt-1">
                    17 Courses
                  </span>
                  <span className="font-body-sm text-body-sm text-on-surface-variant">Completed</span>
                </div>
              </div>

              {/* Weekly Watch Time Bar Chart */}
              <div className="bg-surface-container-low p-5 rounded-2xl flex flex-col gap-3 group hover:border-outline-variant/50 transition-colors">
                <div className="flex items-center justify-between">
                  <span className="font-headline-sm text-headline-sm text-on-surface font-bold">
                    Watch Time
                  </span>
                  <span className="text-primary font-label-md text-label-md font-bold group-hover:scale-105 transition-transform">
                    4h 24m Today
                  </span>
                </div>
                <svg
                  className="w-full h-20 text-surface-container-high"
                  fill="none"
                  viewBox="0 0 200 60"
                  xmlns="http://www.w3.org/2000/svg"
                >
                  <rect className="hover:fill-primary-fixed transition-colors cursor-pointer" fill="currentColor" height="30" rx="4" width="16" x="10" y="30" />
                  <rect className="hover:fill-primary-fixed transition-colors cursor-pointer" fill="currentColor" height="40" rx="4" width="16" x="40" y="20" />
                  <rect className="hover:fill-primary-fixed transition-colors cursor-pointer" fill="currentColor" height="25" rx="4" width="16" x="70" y="35" />
                  <rect className="text-primary-container hover:text-secondary-container transition-colors cursor-pointer" fill="currentColor" height="50" rx="4" width="16" x="100" y="10" />
                  <rect className="hover:fill-primary-fixed transition-colors cursor-pointer" fill="currentColor" height="15" rx="4" width="16" x="130" y="45" />
                  <rect className="hover:fill-primary-fixed transition-colors cursor-pointer" fill="currentColor" height="35" rx="4" width="16" x="160" y="25" />
                </svg>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
