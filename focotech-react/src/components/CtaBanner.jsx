import React from 'react';

export default function CtaBanner() {
  return (
    <section className="w-full bg-surface py-16 lg:py-24">
      <div className="max-w-7xl mx-auto px-6 lg:px-12">
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-primary via-primary-container to-secondary-container p-8 md:p-16 text-on-primary shadow-2xl flex flex-col lg:flex-row items-center justify-between gap-8 group">
          {/* Animated Shimmer Background Overlay */}
          <div className="pointer-events-none absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-in-out" />

          {/* Ambient decorative ambient circles */}
          <div className="pointer-events-none absolute -right-16 -top-16 w-80 h-80 rounded-full bg-white/15 blur-2xl animate-orb-glow" />
          <div
            className="pointer-events-none absolute -left-16 -bottom-16 w-80 h-80 rounded-full bg-black/15 blur-2xl animate-orb-glow"
            style={{ animationDelay: '-4s' }}
          />

          <div className="flex flex-col gap-4 max-w-2xl z-10 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md w-fit mx-auto lg:mx-0 shadow-xs border border-white/20 hover:bg-white/30 transition-all cursor-default">
              <span className="font-label-sm text-label-sm font-bold uppercase tracking-wider text-white">
                Join the Community
              </span>
            </div>
            <h2 className="font-headline-xl text-headline-xl font-bold tracking-tight text-white leading-tight">
              Ready to level up your craft and build the future?
            </h2>
            <p className="font-body-lg text-body-lg text-white/90 leading-relaxed">
              Join over 120,000+ designers and developers today. Get unlimited access to 85+ production courses, daily streak tools, and 1-on-1 critiques.
            </p>
            <div className="flex flex-wrap items-center justify-center lg:justify-start gap-4 text-white/90 font-label-sm text-label-sm pt-2">
              <span className="flex items-center gap-1 hover:text-white transition-colors">
                <span className="material-symbols-outlined text-[16px]">check_circle</span> 14-day free trial
              </span>
              <span className="flex items-center gap-1 hover:text-white transition-colors">
                <span className="material-symbols-outlined text-[16px]">check_circle</span> Cancel anytime
              </span>
              <span className="flex items-center gap-1 hover:text-white transition-colors">
                <span className="material-symbols-outlined text-[16px]">check_circle</span> Recognized certificates
              </span>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-center gap-4 z-10 shrink-0">
            <a
              href="#get-started"
              className="px-8 py-4 rounded-full bg-white text-primary font-label-lg text-label-lg shadow-xl hover:bg-surface-container-low hover:scale-105 active:scale-95 hover:shadow-2xl transition-all font-bold"
            >
              Start Learning Free
            </a>
            <a
              href="#pricing"
              className="px-7 py-4 rounded-full bg-transparent text-white border-2 border-white/40 hover:border-white hover:bg-white/10 hover:scale-105 active:scale-95 font-label-lg text-label-lg transition-all font-semibold"
            >
              View Tuition Plans
            </a>
          </div>
        </div>
      </div>
    </section>
  );
}
