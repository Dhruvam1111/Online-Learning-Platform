import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminAPI } from '../services/api';

export default function AdminDashboard({ onNavigateHome }) {
  const { user, logout } = useAuth();
  const [activeTab, setActiveTab] = useState('users'); // 'users' | 'instructors' | 'courses'
  const [users, setUsers] = useState([]);
  const [pendingInstructors, setPendingInstructors] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3500);
  };

  const loadData = () => {
    setLoading(true);

    adminAPI
      .getUsers({ size: 20 })
      .then((res) => {
        if (res.data?.content) setUsers(res.data.content);
        else if (Array.isArray(res.data)) setUsers(res.data);
      })
      .catch(() => {});

    adminAPI
      .getPendingInstructors({ size: 20 })
      .then((res) => {
        if (res.data?.content) setPendingInstructors(res.data.content);
        else if (Array.isArray(res.data)) setPendingInstructors(res.data);
      })
      .catch(() => {});

    adminAPI
      .getCourses({ size: 20 })
      .then((res) => {
        if (res.data?.content) setCourses(res.data.content);
        else if (Array.isArray(res.data)) setCourses(res.data);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApproveInstructor = async (id) => {
    try {
      await adminAPI.approveInstructor(id);
      showToast('Instructor approved successfully!');
      loadData();
    } catch (err) {
      showToast('Failed to approve instructor.');
    }
  };

  const handleRejectInstructor = async (id) => {
    try {
      await adminAPI.rejectInstructor(id);
      showToast('Instructor application rejected.');
      loadData();
    } catch (err) {
      showToast('Failed to reject instructor.');
    }
  };

  const handleToggleUserSuspend = async (u) => {
    try {
      if (u.accountStatus === 'suspended') {
        await adminAPI.activateUser(u.id);
        showToast(`User ${u.name} reactivated.`);
      } else {
        await adminAPI.suspendUser(u.id);
        showToast(`User ${u.name} suspended.`);
      }
      loadData();
    } catch (err) {
      showToast('Failed to toggle user status.');
    }
  };

  const handleApproveCourse = async (id) => {
    try {
      await adminAPI.approveCourse(id);
      showToast('Course approved and published to catalog!');
      loadData();
    } catch (err) {
      showToast('Failed to approve course.');
    }
  };

  const handleUnpublishCourse = async (id) => {
    try {
      await adminAPI.unpublishCourse(id);
      showToast('Course unpublished (reverted to draft).');
      loadData();
    } catch (err) {
      showToast('Failed to unpublish course.');
    }
  };

  return (
    <div className="min-h-screen bg-[#fcf6f2] font-sans text-[#262320] flex flex-col antialiased">
      {/* Top Header */}
      <header className="w-full bg-white border-b border-[#faeae0] sticky top-0 z-40 shadow-xs">
        <div className="max-w-[1560px] mx-auto px-4 sm:px-6 h-14 flex items-center justify-between">
          <button
            type="button"
            onClick={onNavigateHome}
            className="flex items-center gap-2.5 cursor-pointer"
          >
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-purple-700 to-indigo-600 flex items-center justify-center text-white shadow-md">
              <span className="material-symbols-outlined text-[19px]">admin_panel_settings</span>
            </div>
            <span className="text-[18px] font-black tracking-tight text-[#1e1916]">
              Admin Governance Portal
            </span>
          </button>

          <div className="flex items-center gap-3">
            <span className="px-3 py-1 rounded-full text-xs font-bold bg-purple-50 text-purple-700 border border-purple-200">
              Admin: {user?.name || 'Platform Administrator'}
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
          <span className="material-symbols-outlined text-purple-400 text-[20px]">info</span>
          <span className="text-xs font-semibold">{toastMessage}</span>
        </div>
      )}

      {/* Main Layout */}
      <div className="w-full max-w-[1560px] mx-auto p-4 sm:p-6 lg:p-8 flex flex-col md:flex-row gap-6">
        {/* Sidebar */}
        <aside className="w-full md:w-[220px] shrink-0 bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col gap-2 self-start">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] px-3 block mb-2">
            ADMIN CONTROLS
          </span>
          <button
            onClick={() => setActiveTab('users')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'users' ? 'bg-purple-50 text-purple-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">group</span>
            <span>All Users ({users.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('instructors')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'instructors' ? 'bg-purple-50 text-purple-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">verified_user</span>
            <span>Pending Instructors ({pendingInstructors.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('courses')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'courses' ? 'bg-purple-50 text-purple-700' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">auto_stories</span>
            <span>Course Governance ({courses.length})</span>
          </button>
        </aside>

        {/* Content */}
        <main className="flex-1 min-w-0">
          {/* TAB 1: All Users */}
          {activeTab === 'users' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-4">
              <div>
                <h2 className="text-[20px] font-extrabold text-[#1e1916]">Users Directory</h2>
                <p className="text-xs text-[#8e8076]">Connected to GET /api/admin/users</p>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase tracking-wider">
                      <th className="py-3 px-3">ID</th>
                      <th className="py-3 px-3">Name</th>
                      <th className="py-3 px-3">Email</th>
                      <th className="py-3 px-3">Role</th>
                      <th className="py-3 px-3">Status</th>
                      <th className="py-3 px-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.id} className="border-b border-[#faeae0] hover:bg-[#fdfaf7]">
                        <td className="py-3 px-3 font-mono font-bold">{u.id}</td>
                        <td className="py-3 px-3 font-bold text-[#1e1916]">{u.name}</td>
                        <td className="py-3 px-3 text-[#6d635c]">{u.email}</td>
                        <td className="py-3 px-3">
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#fbf7f4] border border-[#f0dfd3]">
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3 px-3">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                              u.accountStatus === 'active'
                                ? 'bg-emerald-100 text-emerald-800'
                                : u.accountStatus === 'suspended'
                                ? 'bg-rose-100 text-rose-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {u.accountStatus}
                          </span>
                        </td>
                        <td className="py-3 px-3 text-right">
                          <button
                            onClick={() => handleToggleUserSuspend(u)}
                            className="text-xs font-bold text-purple-700 hover:underline cursor-pointer"
                          >
                            {u.accountStatus === 'suspended' ? 'Reactivate' : 'Suspend'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 2: Pending Instructors */}
          {activeTab === 'instructors' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-4">
              <div>
                <h2 className="text-[20px] font-extrabold text-[#1e1916]">
                  Pending Instructor Applications
                </h2>
                <p className="text-xs text-[#8e8076]">
                  Connected to GET /api/admin/instructors/pending
                </p>
              </div>

              {pendingInstructors.length > 0 ? (
                <div className="grid grid-cols-1 gap-3">
                  {pendingInstructors.map((p) => (
                    <div
                      key={p.id}
                      className="p-4 rounded-2xl bg-[#fdfaf7] border border-[#faeae0] flex items-center justify-between"
                    >
                      <div>
                        <h4 className="font-bold text-sm text-[#1e1916]">{p.name}</h4>
                        <span className="text-xs text-[#6d635c]">{p.email}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleApproveInstructor(p.id)}
                          className="px-3.5 py-1.5 rounded-xl bg-emerald-600 text-white font-bold text-xs hover:bg-emerald-700 cursor-pointer"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => handleRejectInstructor(p.id)}
                          className="px-3.5 py-1.5 rounded-xl bg-rose-50 text-rose-700 font-bold text-xs hover:bg-rose-100 cursor-pointer"
                        >
                          Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-10 text-[#8e8076]">
                  <span className="material-symbols-outlined text-[36px] text-emerald-600">
                    check_circle
                  </span>
                  <p className="font-bold mt-1">No pending instructor applications.</p>
                </div>
              )}
            </div>
          )}

          {/* TAB 3: Course Governance */}
          {activeTab === 'courses' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-4">
              <div>
                <h2 className="text-[20px] font-extrabold text-[#1e1916]">Course Directory &amp; Governance</h2>
                <p className="text-xs text-[#8e8076]">Connected to GET /api/admin/courses</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {courses.map((c) => (
                  <div
                    key={c.id}
                    className="p-5 rounded-2xl border border-[#faeae0] bg-[#fdfaf7] flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            c.status === 'PUBLISHED' || c.status === 'published'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800'
                          }`}
                        >
                          {c.status}
                        </span>
                        <span className="text-[11px] text-[#8e8076]">ID #{c.id}</span>
                      </div>
                      <h4 className="font-extrabold text-[15px] text-[#1e1916]">{c.title}</h4>
                      <p className="text-xs text-[#8e8076] mt-1 line-clamp-2">{c.description}</p>
                    </div>

                    <div className="mt-4 pt-3 border-t border-[#f0dfd3] flex items-center justify-between">
                      <span className="text-xs text-[#6d635c]">{c.instructorName || 'Instructor'}</span>
                      <div className="flex items-center gap-2">
                        {c.status === 'PUBLISHED' || c.status === 'published' ? (
                          <button
                            onClick={() => handleUnpublishCourse(c.id)}
                            className="text-xs font-bold text-amber-700 hover:underline cursor-pointer"
                          >
                            Unpublish
                          </button>
                        ) : (
                          <button
                            onClick={() => handleApproveCourse(c.id)}
                            className="px-3 py-1 rounded-lg bg-emerald-600 text-white text-xs font-bold hover:bg-emerald-700 cursor-pointer"
                          >
                            Approve
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
