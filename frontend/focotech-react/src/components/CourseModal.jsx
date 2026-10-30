import React, { useState, useEffect } from 'react';
import { courseData } from '../data/courses';
import { courseAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import CourseSyllabusTree from './CourseSyllabusTree';

export default function CourseModal({ courseId, isOpen, onClose }) {
  const { user, isAuthenticated } = useAuth();
  const [loading, setLoading] = useState(false);
  const [dbCourse, setDbCourse] = useState(null);
  const [reviewsData, setReviewsData] = useState({ reviews: [], averageRating: null, totalReviews: 0 });
  const [enrolled, setEnrolled] = useState(false);
  const [bookmarked, setBookmarked] = useState(false);
  const [enrollLoading, setEnrollLoading] = useState(false);
  const [activeLecture, setActiveLecture] = useState(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [notice, setNotice] = useState(null);

  // Fallback template course
  const fallbackCourse = courseData[courseId] || courseData['advance-ui-ux'];

  useEffect(() => {
    if (!isOpen || !courseId) return;

    setNotice(null);
    setActiveLecture(null);

    // If courseId is numeric or ID from DB, fetch from backend
    if (!isNaN(courseId)) {
      setLoading(true);
      courseAPI
        .getById(courseId)
        .then((res) => {
          if (res.data) {
            setDbCourse(res.data);
            setEnrolled(!!res.data.enrolled);
            setBookmarked(!!res.data.bookmarked);
            if (res.data.lectures && res.data.lectures.length > 0) {
              setActiveLecture(res.data.lectures[0]);
            }
          }
        })
        .catch(() => {
          setDbCourse(null);
        })
        .finally(() => setLoading(false));

      // Fetch reviews
      courseAPI
        .getReviews(courseId)
        .then((res) => {
          if (res.data) {
            setReviewsData({
              reviews: res.data.reviews || [],
              averageRating: res.data.averageRating,
              totalReviews: res.data.totalReviews || 0
            });
          }
        })
        .catch(() => {});
    } else {
      setDbCourse(null);
      setReviewsData({ reviews: [], averageRating: null, totalReviews: 0 });
    }
  }, [isOpen, courseId]);

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

  // Resolved course fields
  const course = dbCourse
    ? {
        id: dbCourse.id,
        title: dbCourse.title,
        desc: dbCourse.description || fallbackCourse.desc,
        badge: 'Database Course',
        badgeBg: 'bg-emerald-600',
        category: 'Live Curriculum',
        rating: dbCourse.averageRating || fallbackCourse.rating,
        fullReviews: `(${dbCourse.totalReviews || 0} reviews)`,
        hours: `${(dbCourse.lectures?.length || 4) * 2} Hours`,
        lessons: `${dbCourse.lectures?.length || 4} Lessons`,
        fullLessons: `${dbCourse.lectures?.length || 4} Interactive Lessons`,
        level: 'All Levels',
        price: 'Free',
        origPrice: '₹3,999',
        instructor: {
          name: dbCourse.instructorName || 'Lead Instructor',
          role: 'Platform Verified Instructor',
          avatar: dbCourse.instructorName ? dbCourse.instructorName.charAt(0) : 'I',
          avatarBg: 'bg-emerald-100 text-emerald-800'
        },
        trailerImg: fallbackCourse.trailerImg,
        capstoneTitle: `${dbCourse.title} Project`,
        capstoneDesc: 'Build and deploy a comprehensive real-world application with mentored feedback.',
        lectures: dbCourse.lectures || []
      }
    : fallbackCourse;

  const showNotice = (msg, isErr = false) => {
    setNotice({ text: msg, isErr });
    setTimeout(() => setNotice(null), 4000);
  };

  const handleEnrollClick = async () => {
    if (!isAuthenticated) {
      showNotice('Please sign in as a student to enroll in this course.', true);
      return;
    }

    if (!dbCourse) {
      // Mock enrollment for template course
      setEnrolled(true);
      showNotice(`Enrolled in "${course.title}"! Added to your learning track.`);
      return;
    }

    setEnrollLoading(true);
    try {
      const res = await courseAPI.enroll(dbCourse.id);
      setEnrollLoading(false);
      setEnrolled(true);
      showNotice(res.data?.message || `Successfully enrolled in ${course.title}!`);
    } catch (err) {
      setEnrollLoading(false);
      const msg = err.response?.data?.message || 'Enrollment failed. Please try again.';
      showNotice(msg, true);
    }
  };

  const handleBookmarkToggle = async () => {
    if (!isAuthenticated) {
      showNotice('Please sign in to bookmark courses.', true);
      return;
    }

    if (!dbCourse) {
      setBookmarked(!bookmarked);
      showNotice(!bookmarked ? 'Course bookmarked!' : 'Bookmark removed.');
      return;
    }

    try {
      if (bookmarked) {
        await courseAPI.removeBookmark(dbCourse.id);
        setBookmarked(false);
        showNotice('Bookmark removed.');
      } else {
        await courseAPI.addBookmark(dbCourse.id);
        setBookmarked(true);
        showNotice('Course added to your bookmarks!');
      }
    } catch (err) {
      showNotice(err.response?.data?.message || 'Bookmark action failed.', true);
    }
  };

  const handleAddReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) {
      showNotice('Please sign in to submit a review.', true);
      return;
    }

    if (!dbCourse) {
      showNotice('Reviews are currently enabled for live database courses.');
      return;
    }

    setSubmittingReview(true);
    try {
      const res = await courseAPI.addReview(dbCourse.id, {
        rating: Number(reviewRating),
        comment: reviewComment.trim()
      });
      setSubmittingReview(false);
      setReviewComment('');
      showNotice('Review submitted successfully! Thank you for your feedback.');
      // Refresh reviews list
      setReviewsData((prev) => ({
        ...prev,
        reviews: [res.data, ...prev.reviews],
        totalReviews: prev.totalReviews + 1
      }));
    } catch (err) {
      setSubmittingReview(false);
      const msg = err.response?.data?.message || 'Failed to submit review. (You must be enrolled).';
      showNotice(msg, true);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-course-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-fadeIn"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-md transition-opacity"
      />

      {/* Modal Window */}
      <div className="relative w-full max-w-4xl bg-surface rounded-3xl shadow-2xl border border-outline-variant/30 overflow-hidden z-10 flex flex-col max-h-[92vh] animate-scaleUp">
        {/* Top Header Bar */}
        <div className="sticky top-0 bg-white/95 backdrop-blur-md z-30 px-6 py-4 border-b border-surface-container flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span
              className={`px-3 py-1 rounded-full text-white font-label-sm font-bold shadow-xs uppercase tracking-wide ${course.badgeBg}`}
            >
              {course.badge}
            </span>
            <span className="font-label-md text-on-surface-variant font-semibold">
              {course.category}
            </span>
            <span className="hidden sm:inline-block w-1 h-1 rounded-full bg-outline-variant" />
            <span className="hidden sm:inline-flex items-center gap-1 text-label-sm text-secondary-container font-semibold">
              <span
                className="material-symbols-outlined text-[16px] text-amber-500"
                style={{ fontVariationSettings: "'FILL' 1" }}
              >
                star
              </span>
              <span className="text-on-surface font-bold">{course.rating}</span>
              <span className="text-on-surface-variant">{course.fullReviews}</span>
            </span>
          </div>

          <div className="flex items-center gap-2">
            {/* Bookmark button */}
            <button
              type="button"
              onClick={handleBookmarkToggle}
              title={bookmarked ? 'Remove Bookmark' : 'Add to Bookmarks'}
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-all cursor-pointer ${
                bookmarked
                  ? 'bg-primary-container text-white'
                  : 'bg-surface-container hover:bg-surface-container-high text-on-surface-variant'
              }`}
            >
              <span
                className="material-symbols-outlined text-[20px]"
                style={bookmarked ? { fontVariationSettings: "'FILL' 1" } : {}}
              >
                bookmark
              </span>
            </button>

            {/* Close button */}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close modal preview"
              className="w-10 h-10 rounded-full bg-surface-container hover:bg-surface-container-high text-on-surface flex items-center justify-center transition-all cursor-pointer"
            >
              <span className="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>
        </div>

        {/* Dynamic Alert Banner */}
        {notice && (
          <div
            className={`px-6 py-2.5 text-xs font-bold flex items-center justify-between ${
              notice.isErr ? 'bg-rose-50 text-rose-700 border-b border-rose-200' : 'bg-emerald-50 text-emerald-800 border-b border-emerald-200'
            }`}
          >
            <span>{notice.text}</span>
            <button onClick={() => setNotice(null)} className="underline cursor-pointer">
              Dismiss
            </button>
          </div>
        )}

        {/* Scrollable Modal Content */}
        <div className="overflow-y-auto px-6 py-6 md:p-8 flex flex-col gap-8 bg-surface">
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
                {enrolled && (
                  <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-100 text-emerald-800 font-label-sm font-bold">
                    <span className="material-symbols-outlined text-[18px]">check_circle</span>
                    <span>Enrolled (Active)</span>
                  </span>
                )}
              </div>
            </div>

            {/* Video Player or Trailer */}
            <div className="lg:col-span-5 relative w-full aspect-video rounded-2xl bg-neutral-900 overflow-hidden shadow-lg group flex items-center justify-center">
              {activeLecture?.videoId ? (
                <iframe
                  title={activeLecture.title}
                  src={`https://www.youtube-nocookie.com/embed/${activeLecture.videoId}?autoplay=1`}
                  className="w-full h-full border-0"
                  allow="accelerated-video; encrypted-media; picture-in-picture"
                  allowFullScreen
                />
              ) : (
                <>
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/40 to-transparent" />
                  <img
                    src={course.trailerImg}
                    alt="Course Preview"
                    className="w-full h-full object-cover opacity-60"
                  />
                  <div className="relative flex flex-col items-center justify-center gap-2 z-10 text-center p-3">
                    <div className="w-14 h-14 rounded-full bg-primary-container text-white flex items-center justify-center shadow-xl">
                      <span
                        className="material-symbols-outlined text-[32px] ml-0.5"
                        style={{ fontVariationSettings: "'FILL' 1" }}
                      >
                        play_arrow
                      </span>
                    </div>
                    <span className="font-label-sm text-white font-bold tracking-wide drop-shadow">
                      {course.lectures && course.lectures.length > 0
                        ? `Select a Lecture below to watch`
                        : `Interactive Video Syllabus`}
                    </span>
                  </div>
                </>
              )}
            </div>
          </div>

          {/* Instructor & Capstone Project Showcase */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 bg-surface-container-low p-6 rounded-2xl border border-outline-variant/30">
            {/* Instructor Card */}
            <div className="flex items-start gap-4">
              <div
                className={`w-14 h-14 rounded-2xl flex items-center justify-center font-bold text-headline-sm shrink-0 shadow-sm ${course.instructor?.avatarBg || 'bg-primary-fixed text-primary'}`}
              >
                {course.instructor?.avatar || 'I'}
              </div>
              <div className="flex flex-col">
                <span className="font-label-sm text-primary uppercase font-bold tracking-wider">
                  Lead Instructor
                </span>
                <h4 className="font-headline-sm text-headline-sm text-on-surface font-bold">
                  {course.instructor?.name}
                </h4>
                <p className="font-body-sm text-body-sm text-on-surface-variant">
                  {course.instructor?.role}
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

          {/* Database Course Lectures (if database course) */}
          {course.lectures && course.lectures.length > 0 ? (
            <div className="flex flex-col gap-4">
              <h3 className="font-headline-sm font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">video_library</span>
                <span>Course Lectures ({course.lectures.length})</span>
              </h3>
              <div className="grid grid-cols-1 gap-2.5">
                {course.lectures.map((lec, idx) => {
                  const isActive = activeLecture?.id === lec.id;
                  return (
                    <div
                      key={lec.id || idx}
                      onClick={() => setActiveLecture(lec)}
                      className={`p-3.5 rounded-2xl border transition-all cursor-pointer flex items-center justify-between ${
                        isActive
                          ? 'bg-primary-container/10 border-primary shadow-xs'
                          : 'bg-white hover:bg-surface-container-low border-surface-container'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-8 h-8 rounded-xl bg-surface-container text-on-surface-variant flex items-center justify-center font-bold text-xs">
                          {idx + 1}
                        </span>
                        <div>
                          <h5 className="font-bold text-sm text-on-surface">{lec.title}</h5>
                          <span className="text-xs text-on-surface-variant">YouTube Lesson</span>
                        </div>
                      </div>
                      <span className="material-symbols-outlined text-primary text-[20px]">
                        {isActive ? 'pause_circle' : 'play_circle'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : (
            /* Template Course Modules / Syllabus Tree */
            <div className="flex flex-col gap-3">
              <CourseSyllabusTree
                initialCourseId={
                  courseId === 'fullstack-web'
                    ? 'course-web-dev'
                    : courseId === 'mobile-app'
                    ? 'course-mobile-app'
                    : 'course-ui-ux'
                }
              />
            </div>
          )}

          {/* Reviews & Student Feedback Section */}
          <div className="flex flex-col gap-4 pt-4 border-t border-surface-container">
            <div className="flex items-center justify-between">
              <h3 className="font-headline-sm font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-amber-500">rate_review</span>
                <span>Student Reviews ({reviewsData.totalReviews})</span>
              </h3>
              {reviewsData.averageRating && (
                <span className="text-sm font-bold text-amber-600 bg-amber-50 px-3 py-1 rounded-full border border-amber-200">
                  ★ {reviewsData.averageRating.toFixed(1)} / 5.0
                </span>
              )}
            </div>

            {/* Existing Reviews List */}
            {reviewsData.reviews && reviewsData.reviews.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {reviewsData.reviews.map((r) => (
                  <div
                    key={r.id}
                    className="p-4 rounded-2xl bg-surface-container-low border border-surface-container flex flex-col gap-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-sm text-on-surface">
                        {r.studentName || 'Student'}
                      </span>
                      <div className="flex items-center text-amber-500 text-xs">
                        {[...Array(5)].map((_, i) => (
                          <span
                            key={i}
                            className="material-symbols-outlined text-[15px]"
                            style={{ fontVariationSettings: i < r.rating ? "'FILL' 1" : "''" }}
                          >
                            star
                          </span>
                        ))}
                      </div>
                    </div>
                    <p className="text-xs text-on-surface-variant leading-relaxed">
                      "{r.comment}"
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-on-surface-variant italic">
                No reviews yet for this course. Be the first to review!
              </p>
            )}

            {/* Write a review form (available for enrolled students) */}
            {enrolled && (
              <form onSubmit={handleAddReview} className="mt-3 p-4 rounded-2xl bg-white border border-surface-container flex flex-col gap-3">
                <span className="font-bold text-sm text-on-surface">Leave Your Review</span>
                <div className="flex items-center gap-3">
                  <label className="text-xs font-semibold text-on-surface-variant">Rating:</label>
                  <select
                    value={reviewRating}
                    onChange={(e) => setReviewRating(e.target.value)}
                    className="px-2.5 py-1 rounded-lg border border-surface-container text-xs font-bold"
                  >
                    <option value="5">★★★★★ (5 Stars - Excellent)</option>
                    <option value="4">★★★★☆ (4 Stars - Very Good)</option>
                    <option value="3">★★★☆☆ (3 Stars - Average)</option>
                    <option value="2">★★☆☆☆ (2 Stars - Needs Improvement)</option>
                    <option value="1">★☆☆☆☆ (1 Star - Poor)</option>
                  </select>
                </div>
                <textarea
                  rows={2}
                  value={reviewComment}
                  onChange={(e) => setReviewComment(e.target.value)}
                  placeholder="Share what you liked about this course, lectures, and exercises..."
                  className="w-full p-2.5 rounded-xl border border-surface-container text-xs focus:outline-none focus:ring-1 focus:ring-primary"
                  required
                />
                <button
                  type="submit"
                  disabled={submittingReview}
                  className="self-end px-4 py-2 rounded-xl bg-primary-container text-white font-bold text-xs hover:bg-secondary-container transition-all cursor-pointer"
                >
                  {submittingReview ? 'Submitting...' : 'Submit Review'}
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Modal Sticky Footer / CTA Bar */}
        <div className="sticky bottom-0 bg-white/95 backdrop-blur-md px-6 py-4 md:px-8 border-t border-surface-container flex flex-col sm:flex-row items-center justify-between gap-4 z-20">
          <div className="flex items-baseline gap-3">
            <div className="flex flex-col">
              <span className="text-xs text-on-surface-variant font-medium">Platform Enrollment</span>
              <div className="flex items-baseline gap-2">
                <span className="font-headline-xl text-headline-xl font-bold text-on-surface">
                  {course.price}
                </span>
                <span className="line-through text-on-surface-variant text-body-sm">
                  {course.origPrice}
                </span>
                <span className="text-xs bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded font-bold">
                  Free Access
                </span>
              </div>
            </div>
            <span className="text-xs text-on-surface-variant hidden md:inline-flex items-center gap-1 ml-2">
              <span className="material-symbols-outlined text-[16px] text-emerald-600">verified_user</span> Lifetime access &amp; Certificate
            </span>
          </div>

          <div className="flex items-center gap-3 w-full sm:w-auto">
            {enrolled ? (
              <span className="px-6 py-3 rounded-full bg-emerald-600 text-white font-label-lg font-bold shadow-sm flex items-center gap-2">
                <span className="material-symbols-outlined text-[18px]">verified</span>
                <span>Enrolled (Active)</span>
              </span>
            ) : (
              <button
                type="button"
                onClick={handleEnrollClick}
                disabled={enrollLoading}
                className="flex-1 sm:flex-none px-7 py-3 rounded-full bg-primary-container hover:bg-secondary-container text-white font-label-lg font-bold shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all duration-200 flex items-center justify-center gap-2 cursor-pointer"
              >
                <span>{enrollLoading ? 'Enrolling...' : 'Enroll Now'}</span>
                <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
