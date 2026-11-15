import useModalAccessibility from '../hooks/useModalAccessibility';
import PaymentGovernance from './PaymentGovernance';
import { payoutStatusLabel } from '../utils/payoutStatus';
import PaymentPagination from './PaymentPagination';
import { downloadFinancialDocument } from '../utils/financialDocument';
import NotificationBell from './NotificationBell';
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api, { instructorAPI } from '../services/api';

export default function InstructorDashboard({ onNavigateHome }) {
  const { user, logout } = useAuth();
  const [courses, setCourses] = useState([]);
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [lectures, setLectures] = useState([]);
  const [loading, setLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);
  const [activeTab, setActiveTab] = useState('courses'); // 'courses' | 'create' | 'lectures' | 'profile'

  // New Course Form
  const [newCourse, setNewCourse] = useState({ title: '', description: '' });
  const [creatingCourse, setCreatingCourse] = useState(false);

  // New Lecture Form
  const [newLecture, setNewLecture] = useState({ title: '', youtubeUrl: '', orderIndex: 1 });
  const [addingLecture, setAddingLecture] = useState(false);

  // Instructor Profile
  const [profile, setProfile] = useState({ qualifications: '', expertiseAreas: '', bio: '', name: '' });
  const [savingProfile, setSavingProfile] = useState(false);

  // Instructor Earnings & Royalties (80/20 split)
  const [earningsData, setEarningsData] = useState(null);
  const [loadingEarnings, setLoadingEarnings] = useState(false);
  const [payoutModalOpen, setPayoutModalOpen] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [withdrawalSuccess, setWithdrawalSuccess] = useState(null);
  const [savedAccount, setSavedAccount] = useState(null);
  const [payoutQuote, setPayoutQuote] = useState(null);
  useEffect(() => { if (payoutModalOpen) { setPayoutQuote(null); api.get('/payments/instructor/payouts/quote').then(r => setPayoutQuote(r.data)).catch(() => showToast('Could not quote the payout. Please retry.')); } }, [payoutModalOpen]);
  const payoutDialogRef = useModalAccessibility(payoutModalOpen, () => { if (!isWithdrawing) setPayoutModalOpen(false); });
  const [payoutHistory, setPayoutHistory] = useState([]);
  const [earningsPage, setEarningsPage] = useState(0);
  const [earningsPages, setEarningsPages] = useState(1);
  useEffect(() => { loadEarnings(); }, [earningsPage]);

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const loadEarnings = () => {
    setLoadingEarnings(true);
    instructorAPI.getPayoutHistory().then(res => setPayoutHistory(res.data)).catch(() => showToast("Could not load payout history."));
    api.get(`/payments/instructor/earnings/page?page=${earningsPage}&size=50`)
      .then((res) => {
        if (res.data) { setEarningsData(res.data.summary); setEarningsPages(res.data.totalPages); }
      })
      .catch(() => {})
      .finally(() => setLoadingEarnings(false));
  };

  const handleWithdrawPayout = async () => {
    setIsWithdrawing(true);
    try {
      const res = await api.post('/payments/instructor/payouts/withdraw-saved', {});
      setWithdrawalSuccess(res.data);
      showToast(`Simulated payout of ₹${res.data.amountWithdrawn} settled.`);
      loadEarnings();
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to simulate payout withdrawal.');
    } finally {
      setIsWithdrawing(false);
    }
  };

  const loadCourses = () => {
    setLoading(true);
    instructorAPI
      .getCourses()
      .then((res) => {
        if (Array.isArray(res.data)) {
          setCourses(res.data);
          if (res.data.length > 0 && !selectedCourse) {
            setSelectedCourse(res.data[0]);
          }
        }
      })
      .catch((err) => {
        showToast(err.response?.data?.message || 'Failed to load instructor courses.');
      })
      .finally(() => setLoading(false));
  };

  const loadProfile = () => {
    instructorAPI
      .getProfile()
      .then((res) => {
        if (res.data) {
          setProfile({
            name: res.data.name || user?.name || '',
            qualifications: res.data.qualifications || '',
            expertiseAreas: res.data.expertiseAreas || '',
            bio: res.data.bio || ''
          });
        }
      })
      .catch(() => {});
  };

  useEffect(() => {
    loadCourses();
    loadProfile();
    loadEarnings();
  }, []);

  useEffect(() => {
    if (selectedCourse) {
      instructorAPI
        .getLectures(selectedCourse.id)
        .then((res) => {
          if (Array.isArray(res.data)) setLectures(res.data);
        })
        .catch(() => setLectures([]));
    }
  }, [selectedCourse]);

  const handleCreateCourse = async (e) => {
    e.preventDefault();
    if (!newCourse.title.trim()) return;

    setCreatingCourse(true);
    try {
      const res = await instructorAPI.createCourse(newCourse);
      setCreatingCourse(false);
      setNewCourse({ title: '', description: '' });
      showToast(`Course "${res.data.title}" created successfully!`);
      loadCourses();
      setActiveTab('courses');
    } catch (err) {
      setCreatingCourse(false);
      showToast(err.response?.data?.message || 'Failed to create course.');
    }
  };

  const handleToggleStatus = async (course) => {
    const nextStatus = course.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED';
    try {
      await instructorAPI.updateCourseStatus(course.id, { status: nextStatus });
      showToast(`Course status updated to ${nextStatus}.`);
      loadCourses();
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to update status.');
    }
  };

  const handleAddLecture = async (e) => {
    e.preventDefault();
    if (!selectedCourse) return;

    setAddingLecture(true);
    try {
      await instructorAPI.createLecture(selectedCourse.id, {
        title: newLecture.title.trim(),
        youtubeUrl: newLecture.youtubeUrl.trim(),
        orderIndex: Number(newLecture.orderIndex) || lectures.length + 1
      });
      setAddingLecture(false);
      setNewLecture({ title: '', youtubeUrl: '', orderIndex: lectures.length + 2 });
      showToast('Lecture created successfully!');
      // Refresh lectures
      instructorAPI.getLectures(selectedCourse.id).then((res) => {
        if (Array.isArray(res.data)) setLectures(res.data);
      });
    } catch (err) {
      setAddingLecture(false);
      showToast(err.response?.data?.message || 'Failed to add lecture (must be valid YouTube URL).');
    }
  };

  const handleDeleteLecture = async (lectureId) => {
    if (!selectedCourse) return;
    try {
      await instructorAPI.deleteLecture(selectedCourse.id, lectureId);
      showToast('Lecture deleted.');
      setLectures(lectures.filter((l) => l.id !== lectureId));
    } catch (err) {
      showToast('Failed to delete lecture.');
    }
  };

  const handleSaveProfile = async (e) => {
    e.preventDefault();
    setSavingProfile(true);
    try {
      const res = await instructorAPI.updateProfile(profile);
      setSavingProfile(false);
      setProfile({
        name: res.data.name || user?.name || '',
        qualifications: res.data.qualifications || '',
        expertiseAreas: res.data.expertiseAreas || '',
        bio: res.data.bio || ''
      });
      showToast('Instructor profile updated successfully!');
    } catch (err) {
      setSavingProfile(false);
      showToast(err.response?.data?.message || 'Failed to update profile.');
    }
  };

  return (
    <div className="min-h-screen bg-[#fcf6f2] font-sans text-[#262320] flex flex-col antialiased">
      {/* Header */}
      <header className="w-full bg-white border-b border-[#faeae0] sticky top-0 z-40 shadow-xs">
        <div className="max-w-[1560px] mx-auto px-4 sm:px-6 h-14 flex items-center justify-between">
          <button
            type="button"
            onClick={onNavigateHome}
            className="flex items-center gap-2.5 cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#0284c7] to-[#38bdf8] flex items-center justify-center text-white shadow-md">
              <span className="material-symbols-outlined text-[19px]">psychology</span>
            </div>
            <span className="text-[18px] font-black tracking-tight text-[#1e1916]">
              Instructor Portal
            </span>
          </button>

          <div className="flex items-center gap-3">
            <NotificationBell isAuthenticated={true} />
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
              Instructor: {user?.name || 'Dr. Instructor'}
            </span>
            <button
              type="button"
              onClick={() => { setActiveTab('earnings'); loadEarnings(); }}
              className={`px-3 py-1 rounded-full text-xs font-extrabold transition cursor-pointer flex items-center gap-1.5 shadow-xs ${
                (Number(earningsData?.unpaidBalance) || 0) > 0
                  ? 'bg-emerald-50 hover:bg-emerald-100 text-emerald-800 border border-emerald-300'
                  : 'bg-neutral-100 hover:bg-neutral-200 text-neutral-700 border border-neutral-300'
              }`}
              title="Click to view full instructor royalties ledger"
            >
              <span className={`w-2 h-2 rounded-full ${
                (Number(earningsData?.unpaidBalance) || 0) > 0 ? 'bg-emerald-500 animate-pulse' : 'bg-neutral-400'
              }`}></span>
              <span>
                Pending Royalties: ₹{(Number(earningsData?.unpaidBalance) || 0).toFixed(2)}
              </span>
            </button>
            <button
              onClick={onNavigateHome}
              className="px-3 py-1.5 rounded-full bg-surface-container text-xs font-bold text-on-surface hover:bg-surface-container-high transition-colors cursor-pointer"
            >
              Public Site
            </button>
            <button
              onClick={() => {
                logout();
                if (onNavigateHome) onNavigateHome();
              }}
              className="px-3 py-1.5 rounded-full text-xs font-bold text-rose-600 hover:bg-rose-50 transition-colors cursor-pointer"
            >
              Sign Out
            </button>
          </div>
        </div>
      </header>

      {/* Toast Notification */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-[#1e1916] text-white px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-3 border border-white/10 animate-slideUp">
          <span className="material-symbols-outlined text-blue-400 text-[20px]">info</span>
          <span className="text-xs font-semibold">{toastMessage}</span>
        </div>
      )}

      {/* Main Content */}
      <div className="w-full max-w-[1560px] mx-auto p-4 sm:p-6 lg:p-8 flex flex-col md:flex-row gap-6">
        {/* Navigation Sidebar */}
        <aside className="w-full md:w-[220px] shrink-0 bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col gap-2 self-start">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] px-3 block mb-2">
            INSTRUCTOR MENU
          </span>
          <button
            onClick={() => setActiveTab('courses')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'courses' ? 'bg-blue-50 text-blue-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">menu_book</span>
            <span>My Courses ({courses.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('create')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'create' ? 'bg-blue-50 text-blue-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">add_circle</span>
            <span>Create Course</span>
          </button>
          <button
            onClick={() => setActiveTab('lectures')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'lectures' ? 'bg-blue-50 text-blue-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">smart_display</span>
            <span>Manage Lectures</span>
          </button>
          <button
            onClick={() => setActiveTab('profile')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'profile' ? 'bg-blue-50 text-blue-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">account_box</span>
            <span>Instructor Profile</span>
          </button>
          <button
            onClick={() => { setActiveTab('earnings'); loadEarnings(); }}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'earnings' ? 'bg-emerald-50 text-emerald-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px] text-emerald-600">payments</span>
            <span>Earnings &amp; Royalties </span>
          </button>
        </aside>

        {/* Tab Content */}
        <main className="flex-1 min-w-0">
          {/* TAB 1: My Courses */}
          {activeTab === 'courses' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1e1916]">My Authored Courses</h2>
                  <p className="text-xs text-[#8e8076]">Connected to GET /api/instructor/courses</p>
                </div>
                <button
                  onClick={() => setActiveTab('create')}
                  className="px-4 py-2 rounded-xl bg-blue-600 text-white font-bold text-xs hover:bg-blue-700 transition-colors cursor-pointer flex items-center gap-1.5"
                >
                  <span className="material-symbols-outlined text-[18px]">add</span>
                  <span>New Course</span>
                </button>
              </div>

              {/* Instant 80% Royalty Summary Widget */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-2xl bg-gradient-to-r from-emerald-50 via-[#f0fdf4] to-blue-50 border border-emerald-200">
                <div className="flex items-center gap-3">
                  <div className={`w-10 h-10 rounded-xl flex items-center justify-center shadow-xs ${
                    (Number(earningsData?.unpaidBalance) || 0) > 0 ? 'bg-emerald-600 text-white' : 'bg-neutral-500 text-white'
                  }`}>
                    <span className="material-symbols-outlined text-[22px]">payments</span>
                  </div>
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-800">
                      Pending Net Royalty
                    </span>
                    <h3 className="text-xl font-black text-[#1e1916]">
                      ₹{(Number(earningsData?.unpaidBalance) || 0).toFixed(2)}
                    </h3>
                    <span className="text-[10px] text-[#8e8076] block">
                      Lifetime Earned: ₹{(Number(earningsData?.netTakeHomeEarnings) || 0).toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-blue-600 text-white flex items-center justify-center shadow-xs">
                    <span className="material-symbols-outlined text-[22px]">shopping_cart</span>
                  </div>
                  <div>
                    <span className="text-[10px] font-bold uppercase tracking-wider text-blue-800">Gross Student Sales</span>
                    <h3 className="text-xl font-black text-[#1e1916]">
                      ₹{(Number(earningsData?.lifetimeGrossRevenue) || 0).toFixed(2)}
                    </h3>
                    <span className="text-[10px] text-[#8e8076] block">
                      Platform Fee : -₹{(Number(earningsData?.platformCommissionDeducted) || 0).toFixed(2)}
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-start sm:justify-end gap-2 flex-wrap">
                  {(Number(earningsData?.unpaidBalance) || 0) > 0 ? (
                    <button
                      type="button"
                      onClick={() => { setPayoutModalOpen(true); setWithdrawalSuccess(null); }}
                      className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white text-xs font-bold transition flex items-center gap-1 shadow-xs cursor-pointer"
                    >
                      <span>⚡ Withdraw ₹{(Number(earningsData?.unpaidBalance) || 0).toFixed(2)}</span>
                    </button>
                  ) : (
                    <span className="px-3.5 py-2 rounded-xl bg-emerald-100 text-emerald-800 border border-emerald-300 text-xs font-bold flex items-center gap-1">
                      <span className="material-symbols-outlined text-[16px]">check_circle</span>
                      <span>No Royalties Available for Withdrawal</span>
                    </span>
                  )}
                  <button
                    type="button"
                    onClick={() => { setActiveTab('earnings'); loadEarnings(); }}
                    className="px-3.5 py-2 rounded-xl bg-white border border-emerald-300 hover:bg-emerald-50 text-emerald-800 text-xs font-bold transition flex items-center gap-1 shadow-xs cursor-pointer"
                  >
                    <span>Full Ledger</span>
                    <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                  </button>
                </div>
              </div>

              {courses.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {courses.map((c) => (
                    <div
                      key={c.id}
                      className="p-5 rounded-2xl border border-[#faeae0] bg-[#fdfaf7] flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold ${
                              c.status === 'PUBLISHED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {c.status}
                          </span>
                          <span className="text-[11px] text-[#8e8076]">ID #{c.id}</span>
                        </div>
                        <h4 className="font-extrabold text-[16px] text-[#1e1916]">{c.title}</h4>
                        <p className="text-xs text-[#8e8076] mt-1 line-clamp-2">{c.description}</p>
                      </div>

                      <div className="mt-4 pt-3 border-t border-[#f0dfd3] flex items-center justify-between">
                        <button
                          onClick={() => {
                            setSelectedCourse(c);
                            setActiveTab('lectures');
                          }}
                          className="text-xs font-bold text-blue-600 hover:underline cursor-pointer flex items-center gap-1"
                        >
                          <span className="material-symbols-outlined text-[16px]">video_library</span>
                          <span>Lectures</span>
                        </button>

                        <button
                          onClick={() => handleToggleStatus(c)}
                          className={`px-3 py-1 rounded-lg text-xs font-bold cursor-pointer transition-colors ${
                            c.status === 'PUBLISHED'
                              ? 'bg-amber-50 text-amber-700 hover:bg-amber-100'
                              : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                          }`}
                        >
                          {c.status === 'PUBLISHED' ? 'Set as Draft' : 'Publish Course'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12 text-[#8e8076]">
                  <p>You have not created any courses yet.</p>
                </div>
              )}
            </div>
          )}

          {/* TAB 2: Create Course */}
          {activeTab === 'create' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs max-w-2xl">
              <h2 className="text-[20px] font-extrabold text-[#1e1916] mb-1">Create New Course</h2>
              <p className="text-xs text-[#8e8076] mb-6">
                Connected to POST /api/instructor/courses. Courses start in DRAFT status.
              </p>

              <form onSubmit={handleCreateCourse} className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Course Title *
                  </label>
                  <input
                    type="text"
                    value={newCourse.title}
                    onChange={(e) => setNewCourse({ ...newCourse, title: e.target.value })}
                    placeholder="e.g. Full-Stack Spring Boot & React Microservices"
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Course Description
                  </label>
                  <textarea
                    rows={4}
                    value={newCourse.description}
                    onChange={(e) => setNewCourse({ ...newCourse, description: e.target.value })}
                    placeholder="Provide a comprehensive summary of what students will learn in this course..."
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  disabled={creatingCourse}
                  className="mt-2 py-3 px-6 bg-blue-600 text-white rounded-xl font-bold text-sm hover:bg-blue-700 transition-all self-start cursor-pointer shadow-md shadow-blue-500/20"
                >
                  {creatingCourse ? 'Creating Course...' : 'Create Course in Database'}
                </button>
              </form>
            </div>
          )}

          {/* TAB 3: Manage Lectures */}
          {activeTab === 'lectures' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1e1916]">Course Lectures</h2>
                  <p className="text-xs text-[#8e8076]">
                    Connected to /api/instructor/courses/&#123;courseId&#125;/lectures
                  </p>
                </div>

                {/* Course Selector Dropdown */}
                <div className="flex items-center gap-2">
                  <label className="text-xs font-bold text-[#6d635c]">Select Course:</label>
                  <select
                    value={selectedCourse?.id || ''}
                    onChange={(e) => {
                      const found = courses.find((c) => c.id === Number(e.target.value));
                      if (found) setSelectedCourse(found);
                    }}
                    className="p-2 rounded-xl border border-[#faeae0] text-xs font-bold bg-white"
                  >
                    {courses.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.title}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Add Lecture Form */}
              <form onSubmit={handleAddLecture} className="p-4 rounded-2xl bg-blue-50/50 border border-blue-100 flex flex-col gap-3">
                <span className="font-bold text-xs uppercase tracking-wider text-blue-800">
                  Add Video Lecture (YouTube URL)
                </span>
                <div className="grid grid-cols-1 sm:grid-cols-12 gap-3">
                  <div className="sm:col-span-6">
                    <input
                      type="text"
                      placeholder="Lecture Title (e.g. 01 - Introduction & Setup)"
                      value={newLecture.title}
                      onChange={(e) => setNewLecture({ ...newLecture, title: e.target.value })}
                      className="w-full p-2 rounded-xl border border-blue-200 bg-white text-xs focus:outline-none"
                      required
                    />
                  </div>
                  <div className="sm:col-span-4">
                    <input
                      type="url"
                      placeholder="https://www.youtube.com/watch?v=..."
                      value={newLecture.youtubeUrl}
                      onChange={(e) => setNewLecture({ ...newLecture, youtubeUrl: e.target.value })}
                      className="w-full p-2 rounded-xl border border-blue-200 bg-white text-xs focus:outline-none"
                      required
                    />
                  </div>
                  <div className="sm:col-span-2">
                    <button
                      type="submit"
                      disabled={addingLecture}
                      className="w-full py-2 px-3 bg-blue-600 text-white rounded-xl font-bold text-xs hover:bg-blue-700 transition-colors cursor-pointer"
                    >
                      {addingLecture ? 'Adding...' : 'Add Lecture'}
                    </button>
                  </div>
                </div>
              </form>

              {/* Existing Lectures List */}
              <div className="flex flex-col gap-2">
                <span className="text-xs font-bold text-[#8e8076] uppercase">
                  Current Lectures ({lectures.length})
                </span>
                {lectures.length > 0 ? (
                  lectures.map((lec, idx) => (
                    <div
                      key={lec.id}
                      className="p-3.5 rounded-xl border border-[#faeae0] bg-[#fbf7f4] flex items-center justify-between text-xs"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-6 h-6 rounded-full bg-white flex items-center justify-center font-bold text-[11px] text-[#1e1916]">
                          {idx + 1}
                        </span>
                        <div>
                          <h5 className="font-bold text-sm text-[#1e1916]">{lec.title}</h5>
                          <span className="text-[11px] text-[#8e8076] truncate max-w-sm block">
                            {lec.youtubeUrl} (Video ID: {lec.videoId || 'extracted'})
                          </span>
                        </div>
                      </div>
                      <button
                        onClick={() => handleDeleteLecture(lec.id)}
                        className="text-rose-600 hover:text-rose-800 text-xs font-bold cursor-pointer"
                      >
                        Delete
                      </button>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-[#8e8076] italic">
                    No lectures added to this course yet.
                  </p>
                )}
              </div>
            </div>
          )}

          {/* TAB 4: Instructor Profile */}
          {activeTab === 'profile' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs max-w-2xl">
              <h2 className="text-[20px] font-extrabold text-[#1e1916] mb-1">Instructor Profile</h2>
              <p className="text-xs text-[#8e8076] mb-6">
                Connected to PUT /api/instructor/profile
              </p>

              <form onSubmit={handleSaveProfile} className="flex flex-col gap-4">
                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Full Name
                  </label>
                  <input
                    type="text"
                    value={profile.name}
                    onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Qualifications &amp; Degrees
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Ph.D in Computer Science, Stanford University"
                    value={profile.qualifications}
                    onChange={(e) => setProfile({ ...profile, qualifications: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Expertise Areas
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Distributed Systems, Algorithms, React & Cloud"
                    value={profile.expertiseAreas}
                    onChange={(e) => setProfile({ ...profile, expertiseAreas: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase tracking-wider mb-1">
                    Instructor Bio
                  </label>
                  <textarea
                    rows={4}
                    value={profile.bio}
                    onChange={(e) => setProfile({ ...profile, bio: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-sm focus:border-blue-500 focus:outline-none"
                  />
                </div>

                <button
                  type="submit"
                  disabled={savingProfile}
                  className="mt-2 py-3 px-6 bg-blue-600 text-white rounded-xl font-bold text-sm hover:bg-blue-700 transition-all self-start cursor-pointer shadow-md shadow-blue-500/20"
                >
                  {savingProfile ? 'Saving...' : 'Save Profile Changes'}
                </button>
              </form>
            </div>
          )}

          {/* TAB 5: Earnings & Royalties (80/20 Split) */}
          {activeTab === 'earnings' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#faeae0] pb-4">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Instructor Earnings &amp; Royalties</h2>
                  <p className="text-xs text-[#8e8076] mt-0.5">
                    Revenue share calculated using each order’s financial policy linked to student course enrollments.
                  </p>
                </div>
                <div className="flex items-center gap-2 flex-wrap">
                  <button
                    type="button"
                    onClick={() => { setPayoutModalOpen(true); setWithdrawalSuccess(null); }}
                    className="px-4 py-1.5 rounded-full bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 text-white text-xs font-bold transition cursor-pointer flex items-center gap-1.5 shadow-xs"
                  >
                    <span>⚡ Simulate Bank Payout</span>
                  </button>
                  <button
                    onClick={loadEarnings}
                    className="px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold hover:bg-emerald-100 transition cursor-pointer flex items-center gap-1.5 self-start sm:self-auto"
                  >
                    <span className="material-symbols-outlined text-[16px]">refresh</span>
                    Refresh Ledger
                  </button>
                </div>
              </div>

              {/* Metric Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="p-5 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-1">
                  <span className="text-xs font-bold text-[#8e8076] uppercase">Gross Course Sales</span>
                  <span className="text-2xl font-black text-[#1f1a17]">
                    ₹{earningsData?.lifetimeGrossRevenue || 0}
                  </span>
                  <span className="text-[11px] text-[#8e8076]">Total student payments</span>
                </div>

                <div className="p-5 rounded-2xl bg-rose-50 border border-rose-200 flex flex-col gap-1">
                  <span className="text-xs font-bold text-rose-700 uppercase">Platform Fee </span>
                  <span className="text-2xl font-black text-rose-800">
                    -₹{earningsData?.platformCommissionDeducted || 0}
                  </span>
                  <span className="text-[11px] text-rose-600">Platform maintenance</span>
                </div>

                <div className="p-5 rounded-2xl bg-emerald-50 border border-emerald-200 flex flex-col gap-1">
                  <span className="text-xs font-bold text-emerald-700 uppercase">Royalties after refunds </span>
                  <span className="text-2xl font-black text-emerald-800">
                    ₹{(Number(earningsData?.netTakeHomeEarnings) || 0).toFixed(2)}
                  </span>
                  <span className="text-[11px] text-emerald-600">Total cumulative earnings</span>
                </div>

                <div className={`p-5 rounded-2xl flex flex-col gap-1 border ${
                  (Number(earningsData?.unpaidBalance) || 0) > 0
                    ? 'bg-amber-50 border-amber-200'
                    : 'bg-emerald-50 border-emerald-200'
                }`}>
                  <span className={`text-xs font-bold uppercase ${
                    (Number(earningsData?.unpaidBalance) || 0) > 0 ? 'text-amber-700' : 'text-emerald-700'
                  }`}>
                    Pending Net Royalty
                  </span>
                  <span className={`text-2xl font-black ${
                    (Number(earningsData?.unpaidBalance) || 0) > 0 ? 'text-amber-800' : 'text-emerald-800'
                  }`}>
                    ₹{(Number(earningsData?.unpaidBalance) || 0).toFixed(2)}
                  </span>
                  <span className={`text-[11px] ${
                    (Number(earningsData?.unpaidBalance) || 0) > 0 ? 'text-amber-700' : 'text-emerald-600 font-bold'
                  }`}>
                    {(Number(earningsData?.unpaidBalance) || 0) === 0 ? '₹0.00 royalty balance before adjustments' : 'Royalty balance before adjustments'}
                  </span>
                </div>
              </div>

              {/* Sales Ledger Table */}
              <div>
                <h3 className="text-sm font-extrabold text-[#1f1a17] mb-3">Itemized Course Sales</h3>
                <PaymentPagination page={earningsPage} totalPages={earningsPages} onPage={setEarningsPage} label="sales" />
                {earningsData?.sales && earningsData.sales.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase text-[11px]">
                          <th className="py-3 px-3">Order Ref</th>
                          <th className="py-3 px-3">Course</th>
                          <th className="py-3 px-3">Gross</th>
                          <th className="py-3 px-3">Platform Fee </th>
                          <th className="py-3 px-3">Your Net </th>
                          <th className="py-3 px-3">Status</th>
                          <th className="py-3 px-3">Date</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#f7f0eb]">
                        {earningsData.sales.map((sale) => (
                          <tr key={sale.id || sale.orderId} className="hover:bg-[#fbf7f4] transition">
                            <td className="py-3 px-3 font-mono font-bold text-[#1f1a17]">
                              {sale.orderId}
                            </td>
                            <td className="py-3 px-3 font-semibold text-[#1f1a17]">
                              {sale.courseTitle}
                            </td>
                            <td className="py-3 px-3 font-mono text-[#1f1a17]">
                              ₹{sale.grossAmount}
                            </td>
                            <td className="py-3 px-3 font-mono text-rose-600">
                              -₹{sale.platformFee}
                            </td>
                            <td className="py-3 px-3 font-mono font-extrabold text-emerald-700">
                              ₹{sale.netEarning}
                            </td>
                            <td className="py-3 px-3">
                              <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                                sale.payoutStatus === 'paid'
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : 'bg-amber-100 text-amber-800'
                              }`}>
                                {payoutStatusLabel(sale.payoutStatus)}
                              </span>
                            </td>
                            <td className="py-3 px-3 text-[#8e8076]">
                              {sale.soldAt ? new Date(sale.soldAt).toLocaleDateString() : 'Recent'}
                              <button className="block underline mt-2" onClick={() => downloadFinancialDocument(`/payments/invoices/${encodeURIComponent(sale.orderId)}/commission`, 'commission-invoice.html').catch(() => showToast('Commission invoice unavailable.'))}>Commission invoice</button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="py-12 text-center text-xs text-[#8e8076] bg-[#fbf7f4] rounded-2xl border border-[#faeae0]">
                    <span className="material-symbols-outlined text-[36px] text-[#b0a39a] block mb-2">payments</span>
                    <p className="font-bold text-[#1f1a17]">No course sales yet</p>
                    <p className="mt-1">When students enroll in your published courses, your royalty earnings will appear here.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* BANK PAYOUT SIMULATION MODAL */}
      {activeTab === 'earnings' && <section className="p-6 border rounded-2xl mx-6 my-4">
        <h3 className="font-bold">Simulated payout history</h3>
        {!payoutHistory.length && <p>No payouts recorded yet.</p>}
        {payoutHistory.map(p => <div key={p.eventId} className="border-t py-3 text-sm">
          <strong>₹{Number(p.amount).toFixed(2)}</strong> · {p.bankName} · {p.account}<br />
          <code>{p.reference}</code> · {p.recordedAt}
          <p>Gross royalty: ₹{Number(p.grossRoyalty ?? p.amount).toFixed(2)} · Refund debt: ₹{Number(p.refundDebtRecovered || 0).toFixed(2)} · Withheld: ₹{Number(p.withholdingAmount || 0).toFixed(2)}</p>
          <button className="underline" onClick={() => downloadFinancialDocument(`/payments/instructor/payouts/${encodeURIComponent(p.reference)}/statement`, 'payout-statement.html').catch(() => showToast('Statement unavailable.'))}>Download payout statement</button>
        </div>)}
      </section>}
      {payoutModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-fadeIn">
          <div ref={payoutDialogRef} role="dialog" aria-modal="true" aria-label="Instructor payout" tabIndex={-1} className="bg-white rounded-3xl max-w-md w-full max-h-[92vh] overflow-y-auto p-6 sm:p-8 shadow-2xl border border-[#fae8dc] flex flex-col gap-5 relative animate-scaleUp">
            <button
              onClick={() => setPayoutModalOpen(false)}
              className="absolute top-5 right-5 w-8 h-8 rounded-full bg-[#f6eee9] hover:bg-[#fae8dc] text-[#554a43] flex items-center justify-center font-bold text-sm cursor-pointer transition"
            >
              <span aria-hidden="true">✕</span><span className="sr-only">Close payout dialog</span>
            </button>

            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-emerald-600 to-teal-500 text-white flex items-center justify-center shadow-md">
                <span className="material-symbols-outlined text-[26px]">account_balance</span>
              </div>
              <div>
                <h3 className="text-lg font-black text-[#1e1916]">Direct Bank Payout Simulation</h3>
                <p className="text-xs text-[#8e8076]">Instant IMPS / NEFT Royalty Settlement</p>
              </div>
            </div>

            {!withdrawalSuccess ? (
              <div className="flex flex-col gap-4">
                <div className="p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-2.5 text-xs">
                  <div className="flex justify-between items-center border-b border-[#f0dfd3] pb-2">
                    <span className="text-[#8e8076] font-semibold">Beneficiary Name</span>
                    <span className="font-extrabold text-[#1f1a17]">{user?.name || 'Dr. Sarah Chen'}</span>
                  </div>
                  <PaymentGovernance user={user} onAccountChanged={setSavedAccount} />
                  <section aria-label="Payout quote" className="space-y-2">
                    <p>Gross royalty balance: ₹{Number(payoutQuote?.grossRoyalty || 0).toFixed(2)}</p>
                    <p>Outstanding refund debt: ₹{Number(payoutQuote?.outstandingRefundDebt || 0).toFixed(2)}</p>
                    <p>Refund debt offset in this payout: ₹{Number(payoutQuote?.refundDebtRecovered || 0).toFixed(2)}</p>
                    <p>Configured withholding: ₹{Number(payoutQuote?.withholdingAmount || 0).toFixed(2)}</p>
                    <p className="font-bold">Net simulated payout: ₹{Number(payoutQuote?.netPayout || 0).toFixed(2)}</p>
                  </section>
                  <div className="flex justify-between items-center pt-1">
                    <span className="text-emerald-800 font-bold uppercase text-[11px]">Net Accrued Royalty </span>
                    <span className="font-mono font-black text-emerald-700 text-[18px]">
                      ₹{earningsData?.unpaidBalance ?? '0.00'}
                    </span>
                  </div>
                </div>

                <div className="text-[11px] text-[#8e8076] bg-emerald-50 text-emerald-800 p-3 rounded-xl border border-emerald-200 flex items-start gap-2">
                  <span className="material-symbols-outlined text-[18px] text-emerald-600 shrink-0">verified_user</span>
                  <span>
                    This simulation verifies the banking payout pipeline. Confirming will settle all accrued royalties, generate a simulated transfer reference, and update your ledger to Paid Out.
                  </span>
                </div>

                <div className="flex items-center gap-2 mt-1">
                  <button
                    type="button"
                    onClick={() => setPayoutModalOpen(false)}
                    className="flex-1 py-3 rounded-xl border border-[#faeae0] text-xs font-bold text-[#6d635c] hover:bg-[#fbf7f4] transition cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="button"
                    disabled={isWithdrawing || !Number(earningsData?.unpaidBalance) || savedAccount?.status !== 'verified' || !payoutQuote}
                    onClick={handleWithdrawPayout}
                    className="flex-1 py-3 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-700 hover:to-teal-700 disabled:opacity-50 text-white text-xs font-black shadow-md shadow-emerald-500/20 transition cursor-pointer flex items-center justify-center gap-1.5"
                  >
                    <span>{isWithdrawing ? 'Processing Bank IMPS...' : '⚡ Confirm Transfer (Demo)'}</span>
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center gap-4 text-center py-2 animate-fadeIn">
                <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shadow-inner">
                  <span className="material-symbols-outlined text-[36px]">check_circle</span>
                </div>
                <div>
                  <h4 className="text-lg font-black text-[#1e1916]">Payout Transfer Successful!</h4>
                  <p className="text-xs text-emerald-700 font-semibold mt-1">
                    ₹{withdrawalSuccess.amountWithdrawn} settled in the simulator. No money was transferred.
                  </p>
                </div>

                <div className="w-full p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-2 text-left text-xs">
                  <div className="flex justify-between items-center">
                    <span className="text-[#8e8076]">Banking UTR Reference:</span>
                    <span className="font-mono font-bold text-[#1f1a17]">{withdrawalSuccess.utrNumber}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#8e8076]">Credited Account:</span>
                    <span className="font-bold text-[#1f1a17]">{withdrawalSuccess.bankName}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#8e8076]">Settled Sales Count:</span>
                    <span className="font-bold text-[#1f1a17]">{withdrawalSuccess.itemsSettled} courses</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#8e8076]">Settlement Timestamp:</span>
                    <span className="font-mono text-[#8e8076] text-[11px]">Just now (Real-time)</span>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => { setPayoutModalOpen(false); setWithdrawalSuccess(null); }}
                  className="w-full py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition shadow-xs cursor-pointer"
                >
                  Done &amp; View Updated Ledger
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
