import { payoutStatusLabel } from '../utils/payoutStatus';
import PaymentPagination from './PaymentPagination';
import { downloadFinancialDocument } from '../utils/financialDocument';
import PaymentGovernance from './PaymentGovernance';
import PaymentPolicies from './PaymentPolicies';
import { openPaymentReceipt } from '../utils/paymentReceipt';
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api, { adminAPI } from '../services/api';

/**
 * Enterprise Admin Governance & Financial Management Portal
 * Implements:
 * - User directory and role management
 * - Instructor approval and credential review
 * - Course catalog review and publishing
 * - Platform financial ledger, transaction tracking, and GST tax invoices
 * - Exceptional refund approval/rejection outside automated policy
 * - Commercial rules, 20% commission policy, and promotional discount campaigns
 * - Instructor payout settlement and administrative dispute halting
 */
export default function AdminDashboard({ onNavigateHome }) {
  const { user, logout } = useAuth();
  const [financialPolicy, setFinancialPolicy] = useState(null);
  const [activeTab, setActiveTab] = useState('users'); // 'users' | 'instructors' | 'courses' | 'finance' | 'payouts' | 'commercial'
  useEffect(() => { api.get('/payments/policy').then(res => setFinancialPolicy(res.data)).catch(() => setFinancialPolicy(null)); }, [activeTab]);
  const [users, setUsers] = useState([]);
  const [pendingInstructors, setPendingInstructors] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [toastMessage, setToastMessage] = useState(null);

  // Financial Ledger & Payouts
  const [transactions, setTransactions] = useState([]);
  const [transactionPage, setTransactionPage] = useState(0); const [transactionPages, setTransactionPages] = useState(1);
  const [refundPage, setRefundPage] = useState(0); const [refundPages, setRefundPages] = useState(1);
  const [payoutPage, setPayoutPage] = useState(0); const [payoutPages, setPayoutPages] = useState(1);
  const [reviewEvidence, setReviewEvidence] = useState(null);
  useEffect(() => { if (['finance', 'payouts'].includes(activeTab)) loadFinancialData(); }, [transactionPage, refundPage, payoutPage]);
  const [refunds, setRefunds] = useState([]);
  const [exceptions, setExceptions] = useState([]);
  const [auditRows, setAuditRows] = useState([]);
  const [fxEdit, setFxEdit] = useState({currency:'USD',rate:'84.50'});
  const updateFx = async () => {
    try { await api.patch('/payments/currencies/rates', {...fxEdit,rate:Number(fxEdit.rate)}); showToast('Simulated FX rate updated.'); loadFinancialData(); }
    catch(e) { showToast(e.response?.data?.message || 'Rate update failed.'); }
  };
  const [decisionReasons, setDecisionReasons] = useState({});
  const [decidingId, setDecidingId] = useState(null);
  const [reconciliation, setReconciliation] = useState(null);
  const [instructorPayouts, setInstructorPayouts] = useState([]);
  const [settlingId, setSettlingId] = useState(null);

  // Exceptional Refund Modal State
  const [exceptionalRefundModalTx, setExceptionalRefundModalTx] = useState(null);
  const [exceptionalReason, setExceptionalReason] = useState('Admin discretionary refund override outside standard policy');
  const [isRefunding, setIsRefunding] = useState(false);

  // Commercial Rules & Coupons State
  const [coupons, setCoupons] = useState([]);
  const [couponModalOpen, setCouponModalOpen] = useState(false);
  const [creatingCoupon, setCreatingCoupon] = useState(false);
  const [newCoupon, setNewCoupon] = useState({
    code: '',
    discountPercentage: 20,
    maxDiscountAmount: 200,
    minOrderAmount: 0,
    usageLimit: 500,
    validUntil: '2027-12-31T23:59:59'
  });

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

    loadFinancialData();
  };

  const loadFinancialData = () => {
    api.get('/payments/governance/audit').then(r => setAuditRows(r.data)).catch(() => showToast('Audit history unavailable.'));
    adminAPI.getRefundExceptions().then(res => setExceptions(res.data)).catch(() => showToast('Could not load refund requests.'));
    adminAPI.getReconciliation().then(res => setReconciliation(res.data)).catch(() => showToast('Could not load reconciliation.'));
    api.get(`/payments/admin/transactions?page=${transactionPage}&size=50`)
      .then((res) => {
        if (res.data?.content) { setTransactions(res.data.content); setTransactionPages(res.data.totalPages); }
      })
      .catch(() => {});

    api.get(`/payments/refunds/page?page=${refundPage}&size=50`)
      .then((res) => {
        if (res.data?.content) { setRefunds(res.data.content); setRefundPages(res.data.totalPages); }
      })
      .catch(() => {});

    api.get(`/payments/instructor/earnings/page?page=${payoutPage}&size=50`)
      .then((res) => {
        if (res.data?.summary) { setInstructorPayouts(res.data.summary.sales); setPayoutPages(res.data.totalPages); }
      })
      .catch(() => {});

    loadCoupons();
  };

  const loadCoupons = () => {
    adminAPI
      .getAllCoupons()
      .then((res) => {
        if (Array.isArray(res.data)) setCoupons(res.data);
      })
      .catch(() => {});
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

  // Settle Instructor Payout
  const handleSettlePayout = async (earningId) => {
    setSettlingId(earningId);
    try {
      await adminAPI.settlePayout(earningId);
      showToast(`Payout ID #${earningId} settled successfully!`);
      loadFinancialData();
    } catch (err) {
      showToast('Failed to settle payout.');
    } finally {
      setSettlingId(null);
    }
  };

  // Halt instructor payout
  const handleHaltPayout = async (earningId) => {
    try {
      const reason = window.prompt('Reason for holding this payout:');
      if (!reason?.trim()) return;
      await adminAPI.haltPayout(earningId, reason);
      showToast(`Payout ID #${earningId} placed on administrative hold.`);
      loadFinancialData();
    } catch (err) {
      showToast('Failed to halt payout.');
    }
  };

  // Release hold on instructor payout
  const handleUnhaltPayout = async (earningId) => {
    try {
      const reason = window.prompt('Reason for releasing this payout hold:');
      if (!reason?.trim()) return;
      await adminAPI.unhaltPayout(earningId, reason);
      showToast(`Payout ID #${earningId} hold released back to pending.`);
      loadFinancialData();
    } catch (err) {
      showToast('Failed to release payout hold.');
    }
  };

  const decideException = async (id, decision) => {
    const reason = decisionReasons[id]?.trim();
    if (!reason) { showToast('Enter a decision reason.'); return; }
    setDecidingId(id);
    try { await adminAPI.decideRefundException(id, decision, reason); loadFinancialData(); showToast(`Refund request ${decision}.`); }
    catch (err) { showToast(err.response?.data?.message || 'Decision failed.'); }
    finally { setDecidingId(null); }
  };
  // Process exceptional refund
  const handleProcessExceptionalRefund = async () => {
    if (!exceptionalRefundModalTx) return;
    setIsRefunding(true);
    try {
      await adminAPI.processExceptionalRefund(
        exceptionalRefundModalTx.orderId,
        exceptionalReason
      );
      showToast(`Exceptional refund submitted for review for ${exceptionalRefundModalTx.orderId}!`);
      setExceptionalRefundModalTx(null);
      loadFinancialData();
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to process exceptional refund.');
    } finally {
      setIsRefunding(false);
    }
  };

  // Create new promo coupon
  const handleCreateCoupon = async (e) => {
    e.preventDefault();
    if (!newCoupon.code.trim()) return;
    setCreatingCoupon(true);
    try {
      await adminAPI.createCoupon({
        ...newCoupon,
        fixedDiscountAmount: newCoupon.fixedDiscountAmount ? Number(newCoupon.fixedDiscountAmount) : null,
        code: newCoupon.code.toUpperCase().trim()
      });
      showToast(`Promo campaign "${newCoupon.code.toUpperCase()}" created successfully!`);
      setCouponModalOpen(false);
      setNewCoupon({
        code: '',
        discountPercentage: 20,
        maxDiscountAmount: 200,
        minOrderAmount: 0,
        usageLimit: 500,
        validUntil: '2027-12-31T23:59:59'
      });
      loadCoupons();
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to create promo campaign.');
    } finally {
      setCreatingCoupon(false);
    }
  };

  // Toggle coupon status
  const handleToggleCoupon = async (couponId) => {
    try {
      await adminAPI.toggleCoupon(couponId);
      showToast('Coupon status updated.');
      loadCoupons();
    } catch (err) {
      showToast('Failed to toggle coupon status.');
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
        <aside className="w-full md:w-[230px] shrink-0 bg-white rounded-3xl p-5 border border-[#fae8dc] shadow-xs flex flex-col gap-2 self-start">
          <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] px-3 block mb-2">
            ADMIN CONTROLS
          </span>
          <button
            onClick={() => setActiveTab('users')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'users' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">group</span>
            <span>All Users ({users.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('instructors')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'instructors' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">verified_user</span>
            <span>Pending Instructors ({pendingInstructors.length})</span>
          </button>
          <button
            onClick={() => setActiveTab('courses')}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'courses' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px]">auto_stories</span>
            <span>Course Governance ({courses.length})</span>
          </button>

          <span className="text-[11px] font-bold uppercase tracking-wider text-[#9d8d85] px-3 block mt-3 mb-1">
            PAYMENT GATEWAY
          </span>
          <button
            onClick={() => { setActiveTab('finance'); loadFinancialData(); }}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'finance' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px] text-purple-600">receipt_long</span>
            <span>Financial Ledger</span>
          </button>
          <button
            onClick={() => { setActiveTab('payouts'); loadFinancialData(); }}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'payouts' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px] text-emerald-600">account_balance</span>
            <span>Instructor Payouts</span>
          </button>
          <button
            onClick={() => { setActiveTab('commercial'); loadCoupons(); }}
            className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-2xl text-xs font-bold transition-all text-left cursor-pointer ${
              activeTab === 'commercial' ? 'bg-purple-50 text-purple-700 font-extrabold' : 'text-[#6d635c] hover:bg-[#fbf7f4]'
            }`}
          >
            <span className="material-symbols-outlined text-[18px] text-amber-600">sell</span>
            <span>Commercial &amp; Coupons</span>
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
                  <tbody className="divide-y divide-[#f7f0eb]">
                    {users.map((u) => (
                      <tr key={u.id} className="hover:bg-[#fbf7f4] transition-colors">
                        <td className="py-3.5 px-3 font-mono text-[#8e8076]">#{u.id}</td>
                        <td className="py-3.5 px-3 font-bold text-[#1e1916]">{u.name}</td>
                        <td className="py-3.5 px-3 text-[#6d635c]">{u.email}</td>
                        <td className="py-3.5 px-3">
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${
                              u.role === 'admin'
                                ? 'bg-purple-100 text-purple-800'
                                : u.role === 'instructor'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-neutral-100 text-neutral-800'
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>
                        <td className="py-3.5 px-3">
                          <span
                            className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                              u.accountStatus === 'active'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-rose-100 text-rose-800'
                            }`}
                          >
                            {u.accountStatus}
                          </span>
                        </td>
                        <td className="py-3.5 px-3 text-right">
                          {u.role !== 'admin' && (
                            <button
                              onClick={() => handleToggleUserSuspend(u)}
                              className={`px-3 py-1 rounded-lg text-xs font-bold transition-colors cursor-pointer ${
                                u.accountStatus === 'suspended'
                                  ? 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                                  : 'bg-rose-50 text-rose-700 hover:bg-rose-100'
                              }`}
                            >
                              {u.accountStatus === 'suspended' ? 'Reactivate' : 'Suspend'}
                            </button>
                          )}
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
                <p className="text-xs text-[#8e8076]">Connected to GET /api/admin/instructors/pending</p>
              </div>

              {pendingInstructors.length > 0 ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {pendingInstructors.map((inst) => (
                    <div
                      key={inst.id}
                      className="p-5 rounded-2xl border border-[#faeae0] bg-[#fdfaf7] flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <span className="font-extrabold text-[15px] text-[#1e1916]">{inst.name}</span>
                          <span className="px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 text-[10px] font-bold">
                            PENDING REVIEW
                          </span>
                        </div>
                        <p className="text-xs text-[#6d635c] mb-2">{inst.email}</p>
                        {inst.qualifications && (
                          <p className="text-xs text-[#8e8076] mt-1">
                            <strong className="text-[#554a43]">Qualifications:</strong>{' '}
                            {inst.qualifications}
                          </p>
                        )}
                        {inst.expertiseAreas && (
                          <p className="text-xs text-[#8e8076] mt-1">
                            <strong className="text-[#554a43]">Expertise:</strong>{' '}
                            {inst.expertiseAreas}
                          </p>
                        )}
                      </div>

                      <div className="flex items-center gap-2 mt-4 pt-3 border-t border-[#f0dfd3]">
                        <button
                          onClick={() => handleApproveInstructor(inst.id)}
                          className="flex-1 py-1.5 rounded-xl bg-emerald-600 text-white font-bold text-xs hover:bg-emerald-700 transition-colors cursor-pointer"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => handleRejectInstructor(inst.id)}
                          className="flex-1 py-1.5 rounded-xl bg-rose-50 text-rose-700 font-bold text-xs hover:bg-rose-100 transition-colors cursor-pointer"
                        >
                          Reject
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-12 text-center text-xs text-[#8e8076] bg-[#fbf7f4] rounded-2xl border border-[#faeae0]">
                  <span className="material-symbols-outlined text-[36px] text-[#b0a39a] block mb-2">
                    how_to_reg
                  </span>
                  No pending instructor verification requests at this time.
                </div>
              )}
            </div>
          )}

          {/* TAB 3: Course Governance */}
          {activeTab === 'courses' && (
            <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-4">
              <div>
                <h2 className="text-[20px] font-extrabold text-[#1e1916]">Course Catalog Governance</h2>
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

          {/* TAB 4: Financial Ledger & Invoices */}
          {activeTab === 'finance' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#faeae0] pb-4">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Platform Financial Ledger</h2>
                  <p className="text-xs text-[#8e8076] mt-0.5">
                    Live system-wide payment transactions, GST tax invoices, and exceptional refund management.
                  </p>
                </div>
                <button
                  onClick={loadFinancialData}
                  className="px-3.5 py-1.5 rounded-full bg-purple-50 text-purple-700 border border-purple-200 text-xs font-bold hover:bg-purple-100 transition cursor-pointer flex items-center gap-1.5 self-start sm:self-auto"
                >
                  <span className="material-symbols-outlined text-[16px]">refresh</span>
                  Refresh Finance
                </button>
              </div>

              <section className="p-4 border rounded-2xl">
                <h3 className="font-bold">Exceptional refund review</h3>
                {exceptions.length === 0 && <p className="text-sm">No exceptional refund requests.</p>}
                {exceptions.map(r => <div key={r.eventId} className="border-t py-3 text-sm">
                  <strong>{r.orderId}</strong> · ₹{r.amount} · {r.status}<p>{r.reason}</p>
                  {r.status === 'pending' ? <div className="flex gap-2 flex-wrap">
                    <input aria-label={`Decision reason for ${r.orderId}`} placeholder="Mandatory decision reason" className="border rounded p-2" value={decisionReasons[r.eventId] || ''} onChange={e => setDecisionReasons({...decisionReasons,[r.eventId]:e.target.value})} />
                    <button disabled={decidingId !== null} onClick={() => decideException(r.eventId,'approved')} className="border rounded p-2">Approve refund</button>
                    <button disabled={decidingId !== null} onClick={() => decideException(r.eventId,'rejected')} className="border rounded p-2">Reject refund</button>
                  </div> : <p>Decision: {r.decisionRecord?.reason} · Admin #{r.decisionRecord?.actorId} · {r.decisionRecord?.recordedAt}</p>}
                </div>)}
              </section>
              <section className="p-4 border rounded-2xl">
                <h3 className="font-bold">Simulated gateway reconciliation: {reconciliation?.status || 'Loading'}</h3>
                {reconciliation?.discrepancyDetails?.map((d,i) => <p key={i} className="text-sm text-red-700">{d}</p>)}
              </section>
              <section className="p-4 border rounded-2xl">
                <h3 className="font-bold">Simulated exchange rates</h3>
                <p className="text-xs">INR per unit of foreign currency. Checkout remains in INR; display estimates refresh every 30 seconds.</p>
                <select aria-label="Rate currency" value={fxEdit.currency} onChange={e => setFxEdit({...fxEdit,currency:e.target.value})}>{['USD','EUR','GBP','AED','SGD'].map(c=><option key={c}>{c}</option>)}</select>
                <input aria-label="Simulated exchange rate" type="number" min="0.000001" step="0.000001" value={fxEdit.rate} onChange={e => setFxEdit({...fxEdit,rate:e.target.value})} className="border rounded p-2" />
                <button onClick={updateFx} className="border rounded p-2">Update demo rate</button>
              </section>
              <details className="p-4 border rounded-2xl"><summary>Financial decision audit ({auditRows.length})</summary>
                {auditRows.map(row => <p key={row.eventId} className="text-xs border-t py-2">{row.recordedAt} · {row.type} · {row.orderId || row.earningId || row.reference || row.currency} · {row.reason || row.status || row.rate} · Actor #{row.actorId || row.instructorId || row.requesterId}</p>)}
              </details>
              {/* Financial Metric Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="p-5 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-1">
                  <span className="text-xs font-bold text-[#8e8076] uppercase">Gross Platform Volume</span>
                  <span className="text-2xl font-black text-[#1f1a17]">
                    ₹{Number(reconciliation?.grossCapturedVolume || 0).toFixed(2)}
                  </span>
                  <span className="text-[11px] text-[#8e8076]">
                    {reconciliation?.successfulOrdersCount || 0} confirmed transactions
                  </span>
                </div>

                <div className="p-5 rounded-2xl bg-purple-50 border border-purple-200 flex flex-col gap-1">
                  <span className="text-xs font-bold text-purple-700 uppercase">Platform Revenue</span>
                  <span className="text-2xl font-black text-purple-800">
                    ₹{Number(reconciliation?.netPlatformRevenue || 0).toFixed(2)}
                  </span>
                  <span className="text-[11px] text-purple-600">Net platform retention</span>
                </div>

                <div className="p-5 rounded-2xl bg-emerald-50 border border-emerald-200 flex flex-col gap-1">
                  <span className="text-xs font-bold text-emerald-700 uppercase">Instructor Royalties</span>
                  <span className="text-2xl font-black text-emerald-800">
                    ₹{Number(reconciliation?.totalInstructorLiabilities || 0).toFixed(2)}
                  </span>
                  <span className="text-[11px] text-emerald-600">Total instructor pool</span>
                </div>

                <div className="p-5 rounded-2xl bg-amber-50 border border-amber-200 flex flex-col gap-1">
                  <span className="text-xs font-bold text-amber-700 uppercase">Total Refunds Processed</span>
                  <span className="text-2xl font-black text-amber-800">
                    ₹{Number(reconciliation?.totalRefundedAmount || 0).toFixed(2)}
                  </span>
                  <span className="text-[11px] text-amber-700">
                    {reconciliation?.refundedOrdersCount || 0} reversed transactions
                  </span>
                </div>
              </div>

              {/* Transactions Table */}
              <div>
                <h3 className="text-sm font-extrabold text-[#1f1a17] mb-3">All Student Orders</h3>
                <PaymentPagination page={transactionPage} totalPages={transactionPages} onPage={setTransactionPage} label="transactions" />
                {reviewEvidence && <section aria-label="Dispute evidence" className="border rounded p-4"><h3 className="font-bold">Dispute evidence</h3><button className="underline" onClick={() => setReviewEvidence(null)}>Close evidence</button><pre className="whitespace-pre-wrap break-all text-xs">{JSON.stringify(reviewEvidence, null, 2)}</pre></section>}
                <details className="border rounded p-4"><summary>Refund records</summary><PaymentPagination page={refundPage} totalPages={refundPages} onPage={setRefundPage} label="refunds" />{refunds.map(r => <p key={r.refundId}>{r.orderId} · ₹{Number(r.amount).toFixed(2)} · {r.status} · {r.refundId}</p>)}</details>
                {transactions.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase text-[11px]">
                          <th className="py-3 px-3">Order / Invoice</th>
                          <th className="py-3 px-3">Student</th>
                          <th className="py-3 px-3">Course</th>
                          <th className="py-3 px-3">Amount</th>
                          <th className="py-3 px-3">Method</th>
                          <th className="py-3 px-3">Status</th>
                          <th className="py-3 px-3 text-right">Actions / Receipts</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#f7f0eb]">
                        {transactions.map((tx) => (
                            <tr key={tx.id || tx.orderId} className="hover:bg-[#fbf7f4] transition">
                              <td className="py-3.5 px-3">
                                <span className="font-mono font-bold text-[#1f1a17] block">
                                  {tx.invoiceNumber || tx.orderId}
                                </span>
                                <span className="text-[10px] text-[#8e8076] font-mono">
                                  Ref: {tx.orderId}
                                </span>
                              </td>
                              <td className="py-3.5 px-3">
                                <span className="font-semibold text-[#1f1a17] block">
                                  {tx.studentName || 'Student'}
                                </span>
                                <span className="text-[11px] text-[#8e8076]">
                                  {tx.studentEmail}
                                </span>
                              </td>
                              <td className="py-3.5 px-3 font-semibold text-[#1f1a17]">
                                {tx.courseTitle}
                              </td>
                              <td className="py-3.5 px-3 font-mono font-extrabold text-[#1f1a17]">
                                ₹{tx.amount}
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
                                    {tx.status}
                                  </span>
                                )}
                              </td>
                              <td className="py-3.5 px-3 text-right">
                                <div className="flex items-center justify-end gap-1.5 flex-wrap">
                                  {tx.status === 'success' && tx.refundStatus !== 'refunded' && (
                                    <button
                                      type="button"
                                      onClick={() => {
                                        setExceptionalRefundModalTx(tx);
                                        setExceptionalReason('Admin discretionary refund override outside standard policy');
                                      }}
                                      className="px-2.5 py-1 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-lg text-[10px] font-bold transition cursor-pointer"
                                      title="Process exceptional refund outside automated policy"
                                    >
                                      💸 Refund
                                    </button>
                                  )}
                                  <button className="underline text-xs" onClick={() => api.get(`/payments/disputes/${encodeURIComponent(tx.orderId)}/evidence`).then(r => setReviewEvidence(r.data)).catch(() => showToast('Evidence unavailable.'))}>Dispute evidence</button>
                                  <button className="underline text-xs" onClick={() => downloadFinancialDocument(`/payments/invoices/${encodeURIComponent(tx.orderId)}/commission`, 'commission-invoice.html').catch(() => showToast('Commission invoice unavailable before settlement.'))}>Commission invoice</button>
                                  <button onClick={() => openPaymentReceipt(tx,true).catch(e => showToast(e.message || 'Receipt unavailable.'))} className="px-2.5 py-1 bg-[#1f1a17] text-white rounded-lg text-xs">Print</button>
                                  <button onClick={() => openPaymentReceipt(tx).catch(e => showToast(e.message || 'Receipt unavailable.'))} className="px-2.5 py-1 bg-emerald-600 text-white rounded-lg text-xs">Download</button>
                                </div>
                              </td>
                            </tr>
                          ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="py-10 text-center text-xs text-[#8e8076] bg-[#fbf7f4] rounded-2xl border border-[#faeae0]">
                    <span className="material-symbols-outlined text-[36px] text-[#b0a39a] block mb-2">receipt</span>
                    <p className="font-bold text-[#1f1a17]">No completed transactions recorded</p>
                  </div>
                )}
              </div>

              {/* Refunds Audit Trail */}
              {refunds.length > 0 && (
                <div className="pt-4 border-t border-[#faeae0]">
                  <h3 className="text-sm font-extrabold text-[#1f1a17] mb-3">Refund &amp; Reversal Audit Log</h3>
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase text-[11px]">
                          <th className="py-3 px-3">Refund Ref</th>
                          <th className="py-3 px-3">Order Ref</th>
                          <th className="py-3 px-3">Course</th>
                          <th className="py-3 px-3">Amount</th>
                          <th className="py-3 px-3">Reason</th>
                          <th className="py-3 px-3">Enrollment Revoked</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#f7f0eb]">
                        {refunds.map((r) => (
                          <tr key={r.refundId || r.paymentId} className="hover:bg-[#fbf7f4] transition">
                            <td className="py-3 px-3 font-mono font-bold text-rose-700">
                              {r.refundId}
                            </td>
                            <td className="py-3 px-3 font-mono text-[#1f1a17]">
                              {r.orderId}
                            </td>
                            <td className="py-3 px-3 font-semibold text-[#1f1a17]">
                              {r.courseTitle}
                            </td>
                            <td className="py-3 px-3 font-mono font-extrabold text-rose-700">
                              ₹{r.amount}
                            </td>
                            <td className="py-3 px-3 text-[#6d635c]">
                              {r.reason}
                            </td>
                            <td className="py-3 px-3">
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-100 text-rose-800">
                                Yes (Access Revoked)
                              </span>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* TAB 5: Instructor Royalty Governance */}
          {activeTab === 'payouts' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#faeae0] pb-4">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1e1916]">Instructor Royalty Governance</h2>
                  <p className="text-xs text-[#8e8076] mt-0.5">
                    Instructor revenue distribution ledger, administrative payout settlement, and dispute holds.
                  </p>
                </div>
                <button
                  onClick={loadFinancialData}
                  className="px-3.5 py-1.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-bold hover:bg-emerald-100 transition cursor-pointer flex items-center gap-1.5 self-start sm:self-auto"
                >
                  <span className="material-symbols-outlined text-[16px]">refresh</span>
                  Refresh Payouts
                </button>
              </div>

              <PaymentPagination page={payoutPage} totalPages={payoutPages} onPage={setPayoutPage} label="payouts" />
              {instructorPayouts.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase text-[11px]">
                        <th className="py-3 px-3">Earning ID</th>
                        <th className="py-3 px-3">Order Ref</th>
                        <th className="py-3 px-3">Course</th>
                        <th className="py-3 px-3">Gross Sale</th>
                        <th className="py-3 px-3">Platform Fee</th>
                        <th className="py-3 px-3">Instructor Share</th>
                        <th className="py-3 px-3">Payout Status</th>
                        <th className="py-3 px-3 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-[#f7f0eb]">
                      {instructorPayouts.map((p) => (
                        <tr key={p.id} className="hover:bg-[#fbf7f4] transition">
                          <td className="py-3.5 px-3 font-mono font-bold text-[#1f1a17]">
                            #{p.id}
                          </td>
                          <td className="py-3.5 px-3 font-mono text-[#8e8076]">
                            {p.orderId}
                          </td>
                          <td className="py-3.5 px-3 font-semibold text-[#1f1a17]">
                            {p.courseTitle}
                          </td>
                          <td className="py-3.5 px-3 font-mono text-[#1f1a17]">
                            ₹{p.grossAmount}
                          </td>
                          <td className="py-3.5 px-3 font-mono text-purple-700">
                            -₹{p.platformFee}
                          </td>
                          <td className="py-3.5 px-3 font-mono font-extrabold text-emerald-700">
                            ₹{p.netEarning}
                          </td>
                          <td className="py-3.5 px-3">
                            <span
                              className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                                p.payoutStatus === 'paid'
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : p.payoutStatus === 'halted'
                                  ? 'bg-rose-100 text-rose-800 border border-rose-300'
                                  : 'bg-amber-100 text-amber-800'
                              }`}
                            >
                              {payoutStatusLabel(p.payoutStatus)}
                            </span>
                          </td>
                          <td className="py-3.5 px-3 text-right">
                            {p.payoutStatus === 'paid' ? (
                              <span className="text-[11px] font-bold text-emerald-700 flex items-center justify-end gap-1">
                                <span className="material-symbols-outlined text-[15px]">check_circle</span>
                                Transferred
                              </span>
                            ) : p.payoutStatus === 'halted' ? (
                              <button
                                onClick={() => handleUnhaltPayout(p.id)}
                                className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-xl text-xs font-bold transition shadow-xs cursor-pointer inline-flex items-center gap-1"
                              >
                                <span>▶️ Release Hold</span>
                              </button>
                            ) : ['unpaid', 'processing', 'failed', 'delayed'].includes(p.payoutStatus) ? (
                              <div className="flex items-center justify-end gap-1.5 flex-wrap">
                                <button
                                  onClick={() => handleSettlePayout(p.id)}
                                  disabled={settlingId === p.id}
                                  className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition shadow-xs cursor-pointer inline-flex items-center gap-1"
                                >
                                  {settlingId === p.id ? 'Settling...' : 'Settle Payout'}
                                </button>
                                <button
                                  onClick={() => handleHaltPayout(p.id)}
                                  className="px-2.5 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-bold transition cursor-pointer"
                                  title="Halt payout due to dispute or policy review"
                                >
                                  <span>⏸️ Halt</span>
                                </button>
                              </div>
                            ) : <span>No payout action for this status</span>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="py-12 text-center text-xs text-[#8e8076] bg-[#fbf7f4] rounded-2xl border border-[#faeae0]">
                  <span className="material-symbols-outlined text-[36px] text-[#b0a39a] block mb-2">account_balance</span>
                  <p className="font-bold text-[#1f1a17]">No instructor royalty records found</p>
                  <p className="mt-1">When students purchase courses, instructor earnings will automatically accumulate here for settlement.</p>
                </div>
              )}
            </div>
          )}

          {/* TAB 6: Commercial Rules & Coupons */}
          {activeTab === 'commercial' && (
            <div className="bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#faeae0] pb-4">
                <div>
                  <h2 className="text-[20px] font-extrabold text-[#1f1a17]">Commercial Rules &amp; Promotional Campaigns</h2>
                  <p className="text-xs text-[#8e8076] mt-0.5">
                    Centrally manage platform pricing policies, commission percentages, and discount coupon campaigns.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setCouponModalOpen(true)}
                    className="px-4 py-1.5 rounded-full bg-amber-600 hover:bg-amber-700 text-white text-xs font-bold transition cursor-pointer flex items-center gap-1.5 shadow-xs"
                  >
                    <span className="material-symbols-outlined text-[16px]">add</span>
                    New Promo Campaign
                  </button>
                  <button
                    type="button"
                    onClick={loadCoupons}
                    className="px-3.5 py-1.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200 text-xs font-bold hover:bg-amber-100 transition cursor-pointer flex items-center gap-1"
                  >
                    <span className="material-symbols-outlined text-[16px]">refresh</span>
                    Refresh
                  </button>
                </div>
              </div>

              <PaymentPolicies user={user} methods={[]} onPolicyChange={setFinancialPolicy} />
              <PaymentGovernance user={user} />
              {/* Platform Commercial Rules Card */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <div className="p-5 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-1">
                  <span className="text-xs font-bold text-[#8e8076] uppercase">Platform Commission</span>
                  <span className="text-2xl font-black text-purple-800">{financialPolicy ? `${(Number(financialPolicy.commissionRate) * 100).toFixed(2)}%` : 'Unavailable'}</span>
                  <span className="text-[11px] text-[#8e8076]">Per-order policies preserve historical revenue shares</span>
                </div>

                <div className="p-5 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-1">
                  <span className="text-xs font-bold text-[#8e8076] uppercase">Base Course Price Floor</span>
                  <span className="text-2xl font-black text-[#1f1a17]">{financialPolicy ? `₹${Number(financialPolicy.defaultPrice).toFixed(2)}` : 'Unavailable'}</span>
                  <span className="text-[11px] text-[#8e8076]">Default catalog tier pricing</span>
                </div>

                <div className="p-5 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-1">
                  <span className="text-xs font-bold text-[#8e8076] uppercase">Money-Back Guarantee</span>
                  <span className="text-2xl font-black text-emerald-700">30 Days</span>
                  <span className="text-[11px] text-[#8e8076]">Automatic refund eligibility</span>
                </div>
              </div>

              {/* Active Promotional Discount Campaigns Table */}
              <div>
                <h3 className="text-sm font-extrabold text-[#1f1a17] mb-3">Promotional Discount Coupons</h3>
                {coupons.length > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse text-xs">
                      <thead>
                        <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase text-[11px]">
                          <th className="py-3 px-3">Promo Code</th>
                          <th className="py-3 px-3">Discount %</th>
                          <th className="py-3 px-3">Max Discount</th>
                          <th className="py-3 px-3">Usage</th>
                          <th className="py-3 px-3">Valid Until</th>
                          <th className="py-3 px-3">Status</th>
                          <th className="py-3 px-3 text-right">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#f7f0eb]">
                        {coupons.map((c) => (
                          <tr key={c.id} className="hover:bg-[#fbf7f4] transition">
                            <td className="py-3.5 px-3">
                              <span className="font-mono font-black text-[#1f1a17] bg-amber-50 text-amber-900 border border-amber-200 px-2 py-0.5 rounded-lg">
                                {c.code}
                              </span>
                            </td>
                            <td className="py-3.5 px-3 font-mono font-bold text-emerald-700">
                              {c.fixedDiscountAmount != null ? `₹${c.fixedDiscountAmount}` : `${c.discountPercentage}%`} OFF
                            </td>
                            <td className="py-3.5 px-3 font-mono text-[#1f1a17]">
                              ₹{c.maxDiscountAmount || 'No cap'}
                            </td>
                            <td className="py-3.5 px-3 text-[#6d635c]">
                              {c.timesUsed || 0} / {c.usageLimit || 1000}
                            </td>
                            <td className="py-3.5 px-3 text-[#8e8076]">
                              {c.validUntil ? new Date(c.validUntil).toLocaleDateString() : 'No expiry'}
                            </td>
                            <td className="py-3.5 px-3">
                              <span
                                className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                                  c.isActive
                                    ? 'bg-emerald-100 text-emerald-800'
                                    : 'bg-neutral-100 text-neutral-600'
                                }`}
                              >
                                {c.isActive ? 'Active' : 'Inactive'}
                              </span>
                            </td>
                            <td className="py-3.5 px-3 text-right">
                              <button
                                onClick={() => handleToggleCoupon(c.id)}
                                className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                                  c.isActive
                                    ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200'
                                    : 'bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200'
                                }`}
                              >
                                {c.isActive ? 'Deactivate' : 'Activate'}
                              </button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="py-12 text-center text-xs text-[#8e8076] bg-[#fbf7f4] rounded-2xl border border-[#faeae0]">
                    <span className="material-symbols-outlined text-[36px] text-[#b0a39a] block mb-2">sell</span>
                    <p className="font-bold text-[#1f1a17]">No promo campaigns configured yet</p>
                    <p className="mt-1">Click &quot;New Promo Campaign&quot; to configure a commercial discount policy.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </main>
      </div>

      {/* EXCEPTIONAL REFUND OVERRIDE MODAL */}
      {exceptionalRefundModalTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-fadeIn">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-[#fae8dc] flex flex-col gap-4 relative animate-scaleUp">
            <button
              onClick={() => setExceptionalRefundModalTx(null)}
              className="absolute top-5 right-5 w-8 h-8 rounded-full bg-[#f6eee9] hover:bg-[#fae8dc] text-[#554a43] flex items-center justify-center font-bold text-sm cursor-pointer transition"
            >
              ✕
            </button>

            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-rose-100 text-rose-700 flex items-center justify-center shadow-xs">
                <span className="material-symbols-outlined text-[26px]">assignment_late</span>
              </div>
              <div>
                <h3 className="text-lg font-black text-[#1e1916]">Exceptional Refund</h3>
                <p className="text-xs text-[#8e8076]">Administrative Discretionary Override</p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-[#fbf7f4] border border-[#f0dfd3] flex flex-col gap-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-[#8e8076]">Order Reference:</span>
                <span className="font-mono font-bold text-[#1f1a17]">{exceptionalRefundModalTx.orderId}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#8e8076]">Student Name:</span>
                <span className="font-bold text-[#1f1a17]">{exceptionalRefundModalTx.studentName}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-[#8e8076]">Course Title:</span>
                <span className="font-semibold text-[#1f1a17]">{exceptionalRefundModalTx.courseTitle}</span>
              </div>
              <div className="flex justify-between items-center pt-2 border-t border-[#f0dfd3]">
                <span className="text-rose-700 font-bold">Refund Amount to Reverse:</span>
                <span className="font-mono font-black text-rose-700 text-base">₹{exceptionalRefundModalTx.amount}</span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                Administrative Reason &amp; Audit Justification
              </label>
              <textarea
                rows={3}
                value={exceptionalReason}
                onChange={(e) => setExceptionalReason(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs focus:border-rose-500 focus:outline-none"
                placeholder="Enter justification for approving exceptional refund..."
                required
              />
            </div>

            <div className="text-[11px] text-rose-800 bg-rose-50 p-2.5 rounded-xl border border-rose-200">
              ⚠️ Submitting creates a pending request. An explicit approval or rejection with a reason is required in the review queue.
            </div>

            <div className="flex items-center gap-2 mt-1">
              <button
                type="button"
                onClick={() => setExceptionalRefundModalTx(null)}
                className="flex-1 py-2.5 rounded-xl border border-[#faeae0] text-xs font-bold text-[#6d635c] hover:bg-[#fbf7f4] transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={isRefunding}
                onClick={handleProcessExceptionalRefund}
                className="flex-1 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 disabled:opacity-50 text-white text-xs font-bold transition shadow-xs cursor-pointer"
              >
                {isRefunding ? 'Processing...' : 'Submit for Review'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* CREATE NEW PROMO CAMPAIGN MODAL */}
      {couponModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4 animate-fadeIn">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-[#fae8dc] flex flex-col gap-4 relative animate-scaleUp">
            <button
              onClick={() => setCouponModalOpen(false)}
              className="absolute top-5 right-5 w-8 h-8 rounded-full bg-[#f6eee9] hover:bg-[#fae8dc] text-[#554a43] flex items-center justify-center font-bold text-sm cursor-pointer transition"
            >
              ✕
            </button>

            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-800 flex items-center justify-center shadow-xs">
                <span className="material-symbols-outlined text-[26px]">sell</span>
              </div>
              <div>
                <h3 className="text-lg font-black text-[#1e1916]">New Promo Campaign</h3>
                <p className="text-xs text-[#8e8076]">Configure Commercial Discount Policy</p>
              </div>
            </div>

            <form onSubmit={handleCreateCoupon} className="flex flex-col gap-3">
              <div>
                <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                  Promo Coupon Code
                </label>
                <input
                  type="text"
                  placeholder="e.g. SUMMER50, DIWALI20"
                  value={newCoupon.code}
                  onChange={(e) => setNewCoupon({ ...newCoupon, code: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs font-mono font-bold uppercase focus:border-amber-600 focus:outline-none"
                  required
                />
              </div>

              <label className="block text-xs font-bold">Fixed discount (₹, optional)
                <input type="number" min="0.01" step="0.01" value={newCoupon.fixedDiscountAmount || ''} onChange={e => setNewCoupon({...newCoupon,fixedDiscountAmount:e.target.value})} className="w-full border rounded p-2" placeholder="Leave blank for percentage discount" />
              </label>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                    Discount % (unless fixed amount supplied)
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="100"
                    value={newCoupon.discountPercentage}
                    onChange={(e) => setNewCoupon({ ...newCoupon, discountPercentage: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs focus:border-amber-600 focus:outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                    Max Discount (₹)
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={newCoupon.maxDiscountAmount}
                    onChange={(e) => setNewCoupon({ ...newCoupon, maxDiscountAmount: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs focus:border-amber-600 focus:outline-none"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                    Usage Limit
                  </label>
                  <input
                    type="number"
                    min="1"
                    value={newCoupon.usageLimit}
                    onChange={(e) => setNewCoupon({ ...newCoupon, usageLimit: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs focus:border-amber-600 focus:outline-none"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-[#554a43] uppercase mb-1">
                    Min Order (₹)
                  </label>
                  <input
                    type="number"
                    min="0"
                    value={newCoupon.minOrderAmount}
                    onChange={(e) => setNewCoupon({ ...newCoupon, minOrderAmount: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] text-xs focus:border-amber-600 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex items-center gap-2 mt-2">
                <button
                  type="button"
                  onClick={() => setCouponModalOpen(false)}
                  className="flex-1 py-2.5 rounded-xl border border-[#faeae0] text-xs font-bold text-[#6d635c] hover:bg-[#fbf7f4] transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingCoupon}
                  className="flex-1 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 disabled:opacity-50 text-white text-xs font-bold transition shadow-xs cursor-pointer"
                >
                  {creatingCoupon ? 'Creating...' : 'Create Campaign'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
