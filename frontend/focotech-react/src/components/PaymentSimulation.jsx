import PaymentPolicies from './PaymentPolicies';
import React, { useEffect, useState } from 'react';
import { paymentAPI, paymentSimulationAPI as sim } from '../services/api';

const field = 'border rounded-lg px-3 py-2 w-full bg-white text-gray-900';
const button = 'rounded-lg bg-orange-700 text-white px-4 py-2 disabled:opacity-50';
const events = ['payment.confirmed', 'payment.failed', 'payment.pending', 'refund.pending', 'refund.processed', 'refund.failed', 'payout.processing', 'payout.successful', 'payout.failed', 'payout.delayed', 'chargeback.opened', 'chargeback.resolved'];

export default function PaymentSimulation({ user }) {
  const [methods, setMethods] = useState([]);
  const [method, setMethod] = useState('card');
  const [consent, setConsent] = useState(false);
  const [orderId, setOrderId] = useState('');
  const [courseId, setCourseId] = useState('');
  const [wallet, setWallet] = useState(false);
  const [event, setEvent] = useState(events[0]);
  const [reference, setReference] = useState('');
  const [outcome, setOutcome] = useState('won');
  const [refundAmount, setRefundAmount] = useState('');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const admin = user?.role === 'admin';
  const canPurchase = ['student', 'admin'].includes(user?.role);
  const refresh = async () => { if (canPurchase) setMethods((await sim.methods()).data); };
  useEffect(() => { refresh().catch(() => setMessage('Could not load saved methods. Please sign in again.')); }, [user?.id]);
  async function run(action) {
    setBusy(true); setMessage('');
    try { await action(); await refresh(); }
    catch (e) { setMessage(e.response?.data?.message || e.message || 'Request failed. Please try again.'); }
    finally { setBusy(false); }
  }
  async function sendEvent() {
    const body = { event, event_id: crypto.randomUUID(), order_id: orderId.trim() };
    if (event.startsWith('refund.')) body.refund_id = reference.trim();
    if (event.startsWith('payout.')) body.earning_id = Number(reference);
    if (event === 'chargeback.resolved') body.outcome = outcome;
    await sim.event(body); setMessage('Simulation update accepted.');
  }
  return <main className="min-h-screen bg-orange-50 text-gray-900 p-6">
    <div className="max-w-3xl mx-auto space-y-6">
      <a href="#dashboard" className="underline">Back to dashboard</a>
      <h1 className="text-3xl font-bold">Payment simulation</h1>
      <p>Practice purchases, saved payment methods and refunds. No real money moves. Use demo methods only; no card numbers or bank details are needed.</p>
      <p role="status" aria-live="polite" className="font-medium">{message}</p>
      <PaymentPolicies user={user} methods={methods} />
      {canPurchase && <>
        <section className="rounded-xl bg-white p-5 space-y-3 shadow-sm">
          <h2 className="text-xl font-semibold">Create a purchase or renewal</h2>
          <label className="block">Course ID<input className={field} type="number" min="1" value={courseId} onChange={e => setCourseId(e.target.value)} /></label>
          <p>The server uses the course catalog price for each order.</p>
          <label className="block"><input type="checkbox" checked={wallet} onChange={e => setWallet(e.target.checked)} /> Use available wallet credit</label>
          <button className={button} disabled={busy || !courseId} onClick={() => run(async () => {
            const { data } = await paymentAPI.createOrder(Number(courseId), null, 'INR', null, wallet);
            setOrderId(data.orderId); setMessage(`Order ${data.orderId}: ${data.status || 'created'}. Amount due: ₹${data.amount}.`);
          })}>Create order</button>
        </section>
        <section className="rounded-xl bg-white p-5 space-y-3 shadow-sm">
          <h2 className="text-xl font-semibold">Saved demo methods</h2>
          <label className="block">Method<select className={field} value={method} onChange={e => setMethod(e.target.value)}>
            <option value="card">Demo card</option><option value="upi">Demo UPI</option><option value="netbanking">Demo net banking</option><option value="wallet">Demo digital wallet</option>
          </select></label>
          <label className="block"><input type="checkbox" checked={consent} onChange={e => setConsent(e.target.checked)} /> I consent to saving this demo method for purchases and subscription renewals I authorize.</label>
          <button className={button} disabled={busy || !consent} onClick={() => run(async () => { await sim.saveMethod(method, consent); setConsent(false); setMessage('Demo method saved. You can revoke it at any time.'); })}>Save demo method</button>
          <label className="block">Order ID<input className={field} value={orderId} onChange={e => setOrderId(e.target.value)} placeholder="OLP-…" /></label>
          {methods.length === 0 && <p>No saved methods.</p>}
          <ul className="space-y-3">{methods.map(m => <li key={m.token} className="border rounded-lg p-3 flex flex-wrap items-center gap-3">
            <span>{m.method} · {m.token.slice(-8)}</span>
            <button className={button} disabled={busy || !orderId.trim()} onClick={() => run(async () => { const { data } = await sim.pay(m.token, orderId.trim()); setMessage(`Payment ${data.status}. Order ${data.orderId}.`); })}>Pay this order</button>
            <button className="underline" disabled={busy} onClick={() => run(async () => { await sim.revokeMethod(m.token); setMessage('Saved method revoked.'); })}>Revoke</button>
          </li>)}</ul>
          <p className="text-sm">Each renewal uses a new order. Retrying the same order does not create another payment.</p>
        </section>
        <section className="rounded-xl bg-white p-5 space-y-3 shadow-sm">
          <h2 className="text-xl font-semibold">Request a refund</h2>
          <label className="block">Order ID<input className={field} value={orderId} onChange={e => setOrderId(e.target.value)} /></label>
          <label className="block">Refund amount (leave blank for full refund)<input className={field} type="number" min="0.01" step="0.01" value={refundAmount} onChange={e => setRefundAmount(e.target.value)} /></label>
          <button className={button} disabled={busy || !orderId.trim()} onClick={() => run(async () => {
            const { data } = await sim.requestRefund(orderId.trim(), refundAmount ? Number(refundAmount) : null, 'Demo cancellation');
            setReference(data.refundId); setMessage(`Refund ${data.refundId} is pending. An admin can simulate its outcome.`);
          })}>Request refund</button>
        </section>
      </>}
      {admin && <section className="rounded-xl bg-white p-5 space-y-3 shadow-sm">
        <h2 className="text-xl font-semibold">Admin: simulate a gateway update</h2>
        <label className="block">Update<select className={field} value={event} onChange={e => setEvent(e.target.value)}>{events.map(e => <option key={e}>{e}</option>)}</select></label>
        <label className="block">Order ID<input className={field} value={orderId} onChange={e => setOrderId(e.target.value)} /></label>
        {(event.startsWith('refund.') || event.startsWith('payout.')) && <label className="block">{event.startsWith('refund.') ? 'Refund reference' : 'Instructor earning ID'}<input className={field} value={reference} onChange={e => setReference(e.target.value)} /></label>}
        {event === 'chargeback.resolved' && <label className="block">Dispute outcome<select className={field} value={outcome} onChange={e => setOutcome(e.target.value)}><option value="won">Won</option><option value="lost">Lost</option></select></label>}
        <button className={button} disabled={busy || !orderId.trim()} onClick={() => run(sendEvent)}>Apply simulated update</button>
      </section>}
    </div>
  </main>;
}
