import React, { useEffect, useState, useRef } from 'react';
import api from '../services/api';
export default function StudentWallet({ revision }) {
  const [wallet, setWallet] = useState(null);
  const [amount, setAmount] = useState('100');
  const [outcome, setOutcome] = useState('success');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const pending = useRef(null);
  const refresh = () => api.get('/payments/wallet').then(r => setWallet(r.data)).catch(() => setMessage('Could not load wallet.'));
  useEffect(() => { refresh(); }, [revision]);
  const topUp = async e => {
    e.preventDefault(); setBusy(true);
    const signature = `${amount}:${outcome}`;
    if (!pending.current || pending.current.signature !== signature) pending.current = { signature, requestId: crypto.randomUUID() };
    try {
      const {data} = await api.post('/payments/wallet/top-up', {amount:Number(amount),outcome,requestId:pending.current.requestId});
      setMessage(`Simulated top-up ${data.status}. No real money was transferred.`);
      pending.current = null; await refresh();
    } catch(err) { setMessage(err.response?.data?.message || 'Connection failed. Retry safely with the same details.'); }
    finally { setBusy(false); }
  };
  return <section className="border rounded-2xl p-5 mb-5 bg-white">
    <h3 className="font-bold">Student wallet · ₹{Number(wallet?.balance || 0).toFixed(2)}</h3>
    <p className="text-xs mb-3">Demo credits for simulated course purchases.</p>
    <form onSubmit={topUp} className="flex gap-2 flex-wrap">
      <label>Amount (₹) <input aria-label="Wallet top-up amount" type="number" min="0.01" max="100000" step="0.01" required value={amount} onChange={e=>setAmount(e.target.value)} className="border rounded p-2" /></label>
      <label>Demo outcome <select value={outcome} onChange={e=>setOutcome(e.target.value)} className="border rounded p-2"><option value="success">Success</option><option value="failed">Failed</option><option value="cancelled">Cancelled</option></select></label>
      <button disabled={busy} className="border rounded p-2">{busy?'Processing…':'Simulate top-up'}</button>
    </form>
    <p role="status" className="text-sm py-2">{message}</p>
    <details><summary>Wallet transaction history</summary>
      {wallet?.transactions?.map(t=><p key={t.id} className="text-xs border-t py-2">{t.createdAt} · {t.type} ₹{Number(t.amount).toFixed(2)} · {t.description}</p>)}
    </details>
  </section>;
}
