import React, { useState, useEffect } from 'react';
import { courseData } from '../data/courses';

export default function CourseModal({ courseId, isOpen, onClose }) {
  const [loadingSandbox, setLoadingSandbox] = useState(false);
  const [sandboxReady, setSandboxReady] = useState(false);

  const course = courseData[courseId] || courseData['advance-ui-ux'];

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      window.addEventListener('keydown', handleKeyDown);
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSandboxClick = () => {
    setLoadingSandbox(true);
    setTimeout(() => {
      setLoadingSandbox(false);
      setSandboxReady(true);
      setTimeout(() => setSandboxReady(false), 2000);
    }, 800);
  };

  const handleEnrollClick = () => {
    alert(`Thank you for choosing Focotech! Enrolling in ${course.title}...`);
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-course-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 md:p-8 overflow-y-auto animate-fadeIn"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-inverse-surface/60 backdrop-blur-md transition-opacity"
      />

      {/* Modal Dialog Window */}
      <div className="relative w-full max-w-4xl bg-surface-container-lowest rounded-3xl shadow-2xl border border-outline-variant/40 overflow-hidden z-10 flex flex-col max-h-[92vh] animate-scaleUp">
        {/* Modal Header Bar */}
        <div className="sticky top-0 bg-surface-container-lowest/95 backdrop-blur-md z-30 px-6 py-4 border-b border-surface-container flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span
              className={`px-3 py-1 rounded-full text-on-primary font-label-sm font-bold shadow-xs uppercase tracking-wide ${course.badgeBg}`}
            >
              {course.badge}
            </span>
            <span className="font-label-md text-on-surface-variant font-semibold">
              {course.category}
            </span>
            <span className="hidden sm:inline-block w-1 h-1 rounded-full bg-outline-variant" />
            <span className="hidden sm:inline-flex items-center gap-1 text-label-sm text-secondary-container font-semibold">
              <span
                className="material-symbols-outlined text-[16px]"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                star
              </span>
              <span className="text-on-surface font-bold">{course.rating}</span>
              <span className="text-on-surface-variant">{course.fullReviews}</span>
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal preview"
            className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-container-high text-on-surface flex items-center justify-center transition-all duration-200 hover:scale-110 active:scale-95"
          >
            <span className="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        {/* Scrollable Modal Content */}
        <div className="overflow-y-auto px-6 py-6 md:p-8 flex flex-col gap-8">
          {/* Top Title and Trailer Row */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            <div className="lg:col-span-7 flex flex-col gap-4">
              <h2
                id="modal-course-title"
                className="font-headline-xl text-headline-xl text-on-surface font-bold tracking-tight leading-tight"
              >
                {course.title}
              </h2>
              <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                {course.desc}
              </p>

              {/* Key Course Metadata Chips */}
              <div className="flex flex-wrap items-center gap-3 pt-1">
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-container text-on-surface font-label-sm font-semibold">
                  <span className="material-symbols-outlined text-primary text-[18px]">schedule</span>
                  <span>{course.hours}</span>
                </span>
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-container text-on-surface font-label-sm font-semibold">
                  <span className="material-symbols-outlined text-secondary text-[18px]">menu_book</span>
                  <span>{course.fullLessons}</span>
                </span>
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-container text-on-surface font-label-sm font-semibold">
                  <span className="material-symbols-outlined text-tertiary text-[18px]">signal_cellular_alt</span>
                  <span>{course.level}</span>
                </span>
                <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-surface-container text-on-surface font-label-sm font-semibold">
                  <span className="material-symbols-outlined text-emerald-600 text-[18px]">workspace_premium</span>
                  <span>Certificate Included</span>
                </span>
              </div>
            </div>

            {/* Interactive Trailer Preview */}
            <div className="lg:col-span-5 relative w-full aspect-video rounded-2xl bg-neutral-900 overflow-hidden shadow-lg group cursor-pointer flex items-center justify-center">
              <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
              <img
                src={course.trailerImg}
                alt="Interactive course trailer preview"
                className="w-full h-full object-cover opacity-60 group-hover:scale-105 transition-transform duration-500"
              />
              <div className="relative flex flex-col items-center justify-center gap-2 z-10">
                <span className="absolute w-16 h-16 rounded-full bg-primary-container/30 animate-ping" />
                <div className="w-14 h-14 rounded-full bg-primary-container text-on-primary flex items-center justify-center shadow-xl group-hover:scale-115 group-hover:bg-secondary-container transition-all duration-300">
                  <span
                    className="material-symbols-outlined text-[32px] ml-0.5"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    play_arrow
                  </span>
                </div>
                <span className="font-label-sm text-white font-bold tracking-wide drop-shadow">
                  Watch 2-Min Teaser
                </span>
              </div>
              <div className="absolute top-3 left-3 bg-black/60 backdrop-blur-xs text-white font-label-sm px-2 py-0.5 rounded">
                Free Preview
              </div>
            </div>
          </div>

          {/* Instructor & Capstone Project Showcase */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-surface-container-low p-6 rounded-2xl border border-outline-variant/30">
            {/* Instructor Card */}
            <div className="flex items-start gap-4">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center font-bold text-headline-sm shrink-0 shadow-sm ${course.instructor.avatarBg}`}
              >
                {course.instructor.avatar}
              </div>
              <div className="flex flex-col">
                <span className="font-label-sm text-primary uppercase font-bold tracking-wider">
                  Lead Instructor
                </span>
                <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                  {course.instructor.name}
                </h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  {course.instructor.role}
                </p>
                <div className="flex items-center gap-1.5 mt-2 text-label-sm text-on-surface font-semibold">
                  <span className="material-symbols-outlined text-[16px] text-emerald-600">verified</span>
                  <span>1-on-1 Loom Feedback Included</span>
                </div>
              </div>
            </div>

            {/* Capstone Project Overview */}
            <div className="flex flex-col gap-2 border-t md:border-t-0 md:border-l border-surface-container md:pl-6 pt-4 md:pt-0">
              <span className="font-label-sm text-secondary uppercase font-bold tracking-wider">
                What You'll Ship (Capstone)
              </span>
              <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                {course.capstoneTitle}
              </h4>
              <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed">
                {course.capstoneDesc}
              </p>
            </div>
          </div>

          {/* Course Modules / Syllabus */}
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <h3 className="font-headline-md text-headline-md text-on-surface font-bold">
                Interactive Syllabus (4 Core Modules)
              </h3>
              <span className="font-label-sm text-primary font-bold">
                Self-paced with daily momentum triggers
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {course.modules.map((mod) => (
                <div
                  key={mod.num}
                  className="p-3.5 rounded-xl bg-surface-container-low hover:bg-surface-container transition-colors flex items-start gap-3 border border-outline-variant/20"
                >
                  <span className="w-7 h-7 rounded-lg bg-surface-container-high text-primary font-bold font-label-sm flex items-center justify-center shrink-0">
                    {mod.num}
                  </span>
                  <div className="flex flex-col min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <h5 className="font-label-md text-on-surface font-bold truncate">
                        {mod.title}
                      </h5>
                      <span className="text-xs text-primary font-semibold shrink-0">
                        {mod.lessons}
                      </span>
                    </div>
                    <p className="text-xs text-on-surface-variant line-clamp-1 mt-0.5">
                      {mod.desc}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Modal Sticky Footer / CTA Bar */}
        <div className="sticky bottom-0 bg-surface-container-lowest/95 backdrop-blur-md px-6 py-4 md:px-8 border-t border-surface-container flex flex-col sm:flex-row items-center justify-between gap-4 z-20">
          <div className="flex items-baseline gap-3">
            <div className="flex flex-col">
              <span className="text-xs text-on-surface-variant font-medium">One-time enrollment</span>
              <div className="flex items-baseline gap-2">
                <span className="font-headline-xl text-headline-xl font-bold text-on-surface">
                  {course.price}
                </span>
                <span className="line-through text-on-surface-variant text-body-sm">
                  {course.origPrice}
                </span>
                <span className="text-xs bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded font-bold">
                  Save 50%
                </span>
              </div>
            </div>
            <span className="text-xs text-on-surface-variant hidden md:inline-flex items-center gap-1 ml-2">
              <span className="material-symbols-outlined text-[16px] text-emerald-600">verified_user</span> 30-day money-back guarantee
            </span>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            <button
              type="button"
              onClick={handleSandboxClick}
              disabled={loadingSandbox}
              className="flex-1 sm:flex-none px-5 py-3 rounded-full bg-surface-container-low hover:bg-surface-container text-on-surface font-label-md font-semibold transition-all duration-200"
            >
              {loadingSandbox ? 'Loading Sandbox...' : sandboxReady ? '✓ Sandbox Ready!' : 'Free Preview Lesson'}
            </button>
            <button
              type="button"
              onClick={handleEnrollClick}
              className="flex-1 sm:flex-none px-7 py-3 rounded-full bg-primary-container hover:bg-secondary-container text-on-primary font-label-lg font-bold shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all duration-200 flex items-center justify-center gap-2"
            >
              <span>Enroll Now</span>
              <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
