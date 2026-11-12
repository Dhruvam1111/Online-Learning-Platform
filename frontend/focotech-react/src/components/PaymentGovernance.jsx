import React, { useEffect, useState } from 'react';
import api from '../services/api';
const field = 'border rounded-lg px-3 py-2 w-full bg-white text-gray-900';
const button = 'rounded-lg bg-orange-700 text-white px-4 py-2 disabled:opacity-50';
export default function PaymentGovernance({ user, onAccountChanged }) {
  const [account, setAccount] = useState(null);
  const [bank, setBank] = useState({ holderName: user?.name || '', bankName: '', ifscCode: '', accountNumber: '', consent: false });
  const [pending, setPending] = useState([]);
  const [policy, setPolicy] = useState(null);
  const [reason, setReason] = useState('');
  const [identityChecked, setIdentityChecked] = useState(false);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  async function refresh() {
    if (user?.role === 'instructor') { const { data } = await api.get('/payments/bank-accounts'); setAccount(data); onAccountChanged?.(data); }
    if (user?.role === 'admin') { setPending((await api.get('/payments/bank-accounts/pending')).data); setPolicy((await api.get('/payments/governance/policy')).data); }
  }
  useEffect(() => { refresh().catch(() => setMessage('Could not load financial settings. Please retry.')); }, [user?.id]);
  async function run(action) { setBusy(true); setMessage(''); try { await action(); await refresh(); } catch (e) { setMessage(e.response?.data?.message || 'Request failed. Please retry.'); } finally { setBusy(false); } }
  if (!['admin', 'instructor'].includes(user?.role)) return null;
  return <section className="bg-white border rounded-xl p-4 space-y-4">
    <h2 className="text-xl font-bold">Payout accounts and financial governance</h2>
    <p>Demo verification only. Use fictitious bank details; no real bank or identity service is contacted.</p>
    <p role="status" aria-live="polite">{message}</p>
    {user.role === 'instructor' && <>
      <p>Saved payout account: <strong>{account?.status || 'Loading…'}</strong> {account?.account} {account?.bankName}</p>
      <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => { await api.put('/payments/bank-accounts', bank); setBank({ ...bank, accountNumber: '', consent: false }); setMessage('Account saved for simulated administrator verification. Updating an account requires a new verification.'); }); }}>
        <label className="block">Account-holder name (your instructor profile name)<input className={field} required maxLength={100} value={bank.holderName} onChange={e => setBank({ ...bank, holderName: e.target.value })} /></label>
        <label className="block">Demo bank name<input className={field} required maxLength={80} value={bank.bankName} onChange={e => setBank({ ...bank, bankName: e.target.value })} /></label>
        <label className="block">Demo account number<input className={field} required inputMode="numeric" pattern="[0-9]{9,18}" maxLength={18} autoComplete="off" value={bank.accountNumber} onChange={e => setBank({ ...bank, accountNumber: e.target.value })} /></label>
        <label className="block">IFSC<input className={field} required pattern="[A-Z]{4}0[A-Z0-9]{6}" maxLength={11} value={bank.ifscCode} onChange={e => setBank({ ...bank, ifscCode: e.target.value.toUpperCase() })} /></label>
        <label className="block"><input type="checkbox" checked={bank.consent} onChange={e => setBank({ ...bank, consent: e.target.checked })} /> I consent to saving this simulated payout account.</label>
        <button className={button} disabled={busy || !bank.consent}>Save payout account</button>
      </form>
      <button className="underline" disabled={busy} onClick={() => run(refresh)}>Refresh verification status</button>
    </>}
    {user.role === 'admin' && <>
      <h3 className="font-bold">Pending demo account reviews</h3>
      <label className="block">Review reason<input className={field} required maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} /></label>
      <label className="block"><input type="checkbox" checked={identityChecked} onChange={e => setIdentityChecked(e.target.checked)} /> I reviewed the simulated holder identity and ownership details.</label>
      {pending.length === 0 && <p>No pending accounts.</p>}
      <ul className="space-y-3">{pending.map(p => <li key={p.id} className="border rounded p-3 space-y-2">
        <p>Instructor {p.instructorId}: {p.holderName} · {p.bankName} · {p.account} · {p.ifscCode}</p>
        {[true, false].map(approved => <button key={String(approved)} className={`${button} mr-3`} disabled={busy || !reason.trim() || (approved && !identityChecked)} onClick={() => run(async () => { await api.post('/payments/bank-accounts/decide', { instructorId: p.instructorId, accountId: p.id, approved, identityChecked, reason }); setMessage(approved ? 'Demo account verified.' : 'Demo account rejected.'); })}>{approved ? 'Verify demo account' : 'Reject account'}</button>)}
      </li>)}</ul>
      {policy && <form className="space-y-3" onSubmit={e => { e.preventDefault(); run(async () => { await api.put('/payments/governance/policy', { financialRetentionDays: Number(policy.financialRetentionDays), adminAuditRetentionDays: Number(policy.adminAuditRetentionDays), withholdingRate: Number(policy.withholdingRate), commissionTaxRate: Number(policy.commissionTaxRate ?? 0.18) }); setMessage('Retention minimums and simulated withholding policy saved.'); }); }}>
        <h3 className="font-bold">Retention and withholding policy</h3>
        <label className="block">Minimum financial retention days<input className={field} type="number" min="2557" max="36500" required value={policy.financialRetentionDays} onChange={e => setPolicy({ ...policy, financialRetentionDays: e.target.value })} /></label>
        <label className="block">Minimum admin-audit retention days<input className={field} type="number" min="365" max="36500" required value={policy.adminAuditRetentionDays} onChange={e => setPolicy({ ...policy, adminAuditRetentionDays: e.target.value })} /></label>
        <label className="block">Simulated instructor withholding rate (0–1)<input className={field} type="number" min="0" max="1" step="0.0001" required value={policy.withholdingRate} onChange={e => setPolicy({ ...policy, withholdingRate: e.target.value })} /></label>
        <label className="block">Platform commission tax rate (0–1)<input className={field} type="number" min="0" max="1" step="0.0001" required value={policy.commissionTaxRate ?? 0.18} onChange={e => setPolicy({ ...policy, commissionTaxRate: e.target.value })} /></label>
        <p>Records are retained indefinitely online. Backup retention must also be configured operationally. Withholding here models an admin-selected rate; it does not determine statutory applicability.</p>
        <button className={button} disabled={busy}>Save governance policy</button>
      </form>}
    </>}
  </section>;
}
