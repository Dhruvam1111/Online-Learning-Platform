import React, { useEffect, useState } from 'react';
import api from '../services/api';

const field = 'border rounded-lg px-3 py-2 w-full bg-white text-gray-900';
const button = 'rounded-lg bg-orange-700 text-white px-4 py-2 disabled:opacity-50';
export default function PaymentPolicies({ user, methods, onPolicyChange }) {
  const [policy, setPolicy] = useState(null);
  const [subscriptions, setSubscriptions] = useState([]);
  const [coursePrice, setCoursePrice] = useState('');
  const [courseTax, setCourseTax] = useState('0.18');
  const [course, setCourse] = useState('');
  const [token, setToken] = useState('');
  const [consent, setConsent] = useState(false);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [quote, setQuote] = useState(null);
  async function refresh() {
    const current = (await api.get('/payments/policy')).data;
    setPolicy(current);
    onPolicyChange?.(current);
    if (user?.role === 'student') setSubscriptions((await api.get('/payments/subscriptions')).data);
  }
  useEffect(() => { refresh().catch(() => setMessage('Could not load payment settings. Please retry.')); }, [user?.id]);
  async function run(action) {
    setBusy(true); setMessage('');
    try { await action(); await refresh(); }
    catch (e) { setMessage(e.response?.data?.message || 'Request failed. Please retry.'); }
    finally { setBusy(false); }
  }
  return <section className="rounded-xl bg-white p-5 space-y-4 shadow-sm">
    <h2 className="text-xl font-semibold">Payment policies and subscriptions</h2>
    <p role="status" aria-live="polite">{message}</p>
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
      <p>Monthly demo renewals require consent, a notice at least 24 hours beforehand, and your approval of the quoted total. No automatic debit occurs. The first renewal is due in 30 days.</p>
      <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => { await api.post('/payments/subscriptions', { courseId: Number(course), token, consent }); setConsent(false); setMessage('Subscription created. You can cancel at any time.'); }); }}>
        <label className="block">Subscription course ID<input className={field} type="number" min="1" step="1" required value={course} onChange={e => setCourse(e.target.value)} /></label>
        <label className="block">Saved demo payment method<select className={field} required value={token} onChange={e => setToken(e.target.value)}><option value="">Choose a method</option>{methods.map(m => <option key={m.token} value={m.token}>{m.method} · {m.token.slice(-8)}</option>)}</select></label>
        <label className="block"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)} /> I consent to this monthly subscription and will approve each debit.</label>
        <button className={button} disabled={busy || !consent || !token}>Create subscription</button>
      </form>
      <ul className="space-y-3">{subscriptions.map(s => <li key={s.id} className="border rounded-lg p-3 space-y-2 break-words">
        <p>Course {s.courseId} · {s.status} · Next renewal: {new Date(s.nextRenewalAt).toLocaleString()}</p>
        {s.notice && <p>{s.notice.message}</p>}
        {s.status !== 'cancelled' && <div className="flex flex-wrap gap-3">
          <button className={button} disabled={busy || !s.notice || new Date(s.nextRenewalAt) > new Date()} onClick={() => run(async () => { const { data } = await api.get(`/payments/subscriptions/${s.id}/quote`); setQuote({ ...data, id: s.id, requestId: crypto.randomUUID() }); })}>Review renewal</button>
          <button className="underline" disabled={busy} onClick={() => run(async () => { await api.delete(`/payments/subscriptions/${s.id}`); setQuote(null); setMessage('Subscription cancelled.'); })}>Cancel subscription</button>
        </div>}
      </li>)}</ul>
      {quote && <div className="border-2 rounded-lg p-4 space-y-3" role="region" aria-label="Renewal approval">
        <p>Total: ₹{Number(quote.amount).toFixed(2)} INR, including tax. No additional fees.</p>
        <p>{quote.refundPolicy}</p>
        <button className={button} disabled={busy} onClick={() => run(async () => { await api.post(`/payments/subscriptions/${quote.id}/approve`, { approval: true, expectedAmount: quote.amount, requestId: quote.requestId }); setQuote(null); setMessage('Simulated renewal paid.'); })}>Approve and pay ₹{Number(quote.amount).toFixed(2)}</button>
        <button className="underline ml-3" disabled={busy} onClick={() => setQuote(null)}>Close</button>
      </div>}
    </>}
  </section>;
}
