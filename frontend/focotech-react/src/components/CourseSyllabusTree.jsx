import React, { useState, useEffect } from 'react';
import { coursesCurriculum } from '../data/curriculumData';

/**
 * Unified CourseSyllabusTree Component
 * 
 * - Course-driven architecture: User selects a course, and the entire curriculum
 *   is presented directly based on that course.
 * - Intermediate "Subjects" tab portion removed as requested.
 * - Everything is structured directly by the selected course and its modules.
 * - Interactive topic checkboxes [✓] that update live progress across the course.
 * - Video done/total counters and progress bars per chapter.
 * - Separated video link icons (YouTube red button) with video preview modal.
 */
export default function CourseSyllabusTree({
  initialCourseId = 'course-ui-ux',
  onSelectLesson,
  onCourseChange
}) {
  const [selectedCourseId, setSelectedCourseId] = useState(initialCourseId);
  const [syllabusOpen, setSyllabusOpen] = useState(true);

  // Expanded states
  const [expandedChapters, setExpandedChapters] = useState({});
  const [expandedSubSections, setExpandedSubSections] = useState({});
  const [activeLesson, setActiveLesson] = useState('');

  // Toast feedback
  const [toastMessage, setToastMessage] = useState('');

  const triggerToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3000);
  };

  // Completed topics tracking
  const [completedTopics, setCompletedTopics] = useState({
    'Color Semantics & Primitive Palettes': true,
    'Spacing & Layout Scales (4pt/8pt grids)': true,
    'Typography Tokens & Line-height Ratios': true,
    'Dark Mode & Elevation Variables': true,
    'Button States & Icon Slot Combinations': true,
    'Input Fields with Validation States': true,
    'useState & useReducer State Machines': true,
    'useEffect Cleanup & Event Listeners': true,
    'useMemo & useCallback Performance Tuning': true,
    'Custom Theme Extensions in tailwind.config': true,
    'View, Text, Image & SafeAreaView Handling': true,
    'Flexbox Alignment on iOS vs Android': true
  });

  // Bookmarked topics tracking
  const [bookmarkedTopics, setBookmarkedTopics] = useState({
    'Typography Tokens & Line-height Ratios': true,
    'useState & useReducer State Machines': true
  });

  // Active Video Modal State
  const [videoModalData, setVideoModalData] = useState(null);

  // Active course
  const activeCourse =
    coursesCurriculum.find((c) => c.id === selectedCourseId) || coursesCurriculum[0];

  // Auto-expand first chapter on course selection
  useEffect(() => {
    if (activeCourse && activeCourse.subjects && activeCourse.subjects.length > 0) {
      const firstSub = activeCourse.subjects[0];
      if (firstSub.chapters && firstSub.chapters.length > 0) {
        const firstCh = firstSub.chapters[0];
        const initialChapters = { [firstCh.id]: true };
        const initialSub = {};
        if (firstCh.subSections && firstCh.subSections.length > 0) {
          initialSub[firstCh.subSections[0].id] = true;
          if (firstCh.subSections[0].lessons && firstCh.subSections[0].lessons.length > 0) {
            setActiveLesson(firstCh.subSections[0].lessons[0]);
          }
        }
        setExpandedChapters(initialChapters);
        setExpandedSubSections(initialSub);
      }
    }
  }, [selectedCourseId]);

  const handleCourseSelect = (courseId) => {
    setSelectedCourseId(courseId);
    if (onCourseChange) onCourseChange(courseId);
  };

  const toggleChapter = (id) => {
    setExpandedChapters((prev) => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const toggleSubSection = (id) => {
    setExpandedSubSections((prev) => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const toggleTopicCompleted = (e, topicTitle) => {
    e.stopPropagation();
    const nextVal = !completedTopics[topicTitle];
    setCompletedTopics((prev) => ({
      ...prev,
      [topicTitle]: nextVal
    }));
    triggerToast(nextVal ? `Completed "${topicTitle}"!` : `Marked "${topicTitle}" incomplete`);
  };

  const toggleBookmark = (e, topicTitle) => {
    e.stopPropagation();
    const nextVal = !bookmarkedTopics[topicTitle];
    setBookmarkedTopics((prev) => ({
      ...prev,
      [topicTitle]: nextVal
    }));
    triggerToast(nextVal ? `Bookmarked "${topicTitle}"` : `Bookmark removed`);
  };

  const handleLessonClick = (lessonTitle, moduleName) => {
    setActiveLesson(lessonTitle);
    if (onSelectLesson) onSelectLesson(lessonTitle, moduleName, activeCourse.courseName);
  };

  // Open Video Modal
  const openVideoModal = (e, lessonTitle, chapterTitle, moduleName) => {
    e.stopPropagation();
    const isDone = !!completedTopics[lessonTitle];
    const searchQuery = encodeURIComponent(`${activeCourse.courseName} ${lessonTitle} tutorial`);
    const youtubeUrl = `https://www.youtube.com/results?search_query=${searchQuery}`;

    setVideoModalData({
      title: lessonTitle,
      chapter: chapterTitle,
      subject: moduleName,
      course: activeCourse.courseName,
      isDone,
      youtubeUrl,
      duration: '14:28 min',
      instructor: 'Focotech Senior Educator'
    });
  };

  const getTopicDifficulty = (index) => {
    if (index % 3 === 0) return { label: 'Basic', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' };
    if (index % 3 === 1) return { label: 'Medium', color: 'bg-amber-50 text-amber-700 border-amber-200' };
    return { label: 'Advanced', color: 'bg-[#fff0eb] text-[#ff5a36] border-[#fcdcd0]' };
  };

  // Compute stats for all courses
  const getCourseStats = (course) => {
    let total = 0;
    let done = 0;
    let chaptersCount = 0;
    course.subjects?.forEach((s) => {
      s.chapters?.forEach((c) => {
        chaptersCount += 1;
        c.subSections?.forEach((sub) => {
          sub.lessons?.forEach((l) => {
            total += 1;
            if (completedTopics[l]) done += 1;
          });
        });
      });
    });
    const pct = total > 0 ? Math.round((done / total) * 100) : 0;
    return { total, done, pct, chaptersCount };
  };

  const activeStats = getCourseStats(activeCourse);

  return (
    <div className="w-full flex flex-col gap-6">
      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#1f1a17] text-white px-4 py-2.5 rounded-2xl shadow-xl flex items-center gap-2 text-xs font-bold animate-fadeIn border border-white/10">
          <span className="material-symbols-outlined text-[16px] text-[#ff5a36]">check_circle</span>
          <span>{toastMessage}</span>
        </div>
      )}

      {/* ========================================================
          1. COURSE SELECTION CARDS ("Continue Learning")
          ======================================================== */}
      <section className="flex flex-col gap-3.5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1.5">
          <div>
            <h2 className="text-[20px] font-extrabold text-[#1f1a17] tracking-tight">
              Continue Learning
            </h2>
            <p className="text-[12.5px] text-[#8e8076] font-medium">
              Select a course to view its complete syllabus, chapters &amp; video lessons
            </p>
          </div>
          <span className="text-[11px] font-bold px-3 py-1 rounded-full bg-[#fff0eb] text-[#ff5a36] border border-[#fcdcd0] self-start sm:self-auto">
            3 Active Courses Enrolled
          </span>
        </div>

        {/* 3 Enrolled Course Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {coursesCurriculum.map((course) => {
            const isSelected = course.id === selectedCourseId;
            const stats = getCourseStats(course);

            return (
              <div
                key={course.id}
                onClick={() => handleCourseSelect(course.id)}
                className={`pro-card group bg-white rounded-3xl p-5 border flex flex-col justify-between gap-4 cursor-pointer relative ${
                  isSelected
                    ? 'border-[#ff5a36] ring-2 ring-[#ff5a36]/20 shadow-[0_10px_28px_rgba(255,90,54,0.15)] bg-gradient-to-b from-[#fffaf7] to-[#ffffff]'
                    : 'border-[#fae9de] hover:border-[#f5d5c5] shadow-[0_4px_16px_rgba(230,200,180,0.12)]'
                }`}
              >
                {/* Active Indicator Pin */}
                {isSelected && (
                  <div className="absolute -top-2.5 right-4 bg-[#ff5a36] text-white text-[10px] font-extrabold uppercase px-2.5 py-0.5 rounded-full shadow-xs flex items-center gap-1.5 animate-fadeIn">
                    <span className="w-1.5 h-1.5 rounded-full bg-white animate-breathe" />
                    <span>Active Course</span>
                  </div>
                )}

                {/* Card Top: Icon & Title */}
                <div className="flex items-start gap-3.5">
                  <div
                    className={`w-11 h-11 rounded-2xl flex items-center justify-center shrink-0 transition-colors border ${
                      isSelected
                        ? 'bg-[#ff5a36] text-white border-[#e5411d] shadow-sm'
                        : 'bg-[#fff2ed] text-[#ff5a36] border-[#ffded2]'
                    }`}
                  >
                    <span className="material-symbols-outlined text-[24px]">
                      {course.icon || 'school'}
                    </span>
                  </div>
                  <div className="flex flex-col min-w-0 flex-1">
                    <h3
                      className={`font-extrabold text-[15.5px] leading-snug line-clamp-1 transition-colors ${
                        isSelected ? 'text-[#ff5a36]' : 'text-[#1f1b18]'
                      }`}
                    >
                      {course.courseName}
                    </h3>
                    <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] mt-0.5 truncate">
                      {course.category}
                    </span>
                  </div>
                </div>

                {/* Progress Bar & Stats */}
                <div className="flex flex-col gap-2">
                  <div className="w-full bg-[#f4ece7] h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-gradient-to-r from-[#ff7a5c] to-[#ff5a36] h-full rounded-full transition-all duration-500"
                      style={{ width: `${stats.pct}%` }}
                    />
                  </div>
                  <div className="flex items-center justify-between text-[12px] text-[#8c7d74] font-medium">
                    <span className="flex items-center gap-1.5 font-bold text-[#443b35]">
                      <span className="material-symbols-outlined text-[15px] text-[#ff5a36]">
                        menu_book
                      </span>
                      {stats.done}/{stats.total} Lessons Done
                    </span>
                    <span className="text-[11px] font-bold text-[#9d8d85]">
                      {stats.pct}%
                    </span>
                  </div>
                </div>

                {/* Card Footer */}
                <div className="pt-2 border-t border-[#f7ede5] flex items-center justify-between">
                  <div
                    className={`font-bold text-[12.5px] inline-flex items-center gap-1 transition-colors ${
                      isSelected ? 'text-[#ff5a36]' : 'text-[#7d6f66] group-hover:text-[#ff5a36]'
                    }`}
                  >
                    <span>{isSelected ? 'Viewing Syllabus' : 'View Syllabus & Topics'}</span>
                    <span className="transition-transform group-hover:translate-x-0.5">
                      {isSelected ? '↓' : '→'}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleCourseSelect(course.id);
                      triggerToast(`Resumed "${course.courseName}"`);
                    }}
                    className={`pro-btn px-3 py-1 rounded-full text-[11px] font-extrabold cursor-pointer ${
                      isSelected
                        ? 'bg-[#ff5a36] hover:bg-[#e5411d] text-white shadow-xs'
                        : 'bg-[#fff2ed] hover:bg-[#ffe6dc] text-[#ff5a36]'
                    }`}
                  >
                    Resume
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* ========================================================
          2. DIRECT COURSE SYLLABUS ACCORDION
             (Everything structured directly by the selected course)
          ======================================================== */}
      <div className="w-full bg-white rounded-3xl border border-[#fae8dc] shadow-[0_4px_20px_rgba(230,200,180,0.15)] overflow-hidden transition-all">
        {/* Accordion Header Bar */}
        <button
          type="button"
          onClick={() => setSyllabusOpen(!syllabusOpen)}
          className="w-full px-5 sm:px-6 py-4 flex items-center justify-between bg-gradient-to-r from-[#fffaf7] to-[#ffffff] hover:bg-[#fff5f0] border-b border-[#faeae0] transition-colors cursor-pointer text-left focus:outline-none"
        >
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center shrink-0 border border-[#fcdcd0]">
              <span className="material-symbols-outlined text-[22px]">
                {activeCourse.icon || 'menu_book'}
              </span>
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-[17px] text-[#1f1a17] tracking-tight">
                  {activeCourse.courseName} Syllabus
                </span>
                <span className="px-2.5 py-0.5 rounded-full bg-[#fff0eb] text-[#ff5a36] text-[11px] font-bold border border-[#fcdcd0]">
                  {activeStats.chaptersCount} Chapters • {activeStats.total} Lessons
                </span>
              </div>
              <p className="text-[12px] text-[#6d635c] font-medium line-clamp-1 mt-0.5">
                {activeCourse.category} • Complete Structured Curriculum
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Live Course Completion Progress */}
            <div className="hidden sm:flex items-center gap-2 px-3 py-1 bg-[#fff2ec] rounded-full border border-[#fcdcd0]">
              <span className="text-[11px] font-bold text-[#6d635c]">Total Completed:</span>
              <span className="text-[11.5px] font-extrabold text-[#ff5a36]">
                {activeStats.done}/{activeStats.total} ({activeStats.pct}%)
              </span>
            </div>

            <div className="w-8 h-8 rounded-full bg-[#fff2ec] flex items-center justify-center text-[#ff5a36] hover:bg-[#ffe7dc] transition-colors shrink-0">
              <span className="material-symbols-outlined text-[20px] transition-transform duration-200">
                {syllabusOpen ? 'keyboard_arrow_up' : 'keyboard_arrow_down'}
              </span>
            </div>
          </div>
        </button>

        {/* Tree Body */}
        {syllabusOpen && (
          <div className="p-4 sm:p-6 flex flex-col gap-6 bg-[#fdfaf7]/40">
            {activeCourse.subjects && activeCourse.subjects.length > 0 ? (
              activeCourse.subjects.map((module, mIdx) => {
                return (
                  <div key={module.id} className="flex flex-col gap-2.5">
                    {/* Module Title Separator */}
                    <div className="flex items-center justify-between pb-1.5 border-b border-[#f3e5dc]">
                      <div className="flex items-center gap-2">
                        <span className="w-6 h-6 rounded-lg bg-[#ff5a36] text-white flex items-center justify-center text-[11px] font-extrabold">
                          {mIdx + 1}
                        </span>
                        <h4 className="font-extrabold text-[14.5px] text-[#1f1a17]">
                          {module.subjectName}
                        </h4>
                      </div>
                      <span className="text-[11px] font-semibold text-[#8e8076]">
                        {module.chapters?.length || 0} Chapters
                      </span>
                    </div>

                    {/* Chapters inside this module */}
                    <div className="flex flex-col gap-2">
                      {module.chapters?.map((chapter) => {
                        const isChapterOpen = !!expandedChapters[chapter.id];

                        // Calculate chapter-level done / total
                        let chapterTotal = 0;
                        let chapterDone = 0;
                        chapter.subSections?.forEach((s) => {
                          s.lessons?.forEach((l) => {
                            chapterTotal += 1;
                            if (completedTopics[l]) chapterDone += 1;
                          });
                        });
                        const chapterPct =
                          chapterTotal > 0 ? Math.round((chapterDone / chapterTotal) * 100) : 0;

                        return (
                          <div
                            key={chapter.id}
                            className="flex flex-col bg-white rounded-2xl border border-[#faeae0] p-1.5 shadow-2xs transition-all"
                          >
                            {/* Chapter Header (Level 1) */}
                            <button
                              type="button"
                              onClick={() => toggleChapter(chapter.id)}
                              className="flex items-center justify-between py-2.5 px-3 rounded-xl text-[14px] font-extrabold text-[#1f1a17] hover:bg-[#fff3ec] transition-colors cursor-pointer text-left w-full group"
                            >
                              <div className="flex items-center gap-2.5 min-w-0">
                                <span className="w-6 h-6 rounded-lg bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center group-hover:bg-[#ff5a36] group-hover:text-white transition-colors shrink-0">
                                  <span className="material-symbols-outlined text-[17px]">
                                    {isChapterOpen ? 'keyboard_arrow_down' : 'chevron_right'}
                                  </span>
                                </span>
                                <span className="truncate group-hover:text-[#ff5a36] transition-colors">
                                  {chapter.title}
                                </span>
                              </div>

                              {/* Right: Progress bar & done/total counter */}
                              <div className="flex items-center gap-3 shrink-0 ml-2">
                                <div className="w-16 sm:w-20 bg-[#f0ded2] h-1.5 rounded-full overflow-hidden">
                                  <div
                                    className="bg-[#ff5a36] h-full rounded-full transition-all duration-300"
                                    style={{ width: `${chapterPct}%` }}
                                  />
                                </div>
                                <span className="text-[12px] font-bold text-[#8e8076] tabular-nums min-w-[28px] text-right">
                                  {chapterDone}/{chapterTotal}
                                </span>
                              </div>
                            </button>

                            {/* Sub-sections & Lessons (Level 2 & Level 3 Tree) */}
                            {isChapterOpen && chapter.subSections && (
                              <div className="ml-4 sm:ml-7 pl-3.5 sm:pl-4 border-l-2 border-[#f0ded2] flex flex-col gap-3 my-2 relative">
                                {chapter.subSections.map((subSec) => {
                                  const isSubOpen = !!expandedSubSections[subSec.id];

                                  // Sub-section level done / total
                                  const subTotal = subSec.lessons?.length || 0;
                                  let subDone = 0;
                                  subSec.lessons?.forEach((l) => {
                                    if (completedTopics[l]) subDone += 1;
                                  });
                                  const subPct =
                                    subTotal > 0 ? Math.round((subDone / subTotal) * 100) : 0;

                                  return (
                                    <div key={subSec.id} className="relative flex flex-col">
                                      {/* Sub-section Pill with Branch Curve & Progress (Level 2) */}
                                      <div className="flex items-center justify-between pr-2">
                                        <div className="flex items-center">
                                          <span className="w-3.5 h-4 border-b-2 border-[#f0ded2] -ml-3.5 sm:-ml-4 mr-2 rounded-bl-sm" />

                                          <button
                                            type="button"
                                            onClick={() => toggleSubSection(subSec.id)}
                                            className={`inline-flex items-center gap-1.5 py-1 px-3.5 rounded-full text-[12.5px] font-bold transition-all cursor-pointer shadow-2xs border ${
                                              isSubOpen
                                                ? 'bg-[#fff0eb] text-[#ff5a36] border-[#fcdcd0]'
                                                : 'bg-[#f7f2ed] hover:bg-[#fff0eb] text-[#4d443e] border-[#faeae0]'
                                            }`}
                                          >
                                            <span className="material-symbols-outlined text-[15px] text-[#ff5a36]">
                                              {isSubOpen ? 'keyboard_arrow_down' : 'chevron_right'}
                                            </span>
                                            <span>{subSec.title}</span>
                                          </button>
                                        </div>

                                        {/* Sub-section done/total progress indicator */}
                                        <div className="flex items-center gap-2.5">
                                          <div className="w-12 sm:w-16 bg-[#f0ded2] h-1.5 rounded-full overflow-hidden hidden sm:block">
                                            <div
                                              className="bg-emerald-500 h-full rounded-full transition-all duration-300"
                                              style={{ width: `${subPct}%` }}
                                            />
                                          </div>
                                          <span className="text-[11px] font-extrabold text-[#7e7167] tabular-nums">
                                            {subDone}/{subTotal}
                                          </span>
                                        </div>
                                      </div>

                                      {/* Lessons List (Level 3 with Checkbox, Title, Difficulty Tag, and Video Link Icon) */}
                                      {isSubOpen && subSec.lessons && (
                                        <div className="ml-4 sm:ml-6 pl-3 sm:pl-3.5 border-l-2 border-[#f0ded2] flex flex-col gap-1.5 my-2">
                                          {subSec.lessons.map((lesson, idx) => {
                                            const isDone = !!completedTopics[lesson];
                                            const isBookmarked = !!bookmarkedTopics[lesson];
                                            const isCurrent = activeLesson === lesson;
                                            const difficulty = getTopicDifficulty(idx);

                                            return (
                                              <div
                                                key={lesson}
                                                onClick={() =>
                                                  handleLessonClick(lesson, module.subjectName)
                                                }
                                                className={`pro-row group relative flex items-center justify-between gap-2 sm:gap-3 py-2 px-2.5 sm:px-3 rounded-xl text-[13px] cursor-pointer ${
                                                  isCurrent
                                                    ? 'bg-[#fff4ed] text-[#1f1a17] shadow-2xs border border-[#fcd8c9]'
                                                    : isDone
                                                    ? 'bg-white/80 text-[#3d342f] hover:bg-[#fffaf7] border border-emerald-100/60'
                                                    : 'bg-white hover:bg-[#fff8f4] text-[#423933] border border-[#f4eae2]'
                                                }`}
                                              >
                                                <span className="w-3 h-3.5 border-b-2 border-[#f0ded2] -ml-5 sm:-ml-5.5 mr-0.5 rounded-bl-sm shrink-0" />

                                                {/* Left Area: Checkbox + Title + Difficulty Tag */}
                                                <div className="flex items-center gap-2.5 min-w-0 flex-1">
                                                  {/* Checkbox (Clickable toggle with tactile pop) */}
                                                  <button
                                                    type="button"
                                                    title={
                                                      isDone
                                                        ? 'Mark as incomplete'
                                                        : 'Mark as completed'
                                                    }
                                                    onClick={(e) =>
                                                      toggleTopicCompleted(e, lesson)
                                                    }
                                                    className={`w-5 h-5 rounded-md flex items-center justify-center shrink-0 transition-all cursor-pointer ${
                                                      isDone
                                                        ? 'bg-emerald-500 text-white shadow-xs animate-check-pop'
                                                        : 'border-2 border-[#d6c7bb] hover:border-[#ff5a36] bg-white'
                                                    }`}
                                                  >
                                                    {isDone && (
                                                      <span className="material-symbols-outlined text-[14px] font-bold">
                                                        check
                                                      </span>
                                                    )}
                                                  </button>

                                                  {/* Lesson Title */}
                                                  <span
                                                    className={`truncate font-semibold text-[13px] ${
                                                      isDone
                                                        ? 'text-[#6b6058] line-through decoration-emerald-500/50'
                                                        : isCurrent
                                                        ? 'text-[#ff5a36] font-bold'
                                                        : 'text-[#1f1a17]'
                                                    }`}
                                                  >
                                                    {lesson}
                                                  </span>

                                                  {/* Difficulty Tag Badge */}
                                                  <span
                                                    className={`px-2 py-0.5 text-[10px] font-extrabold rounded-md border shrink-0 hidden xs:inline-block ${difficulty.color}`}
                                                  >
                                                    {difficulty.label}
                                                  </span>
                                                </div>

                                                {/* Right Action Icons Strip */}
                                                <div className="flex items-center gap-1 sm:gap-2 shrink-0">
                                                  {/* Video Link Icon (Red YouTube button) */}
                                                  <button
                                                    type="button"
                                                    title={`Watch video tutorial: ${lesson}`}
                                                    onClick={(e) =>
                                                      openVideoModal(
                                                        e,
                                                        lesson,
                                                        chapter.title,
                                                        module.subjectName
                                                      )
                                                    }
                                                    className="w-7 h-7 rounded-lg bg-red-50 hover:bg-red-500 text-red-600 hover:text-white flex items-center justify-center transition-all shadow-2xs group/vid cursor-pointer"
                                                  >
                                                    <span
                                                      className="material-symbols-outlined text-[16px]"
                                                      style={{ fontVariationSettings: "'FILL' 1" }}
                                                    >
                                                      smart_display
                                                    </span>
                                                  </button>

                                                  {/* Document / Article Icon */}
                                                  <button
                                                    type="button"
                                                    title="Read lesson article & documentation"
                                                    onClick={(e) => {
                                                      e.stopPropagation();
                                                      handleLessonClick(
                                                        lesson,
                                                        module.subjectName
                                                      );
                                                    }}
                                                    className="w-7 h-7 rounded-lg hover:bg-[#fff0eb] text-[#8e8076] hover:text-[#ff5a36] flex items-center justify-center transition-colors"
                                                  >
                                                    <span className="material-symbols-outlined text-[16px]">
                                                      article
                                                    </span>
                                                  </button>

                                                  {/* Notes Icon */}
                                                  <button
                                                    type="button"
                                                    title="Take personal notes"
                                                    onClick={(e) => {
                                                      e.stopPropagation();
                                                      handleLessonClick(
                                                        lesson,
                                                        module.subjectName
                                                      );
                                                    }}
                                                    className="w-7 h-7 rounded-lg hover:bg-[#fff0eb] text-[#8e8076] hover:text-[#ff5a36] flex items-center justify-center transition-colors hidden sm:flex"
                                                  >
                                                    <span className="material-symbols-outlined text-[16px]">
                                                      edit_note
                                                    </span>
                                                  </button>

                                                  {/* Bookmark Icon */}
                                                  <button
                                                    type="button"
                                                    title={
                                                      isBookmarked
                                                        ? 'Remove bookmark'
                                                        : 'Bookmark topic'
                                                    }
                                                    onClick={(e) => toggleBookmark(e, lesson)}
                                                    className={`w-7 h-7 rounded-lg flex items-center justify-center transition-colors ${
                                                      isBookmarked
                                                        ? 'text-[#ff5a36] bg-[#fff0eb]'
                                                        : 'text-[#9c8d84] hover:text-[#ff5a36] hover:bg-[#fff0eb]'
                                                    }`}
                                                  >
                                                    <span
                                                      className="material-symbols-outlined text-[16px]"
                                                      style={
                                                        isBookmarked
                                                          ? { fontVariationSettings: "'FILL' 1" }
                                                          : {}
                                                      }
                                                    >
                                                      bookmark
                                                    </span>
                                                  </button>
                                                </div>
                                              </div>
                                            );
                                          })}
                                        </div>
                                      )}
                                    </div>
                                  );
                                })}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="py-8 text-center text-[#8e8076] text-sm">
                No syllabus data currently loaded for this course.
              </div>
            )}
          </div>
        )}
      </div>

      {/* ========================================================
          3. CONNECTED VIDEO PLAYER MODAL
          ======================================================== */}
      {videoModalData && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-black/60 backdrop-blur-sm animate-fadeIn"
          onClick={() => setVideoModalData(null)}
        >
          <div
            className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl border border-[#fae8dc] overflow-hidden flex flex-col animate-scaleUp"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Modal Top Header */}
            <div className="px-5 py-4 bg-gradient-to-r from-[#fffaf7] to-[#ffffff] border-b border-[#faeae0] flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-red-100 text-red-600 flex items-center justify-center shrink-0">
                  <span
                    className="material-symbols-outlined text-[18px]"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    play_arrow
                  </span>
                </div>
                <div>
                  <span className="text-[11px] font-bold uppercase tracking-wider text-red-600">
                    Video Lesson • {videoModalData.duration}
                  </span>
                  <h3 className="text-[15px] font-extrabold text-[#1f1a17] line-clamp-1">
                    {videoModalData.title}
                  </h3>
                </div>
              </div>

              <button
                type="button"
                onClick={() => setVideoModalData(null)}
                className="w-8 h-8 rounded-full bg-[#f4ebe4] hover:bg-[#eae0d8] text-[#4d443e] flex items-center justify-center transition-colors cursor-pointer"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            {/* Video Screen Player Simulation */}
            <div className="relative aspect-video bg-black flex items-center justify-center group overflow-hidden">
              <div className="absolute inset-0 bg-gradient-to-br from-neutral-900 via-neutral-800 to-black opacity-90" />

              <div className="absolute top-4 left-4 right-4 flex items-center justify-between z-10 text-white/90">
                <span className="text-xs font-semibold bg-black/60 backdrop-blur-md px-3 py-1 rounded-full border border-white/10">
                  {videoModalData.subject}
                </span>
                <span className="text-xs font-bold text-red-400 bg-black/60 px-2.5 py-1 rounded-md">
                  1080p HD
                </span>
              </div>

              <a
                href={videoModalData.youtubeUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="relative z-10 w-16 h-16 rounded-full bg-red-600 hover:bg-red-500 text-white flex items-center justify-center shadow-xl shadow-red-600/40 hover:scale-110 active:scale-95 transition-all cursor-pointer group/play"
              >
                <span
                  className="material-symbols-outlined text-[34px] ml-1 group-hover/play:scale-110 transition-transform"
                  style={{ fontVariationSettings: "'FILL' 1" }}
                >
                  play_arrow
                </span>
              </a>

              <div className="absolute bottom-3 left-4 right-4 z-10 flex flex-col gap-1.5">
                <div className="w-full bg-white/20 h-1.5 rounded-full overflow-hidden">
                  <div className="bg-red-600 h-full w-1/3 rounded-full" />
                </div>
                <div className="flex items-center justify-between text-[11px] text-white/80 font-medium">
                  <span>04:42 / {videoModalData.duration}</span>
                  <span>{videoModalData.chapter}</span>
                </div>
              </div>
            </div>

            {/* Modal Bottom Controls */}
            <div className="p-5 flex flex-col sm:flex-row items-center justify-between gap-3 bg-white">
              <div className="flex items-center gap-2 w-full sm:w-auto">
                <button
                  type="button"
                  onClick={() => {
                    toggleTopicCompleted(
                      { stopPropagation: () => {} },
                      videoModalData.title
                    );
                    setVideoModalData((prev) => ({
                      ...prev,
                      isDone: !prev.isDone
                    }));
                  }}
                  className={`flex-1 sm:flex-none flex items-center justify-center gap-2 px-4 py-2 rounded-xl text-xs font-bold border transition-all cursor-pointer ${
                    videoModalData.isDone
                      ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                      : 'bg-white hover:bg-[#fff2ec] text-[#1f1a17] border-[#fae8dc]'
                  }`}
                >
                  <span className="material-symbols-outlined text-[16px] text-emerald-600">
                    {videoModalData.isDone ? 'check_circle' : 'radio_button_unchecked'}
                  </span>
                  <span>{videoModalData.isDone ? 'Completed' : 'Mark as Done'}</span>
                </button>
              </div>

              <a
                href={videoModalData.youtubeUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full sm:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-extrabold shadow-md shadow-red-600/25 transition-all cursor-pointer"
              >
                <span
                  className="material-symbols-outlined text-[16px]"
                  style={{ fontVariationSettings: "'FILL' 1" }}
                >
                  open_in_new
                </span>
                <span>Open Video Link</span>
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
