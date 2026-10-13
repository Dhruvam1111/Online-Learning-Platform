import React, { useState, useMemo } from 'react';
import { courseData } from '../data/courses';
import { DISCIPLINES } from '../data/studentDisciplines';
import StudentCardPreview from './StudentCardPreview';

export default function StudentHomePage({
  currentUser,
  enrolledCourseIds = [],
  onEnrollCourse,
  onOpenCourse,
  onOpenAuth,
  onBackToHub
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDiscipline, setSelectedDiscipline] = useState('all');
  const [selectedLevel, setSelectedLevel] = useState('all');
  const [activeTab, setActiveTab] = useState('all'); // 'all' or 'enrolled'
  const [sortBy, setSortBy] = useState('popular'); // 'popular' | 'rating' | 'duration'
  const [showPassModal, setShowPassModal] = useState(false);

  const allCourses = useMemo(() => Object.values(courseData), []);

  // Filtered courses based on search, discipline, and level
  const filteredCourses = useMemo(() => {
    return allCourses.filter((course) => {
      // Tab filter (all vs enrolled)
      if (activeTab === 'enrolled' && !enrolledCourseIds.includes(course.id)) {
        return false;
      }

      // Discipline filter
      if (selectedDiscipline !== 'all' && course.filterCategory !== selectedDiscipline && course.discipline !== selectedDiscipline) {
        return false;
      }

      // Level filter
      if (selectedLevel !== 'all' && !course.level.toLowerCase().includes(selectedLevel.toLowerCase())) {
        return false;
      }

      // Search query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase().trim();
        const matchesTitle = course.title.toLowerCase().includes(query);
        const matchesDesc = course.desc.toLowerCase().includes(query);
        const matchesInstructor = course.instructor.name.toLowerCase().includes(query);
        const matchesCategory = course.category.toLowerCase().includes(query);
        const matchesCapstone = course.capstoneTitle?.toLowerCase().includes(query);
        return matchesTitle || matchesDesc || matchesInstructor || matchesCategory || matchesCapstone;
      }

      return true;
    }).sort((a, b) => {
      if (sortBy === 'rating') {
        return parseFloat(b.rating) - parseFloat(a.rating);
      }
      if (sortBy === 'duration') {
        const getHours = (h) => parseInt(h.replace(/\D/g, '')) || 0;
        return getHours(a.hours) - getHours(b.hours);
      }
      // default: popular (review count)
      const getReviews = (r) => parseInt(r.replace(/\D/g, '')) || 0;
      return getReviews(b.reviews) - getReviews(a.reviews);
    });
  }, [allCourses, activeTab, enrolledCourseIds, selectedDiscipline, selectedLevel, searchQuery, sortBy]);

  // List of enrolled courses for the "My Study Desk" shelf
  const myEnrolledCourses = useMemo(() => {
    return allCourses.filter((c) => enrolledCourseIds.includes(c.id));
  }, [allCourses, enrolledCourseIds]);

  const handleEnrollClick = (e, courseId) => {
    e.stopPropagation();
    onEnrollCourse(courseId);
  };

  const clearFilters = () => {
    setSearchQuery('');
    setSelectedDiscipline('all');
    setSelectedLevel('all');
    setActiveTab('all');
  };

  return (
    <div className="w-full min-h-screen bg-surface font-body-md text-on-surface antialiased pt-24 pb-20 selection:bg-primary-container selection:text-on-primary">
      {/* Ambient background glows */}
      <div className="pointer-events-none fixed -top-40 right-10 w-[500px] h-[500px] rounded-full bg-primary-fixed/20 blur-[130px] -z-10" />
      <div className="pointer-events-none fixed top-1/2 -left-40 w-[600px] h-[600px] rounded-full bg-secondary-fixed/20 blur-[140px] -z-10" />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-12 flex flex-col gap-10">
        
        {/* ================= STUDENT TOP ACADEMIC HUB BANNER ================= */}
        <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-surface-container-lowest via-surface-container-low to-surface-container p-6 sm:p-8 lg:p-10 border border-outline-variant/30 shadow-xl flex flex-col gap-6">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 z-10">
            {/* Left: Scholar Greeting */}
            <div className="flex items-start gap-4 sm:gap-5">
              <div className="relative">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-tr from-primary to-primary-container text-on-primary font-bold text-2xl sm:text-3xl flex items-center justify-center shadow-lg ring-4 ring-white/80">
                  {currentUser?.avatarInitials || '🎓'}
                </div>
                <span className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-500 border-2 border-white flex items-center justify-center shadow-xs">
                  <span className="w-2 h-2 rounded-full bg-white" />
                </span>
              </div>

              <div className="flex flex-col">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="text-xs font-bold text-primary uppercase tracking-widest bg-primary-fixed/40 px-2.5 py-0.5 rounded-full">
                    Official Student Portal
                  </span>
                  {currentUser?.isEduVerified && (
                    <span className="text-[10px] font-bold text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                      <span className="material-symbols-outlined text-[12px]">verified</span>
                      .EDU Verified Scholar
                    </span>
                  )}
                </div>

                <h1 className="font-headline-xl text-2xl sm:text-3xl lg:text-4xl font-bold text-on-surface tracking-tight mt-1 leading-tight">
                  {currentUser ? `Welcome back, ${currentUser.name}` : 'Welcome, College Scholar'}!
                </h1>

                <p className="text-xs sm:text-sm text-on-surface-variant mt-0.5">
                  {currentUser?.university || 'University Campus'} •{' '}
                  <span className="font-semibold text-on-surface">
                    {currentUser?.majorName || 'Cross-Disciplinary Learning Hub'}
                  </span>{' '}
                  • <span className="font-mono text-primary font-bold">{currentUser?.studentId || 'ID-2026-ACTIVE'}</span>
                </p>
              </div>
            </div>

            {/* Right: Academic Metrics & Pass Button */}
            <div className="flex flex-wrap items-center gap-3 self-start lg:self-center">
              {/* Daily Streak Tile */}
              <div className="flex items-center gap-2.5 px-4 py-2.5 rounded-2xl bg-white/80 backdrop-blur-md border border-outline-variant/30 shadow-xs">
                <span className="text-2xl animate-flame">🔥</span>
                <div className="flex flex-col">
                  <span className="font-bold text-sm text-on-surface leading-tight">
                    {currentUser?.streakDays || 14} Day Streak
                  </span>
                  <span className="text-[10px] text-on-surface-variant">Daily Momentum</span>
                </div>
              </div>

              {/* Enrolled Count Tile */}
              <div className="flex items-center gap-2.5 px-4 py-2.5 rounded-2xl bg-white/80 backdrop-blur-md border border-outline-variant/30 shadow-xs">
                <span className="material-symbols-outlined text-primary text-2xl">menu_book</span>
                <div className="flex flex-col">
                  <span className="font-bold text-sm text-on-surface leading-tight">
                    {enrolledCourseIds.length} Enrolled
                  </span>
                  <span className="text-[10px] text-on-surface-variant">Active Courses</span>
                </div>
              </div>

              {/* View Digital Pass Modal Trigger */}
              <button
                type="button"
                onClick={() => setShowPassModal(true)}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-primary-container text-on-primary font-bold text-xs sm:text-sm shadow-md hover:shadow-lg hover:bg-secondary-container hover:scale-105 active:scale-95 transition-all"
              >
                <span className="material-symbols-outlined text-[18px]">badge</span>
                <span>View Campus Pass</span>
              </button>
            </div>
          </div>

          {/* Guest Sign In Callout (If not logged in) */}
          {!currentUser && (
            <div className="z-10 mt-2 p-3.5 rounded-2xl bg-primary-fixed/30 border border-primary/20 flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2 text-on-surface">
                <span className="material-symbols-outlined text-primary text-[20px]">info</span>
                <span>
                  <b>Browsing as Guest:</b> Sign in or activate your student account to claim 100% free enrollment and track course certificates!
                </span>
              </div>
              <button
                type="button"
                onClick={() => onOpenAuth && onOpenAuth('signin')}
                className="px-4 py-1.5 rounded-full bg-primary-container text-on-primary font-bold hover:bg-secondary-container shrink-0 self-start sm:self-auto"
              >
                Sign In Now
              </button>
            </div>
          )}
        </div>

        {/* ================= MY ENROLLED COURSES (ACTIVE STUDY DESK) ================= */}
        {myEnrolledCourses.length > 0 && (
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[22px]">laptop_mac</span>
                <h2 className="font-headline-md text-xl sm:text-2xl font-bold text-on-surface">
                  My Active Study Desk ({myEnrolledCourses.length})
                </h2>
              </div>
              <span className="text-xs text-primary font-bold">
                100% College Scholar Access
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {myEnrolledCourses.map((course, idx) => (
                <div
                  key={course.id}
                  onClick={() => onOpenCourse(course.id)}
                  className="group bg-white rounded-2xl p-5 border border-outline-variant/30 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300 flex flex-col justify-between cursor-pointer relative"
                >
                  <div className="flex items-start gap-3">
                    <img
                      src={course.trailerImg}
                      alt={course.title}
                      className="w-16 h-16 rounded-xl object-cover shrink-0 shadow-xs"
                    />
                    <div className="flex flex-col min-w-0 flex-1">
                      <span className="text-[10px] font-bold text-primary uppercase tracking-wider">
                        {course.category}
                      </span>
                      <h3 className="font-bold text-sm text-on-surface leading-snug group-hover:text-primary transition-colors line-clamp-2">
                        {course.title}
                      </h3>
                      <span className="text-[11px] text-on-surface-variant truncate mt-0.5">
                        {course.instructor.name}
                      </span>
                    </div>
                  </div>

                  {/* Progress Bar Mock */}
                  <div className="mt-4 pt-3 border-t border-surface-container flex flex-col gap-1.5">
                    <div className="flex items-center justify-between text-[11px]">
                      <span className="text-on-surface-variant font-medium">Course Progress:</span>
                      <span className="font-bold text-primary">
                        {idx === 0 ? '42%' : '15%'} Complete
                      </span>
                    </div>
                    <div className="h-1.5 w-full bg-surface-container rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary-container rounded-full transition-all duration-500"
                        style={{ width: idx === 0 ? '42%' : '15%' }}
                      />
                    </div>
                    <div className="flex items-center justify-between pt-1">
                      <span className="text-[10px] text-emerald-700 font-semibold flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
                        Next: Module {idx + 1}
                      </span>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onOpenCourse(course.id);
                        }}
                        className="text-xs font-bold text-primary hover:underline flex items-center gap-1"
                      >
                        <span>Resume Lesson</span>
                        <span className="material-symbols-outlined text-[14px]">play_circle</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ================= SEARCH & DISCIPLINE FILTER COMMAND CENTER ================= */}
        <div className="flex flex-col gap-6 bg-white/70 backdrop-blur-xl p-6 sm:p-8 rounded-3xl border border-white/80 shadow-md">
          {/* Main Search Input */}
          <div className="flex flex-col sm:flex-row items-center gap-3">
            <div className="relative flex-1 w-full">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search courses by title, topic, major (e.g. Neuroscience, Valuation, Architecture, Full-Stack, Law)..."
                className="w-full pl-11 pr-10 py-3.5 rounded-2xl bg-surface-container-low border border-outline-variant/30 focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10 text-sm text-on-surface placeholder:text-on-surface-variant outline-none transition-all shadow-inner"
              />
              <span className="material-symbols-outlined absolute left-3.5 top-3.5 text-on-surface-variant text-[22px]">
                search
              </span>
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3.5 top-3.5 text-on-surface-variant hover:text-on-surface text-sm w-5 h-5 rounded-full bg-surface-container flex items-center justify-center"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Sort Dropdown */}
            <div className="flex items-center gap-2 shrink-0 w-full sm:w-auto">
              <span className="text-xs text-on-surface-variant font-semibold hidden lg:inline">Sort:</span>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                className="py-3.5 px-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 text-xs sm:text-sm font-semibold text-on-surface outline-none cursor-pointer focus:border-primary w-full sm:w-auto"
              >
                <option value="popular">Most Popular</option>
                <option value="rating">Highest Rated ⭐</option>
                <option value="duration">Duration</option>
              </select>
            </div>
          </div>

          {/* Faculty / Multi-Major Discipline Filter Tabs */}
          <div className="flex flex-col gap-2.5">
            <div className="flex items-center justify-between text-xs">
              <span className="font-bold text-on-surface uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                <span className="material-symbols-outlined text-primary text-[16px]">category</span>
                Explore by Academic Faculty (Not Just Tech)
              </span>
              <span className="text-on-surface-variant text-[11px]">
                Showing <b>{filteredCourses.length}</b> courses
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={() => setSelectedDiscipline('all')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'all'
                    ? 'bg-primary-container text-on-primary border-primary-container shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                🌐 All Disciplines
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('business')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'business'
                    ? 'bg-amber-600 text-white border-amber-600 shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                📊 Business &amp; Finance
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('health')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'health'
                    ? 'bg-emerald-600 text-white border-emerald-600 shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                🩺 Pre-Med &amp; Health
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('law')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'law'
                    ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                ⚖️ Law &amp; Policy
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('arts')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'arts'
                    ? 'bg-rose-600 text-white border-rose-600 shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                🎨 Arts &amp; Architecture
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('sciences')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'sciences'
                    ? 'bg-cyan-600 text-white border-cyan-600 shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                🔬 Sciences &amp; Math
              </button>

              <button
                type="button"
                onClick={() => setSelectedDiscipline('dev')}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition-all border ${
                  selectedDiscipline === 'dev'
                    ? 'bg-primary text-white border-primary shadow-sm scale-105'
                    : 'bg-surface-container-low hover:bg-surface-container text-on-surface border-outline-variant/20'
                }`}
              >
                ⚙️ Engineering &amp; Computing
              </button>
            </div>
          </div>

          {/* Quick Keyword Search Chips */}
          <div className="flex flex-wrap items-center gap-2 pt-1 border-t border-surface-container text-xs">
            <span className="text-on-surface-variant font-semibold">Popular topics:</span>
            {['Financial Modeling', 'Clinical Neuroscience', 'Constitutional Law', 'Biophilic Design', 'Quantum Physics', 'Full-Stack Next.js'].map((tag) => (
              <button
                key={tag}
                type="button"
                onClick={() => setSearchQuery(tag)}
                className="px-2.5 py-1 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface text-[11px] font-medium transition-colors"
              >
                #{tag}
              </button>
            ))}
            {(searchQuery || selectedDiscipline !== 'all' || activeTab !== 'all') && (
              <button
                type="button"
                onClick={clearFilters}
                className="ml-auto text-primary text-[11px] font-bold hover:underline"
              >
                Clear all filters
              </button>
            )}
          </div>
        </div>

        {/* ================= COURSE CATALOG GRID ================= */}
        <div className="flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <h2 className="font-headline-md text-xl sm:text-2xl font-bold text-on-surface">
              {selectedDiscipline === 'all'
                ? 'All Undergraduate & Graduate Courses'
                : `Courses in ${selectedDiscipline.toUpperCase()}`}
            </h2>
            <span className="text-xs text-on-surface-variant">
              Click any course card to inspect syllabus and enroll
            </span>
          </div>

          {filteredCourses.length === 0 ? (
            /* Empty State */
            <div className="w-full bg-white rounded-3xl p-12 text-center flex flex-col items-center justify-center gap-4 border border-outline-variant/30">
              <span className="material-symbols-outlined text-5xl text-on-surface-variant">
                search_off
              </span>
              <h3 className="font-headline-md text-lg font-bold text-on-surface">
                No courses found matching "{searchQuery}"
              </h3>
              <p className="text-xs sm:text-sm text-on-surface-variant max-w-md">
                Try searching for broader keywords like "business", "law", "health", "design", or reset your filters.
              </p>
              <button
                type="button"
                onClick={clearFilters}
                className="px-6 py-2.5 rounded-full bg-primary-container text-on-primary font-bold text-xs"
              >
                Reset Search Filters
              </button>
            </div>
          ) : (
            /* Cards Grid */
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
              {filteredCourses.map((c) => {
                const isEnrolled = enrolledCourseIds.includes(c.id);

                return (
                  <div
                    key={c.id}
                    onClick={() => onOpenCourse(c.id)}
                    className="group bg-surface-container-lowest rounded-3xl overflow-hidden border border-outline-variant/30 hover:border-primary/50 shadow-sm hover:shadow-2xl hover:-translate-y-1.5 transition-all duration-300 flex flex-col justify-between cursor-pointer"
                  >
                    {/* Thumbnail Card Banner */}
                    <div className="relative w-full aspect-video bg-neutral-900 overflow-hidden">
                      <img
                        src={c.trailerImg}
                        alt={c.title}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 opacity-90"
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />

                      {/* Top Badges */}
                      <div className="absolute top-3 left-3 flex items-center gap-2 z-10">
                        <span
                          className={`px-3 py-1 rounded-full text-white text-[10px] font-bold shadow-sm ${c.badgeBg}`}
                        >
                          {c.badge}
                        </span>
                        {isEnrolled && (
                          <span className="px-2.5 py-1 rounded-full bg-emerald-500 text-white text-[10px] font-bold shadow-sm flex items-center gap-1">
                            <span className="material-symbols-outlined text-[12px]">check</span>
                            Enrolled
                          </span>
                        )}
                      </div>

                      {/* Bottom Duration Badge */}
                      <div className="absolute bottom-3 right-3 px-2.5 py-1 rounded-lg bg-black/70 backdrop-blur-md text-white text-[10px] font-semibold flex items-center gap-1">
                        <span className="material-symbols-outlined text-[13px]">schedule</span>
                        <span>{c.hours}</span>
                      </div>
                    </div>

                    {/* Card Body */}
                    <div className="p-6 flex flex-col flex-1 gap-3">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-bold text-primary text-[11px] uppercase tracking-wider">
                          {c.category}
                        </span>
                        <div className="flex items-center gap-1 text-secondary-container">
                          <span
                            className="material-symbols-outlined text-[16px]"
                            style={{ fontVariationSettings: "'FILL' 1" }}
                          >
                            star
                          </span>
                          <span className="font-bold text-on-surface">{c.rating}</span>
                          <span className="text-[10px] text-on-surface-variant">{c.reviews}</span>
                        </div>
                      </div>

                      <h3 className="font-headline-md text-base sm:text-lg font-bold text-on-surface group-hover:text-primary transition-colors leading-snug">
                        {c.title}
                      </h3>

                      <p className="text-xs text-on-surface-variant line-clamp-2 leading-relaxed">
                        {c.desc}
                      </p>

                      {/* Instructor Meta */}
                      <div className="flex items-center gap-2.5 mt-auto pt-3 border-t border-surface-container">
                        <div
                          className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-xs shrink-0 ${c.instructor.avatarBg}`}
                        >
                          {c.instructor.avatar}
                        </div>
                        <div className="flex flex-col min-w-0">
                          <span className="text-xs font-bold text-on-surface truncate">
                            {c.instructor.name}
                          </span>
                          <span className="text-[10px] text-on-surface-variant truncate">
                            {c.instructor.role.split('•')[0]}
                          </span>
                        </div>
                      </div>

                      {/* Pricing & Enroll Action Footer */}
                      <div className="mt-3 pt-3 border-t border-surface-container flex items-center justify-between gap-3">
                        <div className="flex flex-col">
                          <span className="text-[10px] text-emerald-700 font-bold uppercase tracking-wider">
                            College Perk
                          </span>
                          <div className="flex items-baseline gap-1.5">
                            <span className="font-bold text-base text-primary">Free</span>
                            <span className="line-through text-[11px] text-on-surface-variant">
                              {c.price}
                            </span>
                          </div>
                        </div>

                        {/* Interactive Enroll Button */}
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={(e) => handleEnrollClick(e, c.id)}
                            className={`px-4 py-2 rounded-full text-xs font-bold transition-all shadow-sm hover:shadow-md flex items-center gap-1.5 cursor-pointer ${
                              isEnrolled
                                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                                : 'bg-primary-container hover:bg-secondary-container text-on-primary hover:scale-105 active:scale-95'
                            }`}
                          >
                            {isEnrolled ? (
                              <>
                                <span className="material-symbols-outlined text-[15px]">check_circle</span>
                                <span>Enrolled</span>
                              </>
                            ) : (
                              <>
                                <span>Enroll Now</span>
                                <span className="material-symbols-outlined text-[15px]">arrow_forward</span>
                              </>
                            )}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* ================= DIGITAL CAMPUS PASS MODAL ================= */}
      {showPassModal && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md animate-fadeIn"
        >
          <div className="relative w-full max-w-md bg-white rounded-3xl p-6 sm:p-8 shadow-2xl border border-outline-variant/30 flex flex-col items-center gap-5 animate-scaleUp">
            <div className="flex items-center justify-between w-full">
              <span className="text-xs font-bold text-primary uppercase tracking-wider flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[18px]">badge</span>
                Digital Student ID Pass
              </span>
              <button
                type="button"
                onClick={() => setShowPassModal(false)}
                className="w-8 h-8 rounded-full bg-surface-container flex items-center justify-center hover:bg-surface-container-high text-on-surface"
              >
                ✕
              </button>
            </div>

            <StudentCardPreview
              studentName={currentUser?.name || 'Alex Scholar'}
              universityName={currentUser?.university || 'University Campus'}
              disciplineId={currentUser?.disciplineId || 'health'}
              customMajor={currentUser?.majorName || 'Pre-Med & Human Biology'}
              yearId={currentUser?.year || 'sophomore'}
              email={currentUser?.email || 'scholar@university.edu'}
              studentId={currentUser?.studentId || 'STAN-2026-MED89'}
              isEduVerified={currentUser?.isEduVerified || true}
            />

            <button
              type="button"
              onClick={() => setShowPassModal(false)}
              className="w-full py-2.5 rounded-full bg-surface-container hover:bg-surface-container-high text-xs font-bold text-on-surface"
            >
              Close Pass
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
