import React, { useEffect, useState } from 'react';
import api from '../services/api';

const field = 'border rounded-lg px-3 py-2 w-full bg-white text-gray-900';
const button = 'rounded-lg bg-orange-700 text-white px-4 py-2 disabled:opacity-50';
export default function PaymentPolicies({ user, methods, onPolicyChange }) {
  const [policy, setPolicy] = useState(null);
  const [subscriptions, setSubscriptions] = useState([]);
  const [availableCourses, setAvailableCourses] = useState([]);
  const [catalogPrices, setCatalogPrices] = useState({});
  const [coursePrice, setCoursePrice] = useState('');
  const [courseTax, setCourseTax] = useState('0.18');
  const [course, setCourse] = useState('');
  const [durationMonths, setDurationMonths] = useState(1);
  const [token, setToken] = useState('');
  const [consent, setConsent] = useState(false);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [quote, setQuote] = useState(null);
  const [successInfo, setSuccessInfo] = useState(null);

  async function refresh() {
    const current = (await api.get('/payments/policy')).data;
    setPolicy(current);
    onPolicyChange?.(current);
    if (user?.role === 'student') {
      const [subsRes, catalogRes, coursesRes] = await Promise.all([
        api.get('/payments/subscriptions').catch(() => ({ data: [] })),
        api.get('/payments/policy/catalog').catch(() => ({ data: [] })),
        api.get('/courses').catch(() => ({ data: [] }))
      ]);
      setSubscriptions(subsRes.data || []);
      const priceMap = {};
      (catalogRes.data || []).forEach(row => { if (row.courseId) priceMap[row.courseId] = Number(row.amount); });
      setCatalogPrices(priceMap);
      const cList = Array.isArray(coursesRes.data) ? coursesRes.data : (coursesRes.data?.courses || []);
      setAvailableCourses(cList);
    }
  }
  useEffect(() => { refresh().catch(() => setMessage('Could not load payment settings. Please retry.')); }, [user?.id]);
  async function run(action) {
    setBusy(true); setMessage('');
    try { await action(); await refresh(); }
    catch (e) { setMessage(e.response?.data?.message || 'Request failed. Please retry.'); }
    finally { setBusy(false); }
  }

  const selectedCourseObj = availableCourses.find(c => String(c.id || c.courseId) === String(course));
  const monthlyRate = course && catalogPrices[course] != null
    ? catalogPrices[course]
    : (selectedCourseObj?.price != null ? Number(selectedCourseObj.price) : (policy?.defaultPrice ? Number(policy.defaultPrice) : 499.00));
  const totalCost = monthlyRate * durationMonths;

  return <section className="rounded-xl bg-white p-5 space-y-4 shadow-sm">
    <div className="flex flex-wrap items-center justify-between gap-2 border-b pb-3">
      <h2 className="text-xl font-semibold">Payment policies and subscriptions</h2>
      {user?.role === 'student' && (
        <a href="#dashboard" className="text-xs font-bold text-orange-700 underline">
          Open dedicated student dashboard subscriptions &rarr;
        </a>
      )}
    </div>
    <p role="status" aria-live="polite" className="font-semibold text-orange-800">{message}</p>
    <p>Refund policy: request a full or partial refund within 30 days. Refunds restore the original simulated payment sources; course access is revoked after processing. Exceptional requests require administrator review.</p>
    {policy && user?.role === 'admin' && <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => { await api.put('/payments/policy', { commissionRate: Number(policy.commissionRate), taxRate: Number(policy.taxRate), defaultPrice: Number(policy.defaultPrice) }); setMessage('Financial policy saved. Existing order policies remain unchanged.'); }); }}>
      <label className="block">Commission rate (0–1)<input className={field} type="number" min="0" max="1" step="0.0001" required value={policy.commissionRate} onChange={e => setPolicy({ ...policy, commissionRate: e.target.value })} /></label>
      <label className="block">Tax rate (0–1; 0 for exemption)<input className={field} type="number" min="0" max="1" step="0.0001" required value={policy.taxRate} onChange={e => setPolicy({ ...policy, taxRate: e.target.value })} /></label>
      <label className="block">Default catalog price in INR (tax inclusive)<input className={field} type="number" min="0.01" max="1000000" step="0.01" required value={policy.defaultPrice} onChange={e => setPolicy({ ...policy, defaultPrice: e.target.value })} /></label>
      <p>Special catalog tiers keep their existing prices. Commission and tax changes apply to new orders.</p>
      <button className={button} disabled={busy}>Save financial policy</button>
    </form>}
    {user?.role === 'admin' && <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => { await api.put(`/payments/policy/courses/${course}`, { price: Number(coursePrice), taxRate: Number(courseTax) }); setMessage('Course price and tax policy saved for new orders.'); }); }}>
      <h3 className="font-semibold">Course price and tax override</h3>
      <label className="block">Course ID<input className={field} type="number" min="1" required value={course} onChange={e => setCourse(e.target.value)} /></label>
      <label className="block">Price in INR, including tax<input className={field} type="number" min="0.01" max="1000000" step="0.01" required value={coursePrice} onChange={e => setCoursePrice(e.target.value)} /></label>
      <label className="block">Course tax rate (0–1; 0 for exemption)<input className={field} type="number" min="0" max="1" step="0.0001" required value={courseTax} onChange={e => setCourseTax(e.target.value)} /></label>
      <button className={button} disabled={busy}>Save course policy</button>
    </form>}
    {user?.role === 'student' && <>
      <div className="bg-orange-50 border border-orange-200 rounded-lg p-3 text-sm text-orange-950 space-y-1">
        <strong className="block font-semibold">Consent-Based Subscription Renewal Terms:</strong>
        <p>Monthly demo renewals require your consent, an advance notice at least 24 hours beforehand, and your authenticated approval of the quoted total. No automatic debit occurs. The first renewal is due in 30 days. Cancel anytime with zero penalty.</p>
      </div>

      {successInfo && (
        <div className="p-4 rounded-lg bg-emerald-50 border border-emerald-200 text-sm text-emerald-900 space-y-2">
          <div className="font-bold text-base flex items-center gap-1.5">
            <span>🎉 Subscription Activated Successfully!</span>
          </div>
          <p><strong>Course:</strong> {successInfo.courseTitle}</p>
          <p><strong>Plan:</strong> {successInfo.planName} ({successInfo.durationMonths} Month{successInfo.durationMonths > 1 ? 's' : ''}) • ₹{Number(successInfo.monthlyPrice).toFixed(2)}/mo (Total: ₹{Number(successInfo.totalPlanCost).toFixed(2)})</p>
          <p><strong>Next Renewal Date:</strong> {new Date(successInfo.nextRenewalAt).toLocaleDateString('en-IN')}</p>
          <p className="text-xs text-emerald-800">📩 A confirmation email with plan breakdown and renewal schedule has been sent to {user?.email}.</p>
          <button type="button" className="text-xs font-bold underline" onClick={() => setSuccessInfo(null)}>Dismiss</button>
        </div>
      )}

      <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => {
        const planName = durationMonths === 1 ? 'Monthly Plan' : `${durationMonths}-Month Plan`;
        const res = await api.post('/payments/subscriptions', {
          courseId: Number(course),
          token,
          consent,
          durationMonths: Number(durationMonths),
          planName,
          monthlyPrice: monthlyRate,
          totalAmount: totalCost
        });
        setConsent(false);
        setSuccessInfo({
          courseTitle: selectedCourseObj?.title || res.data?.courseTitle || `Course #${course}`,
          planName,
          durationMonths: Number(durationMonths),
          monthlyPrice: monthlyRate,
          totalPlanCost: totalCost,
          nextRenewalAt: res.data?.nextRenewalAt
        });
        setMessage('Subscription created successfully.');
      }); }}>
        <label className="block">
          Course Selection
          <select className={field} required value={course} onChange={e => setCourse(e.target.value)}>
            <option value="">-- Choose a course --</option>
            {availableCourses.map(c => {
              const id = c.id || c.courseId;
              const p = catalogPrices[id] != null ? catalogPrices[id] : (c.price != null ? Number(c.price) : 499.00);
              return <option key={id} value={id}>{c.title} (₹{p.toFixed(2)}/mo)</option>;
            })}
          </select>
        </label>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label className="block">
            Subscription Duration
            <select className={field} value={durationMonths} onChange={e => setDurationMonths(Number(e.target.value))}>
              <option value={1}>1 Month (Monthly Billing)</option>
              <option value={3}>3 Months (Quarterly Plan)</option>
              <option value={6}>6 Months (Semester Plan)</option>
              <option value={12}>12 Months (Annual Plan)</option>
            </select>
          </label>

          <label className="block">
            Saved Demo Payment Method
            <select className={field} required value={token} onChange={e => setToken(e.target.value)}>
              <option value="">Choose a recognizable method</option>
              {methods.map(m => {
                const label = m.displayName || `${m.cardBrand || m.method} · ${m.last4 || m.token.slice(-4)}`;
                return <option key={m.token} value={m.token}>{label}</option>;
              })}
            </select>
          </label>
        </div>

        {course && (
          <div className="p-3 bg-gray-50 border rounded-lg text-xs space-y-1">
            <div className="flex justify-between"><span>Monthly Price (incl. tax):</span><strong className="text-gray-900">₹{monthlyRate.toFixed(2)} / month</strong></div>
            <div className="flex justify-between"><span>Duration:</span><strong className="text-gray-900">{durationMonths} Month{durationMonths > 1 ? 's' : ''}</strong></div>
            <div className="flex justify-between text-sm border-t pt-1"><span className="font-bold text-gray-900">Total Plan Cost:</span><strong className="text-orange-700">₹{totalCost.toFixed(2)}</strong></div>
            <div className="text-[11px] text-gray-500 pt-1">Renewal Terms: Explicit authenticated approval required for every debit. Advance notice 24h prior. Cancel anytime. 30-day money-back eligibility.</div>
          </div>
        )}

        <label className="block"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)} /> I consent to this course subscription plan and will approve each periodic debit.</label>
        <button className={button} disabled={busy || !consent || !token || !course}>Create subscription (₹{totalCost.toFixed(2)})</button>
      </form>

      <div className="space-y-3 pt-3">
        <h3 className="font-semibold text-base">Your Active &amp; Recent Subscriptions</h3>
        {subscriptions.length === 0 && <p className="text-sm text-gray-500">No active subscriptions found.</p>}
        <ul className="space-y-3">{subscriptions.map(s => {
          const payable = s.payableAmount != null ? Number(s.payableAmount) : (s.monthlyPrice != null ? Number(s.monthlyPrice) : 499.00);
          return <li key={s.id} className="border rounded-lg p-3 space-y-2 break-words bg-white">
            <div className="flex justify-between items-start">
              <div>
                <strong className="block text-gray-900">{s.courseTitle || `Course #${s.courseId}`}</strong>
                <span className="text-xs text-gray-500">Plan: {s.planName || 'Monthly Plan'} • Ref: {s.id}</span>
              </div>
              <span className={`px-2 py-0.5 rounded text-xs font-bold ${s.status === 'active' ? 'bg-emerald-100 text-emerald-800' : 'bg-gray-100 text-gray-600'}`}>
                {s.status.toUpperCase()}
              </span>
            </div>
            <div className="text-xs text-gray-700 grid grid-cols-2 gap-2 bg-gray-50 p-2 rounded">
              <div><strong>Payable Amount:</strong> ₹{payable.toFixed(2)} / month</div>
              <div><strong>Next Renewal:</strong> {new Date(s.nextRenewalAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' })}</div>
            </div>
            {s.notice && <div className="p-2 bg-amber-50 border border-amber-200 rounded text-xs text-amber-900"><strong>Notice:</strong> {s.notice.message}</div>}
            {s.status !== 'cancelled' && <div className="flex flex-wrap gap-3 pt-1">
              <button className={button} disabled={busy || !s.notice || new Date(s.nextRenewalAt) > new Date()} onClick={() => run(async () => { const { data } = await api.get(`/payments/subscriptions/${s.id}/quote`); setQuote({ ...data, id: s.id, requestId: crypto.randomUUID() }); })}>Review renewal</button>
              <button className="underline text-sm text-rose-700" disabled={busy} onClick={() => run(async () => { await api.delete(`/payments/subscriptions/${s.id}`); setQuote(null); setMessage('Subscription cancelled.'); })}>Cancel subscription</button>
            </div>}
          </li>;
        })}</ul>
      </div>

      {quote && <div className="border-2 rounded-lg p-4 space-y-3 bg-white" role="region" aria-label="Renewal approval">
        <h4 className="font-bold text-sm">Review &amp; Approve Renewal</h4>
        <p>Payable Total: <strong>₹{Number(quote.amount).toFixed(2)} {quote.currency}</strong> (All taxes included; zero extra fees).</p>
        <p className="text-xs text-gray-600">{quote.refundPolicy}</p>
        <button className={button} disabled={busy} onClick={() => run(async () => { await api.post(`/payments/subscriptions/${quote.id}/approve`, { approval: true, expectedAmount: quote.amount, requestId: quote.requestId }); setQuote(null); setMessage('Simulated renewal paid.'); })}>Approve and pay ₹{Number(quote.amount).toFixed(2)}</button>
        <button className="underline ml-3 text-sm" disabled={busy} onClick={() => setQuote(null)}>Close</button>
      </div>}
    </>}
  </section>;
}
