import React, { useState, useEffect } from 'react';
import StudentWallet from './StudentWallet';
import StudentSubscriptions from './StudentSubscriptions';
import DailyStreakCalendar from './DailyStreakCalendar';
import CourseSyllabusTree from './CourseSyllabusTree';
import RazorpayPaymentModal from './RazorpayPaymentModal';
import StudentStudyGuideView from './StudentStudyGuideView';
import { useAuth } from '../context/AuthContext';
import api, { studentAPI, courseAPI, paymentAPI } from '../services/api';
import { getCoursePricing } from '../utils/pricing';

const purchaseTotal = (transaction) =>
  (Number(transaction.amount || 0) + Number(transaction.walletAmountUsed || 0)).toFixed(2);

export default function StudentDashboard({ onNavigateHome }) {
  const { user, logout, refreshUser } = useAuth();
  const [catalogPrices, setCatalogPrices] = useState({});
  const [profile, setProfile] = useState(null);
  const [enrolledCourses, setEnrolledCourses] = useState([]);
  const [bookmarks, setBookmarks] = useState([]);
  const [catalogCourses, setCatalogCourses] = useState([]);
  const [activeMenu, setActiveMenu] = useState('Overview');
  const [enrollModal, setEnrollModal] = useState(null);
  const [paymentModalCourse, setPaymentModalCourse] = useState(null);
  const [activeWatchCourse, setActiveWatchCourse] = useState(null);
  const [courseLectures, setCourseLectures] = useState([]);
  const [activeLecture, setActiveLecture] = useState(null);
  const [toastMessage, setToastMessage] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isApiLoading, setIsApiLoading] = useState(false);
  const [transactions, setTransactions] = useState([]);
  const [refundReviews, setRefundReviews] = useState([]);
  const [refundReviewError, setRefundReviewError] = useState('');
  const [walletRevision, setWalletRevision] = useState(0);
  const [activeSubscriptionsCount, setActiveSubscriptionsCount] = useState(0);

  // Refund Modal State
  const [refundModalTx, setRefundModalTx] = useState(null);
  const [refundReason, setRefundReason] = useState('Purchased by mistake');
  const [customRefundReason, setCustomRefundReason] = useState('');
  const [refundComment, setRefundComment] = useState('');
  const [isRefunding, setIsRefunding] = useState(false);

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
    api.get('/payments/refunds/exceptions').then(({ data }) => {
      setRefundReviews(Array.isArray(data) ? data : []);
      setRefundReviewError('');
    }).catch(() => setRefundReviewError('Administrator review statuses could not be loaded. Please refresh purchases.'));

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
      .catch(() => {});

    paymentAPI
      .getMyTransactions()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setTransactions(res.data);
        }
      })
      .catch(() => {})
      .finally(() => setIsApiLoading(false));
  };

  const checkStripeReturn = async () => {
    let sessionId = null;
    if (window.location.search) {
      const sp = new URLSearchParams(window.location.search);
      sessionId = sp.get('stripe_session_id') || sp.get('session_id');
    }
    if (!sessionId && window.location.hash.includes('?')) {
      const hp = new URLSearchParams(window.location.hash.split('?')[1]);
      sessionId = hp.get('stripe_session_id') || hp.get('session_id');
    }

    if (sessionId) {
      showToast('⚡ Verifying Stripe Checkout with cloud servers...');
      try {
        const res = await paymentAPI.verifyStripeSession(sessionId);
        showToast(`🎉 Stripe Payment Verified! Enrolled in course. Invoice: ${res.data?.invoiceNumber || 'INV-STRIPE'}`);
        const cleanUrl = window.location.origin + window.location.pathname + '#dashboard';
        window.history.replaceState({}, document.title, cleanUrl);
        loadDashboardData();
        setActiveMenu('Courses');
      } catch (err) {
        showToast(err.response?.data?.message || 'Stripe session verification completed.', false);
        loadDashboardData();
      }
    }
  };

  useEffect(() => {
    loadDashboardData();
    checkStripeReturn();
  }, []);

  useEffect(() => {
    loadDashboardData();
  }, [activeMenu]);

  useEffect(() => { api.get('/payments/policy/catalog').then(res => setCatalogPrices(Object.fromEntries(res.data.map(row => [row.courseId, Number(row.amount)])))).catch(() => setCatalogPrices({})); }, [activeMenu]);
  useEffect(() => {
    api.get('/payments/subscriptions')
      .then(res => {
        if (Array.isArray(res.data)) {
          setActiveSubscriptionsCount(res.data.filter(s => s.status !== 'cancelled').length);
        }
      })
      .catch(() => {});
  }, [activeMenu]);
  const pricingFor = course => { const amount = catalogPrices[course.courseId || course.id]; return amount == null ? { ...getCoursePricing(course), formatted: 'Loading current price…' } : getCoursePricing({ ...course, price: amount }); };
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
      const { data } = await api.get(`/payments/policy/courses/${courseId}`);
      setPaymentModalCourse({ id: courseId, title: courseTitle, price: Number(data.amount) });
      if (enrollModal) setEnrollModal(null);
    } catch (error) {
      const status = error.response?.status;
      showToast(status === 401 || status === 403
        ? 'Your session may have expired or this account cannot purchase. Sign out and sign in with a student account, then try again.'
        : 'Could not load the current price. Check your connection and try again.');
    }
  };

  const handleReceipt = async (transaction, print) => {
    const receiptWindow = print ? window.open('about:blank', '_blank') : null;
    if (print && !receiptWindow) {
      showToast('Allow pop-ups to print your receipt.', false);
      return;
    }
    try {
      const response = await api.get(`/payments/invoices/${encodeURIComponent(transaction.orderId)}/download`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'text/html' }));
      if (print) {
        receiptWindow.onload = () => receiptWindow.print();
        receiptWindow.location.href = url;
      } else {
        const link = document.createElement('a');
        link.href = url;
        link.download = `Receipt-${transaction.invoiceNumber || transaction.orderId}.html`;
        document.body.appendChild(link);
        link.click();
        link.remove();
      }
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (error) {
      receiptWindow?.close();
      showToast('Could not load your receipt. Please try again.', false);
    }
  };

  const handleOpenRefundModal = (tx) => {
    setRefundModalTx(tx);
    setRefundReason('Purchased by mistake');
    setCustomRefundReason('');
    setRefundComment('');
  };

  const handleConfirmRefund = async () => {
    if (!refundModalTx) return;
    if (refundReason === 'Other' && !customRefundReason.trim()) {
      showToast('Please specify your refund reason.');
      return;
    }
    const reason = refundReason === 'Other' ? customRefundReason.trim() : refundReason;
    const finalReason = refundComment.trim() ? `${reason}\nStudent comment: ${refundComment.trim()}` : reason;
    if (finalReason.length > 1000) {
      showToast('Please shorten your reason and comment to 1,000 characters combined.');
      return;
    }

    setIsRefunding(true);
    try {
      const refId = refundModalTx.paymentId || refundModalTx.orderId;
      const { data: refund } = await paymentAPI.requestRefund(refId, finalReason);
      if (refund.status === 'pending_review') {
        showToast('Your purchase is older than 30 days. Your refund request has been sent to the administrator for review.');
        setRefundModalTx(null);
        loadDashboardData();
        return;
      }
      showToast(`🎉 Refund of ₹${Number(refund.amount).toFixed(2)} processed successfully! Course enrollment revoked.`);
      setRefundModalTx(null);

      setWalletRevision(revision => revision + 1);

      // Refresh transactions and enrolled courses in state
      paymentAPI.getMyTransactions().then((r) => {
        if (Array.isArray(r.data)) setTransactions(r.data);
      });
      studentAPI.getMyCourses().then((r) => {
        if (Array.isArray(r.data)) setEnrolledCourses(r.data);
      });
    } catch (err) {
      showToast(err.response?.data?.message || 'Refund request failed. Please contact support.', false);
    } finally {
      setIsRefunding(false);
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
    { name: 'Subscriptions', icon: 'autorenew', badge: activeSubscriptionsCount > 0 ? activeSubscriptionsCount : null },
    { name: 'Transactions', icon: 'receipt_long', badge: transactions.filter((t) => t.status !== 'created').length > 0 ? transactions.filter((t) => t.status !== 'created').length : null },
    { name: 'Wallet', icon: 'account_balance_wallet' },
    { name: 'Refunds', icon: 'assignment_return' },
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
          <div className="relative hidden sm:block min-w-0 flex-1 max-w-2xl mx-2 sm:mx-6">
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
            <kbd className="absolute right-3.5 top-1/2 -translate-y-1/2 bg-[#efe4dc] text-[#685b53] px-2 py-0.5 rounded text-[10px] font-bold">
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
            <span className="text-[11px] font-bold uppercase tracking-wider text-[#685b53] px-3 block">
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
              onClick={() => setActiveMenu('Subscriptions')}
              className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-[13.5px] font-semibold w-full transition-all cursor-pointer ${
                activeMenu === 'Subscriptions'
                  ? 'bg-[#fff0eb] text-[#ff5a36] font-bold'
                  : 'text-[#6d635c] hover:bg-[#fff7f4] hover:text-[#ff5a36]'
              }`}
            >
              <span className="material-symbols-outlined text-[20px]">autorenew</span>
              <span>Subscriptions</span>
            </button>
            <button
              type="button"
              onClick={() => {
                logout();
                if (onNavigateHome) onNavigateHome();
              }}
              className="flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-rose-600 hover:bg-rose-50 text-[13.5px] font-semibold w-full transition-colors cursor-pointer mt-1"
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
                    <p className="text-[12px] text-[#685b53]">
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
                    <p className="text-xs text-[#685b53]">
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
                    const pricing = pricingFor(c);

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
                              <span className="text-[#685b53] font-normal">({c.totalReviews || 12})</span>
                            </div>
                          </div>

                          <h4 className="font-extrabold text-[16px] text-[#1e1916] leading-snug group-hover:text-[#ff5a36] transition-colors">
                            {c.title}
                          </h4>
                          <p className="text-xs text-[#685b53] mt-2 line-clamp-2">
                            {c.description || 'Full interactive lecture track with assignments and certification.'}
                          </p>
                          <p className="text-xs font-semibold text-[#554a43] mt-2">
                            Instructor: {c.instructorName || 'Platform Instructor'}
                          </p>
                        </div>

                        <div className="flex items-center justify-between pt-4 mt-4 border-t border-[#f7ede6]">
                          <div className="flex items-baseline gap-2">
                            <span className="text-[17px] font-extrabold text-[#1e1916]">
                              {pricing.formatted}
                            </span>
                            <span className="text-xs text-[#a49992] line-through font-medium">
                              {pricing.formattedOrig}
                            </span>
                            <span className="text-[10px] font-extrabold bg-[#ffebe6] text-[#ff5a36] px-1.5 py-0.5 rounded-md border border-[#ffcfc2]">
                              {pricing.discount}
                            </span>
                          </div>

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
                              onClick={() => handleEnrollCourse(c.id, c.title, pricing.price)}
                              className="px-4 py-1.5 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer flex items-center gap-1.5 shadow-sm hover:shadow"
                            >
                              <span className="material-symbols-outlined text-[15px]">shopping_bag</span>
                              <span>Enroll • {pricing.formatted}</span>
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
                  <p className="text-xs text-[#685b53]">
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
                      <span className="text-xs font-bold text-[#685b53] uppercase tracking-wider mb-1">
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
                        <p className="text-xs text-[#685b53] italic">
                          No lectures published yet for this course.
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Verified AI Study Guide & Flashcards */}
                  {activeLecture && (
                    <StudentStudyGuideView
                      lessonId={activeLecture.id}
                      lessonTitle={activeLecture.title}
                    />
                  )}
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
                          <span className="text-xs text-[#685b53]">
                            {c.enrolledAt ? `Enrolled ${new Date(c.enrolledAt).toLocaleDateString()}` : 'Lifetime Access'}
                          </span>
                        </div>
                        <h3 className="font-extrabold text-[16px] text-[#1e1916] mt-2">
                          {c.title}
                        </h3>
                        <p className="text-xs text-[#685b53] mt-1">
                          Mentor: {c.instructorName || 'Platform Instructor'}
                        </p>
                      </div>

                      <div className="mt-4 pt-3 border-t border-[#f7ede6] flex items-center justify-between">
                        <div className="flex flex-col">
                          <span className="text-xs font-bold text-[#ff5a36]">Progress: 42%</span>

                        </div>
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
                  <p className="text-xs text-[#685b53] max-w-md">
                    Switch to the Overview tab or Catalog to explore courses and enroll. Verified certificate tracks with lifetime mentor access.
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
                <p className="text-xs text-[#685b53]">
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
                        <p className="text-xs text-[#685b53] mt-1 line-clamp-2">
                          {b.description || 'Full course curriculum with interactive exercises.'}
                        </p>
                      </div>

                      <div className="mt-4 pt-3 border-t border-[#f7ede6] flex items-center justify-between">
                        <div className="flex items-baseline gap-2">
                          <span className="text-[15px] font-extrabold text-[#1e1916]">
                            {pricingFor(b).formatted}
                          </span>
                          <span className="text-xs text-[#a49992] line-through font-medium">
                            {pricingFor(b).formattedOrig}
                          </span>
                        </div>
                        <button
                          onClick={() => handleEnrollCourse(b.courseId, b.title, pricingFor(b).price)}
                          className="px-4 py-1.5 rounded-full bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition-all cursor-pointer flex items-center gap-1 shadow-sm"
                        >
                          <span className="material-symbols-outlined text-[15px]">shopping_bag</span>
                          <span>Enroll • {pricingFor(b).formatted}</span>
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
                  <p className="text-xs text-[#685b53]">
                    Click the bookmark icon on any course preview modal to save it here.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* ========================================================
              TAB: COURSE SUBSCRIPTIONS & SAVED METHODS
              ======================================================== */}
          {activeMenu === 'Subscriptions' && (
            <StudentSubscriptions
              user={user}
              catalogCourses={catalogCourses.length > 0 ? catalogCourses : enrolledCourses}
            />
          )}

          {/* ========================================================
              TAB: PURCHASE TRANSACTIONS & GST INVOICES
              ======================================================== */}
          {activeMenu === 'Wallet' && <StudentWallet revision={walletRevision} />}
          {activeMenu === 'Transactions' && (
            <div className="min-w-0 max-w-full bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#faeae0] pb-4">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Purchase & Transaction History</h2>
                  <p className="text-xs text-[#685b53] mt-0.5">
                    Simulated transaction records, tax breakdowns, and enrollment receipts.
                  </p>
                </div>
                <div className="text-xs font-semibold px-3 py-1.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 self-start sm:self-auto flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
                  Verified Ledger
                </div>
              </div>

              {transactions.filter((t) => t.status && !['created', 'pending', 'initiated'].includes(t.status.toLowerCase())).length > 0 ? (
                <div className="max-w-full overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-[#faeae0] text-[#685b53] font-bold uppercase text-[11px]">
                        <th className="py-3 px-3">Invoice / Order</th>
                        <th className="py-3 px-3">Course</th>
                        <th className="py-3 px-3">Amount</th>
                        <th className="py-3 px-3">Payment Method</th>
                        <th className="py-3 px-3">Status</th>
                        <th className="py-3 px-3 text-right">Actions / Receipts</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#f7f0eb]">
                      {transactions
                        .filter((tx) => tx.status && !['created', 'pending', 'initiated'].includes(tx.status.toLowerCase()))
                        .map((tx) => (
                        <tr key={tx.id || tx.orderId} className="hover:bg-[#fbf7f4] transition">
                          <td className="py-3.5 px-3">
                            <span className="font-mono font-bold text-[#1f1a17] block">
                              {tx.invoiceNumber || tx.orderId}
                            </span>
                            <span className="text-[10px] text-[#685b53] font-mono">
                              Ref: {tx.orderId}
                            </span>
                          </td>
                          <td className="py-3.5 px-3 font-semibold text-[#1f1a17]">
                            {tx.courseTitle}
                          </td>
                          <td className="py-3.5 px-3 font-mono font-extrabold text-[#1f1a17]">
                            ₹{purchaseTotal(tx)}
                            {tx.discountAmount > 0 && (
                              <span className="block text-[10px] text-emerald-600 font-sans font-medium">
                                -₹{tx.discountAmount} promo
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-3">
                            <span className="px-2 py-0.5 rounded bg-[#f3eee9] text-[#5c4f47] font-medium text-[11px]">
                              {tx.paymentMethod || 'UPI / QR'}
                            </span>
                          </td>
                          <td className="py-3.5 px-3">
                            {tx.refundStatus === 'refunded' ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold text-[11px] bg-amber-50 text-amber-800 border border-amber-200">
                                <span className="w-1.5 h-1.5 rounded-full bg-amber-600"></span>
                                Refunded
                              </span>
                            ) : tx.status === 'success' ? (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold text-[11px] bg-emerald-100 text-emerald-800">
                                <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
                                Confirmed
                              </span>
                            ) : (
                              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-bold text-[11px] bg-neutral-100 text-neutral-700">
                                {tx.status || 'Pending'}
                              </span>
                            )}
                          </td>
                          <td className="py-3.5 px-3 text-right">
                            <div className="flex items-center justify-end gap-2 flex-wrap">


                              <button
                                onClick={() => handleReceipt(tx, true)}
                                className="inline-flex items-center gap-1 px-3 py-1.5 bg-[#1f1a17] hover:bg-black text-white rounded-xl text-xs font-bold transition shadow-xs"
                              >
                                <span>🧾</span> Print
                              </button>
                              <button
                                onClick={() => handleReceipt(tx, false)}
                                className="inline-flex items-center gap-1 px-3 py-1.5 bg-emerald-700 hover:bg-emerald-800 text-white rounded-xl text-xs font-bold transition shadow-xs"
                              >
                                <span>📥</span> Download
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="py-12 text-center text-xs text-[#685b53] space-y-2">
                  <div className="text-3xl">🧾</div>
                  <p className="font-bold text-[#1f1a17]">No purchase transactions yet</p>
                  <p>When you enroll in courses via UPI or Cards, your GST tax invoices will appear here.</p>
                </div>
              )}
            </div>
          )}

          {activeMenu === 'Refunds' && (
            <section className="min-w-0 bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] space-y-5" aria-labelledby="refund-center-title">
              <h2 id="refund-center-title" className="text-xl font-extrabold text-[#1f1a17]">Refunds</h2>
              <p className="text-sm text-[#685b53]">Manage refund requests for all your purchases here. Select a purchase below to provide a reason and confirm your request.</p>
              <button type="button" onClick={loadDashboardData} disabled={isApiLoading} className="px-4 py-2 rounded-xl border border-[#fae8dc] font-semibold">{isApiLoading ? 'Refreshing…' : 'Refresh purchases'}</button>
              {refundReviewError && <p role="alert" className="text-sm text-rose-700">{refundReviewError}</p>}
              {[
                { title: 'Available to request', description: 'Completed purchases without a refund. Eligibility is checked when you submit.', rows: transactions.filter(tx => tx.status === 'success' && (!tx.refundStatus || tx.refundStatus === 'none') && !refundReviews.some(review => review.orderId === tx.orderId)) },
                { title: 'Pending refunds', description: 'Requests awaiting processing.', rows: [...transactions.filter(tx => ['pending', 'processing'].includes(tx.refundStatus) && !refundReviews.some(r => r.orderId === tx.orderId)), ...refundReviews.filter(r => r.status === 'pending').map(review => ({...transactions.find(tx => tx.orderId === review.orderId), orderId: review.orderId, refundStatus: 'pending_review', review}))] },
                { title: 'Refund history', description: 'Completed refunds and other recorded outcomes.', rows: [...transactions.filter(tx => tx.refundStatus && !['none', 'pending', 'processing'].includes(tx.refundStatus) && !refundReviews.some(r => r.orderId === tx.orderId && r.status !== 'pending')), ...refundReviews.filter(r => r.status !== 'pending').map(review => ({...transactions.find(tx => tx.orderId === review.orderId), orderId: review.orderId, refundStatus: review.status, review}))] },
              ].map(group => (
                <details key={group.title} className="border border-[#fae8dc] rounded-2xl p-4 space-y-3" aria-label={group.title}>
                  <summary className="font-bold text-lg text-[#1f1a17] cursor-pointer">{group.title} <span className="text-sm text-[#685b53]">({group.rows.length})</span></summary>
                  <p className="text-xs text-[#685b53]">{group.description}</p>
                  {group.rows.length ? (
<div className="min-w-0 w-full">
                  <table className="w-full table-fixed text-left text-xs sm:text-sm [overflow-wrap:anywhere]">
                    <colgroup>{(group.title === 'Available to request' ? [35, 17, 25, 23] : [20, 11, 16, 20, 20, 13]).map((width, index) => <col key={index} style={{width: `${width}%`}} />)}</colgroup>
                    <thead><tr className="border-b border-[#fae8dc]">{(group.title === 'Available to request' ? ['Course / Order', 'Paid', 'Refund status', 'Action'] : ['Course / Order', 'Paid', 'Refund status', 'Your comment', 'Admin reply', 'Refunded amount']).map(label => <th key={label} scope="col" className="px-1 py-3 sm:px-2 align-top font-semibold">{label}</th>)}</tr></thead>
                    <tbody>
                    {group.rows.map(tx => {
                      const refunded = ['approved', 'refunded'].includes(tx.refundStatus) && tx.refundAmount != null ? Number(tx.refundAmount) : null;
                      const refundType = refunded == null ? null : Math.round(refunded * 100) === Math.round(Number(purchaseTotal(tx)) * 100) ? 'Full refund' : 'Partial refund';
                      return <tr key={tx.id || tx.orderId} className="border-b border-[#f7f0eb]">
                        <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top"><strong>{tx.courseTitle}</strong><span className="block text-xs break-all">{tx.orderId}</span></td>
                        <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top">₹{purchaseTotal(tx)}</td>
                        <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top"><span className={`inline-flex rounded-lg px-1 py-1 text-xs font-bold ${tx.refundStatus === 'rejected' ? 'bg-rose-50 text-rose-800' : ['approved','refunded'].includes(tx.refundStatus) ? 'bg-emerald-50 text-emerald-800' : 'bg-amber-50 text-amber-800'}`}>{tx.refundStatus === 'rejected' ? 'Request rejected' : ['approved', 'refunded'].includes(tx.refundStatus) ? 'Request approved' : tx.refundStatus === 'pending_review' ? 'Awaiting admin review' : !tx.refundStatus || tx.refundStatus === 'none' ? 'No refund requested' : tx.refundStatus}</span>
                        </td>
                        {group.title !== 'Available to request' && <>
                          <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top">{tx.review?.reason || tx.refundReason || 'No comment provided.'}</td>
                          <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top">{tx.review?.decisionRecord?.reason || (tx.refundStatus === 'pending_review' ? 'Awaiting admin review.' : tx.refundStatus === 'refunded' ? 'Automatically processed under the refund policy.' : 'No administrator reply.')}</td>
                          <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top">{refunded != null ? <><strong className="block">₹{refunded.toFixed(2)}</strong><span className="mt-1 inline-flex rounded-full bg-emerald-50 px-2 py-1 text-xs font-semibold text-emerald-800">{refundType}</span></> : <span>{tx.refundStatus === 'rejected' ? '₹0.00 — Not refunded' : 'Not processed'}</span>}</td>
                        </>}
                        {group.title === 'Available to request' && <td className="px-1 py-3 sm:px-2 whitespace-pre-wrap align-top"><button type="button" onClick={() => handleOpenRefundModal(tx)} className="w-full px-1 py-2 bg-rose-50 text-rose-700 rounded-xl font-bold">Request refund</button></td>}
                      </tr>;
                    })}</tbody>
                  </table>

                </div>
              ) : <p className="py-8 text-center text-[#685b53]">No entries in this section.</p>}
                </details>
              ))}
              <details className="rounded-xl bg-[#fbf7f4] p-4">
                <summary className="font-bold cursor-pointer">Refund policy and common questions</summary>
                <div className="text-sm text-[#685b53] mt-3 space-y-2">
                  <p>Standard refunds must be requested within 30 days. The server checks eligibility when you submit.</p>
                  <p>Processed refunds restore the original simulated payment sources, including wallet credit, and revoke course access. No real money moves.</p>
                  <p>Purchases older than 30 days are automatically sent to an administrator for review when you request a refund. Funds and course access remain unchanged until approval.</p>
                </div>
              </details>
            </section>
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
              <p className="text-xs text-[#685b53] max-w-md">
                Complete all course lectures and assignments to generate your official, cryptographically verified certificate signed by your instructors.
              </p>
              <div className="p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] max-w-sm w-full text-left flex items-center gap-3 mt-2">
                <span className="material-symbols-outlined text-emerald-600">verified</span>
                <div>
                  <h5 className="font-bold text-xs text-[#1e1916]">Web Development Fundamentals</h5>
                  <span className="text-[11px] text-[#685b53]">In progress — 85% completed</span>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================
              TAB 5: SETTINGS & STUDENT PROFILE (PUT /api/student/profile)
              ======================================================== */}
          {activeMenu === 'Settings' && (
            <div className="min-w-0 max-w-full bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="border-b border-[#faeae0] pb-4">
                <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Student Profile Settings</h2>
                <p className="text-xs text-[#685b53]">
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
                <span className="text-[12px] text-[#685b53] font-medium truncate max-w-[200px]">
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

      {/* Enterprise Payment Gateway Checkout Modal */}
      {paymentModalCourse && (
        <RazorpayPaymentModal
          isOpen={!!paymentModalCourse}
          course={paymentModalCourse}
          onClose={() => setPaymentModalCourse(null)}
          onSuccess={(txResult) => {
            const title = paymentModalCourse?.title;
            setPaymentModalCourse(null);
            showToast(`🎉 Payment verified! Enrolled in "${title}". Invoice: ${txResult?.invoiceNumber || 'INV-202611'}`);
            studentAPI.getMyCourses().then((cRes) => {
              if (Array.isArray(cRes.data)) setEnrolledCourses(cRes.data);
            });
            paymentAPI.getMyTransactions().then((tRes) => {
              if (Array.isArray(tRes.data)) setTransactions(tRes.data);
            });
          }}
        />
      )}

      {/* ========================================================
          REFUND CONFIRMATION & ENROLLMENT REVOCATION MODAL
          ======================================================== */}
      {refundModalTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl border border-rose-100 overflow-hidden flex flex-col">
            {/* Modal Header */}
            <div className="bg-gradient-to-r from-rose-50 via-rose-100/50 to-orange-50 p-6 border-b border-rose-100 flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="w-12 h-12 rounded-2xl bg-rose-500 text-white flex items-center justify-center text-2xl shadow-md shrink-0">
                  💸
                </div>
                <div>
                  <h3 className="font-extrabold text-[18px] text-[#1e1916]">
                    Request Course Refund
                  </h3>
                </div>
              </div>
              <button
                type="button"
                onClick={() => !isRefunding && setRefundModalTx(null)}
                disabled={isRefunding}
                className="text-neutral-400 hover:text-neutral-700 p-1 rounded-full cursor-pointer transition disabled:opacity-50"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 flex flex-col gap-4 text-xs">
              {/* Order & Course Summary */}
              <div className="p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-2">
                <div className="flex justify-between items-center">
                  <span className="text-[#685b53] font-medium">Course:</span>
                  <span className="font-bold text-[#1e1916] text-right truncate max-w-[220px]">
                    {refundModalTx.courseTitle}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-[#685b53] font-medium">Order Reference:</span>
                  <span className="font-mono font-bold text-[#1e1916]">
                    {refundModalTx.orderId}
                  </span>
                </div>
                <div className="flex justify-between items-center border-t border-[#ebd8cb] pt-2">
                  <span className="text-[#685b53] font-medium">Refund Amount:</span>
                  <span className="font-mono text-base font-extrabold text-emerald-700">
                    ₹{purchaseTotal(refundModalTx)}
                  </span>
                </div>
              </div>

              {/* Warning / Revocation Notice */}
              <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-900 flex items-start gap-2.5">
                <span className="text-lg shrink-0">⚠️</span>
                <p className="text-[11px] leading-relaxed">
                  <strong>Access Revocation Notice:</strong> Refunds within 30 days are processed if eligible. Older purchases automatically go to administrator review. Course access is revoked only when a refund is processed.
                </p>
              </div>

              {/* Refund Reason Selection */}
              <div className="flex flex-col gap-1.5">
                <label className="font-bold text-[#1e1916]">Reason for Refund *</label>
                <select
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  disabled={isRefunding}
                  className="w-full p-2.5 rounded-xl border border-[#e5dcd5] bg-white text-xs font-medium focus:outline-none focus:ring-2 focus:ring-rose-500"
                >
                  <option value="Purchased by mistake">Purchased by mistake</option>
                  <option value="Course content did not match expectations">Course content did not match expectations</option>
                  <option value="Encountered technical issues with video playback">Encountered technical issues with video playback</option>
                  <option value="Found an alternative course">Found an alternative course</option>
                  <option value="Other">Other reason (specify below)</option>
                </select>
              </div>

              {refundReason === 'Other' && (
                <div className="flex flex-col gap-1">
                  <label className="font-semibold text-[#685b53] text-[11px]">Specify Reason</label>
                  <textarea
                    rows={2}
                    value={customRefundReason}
                    onChange={(e) => setCustomRefundReason(e.target.value)}
                    placeholder="Tell us why you would like a refund..."
                    className="w-full p-2 rounded-xl border border-[#e5dcd5] text-xs focus:outline-none focus:ring-2 focus:ring-rose-500"
                  />
                </div>
              )}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="refund-comment" className="font-bold text-[#1e1916]">Additional comments (optional)</label>
                <textarea id="refund-comment" rows={3} maxLength={700} value={refundComment}
                  onChange={e => setRefundComment(e.target.value)} disabled={isRefunding}
                  placeholder="Share any details that will help us review your refund request."
                  className="w-full p-2.5 rounded-xl border border-[#e5dcd5] text-xs focus:outline-none focus:ring-2 focus:ring-rose-500" />
                <p className="text-xs text-[#685b53]">Your comment is included with your refund reason and is visible to the administrator. {refundComment.length}/700</p>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="p-4 bg-[#fbf7f4] border-t border-[#f0dfd3] flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setRefundModalTx(null)}
                disabled={isRefunding}
                className="px-4 py-2 rounded-xl border border-[#d6c7bc] text-[#5c4f47] hover:bg-[#f0dfd3] text-xs font-bold cursor-pointer transition disabled:opacity-50"
              >
                Cancel
              </button>
                  <button
                type="button"
                onClick={handleConfirmRefund}
                disabled={isRefunding}
                className="px-5 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-md cursor-pointer transition flex items-center gap-2 disabled:opacity-50"
              >
                {isRefunding ? (
                  <>
                    <span className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                    <span>Processing Refund...</span>
                  </>
                ) : (
                  <>
                    <span>💸</span>
                    <span>{`Submit refund request (₹${purchaseTotal(refundModalTx)})`}</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
