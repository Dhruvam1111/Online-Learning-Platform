import React, { useState, useEffect } from 'react';
import DailyStreakCalendar from './DailyStreakCalendar';
import CourseSyllabusTree from './CourseSyllabusTree';
import { useAuth } from '../context/AuthContext';
import { studentAPI, courseAPI } from '../services/api';

export default function StudentDashboard({ onNavigateHome }) {
  const { user, logout, refreshUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [enrolledCourses, setEnrolledCourses] = useState([]);
  const [bookmarks, setBookmarks] = useState([]);
  const [catalogCourses, setCatalogCourses] = useState([]);
  const [activeMenu, setActiveMenu] = useState('Overview');
  const [enrollModal, setEnrollModal] = useState(null);
  const [activeWatchCourse, setActiveWatchCourse] = useState(null);
  const [courseLectures, setCourseLectures] = useState([]);
  const [activeLecture, setActiveLecture] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isApiLoading, setIsApiLoading] = useState(false);

  // Profile Form state for Settings tab
  const [profileForm, setProfileForm] = useState({
    name: '',
    educationLevel: '',
    interests: '',
    learningGoals: '',
    bio: ''
  });
  const [savingProfile, setSavingProfile] = useState(false);

  // Load dashboard data from backend API
  const loadDashboardData = () => {
    setIsApiLoading(true);

    studentAPI
      .getProfile()
      .then((res) => {
        if (res.data) {
          setProfile(res.data);
          setProfileForm({
            name: res.data.name || user?.name || '',
            educationLevel: res.data.educationLevel || '',
            interests: res.data.interests || '',
            learningGoals: res.data.learningGoals || '',
            bio: res.data.bio || ''
          });
        }
      })
      .catch(() => {});

    studentAPI
      .getMyCourses()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setEnrolledCourses(res.data);
        }
      })
      .catch(() => {});

    studentAPI
      .getBookmarks()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setBookmarks(res.data);
        }
      })
      .catch(() => {});

    courseAPI
      .getAll()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setCatalogCourses(res.data);
        }
      })
      .catch(() => {})
      .finally(() => setIsApiLoading(false));
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  const displayName = profile?.name || user?.name || 'Student Learner';
  const displayRole = profile?.bio || profile?.interests || 'Active Student Workspace';

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3500);
  };

  const handleEnrollCourse = async (courseId, courseTitle) => {
    try {
      const res = await courseAPI.enroll(courseId);
      showToast(res.data?.message || `Enrolled in "${courseTitle}"!`);
      // Refresh enrolled courses
      studentAPI.getMyCourses().then((cRes) => {
        if (Array.isArray(cRes.data)) setEnrolledCourses(cRes.data);
      });
      if (enrollModal) setEnrollModal(null);
    } catch (err) {
      showToast(err.response?.data?.message || 'Enrollment failed.', true);
    }
  };

  const handleRemoveBookmark = async (courseId) => {
    try {
      await courseAPI.removeBookmark(courseId);
      showToast('Bookmark removed.');
      studentAPI.getBookmarks().then((res) => {
        if (Array.isArray(res.data)) setBookmarks(res.data);
      });
    } catch (err) {
      showToast('Failed to remove bookmark.', true);
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSavingProfile(true);
    try {
      const res = await studentAPI.updateProfile(profileForm);
      setSavingProfile(false);
      setProfile(res.data);
      if (refreshUser) refreshUser();
      showToast('Student profile updated successfully!');
    } catch (err) {
      setSavingProfile(false);
      showToast(err.response?.data?.message || 'Failed to update profile.', true);
    }
  };

  const handleWatchCourse = (course) => {
    const courseId = course.courseId || course.id;
    setActiveWatchCourse(course);
    courseAPI
      .getLectures(courseId)
      .then((res) => {
        if (Array.isArray(res.data) && res.data.length > 0) {
          setCourseLectures(res.data);
          setActiveLecture(res.data[0]);
        } else {
          setCourseLectures([]);
          setActiveLecture(null);
        }
      })
      .catch(() => {
        setCourseLectures([]);
        setActiveLecture(null);
      });
  };

  const menuItems = [
    { name: 'Overview', icon: 'grid_view' },
    { name: 'Courses', icon: 'video_library', badge: enrolledCourses.length > 0 ? enrolledCourses.length : null },
    { name: 'Bookmarks', icon: 'bookmark', badge: bookmarks.length > 0 ? bookmarks.length : null },
    { name: 'Certificates', icon: 'workspace_premium' },
    { name: 'Settings', icon: 'settings' }
  ];

  return (
    <div className="min-h-screen bg-[#fcf6f2] font-sans text-[#262320] flex flex-col antialiased selection:bg-[#ff5a36] selection:text-white">
      {/* ========================================================
          1. TOP TOOLBAR
          ======================================================== */}
      <header className="w-full bg-white/95 backdrop-blur-md border-b border-[#faeae0] sticky top-0 z-40 shadow-xs">
        <div className="max-w-[1560px] mx-auto px-4 sm:px-6 lg:px-8 h-14 flex items-center justify-between gap-4">
          {/* Left: Logo */}
          <button
            type="button"
            onClick={onNavigateHome}
            className="flex items-center gap-2.5 group cursor-pointer focus:outline-none shrink-0"
          >
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#e5411d] to-[#ff5a36] flex items-center justify-center shadow-md shadow-[#ff5a36]/25 group-hover:scale-105 transition-transform">
              <span className="material-symbols-outlined text-white text-[19px]">school</span>
            </div>
            <span className="text-[20px] font-black tracking-tight text-[#1e1916] group-hover:text-primary transition-colors">
              Focotech
            </span>
          </button>

          {/* Center: Extended Search Bar */}
          <div className="relative flex-1 max-w-2xl mx-2 sm:mx-6">
            <span className="material-symbols-outlined absolute left-3.5 top-1/2 -translate-y-1/2 text-[18px] text-[#a49992]">
              search
            </span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search courses, lessons, topics, practice problems..."
              className="w-full bg-[#fbf6f2] hover:bg-[#f6eee7] focus:bg-white pl-10 pr-12 py-2 rounded-full text-xs sm:text-[13px] text-[#2c2622] placeholder-[#a49992] border border-[#f0e3db] focus:border-[#ff5a36] focus:ring-2 focus:ring-[#ff5a36]/10 focus:outline-none transition-all shadow-2xs"
            />
            <kbd className="absolute right-3.5 top-1/2 -translate-y-1/2 bg-[#efe4dc] text-[#8e8076] px-2 py-0.5 rounded text-[10px] font-bold">
              ⌘K
            </kbd>
          </div>

          {/* Right: API Indicator, Streak, Profile */}
          <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
            {/* Backend connection pill */}
            <span className="hidden md:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              API Connected
            </span>

            {/* Streak Counter */}
            <div
              onClick={() => showToast('Current Streak: 54 Days! Keep it up.')}
              className="flex items-center gap-1 px-2.5 py-1 rounded-full bg-[#fff2ec] hover:bg-[#ffe7dc] text-[#ff5a36] font-extrabold text-xs cursor-pointer transition-colors shadow-2xs"
              title="54 Days Streak"
            >
              <span className="text-[13px]">🔥</span>
              <span>54</span>
            </div>

            {/* Profile Avatar */}
            <div
              onClick={() => setActiveMenu('Settings')}
              className="w-8 h-8 rounded-full ring-2 ring-[#ff5a36]/40 overflow-hidden shrink-0 shadow-xs cursor-pointer hover:ring-[#ff5a36] transition-all flex items-center justify-center bg-primary text-white font-bold text-xs"
              title={displayName}
            >
              {displayName.charAt(0).toUpperCase()}
            </div>

            {/* Back to Home Button */}
            <button
              type="button"
              onClick={onNavigateHome}
              className="px-3 py-1.5 rounded-full bg-surface-container hover:bg-surface-container-high text-xs font-bold text-on-surface transition-colors cursor-pointer"
            >
              Catalog
            </button>
          </div>
        </div>
      </header>

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#1e1916] text-white px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-white/10 animate-slideUp">
          <span className="material-symbols-outlined text-[#ff5a36] text-[20px]">info</span>
          <span className="text-xs font-semibold">{toastMessage}</span>
        </div>
      )}

      {/* Main Workspace Layout */}
      <div className="w-full max-w-[1560px] mx-auto p-4 sm:p-6 lg:p-8 flex flex-col xl:flex-row gap-6">
        {/* Left Sidebar Menu */}
        <aside className="w-full xl:w-[220px] shrink-0 bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col justify-between self-start">
          <div className="flex flex-col gap-4">
            <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] px-3 block">
              STUDENT MENU
            </span>
            <nav className="flex flex-col gap-1">
              {menuItems.map((item) => {
                const isActive = activeMenu === item.name;
                return (
                  <button
                    key={item.name}
                    type="button"
                    onClick={() => setActiveMenu(item.name)}
                    className={`flex items-center justify-between px-3.5 py-2.5 rounded-2xl font-semibold text-[13.5px] transition-all w-full text-left cursor-pointer ${
                      isActive
                        ? 'bg-[#fff0eb] text-[#ff5a36] shadow-2xs font-bold'
                        : 'text-[#6d635c] hover:bg-[#fff7f4] hover:text-[#ff5a36]'
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span
                        className="material-symbols-outlined text-[20px]"
                        style={isActive ? { fontVariationSettings: "'FILL' 1" } : {}}
                      >
                        {item.icon}
                      </span>
                      <span>{item.name}</span>
                    </div>
                    {item.badge != null && (
                      <span className="bg-[#ff5a36] text-white text-[11px] font-bold px-2 py-0.5 rounded-full">
                        {item.badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </nav>
          </div>

          <div className="pt-4 mt-6 border-t border-[#fae8dc]">
            <button
              type="button"
              onClick={() => {
                logout();
                if (onNavigateHome) onNavigateHome();
              }}
              className="flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-rose-600 hover:bg-rose-50 text-[13.5px] font-semibold w-full transition-colors cursor-pointer"
            >
              <span className="material-symbols-outlined text-[20px]">logout</span>
              <span>Sign Out</span>
            </button>
          </div>
        </aside>

        {/* Center Main View (Switched by activeMenu) */}
        <main className="flex-1 min-w-0 flex flex-col gap-7">
          {/* ========================================================
              TAB 1: OVERVIEW
              ======================================================== */}
          {activeMenu === 'Overview' && (
            <>
              {/* Database Enrolled Courses Banner */}
              <div className="bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center gap-3.5">
                  <div className="w-12 h-12 rounded-2xl bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center font-bold">
                    <span className="material-symbols-outlined text-[26px]">school</span>
                  </div>
                  <div>
                    <h4 className="font-extrabold text-[16px] text-[#1e1916]">
                      {enrolledCourses.length > 0
                        ? `${enrolledCourses.length} Active Enrolled Course${enrolledCourses.length > 1 ? 's' : ''}`
                        : 'No Enrolled Courses Yet'}
                    </h4>
                    <p className="text-[12px] text-[#8e8076]">
                      {enrolledCourses.length > 0
                        ? enrolledCourses.map((c) => c.title).join(' • ')
                        : 'Browse courses below and enroll to start learning with video lectures.'}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setActiveMenu('Courses')}
                    className="px-4 py-2 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer"
                  >
                    View My Courses
                  </button>
                </div>
              </div>

              {/* Interactive Curriculum Syllabus Tree */}
              <CourseSyllabusTree
                onSelectLesson={(lesson, subject) =>
                  showToast(`Opened lesson: "${lesson}" in ${subject}`)
                }
              />

              {/* Available Database Courses Section with 1-Click API Enrollment */}
              <section className="flex flex-col gap-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-[18px] font-extrabold text-[#1f1a17]">
                      Featured Database Courses
                    </h3>
                    <p className="text-xs text-[#8e8076]">
                      Synced directly from PostgreSQL backend API points
                    </p>
                  </div>
                  <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200">
                    Live Endpoints
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  {catalogCourses.map((c) => {
                    const isAlreadyEnrolled = enrolledCourses.some(
                      (ec) => (ec.courseId || ec.id) === c.id
                    );

                    return (
                      <article
                        key={c.id}
                        className="bg-white rounded-3xl p-5 border border-[#fae9de] shadow-xs flex flex-col justify-between group hover:shadow-lg transition-all"
                      >
                        <div>
                          <div className="flex items-center justify-between mb-3">
                            <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[11px] font-bold">
                              Verified Course
                            </span>
                            <div className="flex items-center gap-1 text-xs font-bold text-amber-500">
                              <span className="material-symbols-outlined text-[15px]" style={{ fontVariationSettings: "'FILL' 1" }}>
                                star
                              </span>
                              <span>{c.averageRating || '4.9'}</span>
                              <span className="text-[#8e8076] font-normal">({c.totalReviews || 12})</span>
                            </div>
                          </div>

                          <h4 className="font-extrabold text-[16px] text-[#1e1916] leading-snug group-hover:text-[#ff5a36] transition-colors">
                            {c.title}
                          </h4>
                          <p className="text-xs text-[#8e8076] mt-2 line-clamp-2">
                            {c.description || 'Full interactive lecture track with assignments and certification.'}
                          </p>
                          <p className="text-xs font-semibold text-[#554a43] mt-2">
                            Instructor: {c.instructorName || 'Platform Instructor'}
                          </p>
                        </div>

                        <div className="flex items-center justify-between pt-4 mt-4 border-t border-[#f7ede6]">
                          <span className="text-xs font-bold text-emerald-600">Free Access</span>
                          {isAlreadyEnrolled ? (
                            <button
                              onClick={() => {
                                setActiveMenu('Courses');
                                handleWatchCourse(c);
                              }}
                              className="px-4 py-1.5 rounded-full bg-emerald-50 text-emerald-700 text-xs font-bold hover:bg-emerald-100 transition-colors cursor-pointer flex items-center gap-1"
                            >
                              <span className="material-symbols-outlined text-[16px]">play_arrow</span>
                              <span>Go to Lectures</span>
                            </button>
                          ) : (
                            <button
                              onClick={() => handleEnrollCourse(c.id, c.title)}
                              className="px-4 py-1.5 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer flex items-center gap-1"
                            >
                              <span className="material-symbols-outlined text-[16px]">add</span>
                              <span>Enroll Now</span>
                            </button>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              </section>
            </>
          )}

          {/* ========================================================
              TAB 2: MY ENROLLED COURSES & LECTURES WATCHER
              ======================================================== */}
          {activeMenu === 'Courses' && (
            <div className="flex flex-col gap-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1f1a17]">
                    My Enrolled Courses ({enrolledCourses.length})
                  </h2>
                  <p className="text-xs text-[#8e8076]">
                    Courses currently saved in database table 'enrollments'
                  </p>
                </div>
                <button
                  onClick={loadDashboardData}
                  className="px-3.5 py-1.5 rounded-full bg-white border border-[#fae8dc] text-xs font-bold text-[#6d635c] hover:text-[#ff5a36] flex items-center gap-1 cursor-pointer"
                >
                  <span className="material-symbols-outlined text-[16px]">refresh</span> Refresh
                </button>
              </div>

              {/* Video Player Modal/Section if Active Course is Selected */}
              {activeWatchCourse && (
                <div className="bg-white rounded-3xl p-6 border-2 border-[#ff5a36]/30 shadow-lg flex flex-col gap-4">
                  <div className="flex items-center justify-between border-b border-[#faeae0] pb-3">
                    <div>
                      <span className="text-[11px] font-bold text-[#ff5a36] uppercase tracking-wider">
                        Now Watching
                      </span>
                      <h3 className="text-[18px] font-extrabold text-[#1e1916]">
                        {activeWatchCourse.title}
                      </h3>
                    </div>
                    <button
                      onClick={() => setActiveWatchCourse(null)}
                      className="w-8 h-8 rounded-full bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center cursor-pointer hover:bg-[#ffe2d8]"
                    >
                      <span className="material-symbols-outlined text-[18px]">close</span>
                    </button>
                  </div>

                  <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                    {/* Embedded YouTube Player */}
                    <div className="lg:col-span-8 aspect-video bg-black rounded-2xl overflow-hidden shadow-inner">
                      {activeLecture?.videoId ? (
                        <iframe
                          title={activeLecture.title}
                          src={`https://www.youtube-nocookie.com/embed/${activeLecture.videoId}?autoplay=1`}
                          className="w-full h-full border-0"
                          allow="accelerated-video; encrypted-media; picture-in-picture"
                          allowFullScreen
                        />
                      ) : (
                        <div className="w-full h-full flex flex-col items-center justify-center text-white/70 p-4 text-center">
                          <span className="material-symbols-outlined text-[48px] text-[#ff5a36] mb-2">
                            play_circle
                          </span>
                          <p className="font-bold">Select a lecture from the playlist to start watching.</p>
                        </div>
                      )}
                    </div>

                    {/* Lecture Playlist */}
                    <div className="lg:col-span-4 flex flex-col gap-2 max-h-[380px] overflow-y-auto pr-1">
                      <span className="text-xs font-bold text-[#8e8076] uppercase tracking-wider mb-1">
                        Lectures ({courseLectures.length})
                      </span>
                      {courseLectures.length > 0 ? (
                        courseLectures.map((lec, idx) => {
                          const isCurrent = activeLecture?.id === lec.id;
                          return (
                            <div
                              key={lec.id || idx}
                              onClick={() => setActiveLecture(lec)}
                              className={`p-3 rounded-xl border text-xs cursor-pointer transition-all flex items-center justify-between ${
                                isCurrent
                                  ? 'bg-[#fff0eb] border-[#ff5a36] text-[#ff5a36] font-bold'
                                  : 'bg-[#fbf7f4] hover:bg-[#fff7f4] border-[#f0dfd3] text-[#3a322d]'
                              }`}
                            >
                              <div className="flex items-center gap-2 truncate">
                                <span className="w-5 h-5 rounded-full bg-white flex items-center justify-center font-bold text-[10px] shrink-0">
                                  {idx + 1}
                                </span>
                                <span className="truncate">{lec.title}</span>
                              </div>
                              <span className="material-symbols-outlined text-[16px] shrink-0">
                                {isCurrent ? 'pause_circle' : 'play_circle'}
                              </span>
                            </div>
                          );
                        })
                      ) : (
                        <p className="text-xs text-[#8e8076] italic">
                          No lectures published yet for this course.
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              {/* Enrolled Courses Cards Grid */}
              {enrolledCourses.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  {enrolledCourses.map((c) => (
                    <article
                      key={c.courseId || c.id}
                      className="bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[11px] font-bold">
                            Enrolled
                          </span>
                          <span className="text-xs text-[#8e8076]">
                            {c.enrolledAt ? `Enrolled ${new Date(c.enrolledAt).toLocaleDateString()}` : 'Lifetime Access'}
                          </span>
                        </div>
                        <h3 className="font-extrabold text-[16px] text-[#1e1916] mt-2">
                          {c.title}
                        </h3>
                        <p className="text-xs text-[#8e8076] mt-1">
                          Mentor: {c.instructorName || 'Platform Instructor'}
                        </p>
                      </div>

                      <div className="mt-4 pt-3 border-t border-[#f7ede6] flex items-center justify-between">
                        <span className="text-xs font-bold text-[#ff5a36]">Progress: 42%</span>
                        <button
                          onClick={() => handleWatchCourse(c)}
                          className="px-4 py-2 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer flex items-center gap-1.5 shadow-sm"
                        >
                          <span className="material-symbols-outlined text-[17px]">play_arrow</span>
                          <span>Watch Lectures</span>
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="bg-white rounded-3xl p-8 border border-[#fae8dc] text-center flex flex-col items-center justify-center gap-3">
                  <span className="material-symbols-outlined text-[48px] text-[#ff5a36]">
                    video_library
                  </span>
                  <h3 className="font-bold text-lg text-[#1e1916]">You have no active enrollments</h3>
                  <p className="text-xs text-[#8e8076] max-w-md">
                    Switch to the Overview tab or Catalog to enroll in courses. All courses are free for registered students.
                  </p>
                  <button
                    onClick={() => setActiveMenu('Overview')}
                    className="px-5 py-2.5 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer"
                  >
                    Browse Available Courses
                  </button>
                </div>
              )}
            </div>
          )}

          {/* ========================================================
              TAB 3: BOOKMARKS
              ======================================================== */}
          {activeMenu === 'Bookmarks' && (
            <div className="flex flex-col gap-6">
              <div>
                <h2 className="text-[20px] font-extrabold text-[#1f1a17]">
                  Saved Bookmarks ({bookmarks.length})
                </h2>
                <p className="text-xs text-[#8e8076]">
                  Courses bookmarked via backend API endpoint '/api/courses/&#123;id&#125;/bookmark'
                </p>
              </div>

              {bookmarks.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  {bookmarks.map((b) => (
                    <article
                      key={b.courseId}
                      className="bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className="px-3 py-1 rounded-full bg-[#fff0eb] text-[#ff5a36] text-[11px] font-bold">
                            Bookmarked
                          </span>
                          <button
                            onClick={() => handleRemoveBookmark(b.courseId)}
                            className="text-xs text-rose-600 hover:underline cursor-pointer"
                          >
                            Remove
                          </button>
                        </div>
                        <h3 className="font-extrabold text-[16px] text-[#1e1916] mt-2">
                          {b.title}
                        </h3>
                        <p className="text-xs text-[#8e8076] mt-1 line-clamp-2">
                          {b.description || 'Full course curriculum with interactive exercises.'}
                        </p>
                      </div>

                      <div className="mt-4 pt-3 border-t border-[#f7ede6] flex items-center justify-between">
                        <span className="text-xs font-semibold text-[#8e8076]">
                          {b.instructorName ? `By ${b.instructorName}` : 'Instructor'}
                        </span>
                        <button
                          onClick={() => handleEnrollCourse(b.courseId, b.title)}
                          className="px-4 py-2 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer"
                        >
                          Enroll Now
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="bg-white rounded-3xl p-8 border border-[#fae8dc] text-center flex flex-col items-center justify-center gap-3">
                  <span className="material-symbols-outlined text-[48px] text-[#ff5a36]">
                    bookmark_border
                  </span>
                  <h3 className="font-bold text-lg text-[#1e1916]">No bookmarked courses yet</h3>
                  <p className="text-xs text-[#8e8076]">
                    Click the bookmark icon on any course preview modal to save it here.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* ========================================================
              TAB 4: CERTIFICATES
              ======================================================== */}
          {activeMenu === 'Certificates' && (
            <div className="bg-white rounded-3xl p-8 border border-[#fae8dc] flex flex-col gap-4 text-center items-center justify-center">
              <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center">
                <span className="material-symbols-outlined text-[36px]">workspace_premium</span>
              </div>
              <h3 className="font-bold text-lg text-[#1e1916]">Verified Course Certificates</h3>
              <p className="text-xs text-[#8e8076] max-w-md">
                Complete all course lectures and assignments to generate your official, cryptographically verified certificate signed by your instructors.
              </p>
              <div className="p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] max-w-sm w-full text-left flex items-center gap-3 mt-2">
                <span className="material-symbols-outlined text-emerald-600">verified</span>
                <div>
                  <h5 className="font-bold text-xs text-[#1e1916]">Web Development Fundamentals</h5>
                  <span className="text-[11px] text-[#8e8076]">In progress — 85% completed</span>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 5: SETTINGS & STUDENT PROFILE (PUT /api/student/profile)
              ======================================================== */}
          {activeMenu === 'Settings' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="border-b border-[#faeae0] pb-4">
                <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Student Profile Settings</h2>
                <p className="text-xs text-[#8e8076]">
                  Synchronized with backend table 'student_profiles' via PUT /api/student/profile
                </p>
              </div>

              <form onSubmit={handleSaveProfile} className="flex flex-col gap-4 max-w-2xl">
                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Full Name
                  </label>
                  <input
                    type="text"
                    value={profileForm.name}
                    onChange={(e) => setProfileForm({ ...profileForm, name: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-[#ff5a36] focus:outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Education Level
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Undergraduate, Computer Science B.Tech"
                    value={profileForm.educationLevel}
                    onChange={(e) => setProfileForm({ ...profileForm, educationLevel: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-[#ff5a36] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Interests &amp; Topics
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. React, Spring Boot, UI/UX, Cloud Architecture"
                    value={profileForm.interests}
                    onChange={(e) => setProfileForm({ ...profileForm, interests: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-[#ff5a36] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Learning Goals
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Become a full-stack engineer, build scalable web apps"
                    value={profileForm.learningGoals}
                    onChange={(e) => setProfileForm({ ...profileForm, learningGoals: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-[#ff5a36] focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Bio
                  </label>
                  <textarea
                    rows={3}
                    placeholder="Short bio about your background, hobbies, and coding aspirations..."
                    value={profileForm.bio}
                    onChange={(e) => setProfileForm({ ...profileForm, bio: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-[#ff5a36] focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  disabled={savingProfile}
                  className="mt-2 py-3 px-6 bg-[#ff5a36] text-white rounded-xl font-bold text-sm hover:bg-[#e5411d] transition-all self-start cursor-pointer shadow-md shadow-[#ff5a36]/20"
                >
                  {savingProfile ? 'Saving Changes...' : 'Save Profile Changes'}
                </button>
              </form>
            </div>
          )}
        </main>

        {/* Right Sidebar: Profile & Streak */}
        <aside className="w-full xl:w-[320px] shrink-0 flex flex-col gap-6">
          {/* Profile Card */}
          <div className="bg-white rounded-3xl p-5 border border-[#fae9de] shadow-xs flex flex-col gap-4">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-full ring-2 ring-[#ff5a36] ring-offset-2 overflow-hidden shrink-0 shadow-sm flex items-center justify-center bg-primary text-white font-extrabold text-base">
                {displayName.charAt(0).toUpperCase()}
              </div>
              <div className="flex flex-col">
                <h3 className="font-extrabold text-[15px] text-[#1e1916] leading-tight">
                  {displayName}
                </h3>
                <span className="text-[12px] text-[#8e8076] font-medium truncate max-w-[200px]">
                  {displayRole}
                </span>
                <div className="flex items-center gap-1 mt-1">
                  <span className="text-[13px]">🎖️</span>
                  <span className="text-[12px] font-extrabold text-[#955416]">
                    {enrolledCourses.length * 150 + 250} XP Points
                  </span>
                </div>
              </div>
            </div>

            {/* Quick Badges */}
            <div className="grid grid-cols-3 gap-2 pt-3 border-t border-[#f0dfd3] text-center">
              <div className="flex flex-col items-center">
                <div className="flex items-center gap-1">
                  <span className="text-[13px]">🔥</span>
                  <span className="font-extrabold text-[15px] text-[#1e1916]">54</span>
                </div>
                <span className="text-[10px] text-[#8c7e75] font-semibold mt-0.5">Days Streak</span>
              </div>
              <div className="flex flex-col items-center">
                <div className="flex items-center gap-1">
                  <span className="text-[13px]">📚</span>
                  <span className="font-extrabold text-[15px] text-[#1e1916]">{enrolledCourses.length}</span>
                </div>
                <span className="text-[10px] text-[#8c7e75] font-semibold mt-0.5">Enrolled</span>
              </div>
              <div className="flex flex-col items-center">
                <div className="flex items-center gap-1">
                  <span className="text-[13px]">🔖</span>
                  <span className="font-extrabold text-[15px] text-[#1e1916]">{bookmarks.length}</span>
                </div>
                <span className="text-[10px] text-[#8c7e75] font-semibold mt-0.5">Bookmarks</span>
              </div>
            </div>
          </div>

          {/* Daily Streak Calendar */}
          <DailyStreakCalendar />
        </aside>
      </div>
    </div>
  );
}
