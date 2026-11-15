import React, { useEffect, useState } from 'react';
import api, { subscriptionAPI, paymentSimulationAPI as sim, courseAPI } from '../services/api';

const DURATION_PLANS = [
  { months: 1, name: 'Monthly Plan', tag: 'Standard', desc: 'Billed monthly, flexible month-to-month access' },
  { months: 3, name: 'Quarterly Plan', tag: 'Popular', desc: 'Billed every 3 months, ideal for course completion' },
  { months: 6, name: 'Semester Plan', tag: 'Value', desc: 'Billed every 6 months with extended project support' },
  { months: 12, name: 'Annual Plan', tag: 'Best Value', desc: 'Full 12-month access with unlimited lecture revisits' }
];

export default function StudentSubscriptions({ user, catalogCourses = [] }) {
  const [subscriptions, setSubscriptions] = useState([]);
  const [courses, setCourses] = useState(catalogCourses);
  const [methods, setMethods] = useState([]);
  const [catalogPrices, setCatalogPrices] = useState({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [activeTab, setActiveTab] = useState('manage'); // 'manage' | 'new'

  // New Subscription Form State
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [selectedDuration, setSelectedDuration] = useState(1);
  const [selectedToken, setSelectedToken] = useState('');
  const [subscriptionConsent, setSubscriptionConsent] = useState(false);

  // New Saved Method Modal / State
  const [showAddMethodModal, setShowAddMethodModal] = useState(false);
  const [newMethodType, setNewMethodType] = useState('card');
  const [cardBrand, setCardBrand] = useState('Visa');
  const [cardLast4, setCardLast4] = useState('4242');
  const [cardExpiryMonth, setCardExpiryMonth] = useState('12');
  const [cardExpiryYear, setCardExpiryYear] = useState('28');
  const [cardholderName, setCardholderName] = useState(user?.name || 'Student Learner');
  const [upiId, setUpiId] = useState((user?.email ? user.email.split('@')[0] : 'student') + '@okhdfcbank');
  const [bankName, setBankName] = useState('HDFC Bank NetBanking');
  const [walletProvider, setWalletProvider] = useState('OLP Learner Wallet');
  const [methodConsent, setMethodConsent] = useState(false);

  // Renewal Review Quote Modal
  const [quote, setQuote] = useState(null);

  // Subscription Success Modal
  const [successDetails, setSuccessDetails] = useState(null);

  // Load subscriptions, courses, saved methods, and catalog pricing
  const refreshData = async () => {
    try {
      setLoading(true);
      const [subsRes, methodsRes, catalogRes] = await Promise.all([
        subscriptionAPI.getSubscriptions().catch(() => ({ data: [] })),
        sim.methods().catch(() => ({ data: [] })),
        api.get('/payments/policy/catalog').catch(() => ({ data: [] }))
      ]);

      setSubscriptions(subsRes.data || []);
      setMethods(methodsRes.data || []);

      const priceMap = {};
      (catalogRes.data || []).forEach(row => {
        if (row.courseId) priceMap[row.courseId] = Number(row.amount);
      });
      setCatalogPrices(priceMap);

      if (courses.length === 0) {
        const cRes = await courseAPI.getAllCourses().catch(() => ({ data: [] }));
        const list = Array.isArray(cRes.data) ? cRes.data : (cRes.data?.courses || []);
        setCourses(list);
      }
    } catch (err) {
      setMessage('Failed to load subscription data. Please refresh.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshData();
  }, [user?.id]);

  // Selected course object & pricing calculations
  const selectedCourse = courses.find(c => String(c.id || c.courseId) === String(selectedCourseId));
  const baseMonthlyPrice = selectedCourseId && catalogPrices[selectedCourseId] != null
    ? catalogPrices[selectedCourseId]
    : (selectedCourse?.price != null ? Number(selectedCourse.price) : 499.00);

  const selectedPlanObj = DURATION_PLANS.find(p => p.months === selectedDuration) || DURATION_PLANS[0];
  const totalPlanCost = baseMonthlyPrice * selectedDuration;

  // Selected saved method object
  const selectedMethodObj = methods.find(m => m.token === selectedToken);

  // Helper method formatting
  const formatMethodDisplay = (m) => {
    if (!m) return 'Unknown Method';
    if (m.displayName) return m.displayName;
    const type = (m.method || '').toLowerCase();
    const last4 = m.last4 || (m.token ? m.token.slice(-4) : '4242');
    if (type === 'card') return `${m.cardBrand || 'Card'} •••• ${last4} (Exp ${m.expiry || '12/28'})`;
    if (type === 'upi') return `UPI: ${m.upiId || ('student' + last4 + '@upi')}`;
    if (type === 'netbanking') return `${m.bankName || 'Net Banking'} (A/C •••• ${last4})`;
    if (type === 'wallet') return `${m.walletProvider || 'Digital Wallet'}`;
    return `${m.method} · ${last4}`;
  };

  const getMethodIcon = (type) => {
    switch ((type || '').toLowerCase()) {
      case 'card': return 'credit_card';
      case 'upi': return 'qr_code_2';
      case 'netbanking': return 'account_balance';
      case 'wallet': return 'account_balance_wallet';
      default: return 'payment';
    }
  };

  // Create Subscription Action
  const handleCreateSubscription = async (e) => {
    e.preventDefault();
    if (!selectedCourseId) {
      setMessage('Please select a course to subscribe to.');
      return;
    }
    if (!selectedToken) {
      setMessage('Please select or add a saved payment method.');
      return;
    }
    if (!subscriptionConsent) {
      setMessage('Please agree to the subscription terms and renewal approval consent.');
      return;
    }

    setBusy(true);
    setMessage('');
    try {
      const payload = {
        courseId: Number(selectedCourseId),
        token: selectedToken,
        consent: subscriptionConsent,
        durationMonths: selectedDuration,
        planName: selectedPlanObj.name,
        monthlyPrice: baseMonthlyPrice,
        totalAmount: totalPlanCost
      };

      const { data } = await subscriptionAPI.createSubscription(payload);

      setSuccessDetails({
        courseTitle: selectedCourse?.title || data.courseTitle || `Course #${selectedCourseId}`,
        planName: selectedPlanObj.name,
        durationMonths: selectedDuration,
        monthlyPrice: baseMonthlyPrice,
        totalPlanCost: totalPlanCost,
        nextRenewalAt: data.nextRenewalAt,
        paymentMethodDisplay: formatMethodDisplay(selectedMethodObj),
        userEmail: user?.email || 'your registered email',
        subscriptionId: data.id
      });

      // Reset form
      setSubscriptionConsent(false);
      setSelectedCourseId('');
      setSelectedToken('');
      setActiveTab('manage');
      await refreshData();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message || 'Failed to create subscription.');
    } finally {
      setBusy(false);
    }
  };

  // Add Saved Demo Method Action
  const handleSaveMethod = async (e) => {
    e.preventDefault();
    if (!methodConsent) {
      setMessage('Please check the consent box to save this payment method.');
      return;
    }

    setBusy(true);
    setMessage('');
    try {
      const details = {
        cardBrand,
        last4: cardLast4,
        expiryMonth: cardExpiryMonth,
        expiryYear: cardExpiryYear,
        cardholderName,
        upiId,
        bankName,
        walletProvider
      };

      const { data } = await sim.saveMethod(newMethodType, true, details);
      setShowAddMethodModal(false);
      setMethodConsent(false);
      setSelectedToken(data.token);
      setMessage(`Demo payment method (${formatMethodDisplay(data)}) saved successfully.`);
      await refreshData();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message || 'Could not save payment method.');
    } finally {
      setBusy(false);
    }
  };

  // Revoke Method Action
  const handleRevokeMethod = async (token) => {
    if (!window.confirm('Are you sure you want to revoke this saved payment method?')) return;
    setBusy(true);
    try {
      await sim.revokeMethod(token);
      if (selectedToken === token) setSelectedToken('');
      setMessage('Saved payment method revoked.');
      await refreshData();
    } catch (err) {
      setMessage(err.response?.data?.message || 'Could not revoke method.');
    } finally {
      setBusy(false);
    }
  };

  // Cancel Subscription Action
  const handleCancelSubscription = async (subId) => {
    if (!window.confirm('Are you sure you want to cancel this subscription? You will continue to have access until the end of the current period.')) return;
    setBusy(true);
    try {
      await subscriptionAPI.cancelSubscription(subId);
      setMessage('Subscription cancelled successfully.');
      setQuote(null);
      await refreshData();
    } catch (err) {
      setMessage(err.response?.data?.message || 'Failed to cancel subscription.');
    } finally {
      setBusy(false);
    }
  };

  // Review Renewal Action
  const handleReviewRenewal = async (subId) => {
    setBusy(true);
    try {
      const { data } = await subscriptionAPI.getQuote(subId);
      setQuote({ ...data, id: subId, requestId: crypto.randomUUID() });
    } catch (err) {
      setMessage(err.response?.data?.message || 'Unable to review renewal quote at this time.');
    } finally {
      setBusy(false);
    }
  };

  // Approve & Pay Renewal Action
  const handleApproveRenewal = async () => {
    if (!quote) return;
    setBusy(true);
    try {
      await subscriptionAPI.approveRenewal(quote.id, {
        approval: true,
        expectedAmount: quote.amount,
        requestId: quote.requestId
      });
      setMessage(`Renewal approved and paid! Next 30-day cycle is active.`);
      setQuote(null);
      await refreshData();
    } catch (err) {
      setMessage(err.response?.data?.message || err.message || 'Renewal approval failed.');
    } finally {
      setBusy(false);
    }
  };

  const activeSubscriptions = subscriptions.filter(s => s.status !== 'cancelled');
  const pastSubscriptions = subscriptions.filter(s => s.status === 'cancelled');

  return (
    <div className="min-w-0 max-w-full bg-white rounded-3xl p-6 sm:p-8 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
      {/* Header & Sub-Navigation */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#faeae0] pb-5">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[#ff5a36] text-[28px]">autorenew</span>
            <h2 className="text-[22px] font-extrabold text-[#1f1a17]">Course Subscriptions &amp; Saved Methods</h2>
          </div>
          <p className="text-xs text-[#685b53] mt-1">
            Manage your consent-based course subscription plans, renewal approvals, and recognizable payment methods.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-[#fcf6f2] p-1 rounded-2xl border border-[#fae8dc] self-start sm:self-auto">
          <button
            type="button"
            onClick={() => setActiveTab('manage')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'manage'
                ? 'bg-white text-[#ff5a36] shadow-xs'
                : 'text-[#685b53] hover:text-[#1f1a17]'
            }`}
          >
            My Subscriptions ({activeSubscriptions.length})
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('new')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'new'
                ? 'bg-[#ff5a36] text-white shadow-xs'
                : 'text-[#685b53] hover:text-[#ff5a36]'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">add_circle</span>
            <span>New Subscription</span>
          </button>
        </div>
      </div>

      {/* Global Status / Alert Banner */}
      {message && (
        <div
          role="status"
          aria-live="polite"
          className="p-3.5 rounded-2xl bg-[#fff7f4] border border-[#ffcfc2] text-xs font-semibold text-[#c73e1d] flex items-center justify-between gap-3 animate-fadeIn"
        >
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[18px]">info</span>
            <span>{message}</span>
          </div>
          <button
            type="button"
            onClick={() => setMessage('')}
            className="text-[#685b53] hover:text-[#1f1a17] text-xs font-bold"
          >
            ✕
          </button>
        </div>
      )}

      {/* ========================================================
          TAB 1: MANAGE EXISTING SUBSCRIPTIONS & SAVED METHODS
          ======================================================== */}
      {activeTab === 'manage' && (
        <div className="flex flex-col gap-7">
          {/* Active Subscriptions Section */}
          <div>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-extrabold text-[#1f1a17] flex items-center gap-2">
                <span>Active Subscriptions</span>
                <span className="px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-100 text-emerald-800">
                  {activeSubscriptions.length} Active
                </span>
              </h3>
            </div>

            {loading ? (
              <div className="py-12 text-center text-xs text-[#685b53]">Loading subscription records...</div>
            ) : activeSubscriptions.length === 0 ? (
              <div className="bg-[#fdfaf7] rounded-2xl p-8 border border-dashed border-[#fae8dc] text-center flex flex-col items-center justify-center gap-3">
                <span className="material-symbols-outlined text-[44px] text-[#ff5a36]/60">card_membership</span>
                <h4 className="font-bold text-sm text-[#1e1916]">No Active Subscriptions</h4>
                <p className="text-xs text-[#685b53] max-w-md">
                  You don't have any active course subscriptions. Pick any published course from our catalog to subscribe with flexible monthly renewal options.
                </p>
                <button
                  type="button"
                  onClick={() => setActiveTab('new')}
                  className="mt-2 px-5 py-2.5 rounded-xl bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition shadow-xs cursor-pointer"
                >
                  Explore &amp; Subscribe to a Course
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {activeSubscriptions.map((s) => {
                  const courseObj = courses.find(c => String(c.id || c.courseId) === String(s.courseId));
                  const courseTitle = s.courseTitle || courseObj?.title || `Course #${s.courseId}`;
                  const payable = s.payableAmount != null ? Number(s.payableAmount) : (s.monthlyPrice != null ? Number(s.monthlyPrice) : 499.00);
                  const renewalDate = new Date(s.nextRenewalAt);
                  const isDue = renewalDate <= new Date();

                  return (
                    <div
                      key={s.id}
                      className="rounded-2xl border border-[#fae8dc] bg-white p-5 flex flex-col justify-between gap-4 shadow-2xs hover:border-[#ff5a36]/40 transition"
                    >
                      <div className="flex flex-col gap-2.5">
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#ff5a36] block">
                              {courseObj?.category || 'Online Course'}
                            </span>
                            <h4 className="font-extrabold text-[15px] text-[#1e1916] leading-snug">
                              {courseTitle}
                            </h4>
                          </div>
                          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-extrabold bg-emerald-50 text-emerald-700 border border-emerald-200 shrink-0">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                            ACTIVE
                          </span>
                        </div>

                        {/* Plan & Renewal Details Grid */}
                        <div className="grid grid-cols-2 gap-2 p-3 rounded-xl bg-[#fcf9f6] border border-[#f5eae2] text-xs">
                          <div>
                            <span className="text-[10px] text-[#8c7e75] font-semibold block uppercase">Payable Rate</span>
                            <span className="font-extrabold text-[#1f1a17] text-[13px]">
                              ₹{payable.toFixed(2)}
                            </span>
                            <span className="text-[10px] text-[#685b53] block">/ month (tax incl.)</span>
                          </div>

                          <div>
                            <span className="text-[10px] text-[#8c7e75] font-semibold block uppercase">Next Renewal Date</span>
                            <span className="font-bold text-[#0284c7] text-[12px] block">
                              {renewalDate.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                            </span>
                            <span className="text-[10px] text-[#685b53] block">
                              {isDue ? 'Renewal Due Now' : 'Scheduled in advance'}
                            </span>
                          </div>

                          {s.planName && (
                            <div className="col-span-2 pt-2 border-t border-[#f0dfd3] flex items-center justify-between text-[11px]">
                              <span className="text-[#8c7e75]">Plan Term:</span>
                              <span className="font-bold text-[#1f1a17]">{s.planName}</span>
                            </div>
                          )}

                          {s.paymentMethodSummary && (
                            <div className="col-span-2 flex items-center justify-between text-[11px]">
                              <span className="text-[#8c7e75]">Payment Method:</span>
                              <span className="font-semibold text-[#1f1a17]">{s.paymentMethodSummary}</span>
                            </div>
                          )}
                        </div>

                        {/* Notice Banner if prepared */}
                        {s.notice && (
                          <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900 flex items-start gap-2">
                            <span className="material-symbols-outlined text-amber-600 text-[18px] shrink-0 mt-0.5">
                              notifications_active
                            </span>
                            <div>
                              <strong className="block font-bold">24-Hour Advance Renewal Notice</strong>
                              <p className="text-[11px] mt-0.5">{s.notice.message}</p>
                            </div>
                          </div>
                        )}
                      </div>

                      {/* Action Buttons */}
                      <div className="flex items-center justify-between pt-3 border-t border-[#f7ede6] gap-2">
                        <button
                          type="button"
                          disabled={busy || !s.notice || !isDue}
                          onClick={() => handleReviewRenewal(s.id)}
                          className="px-3.5 py-1.5 rounded-xl bg-[#ff5a36] text-white text-xs font-bold hover:bg-[#e5411d] transition disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer flex items-center gap-1 shadow-2xs"
                        >
                          <span className="material-symbols-outlined text-[16px]">verified</span>
                          <span>Review &amp; Approve Renewal</span>
                        </button>

                        <button
                          type="button"
                          disabled={busy}
                          onClick={() => handleCancelSubscription(s.id)}
                          className="text-xs font-bold text-rose-600 hover:text-rose-800 hover:underline cursor-pointer px-2 py-1"
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Past / Cancelled Subscriptions */}
          {pastSubscriptions.length > 0 && (
            <div className="border-t border-[#faeae0] pt-5">
              <h4 className="text-xs font-bold uppercase tracking-wider text-[#8c7e75] mb-3">
                Cancelled / Inactive Subscriptions ({pastSubscriptions.length})
              </h4>
              <div className="divide-y divide-[#f7f0eb] border border-[#fae8dc] rounded-2xl overflow-hidden bg-white text-xs">
                {pastSubscriptions.map((ps) => (
                  <div key={ps.id} className="p-3.5 flex flex-wrap items-center justify-between gap-3 bg-[#faf7f4]/40">
                    <div>
                      <span className="font-bold text-[#1f1a17] text-sm block">
                        {ps.courseTitle || `Course #${ps.courseId}`}
                      </span>
                      <span className="text-[11px] text-[#8c7e75]">Ref: {ps.id}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-gray-100 text-gray-600">
                        CANCELLED
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recognizable Saved Methods Management Section */}
          <div className="border-t border-[#faeae0] pt-6 flex flex-col gap-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <h3 className="text-base font-extrabold text-[#1f1a17] flex items-center gap-2">
                  <span className="material-symbols-outlined text-[#ff5a36] text-[20px]">credit_card</span>
                  <span>Recognizable Saved Payment Methods</span>
                </h3>
                <p className="text-xs text-[#685b53] mt-0.5">
                  Saved simulated tokens used for one-click renewals and course checkouts. No real card or bank data is stored.
                </p>
              </div>

              <button
                type="button"
                onClick={() => setShowAddMethodModal(true)}
                className="px-3.5 py-2 rounded-xl bg-[#fff0eb] text-[#ff5a36] hover:bg-[#ffe3d8] text-xs font-bold transition flex items-center gap-1.5 self-start sm:self-auto cursor-pointer"
              >
                <span className="material-symbols-outlined text-[16px]">add</span>
                <span>Add Demo Method</span>
              </button>
            </div>

            {methods.length === 0 ? (
              <div className="p-6 rounded-2xl bg-[#fdfaf7] border border-[#fae8dc] text-center text-xs text-[#685b53]">
                No payment methods currently saved. Click <strong>Add Demo Method</strong> to simulate a card, UPI ID, or bank account for instant renewals.
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {methods.map((m) => (
                  <div
                    key={m.token}
                    className="p-4 rounded-2xl border border-[#fae8dc] bg-[#fffefe] shadow-2xs flex flex-col justify-between gap-3 hover:border-[#ff5a36]/30 transition"
                  >
                    <div className="flex items-start gap-3">
                      <div className="w-10 h-10 rounded-xl bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-[20px]">
                          {getMethodIcon(m.method)}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="text-[10px] font-extrabold uppercase text-[#ff5a36]">
                            {m.cardBrand || m.method}
                          </span>
                          <span className="text-[10px] font-mono text-[#8c7e75]">
                            •••• {m.last4 || m.token.slice(-4)}
                          </span>
                        </div>
                        <h5 className="font-bold text-xs text-[#1f1a17] truncate mt-0.5">
                          {formatMethodDisplay(m)}
                        </h5>
                        <p className="text-[11px] text-[#685b53] mt-0.5">
                          {m.cardholderName || user?.name || 'Verified Demo Account'}
                        </p>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-[#f7ede6] flex items-center justify-between text-[11px]">
                      <span className="text-emerald-700 font-semibold flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                        Active &amp; Ready
                      </span>
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => handleRevokeMethod(m.token)}
                        className="text-rose-600 hover:text-rose-800 font-bold hover:underline cursor-pointer"
                      >
                        Revoke
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ========================================================
          TAB 2: CREATE NEW SUBSCRIPTION WITH COURSE SELECTION & TERMS
          ======================================================== */}
      {activeTab === 'new' && (
        <form onSubmit={handleCreateSubscription} className="flex flex-col gap-6 max-w-4xl">
          {/* Step 1: Course Selection (No Manual Course ID!) */}
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-[#554a43] uppercase tracking-wider flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-[#ff5a36] text-white flex items-center justify-center text-[10px]">1</span>
                <span>Select Course to Subscribe</span>
              </label>
              <span className="text-[11px] text-[#685b53]">
                Choose from available published catalog courses
              </span>
            </div>

            <div className="relative">
              <select
                value={selectedCourseId}
                onChange={(e) => setSelectedCourseId(e.target.value)}
                required
                className="w-full p-3.5 pr-10 rounded-2xl border border-[#fae8dc] text-sm focus:border-[#ff5a36] focus:outline-none bg-white text-[#1f1a17] font-medium appearance-none cursor-pointer"
              >
                <option value="">-- Choose a course from catalog --</option>
                {courses.map((c) => {
                  const id = c.id || c.courseId;
                  const price = catalogPrices[id] != null ? catalogPrices[id] : (c.price != null ? Number(c.price) : 499.00);
                  return (
                    <option key={id} value={id}>
                      {c.title} • ₹{price.toFixed(2)}/mo • {c.category || 'General'}
                    </option>
                  );
                })}
              </select>
              <span className="material-symbols-outlined absolute right-3.5 top-3.5 text-[#8c7e75] pointer-events-none text-[20px]">
                arrow_drop_down
              </span>
            </div>

            {selectedCourse && (
              <div className="p-4 rounded-2xl bg-[#fffaf6] border border-[#ffcfc2] flex flex-col sm:flex-row sm:items-center justify-between gap-3 animate-fadeIn">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-[#ff5a36]/10 text-[#ff5a36] flex items-center justify-center font-bold shrink-0">
                    <span className="material-symbols-outlined text-[24px]">school</span>
                  </div>
                  <div>
                    <h4 className="font-extrabold text-sm text-[#1e1916]">{selectedCourse.title}</h4>
                    <span className="text-xs text-[#685b53]">{selectedCourse.instructorName || 'Expert Faculty'} • {selectedCourse.level || 'All Levels'}</span>
                  </div>
                </div>
                <div className="text-right">
                  <span className="text-[10px] text-[#8c7e75] uppercase font-bold block">Base Monthly Rate</span>
                  <span className="text-base font-extrabold text-[#ff5a36]">₹{baseMonthlyPrice.toFixed(2)}</span>
                  <span className="text-[10px] text-[#685b53] block">/ month (GST included)</span>
                </div>
              </div>
            )}
          </div>

          {/* Step 2: Duration & Billing Plan Selection */}
          <div className="flex flex-col gap-3">
            <label className="text-xs font-bold text-[#554a43] uppercase tracking-wider flex items-center gap-1.5">
              <span className="w-5 h-5 rounded-full bg-[#ff5a36] text-white flex items-center justify-center text-[10px]">2</span>
              <span>Select Billing Duration &amp; Plan</span>
            </label>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {DURATION_PLANS.map((plan) => {
                const isSelected = selectedDuration === plan.months;
                const cost = baseMonthlyPrice * plan.months;
                return (
                  <div
                    key={plan.months}
                    onClick={() => setSelectedDuration(plan.months)}
                    className={`p-4 rounded-2xl border-2 transition-all cursor-pointer flex flex-col justify-between gap-3 ${
                      isSelected
                        ? 'border-[#ff5a36] bg-[#fffbf9] shadow-xs'
                        : 'border-[#fae8dc] bg-white hover:border-[#ff5a36]/40'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between mb-1.5">
                        <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase ${
                          isSelected ? 'bg-[#ff5a36] text-white' : 'bg-[#f4ece6] text-[#7a6b61]'
                        }`}>
                          {plan.tag}
                        </span>
                        {isSelected && (
                          <span className="material-symbols-outlined text-[#ff5a36] text-[18px]">
                            check_circle
                          </span>
                        )}
                      </div>
                      <h4 className="font-extrabold text-sm text-[#1e1916]">{plan.name}</h4>
                      <p className="text-[11px] text-[#685b53] mt-1 leading-snug">{plan.desc}</p>
                    </div>

                    <div className="pt-2 border-t border-[#f7ede6]">
                      <span className="text-[10px] text-[#8c7e75] block">Total for {plan.months} Month{plan.months > 1 ? 's' : ''}:</span>
                      <span className="text-sm font-extrabold text-[#1e1916]">
                        ₹{cost.toFixed(2)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Step 3: Recognizable Saved Method Selection */}
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-[#554a43] uppercase tracking-wider flex items-center gap-1.5">
                <span className="w-5 h-5 rounded-full bg-[#ff5a36] text-white flex items-center justify-center text-[10px]">3</span>
                <span>Select Saved Payment Method</span>
              </label>

              <button
                type="button"
                onClick={() => setShowAddMethodModal(true)}
                className="text-xs font-bold text-[#ff5a36] hover:underline flex items-center gap-1 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[15px]">add</span>
                <span>Add Demo Method</span>
              </button>
            </div>

            {methods.length === 0 ? (
              <div className="p-4 rounded-2xl bg-[#fff7f4] border border-[#ffcfc2] flex flex-col sm:flex-row sm:items-center justify-between gap-3 text-xs">
                <span className="text-[#685b53]">
                  No saved payment methods found. Save a demo card or UPI account to proceed with subscriptions.
                </span>
                <button
                  type="button"
                  onClick={() => setShowAddMethodModal(true)}
                  className="px-4 py-2 rounded-xl bg-[#ff5a36] text-white font-bold shrink-0 hover:bg-[#e5411d] transition cursor-pointer"
                >
                  Add Method Now
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {methods.map((m) => {
                  const isSelected = selectedToken === m.token;
                  return (
                    <div
                      key={m.token}
                      onClick={() => setSelectedToken(m.token)}
                      className={`p-3.5 rounded-2xl border-2 transition-all cursor-pointer flex items-center gap-3 ${
                        isSelected
                          ? 'border-[#ff5a36] bg-[#fffaf6] shadow-2xs'
                          : 'border-[#fae8dc] bg-white hover:border-[#ff5a36]/40'
                      }`}
                    >
                      <div className="w-9 h-9 rounded-xl bg-[#fff0eb] text-[#ff5a36] flex items-center justify-center shrink-0">
                        <span className="material-symbols-outlined text-[20px]">
                          {getMethodIcon(m.method)}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-xs text-[#1e1916] truncate">
                            {formatMethodDisplay(m)}
                          </span>
                          {isSelected && (
                            <span className="material-symbols-outlined text-[#ff5a36] text-[18px]">
                              check_circle
                            </span>
                          )}
                        </div>
                        <span className="text-[11px] text-[#685b53] block truncate">
                          {m.cardholderName || user?.name || 'Verified Demo Account'}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Step 4: Plan Cost Breakdown & Transparent Renewal Terms */}
          <div className="p-5 rounded-2xl bg-[#fcf9f6] border border-[#f5eae2] flex flex-col gap-4">
            <h4 className="font-extrabold text-xs uppercase tracking-wider text-[#554a43] flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[#ff5a36] text-[18px]">receipt_long</span>
              <span>Order Summary &amp; Renewal Transparency</span>
            </h4>

            {/* Calculations Card */}
            <div className="divide-y divide-[#ebdcd1] text-xs">
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#685b53]">Selected Course:</span>
                <span className="font-bold text-[#1f1a17]">{selectedCourse?.title || 'None Selected'}</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#685b53]">Billing Plan:</span>
                <span className="font-bold text-[#1f1a17]">{selectedPlanObj.name} ({selectedDuration} Month{selectedDuration > 1 ? 's' : ''})</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#685b53]">Monthly Price (incl. 18% GST):</span>
                <span className="font-bold text-[#1f1a17]">₹{baseMonthlyPrice.toFixed(2)} / month</span>
              </div>
              <div className="py-2.5 flex items-center justify-between text-sm">
                <span className="font-extrabold text-[#1f1a17]">Total Plan Cost:</span>
                <span className="font-extrabold text-[#ff5a36] text-base">₹{totalPlanCost.toFixed(2)}</span>
              </div>
            </div>

            {/* Detailed Terms Box */}
            <div className="p-4 rounded-xl bg-white border border-[#fae8dc] text-xs text-[#554a43] flex flex-col gap-2">
              <span className="font-extrabold text-[#1f1a17] flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[#ff5a36] text-[16px]">verified_user</span>
                <span>Explicit Renewal Terms &amp; Consumer Protections</span>
              </span>
              <ul className="list-disc pl-5 space-y-1 text-[11.5px] leading-relaxed text-[#685b53]">
                <li><strong>No Automatic Debits:</strong> Every monthly cycle requires your explicit authenticated approval. No funds will ever be deducted without your manual confirmation.</li>
                <li><strong>Advance Notice:</strong> You will receive a dashboard and email notification at least 24 hours prior to each due renewal date.</li>
                <li><strong>First Renewal Date:</strong> The first renewal cycle is scheduled exactly 30 days from subscription initiation.</li>
                <li><strong>1-Click Cancellation:</strong> You can cancel this subscription at any time prior to renewal with zero penalty.</li>
                <li><strong>30-Day Refund Guarantee:</strong> 100% money-back refund eligible within 30 days back to your original payment method.</li>
              </ul>
            </div>

            {/* Consent Checkbox */}
            <label className="flex items-start gap-2.5 cursor-pointer pt-1">
              <input
                type="checkbox"
                checked={subscriptionConsent}
                onChange={(e) => setSubscriptionConsent(e.target.checked)}
                className="mt-0.5 rounded border-[#fae8dc] text-[#ff5a36] focus:ring-[#ff5a36] cursor-pointer"
                required
              />
              <span className="text-xs text-[#1e1916] font-semibold leading-relaxed">
                I understand and consent to this course subscription plan. I agree that each debit requires my manual approval, and I will be notified at least 24 hours before each renewal.
              </span>
            </label>
          </div>

          {/* Submit Button */}
          <div className="flex items-center gap-3">
            <button
              type="submit"
              disabled={busy || !selectedCourseId || !selectedToken || !subscriptionConsent}
              className="py-3 px-6 rounded-xl bg-[#ff5a36] text-white font-extrabold text-sm hover:bg-[#e5411d] transition disabled:opacity-50 disabled:cursor-not-allowed shadow-md shadow-[#ff5a36]/20 cursor-pointer flex items-center gap-2"
            >
              {busy ? (
                <>
                  <span className="animate-spin material-symbols-outlined text-[18px]">progress_activity</span>
                  <span>Activating Subscription...</span>
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-[18px]">check</span>
                  <span>Confirm &amp; Activate Subscription (₹{totalPlanCost.toFixed(2)})</span>
                </>
              )}
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('manage')}
              className="py-3 px-5 rounded-xl border border-[#fae8dc] text-xs font-bold text-[#685b53] hover:text-[#1f1a17] transition cursor-pointer"
            >
              Back to Subscriptions
            </button>
          </div>
        </form>
      )}

      {/* ========================================================
          MODAL: ADD NEW SAVED PAYMENT METHOD (RECOGNIZABLE DETAILS)
          ======================================================== */}
      {showAddMethodModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-fadeIn">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-lg w-full border border-[#fae8dc] shadow-2xl flex flex-col gap-5">
            <div className="flex items-center justify-between border-b border-[#faeae0] pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[#ff5a36]">add_card</span>
                <h3 className="font-extrabold text-base text-[#1f1a17]">Add Recognizable Demo Payment Method</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowAddMethodModal(false)}
                className="text-gray-400 hover:text-gray-600 font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleSaveMethod} className="flex flex-col gap-4 text-xs">
              <div>
                <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">
                  Payment Method Type
                </label>
                <select
                  value={newMethodType}
                  onChange={(e) => setNewMethodType(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none"
                >
                  <option value="card">Credit / Debit Card</option>
                  <option value="upi">UPI / Virtual Payment Address</option>
                  <option value="netbanking">Net Banking Account</option>
                  <option value="wallet">Digital Wallet</option>
                </select>
              </div>

              {/* Card Details */}
              {newMethodType === 'card' && (
                <>
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Card Brand</label>
                      <select
                        value={cardBrand}
                        onChange={(e) => setCardBrand(e.target.value)}
                        className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none"
                      >
                        <option value="Visa">Visa</option>
                        <option value="Mastercard">Mastercard</option>
                        <option value="RuPay">RuPay</option>
                        <option value="American Express">American Express</option>
                      </select>
                    </div>

                    <div>
                      <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Last 4 Digits</label>
                      <input
                        type="text"
                        maxLength="4"
                        pattern="[0-9]{4}"
                        value={cardLast4}
                        onChange={(e) => setCardLast4(e.target.value.replace(/\D/g, ''))}
                        placeholder="4242"
                        className="w-full p-2.5 rounded-xl border border-[#faeae0] font-mono focus:border-[#ff5a36] focus:outline-none"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Expiry (MM/YY)</label>
                      <div className="flex items-center gap-1">
                        <input
                          type="text"
                          maxLength="2"
                          placeholder="12"
                          value={cardExpiryMonth}
                          onChange={(e) => setCardExpiryMonth(e.target.value.replace(/\D/g, ''))}
                          className="w-1/2 p-2.5 rounded-xl border border-[#faeae0] text-center font-mono focus:border-[#ff5a36] focus:outline-none"
                          required
                        />
                        <span>/</span>
                        <input
                          type="text"
                          maxLength="2"
                          placeholder="28"
                          value={cardExpiryYear}
                          onChange={(e) => setCardExpiryYear(e.target.value.replace(/\D/g, ''))}
                          className="w-1/2 p-2.5 rounded-xl border border-[#faeae0] text-center font-mono focus:border-[#ff5a36] focus:outline-none"
                          required
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Cardholder Name</label>
                      <input
                        type="text"
                        value={cardholderName}
                        onChange={(e) => setCardholderName(e.target.value)}
                        placeholder="Student Learner"
                        className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none"
                        required
                      />
                    </div>
                  </div>
                </>
              )}

              {/* UPI Details */}
              {newMethodType === 'upi' && (
                <div>
                  <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">UPI ID / VPA</label>
                  <input
                    type="text"
                    value={upiId}
                    onChange={(e) => setUpiId(e.target.value)}
                    placeholder="student@okhdfcbank"
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none font-mono"
                    required
                  />
                  <span className="text-[10px] text-[#8c7e75] mt-1 block">Supports Google Pay, PhonePe, Paytm, and BHIM demo VPAs.</span>
                </div>
              )}

              {/* Net Banking Details */}
              {newMethodType === 'netbanking' && (
                <div>
                  <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Bank Name</label>
                  <select
                    value={bankName}
                    onChange={(e) => setBankName(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none"
                  >
                    <option value="HDFC Bank NetBanking">HDFC Bank</option>
                    <option value="State Bank of India NetBanking">State Bank of India (SBI)</option>
                    <option value="ICICI Bank NetBanking">ICICI Bank</option>
                    <option value="Axis Bank NetBanking">Axis Bank</option>
                    <option value="Kotak Mahindra Bank NetBanking">Kotak Mahindra Bank</option>
                  </select>
                </div>
              )}

              {/* Wallet Details */}
              {newMethodType === 'wallet' && (
                <div>
                  <label className="block font-bold text-[#554a43] uppercase tracking-wider mb-1">Wallet Provider</label>
                  <select
                    value={walletProvider}
                    onChange={(e) => setWalletProvider(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-[#faeae0] focus:border-[#ff5a36] focus:outline-none"
                  >
                    <option value="OLP Learner Wallet">OLP Student Wallet</option>
                    <option value="Paytm Wallet">Paytm Wallet</option>
                    <option value="PhonePe Wallet">PhonePe Wallet</option>
                    <option value="Amazon Pay Wallet">Amazon Pay</option>
                  </select>
                </div>
              )}

              <label className="flex items-start gap-2 cursor-pointer mt-1">
                <input
                  type="checkbox"
                  checked={methodConsent}
                  onChange={(e) => setMethodConsent(e.target.checked)}
                  className="mt-0.5 rounded border-[#fae8dc] text-[#ff5a36] focus:ring-[#ff5a36]"
                  required
                />
                <span className="text-[11px] text-[#554a43] leading-snug">
                  I consent to saving this simulated demo payment method for future course renewals and purchases that I explicitly authorize.
                </span>
              </label>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#faeae0]">
                <button
                  type="button"
                  onClick={() => setShowAddMethodModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-[#685b53] hover:text-[#1f1a17]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={busy || !methodConsent}
                  className="px-5 py-2 rounded-xl bg-[#ff5a36] text-white font-bold text-xs hover:bg-[#e5411d] transition disabled:opacity-50"
                >
                  {busy ? 'Saving...' : 'Save Demo Method'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: RENEWAL REVIEW & QUOTE APPROVAL
          ======================================================== */}
      {quote && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-fadeIn">
          <div className="bg-white rounded-3xl p-6 sm:p-7 max-w-lg w-full border border-[#fae8dc] shadow-2xl flex flex-col gap-5">
            <div className="flex items-center justify-between border-b border-[#faeae0] pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-[#ff5a36]">verified</span>
                <h3 className="font-extrabold text-base text-[#1f1a17]">Review &amp; Approve Renewal</h3>
              </div>
              <button
                type="button"
                onClick={() => setQuote(null)}
                className="text-gray-400 hover:text-gray-600 font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <div className="flex flex-col gap-3 text-xs">
              <div className="p-4 rounded-2xl bg-[#fffaf6] border border-[#fae8dc] flex items-center justify-between">
                <div>
                  <span className="text-[10px] text-[#8c7e75] uppercase font-bold block">Renewal Payable Total</span>
                  <span className="text-xl font-extrabold text-[#1f1a17]">₹{Number(quote.amount).toFixed(2)} {quote.currency}</span>
                  <span className="text-[11px] text-emerald-700 font-medium block">All Taxes &amp; GST Included (Zero Hidden Fees)</span>
                </div>
                <span className="px-3 py-1 rounded-full text-xs font-extrabold bg-emerald-100 text-emerald-800">
                  Fixed Rate
                </span>
              </div>

              <div className="p-3.5 rounded-xl bg-[#fcf9f6] text-[11px] leading-relaxed text-[#685b53] border border-[#f5eae2]">
                <strong className="text-[#1e1916] block mb-1">Consumer Refund &amp; Billing Policy:</strong>
                <p>{quote.refundPolicy || '30-day money-back refund eligibility to your original simulated payment method.'}</p>
              </div>

              <p className="text-[11px] text-[#8c7e75]">
                By approving this simulated transaction, the renewal for the next 30-day cycle will be authorized on your saved method.
              </p>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#faeae0]">
                <button
                  type="button"
                  onClick={() => setQuote(null)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-[#685b53] hover:text-[#1f1a17]"
                >
                  Close
                </button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={handleApproveRenewal}
                  className="px-5 py-2.5 rounded-xl bg-[#ff5a36] text-white font-bold text-xs hover:bg-[#e5411d] transition disabled:opacity-50 shadow-sm cursor-pointer flex items-center gap-1.5"
                >
                  <span className="material-symbols-outlined text-[16px]">check_circle</span>
                  <span>Approve &amp; Pay ₹{Number(quote.amount).toFixed(2)}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ========================================================
          MODAL: SUBSCRIPTION CONFIRMATION SUCCESS & EMAIL NOTIFICATION
          ======================================================== */}
      {successDetails && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-xs animate-fadeIn">
          <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full border border-[#fae8dc] shadow-2xl flex flex-col gap-5 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center mx-auto shadow-sm">
              <span className="material-symbols-outlined text-[36px]">check_circle</span>
            </div>

            <div>
              <h3 className="text-xl font-extrabold text-[#1f1a17]">Subscription Activated! 🎉</h3>
              <p className="text-xs text-[#685b53] mt-1">
                Your subscription has been successfully created with your selected plan details.
              </p>
            </div>

            {/* Selected Plan Details Table */}
            <div className="p-4 rounded-2xl bg-[#fcf9f6] border border-[#f5eae2] text-xs text-left divide-y divide-[#ebdcd1]">
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Course:</span>
                <span className="font-bold text-[#1f1a17]">{successDetails.courseTitle}</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Selected Plan:</span>
                <span className="font-bold text-[#1f1a17]">{successDetails.planName} ({successDetails.durationMonths} Months)</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Monthly Rate:</span>
                <span className="font-bold text-[#1f1a17]">₹{Number(successDetails.monthlyPrice).toFixed(2)} / mo</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Total Plan Cost:</span>
                <span className="font-extrabold text-[#ff5a36]">₹{Number(successDetails.totalPlanCost).toFixed(2)}</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Payment Method:</span>
                <span className="font-semibold text-[#1f1a17]">{successDetails.paymentMethodDisplay}</span>
              </div>
              <div className="py-2 flex items-center justify-between">
                <span className="text-[#8c7e75]">Next Renewal Date:</span>
                <span className="font-bold text-[#0284c7]">
                  {new Date(successDetails.nextRenewalAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}
                </span>
              </div>
            </div>

            {/* Email Dispatch Notice */}
            <div className="p-3.5 rounded-xl bg-sky-50 border border-sky-200 text-xs text-sky-900 text-left flex items-start gap-2.5">
              <span className="material-symbols-outlined text-sky-600 text-[20px] shrink-0 mt-0.5">
                mark_email_read
              </span>
              <div>
                <strong className="block font-bold">Confirmation Email Dispatched</strong>
                <p className="text-[11px] text-sky-800 mt-0.5">
                  An official confirmation email containing this full plan schedule, renewal terms, and GST tax invoice has been sent to <strong>{successDetails.userEmail}</strong>.
                </p>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setSuccessDetails(null)}
              className="py-3 px-6 rounded-xl bg-[#ff5a36] text-white font-extrabold text-xs hover:bg-[#e5411d] transition shadow-xs cursor-pointer"
            >
              Done &amp; View Subscriptions
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

