import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { instructorAPI } from '../services/api';

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

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
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
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
              Instructor: {user?.name || 'Dr. Instructor'}
            </span>
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
        </main>
      </div>
    </div>
  );
}
