import useModalAccessibility from '../hooks/useModalAccessibility';
import React, { useState, useEffect, useRef } from 'react';
import api, { paymentAPI } from '../services/api';

/**
 * Enterprise Payment Gateway Checkout Modal
 * UI Reference: Official Stripe Payments Demo (stripe-payments-demo) & Focotech Design System
 * Engineers: Shubh Patel & Dhruvam
 * 
 * Features:
 * - 2-Column Responsive Split Layout (Summary & Invoice Ledger + Multi-Instrument Form)
 * - Simulated card and UPI checkout (no real money movement)
 * - Dynamic NPCI UPI QR Code Generation & Scanning via Google ZXing
 * - Stripe Elements-style Single-Line Card Component with Brand Detection (Visa/Mastercard/RuPay)
 * - Popular Indian NetBanking Selection Grid
 * - Real-time Promotional Coupon Validation Engine
 * - Virtual Student Wallet Split-Tender Deduction
 * - Cryptographic Signature Verification & Atomic Enrollment
 * - Automated GST Tax Invoice Generation & Linking
 */
export default function RazorpayPaymentModal({
  isOpen,
  course,
  onClose,
  onSuccess,
}) {
  if (!isOpen || !course) return null;
  return <CheckoutContent course={course} onClose={onClose} onSuccess={onSuccess} />;
}

function CheckoutContent({ course, onClose, onSuccess }) {
  // Active channel tab: 'upi_qr' | 'stripe' | 'card' | 'netbanking'
  const [activeTab, setActiveTab] = useState('upi_qr');

  // Pricing & Order State
  const parseNumericPrice = (p) => {
    if (typeof p === 'number' && !isNaN(p)) return p;
    if (!p) return 499.0;
    const cleaned = String(p).replace(/[^0-9.]/g, '');
    const num = parseFloat(cleaned);
    return isNaN(num) || num <= 0 ? 499.0 : num;
  };

  const resolveCourseId = (c) => {
    if (!c) return 1;
    if (typeof c.id === 'number' && !isNaN(c.id)) return c.id;
    const str = String(c.id || '').toLowerCase();
    if (!isNaN(str) && str.trim() !== '') return Number(str);
    const slugMap = {
      'advance-ui-ux': 6,
      'design-system': 7,
      'webflow-tutorial': 8,
      'modern-fullstack': 9
    };
    return slugMap[str] || 7;
  };

  const basePrice = parseNumericPrice(course.price);
  const safeCourseId = resolveCourseId(course);
  const [orderData, setOrderData] = useState(null);
  const [displayCurrency, setDisplayCurrency] = useState('INR');
  const [fx, setFx] = useState(null);
  useEffect(() => {
    let live = true;
    const refresh = () => api.get('/payments/currencies/rates').then(r => { if (live) setFx(r.data); }).catch(() => {});
    refresh(); const timer = setInterval(refresh, 30000);
    return () => { live = false; clearInterval(timer); };
  }, []);
  const pendingOrder = useRef(null);
  const [qrCodeData, setQrCodeData] = useState(null);
  const [loadingOrder, setLoadingOrder] = useState(true);

  // Discount & Wallet state
  const [couponCode, setCouponCode] = useState('');
  const [appliedCoupon, setAppliedCoupon] = useState(null);
  const [couponError, setCouponError] = useState('');
  const [couponLoading, setCouponLoading] = useState(false);

  const [useWallet, setUseWallet] = useState(false);
  const [policyAccepted, setPolicyAccepted] = useState(false);
  const [walletBalance, setWalletBalance] = useState(0);

  // Form states (Stripe Payments Demo style)
  const [cardNumber, setCardNumber] = useState('4242 •••• •••• 4242');
  const [cardExpiry, setCardExpiry] = useState('12 / 28');
  const [cardCvv, setCardCvv] = useState('123');
  const [cardName, setCardName] = useState('Demo Student');
  const [postalCode, setPostalCode] = useState('382007');
  const [selectedBank, setSelectedBank] = useState('HDFC Bank');
  const [upiVpa, setUpiVpa] = useState('student@okaxis');

  // Checkout flow states: 'idle' | 'processing' | 'success' | 'failed'
  const [status, setStatus] = useState('idle');
  const [stepMessage, setStepMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [completedOrder, setCompletedOrder] = useState(null);

  const dialogRef = useModalAccessibility(true, () => { if (!loadingOrder && status !== 'processing') { if (status === 'success') handleDismissSuccess(); else handleCloseCheckout(); } });

  // Load wallet balance & create order on modal open
  useEffect(() => {
    // Cancel the queued request if React replays this effect before checkout opens.
    const start = setTimeout(initCheckout, 0);
    return () => clearTimeout(start);
  }, [safeCourseId, basePrice, useWallet, appliedCoupon, displayCurrency]);

  const releasePendingOrder = async () => {
    if (!pendingOrder.current) return false;
    const { data } = await api.post(`/payments/cancel/${encodeURIComponent(pendingOrder.current)}`);
    pendingOrder.current = null;
    setOrderData(null);
    setQrCodeData(null);
    if (data.status === 'success') {
      setCompletedOrder(data);
      setStatus('success');
      onSuccess?.(data);
      return true;
    }
    return false;
  };

  const handleCloseCheckout = async () => {
    if (loadingOrder || status === 'processing') return;
    setLoadingOrder(true);
    try {
      if (!(await releasePendingOrder())) onClose();
    } catch {
      setErrorMessage('Could not release this order. Please try closing checkout again.');
    } finally {
      setLoadingOrder(false);
    }
  };

  const initCheckout = async () => {
    setLoadingOrder(true);
    setErrorMessage('');
    try {
      if (await releasePendingOrder()) {
        setLoadingOrder(false);
        return;
      }
      setStatus('idle');
      // 1. Fetch Student Wallet Balance
      try {
        const walletRes = await paymentAPI.getWallet();
        if (walletRes.data?.balance !== undefined) {
          setWalletBalance(Number(walletRes.data.balance));
        }
      } catch (wErr) {
        console.warn('Wallet fetch notice:', wErr);
      }

      // 2. Create Payment Order with Coupon & Wallet
      const res = await paymentAPI.createOrder(
        safeCourseId,
        basePrice,
        'INR',
        appliedCoupon ? appliedCoupon.code : null,
        useWallet, null, policyAccepted ? 'refund-2026-11-v1' : null, displayCurrency
      );

      const order = res.data;
      pendingOrder.current = order.status === 'success' ? null : order.orderId;
      setOrderData(order);

      // If fully covered by coupon/wallet, order is immediately confirmed
      if (order.status === 'success') {
        const settled = {
          orderId: order.orderId,
          invoiceNumber: 'INV-SETTLED',
          amount: 0,
        };
        setStatus('success');
        setCompletedOrder(settled);
        if (onSuccess) {
          onSuccess(settled);
        }
        setLoadingOrder(false);
        return;
      }

      // 3. Fetch Dynamic NPCI UPI QR Code from backend
      try {
        const qrRes = await paymentAPI.getUpiQr(order.orderId);
        setQrCodeData(qrRes.data);
      } catch (qrErr) {
        console.warn('QR Code generation notice:', qrErr);
      }

      setLoadingOrder(false);
    } catch (err) {
      setStatus('failed');
      console.error('Order creation error:', err);
      setErrorMessage(
        err.response?.data?.message || err.response?.data?.error || 'Failed to initialize payment gateway order.'
      );
      setLoadingOrder(false);
    }
  };

  // Coupon evaluation
  const handleApplyCoupon = async (e) => {
    e.preventDefault();
    if (!couponCode.trim()) return;

    setCouponLoading(true);
    setCouponError('');
    try {
      const res = await paymentAPI.applyCoupon(couponCode.trim(), orderData ? Number(orderData.totalAmount) + Number(orderData.discountAmount || 0) : basePrice);
      setLoadingOrder(true);
      setAppliedCoupon({
        code: res.data.couponCode,
        discountAmount: Number(res.data.discountAmount),
      });
      setCouponLoading(false);
    } catch (err) {
      setCouponLoading(false);
      setCouponError(
        err.response?.data?.message || err.response?.data?.error || 'Invalid or expired coupon code.'
      );
    }
  };

  const removeCoupon = () => {
    setLoadingOrder(true);
    setAppliedCoupon(null);
    setCouponCode('');
    setCouponError('');
  };

  // Process Card / NetBanking / UPI VPA payment
  const acceptPolicy = async () => {
    if (!policyAccepted || !orderData?.orderId) throw new Error('Read and accept the refund policy before paying.');
    await api.post(`/payments/orders/${orderData.orderId}/policy-acceptance`, { policyVersion: orderData.refundPolicyVersion });
  };
  const handleGatewayPay = async () => {
    if (!orderData) return;
    setStatus('processing');
    setErrorMessage('');
    setStepMessage('Preparing simulated payment...');

    try {
      await acceptPolicy();
      await new Promise((r) => setTimeout(r, 700));

      const paymentId = 'pay_' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
      let methodLabel = 'Card';
      if (activeTab === 'card') {
        methodLabel = 'Card';
      } else if (activeTab === 'netbanking') {
        methodLabel = `Net Banking (${selectedBank})`;
      } else if (activeTab === 'upi_qr') {
        methodLabel = 'QR Payment';
      }

      setStepMessage('Verifying simulated payment signature...');
      const signRes = await paymentAPI.sandboxSign(orderData.orderId, paymentId);
      const signature = signRes.data.signature;

      setStepMessage('Locking atomic course enrollment & generating GST invoice...');
      const verifyRes = await paymentAPI.verify(
        orderData.orderId,
        paymentId,
        signature,
        methodLabel
      );

      setStatus('success');
      setCompletedOrder(verifyRes.data);
      if (onSuccess) {
        onSuccess(verifyRes.data);
      }
      // Success window remains open for user control (no auto-close)
    } catch (err) {
      console.error('Payment error:', err);
      setStatus('failed');
      setErrorMessage(
        err.response?.data?.message || err.response?.data?.error || 'Payment verification failed. Please try again.'
      );
    }
  };

  // Simulate Instant UPI QR Scan (Viva Demo) — confirms in DB & unlocks enrollment
  const handleSimulateQrScan = async () => {
    if (!orderData) return;
    setStatus('processing');
    setStepMessage('Scanning QR code & processing payment...');

    try {
      await acceptPolicy();
      await new Promise((r) => setTimeout(r, 700));
      setStepMessage('Payment confirmed! Enrolling student into database & generating invoice...');
      const res = await paymentAPI.simulatePay(orderData.orderId);

      setStatus('success');
      setCompletedOrder(res.data);
      if (onSuccess) {
        onSuccess(res.data);
      }
      // Success window remains open for user control (no auto-close)
    } catch (err) {
      setStatus('failed');
      setErrorMessage(err.response?.data?.message || err.response?.data?.error || 'UPI Simulation failed.');
    }
  };

  // Simulate Payment Failure (Viva Demo) — marks failed in DB, no enrollment, returns to original state
  const handleSimulateFailure = async () => {
    if (!orderData) return;
    setStatus('processing');
    setStepMessage('Simulating payment cancellation / decline...');

    try {
      await new Promise((r) => setTimeout(r, 600));
      await paymentAPI.failPayment(orderData.orderId);
      setStatus('failed');
      setErrorMessage('Payment failed or was cancelled. No charges were made, and the course remains unenrolled.');
    } catch (err) {
      setStatus('failed');
      setErrorMessage(err.response?.data?.message || 'Payment cancelled. State returned to original.');
    }
  };

  // Official Stripe Cloud Sandbox Checkout
  const handleStripeCheckout = async () => {
    setStatus('processing');
    setErrorMessage('');
    setStepMessage('Preparing local simulated checkout...');

    try {
      await acceptPolicy();
      await new Promise((r) => setTimeout(r, 600));
      setStepMessage('Preparing demo provider session...');
      await new Promise((r) => setTimeout(r, 500));

      let orderIdToConfirm = orderData?.orderId;
      if (!orderIdToConfirm) {
        const res = await paymentAPI.createStripeSession(safeCourseId, currentPayable);
        orderIdToConfirm = res.data?.orderId;
      }

      if (!orderIdToConfirm) {
        throw new Error('Payment order could not be initialized.');
      }

      setStepMessage('Confirming simulated payment and course enrollment...');
      const verifyRes = await paymentAPI.confirmPayment(orderIdToConfirm, 'Card');

      setStatus('success');
      setCompletedOrder(verifyRes.data);
      if (onSuccess) {
        onSuccess(verifyRes.data);
      }
    } catch (err) {
      console.error('Stripe Checkout error:', err);
      setStatus('failed');
      setErrorMessage(
        err.response?.data?.message || err.response?.data?.error || err.message || 'Stripe payment failed. Please try again.'
      );
    }
  };

  // Card brand detection helper
  const getCardBrand = (num) => {
    const clean = num.replace(/\D/g, '');
    if (clean.startsWith('4')) return { name: 'Visa', color: 'bg-blue-600 text-white' };
    if (/^5[1-5]/.test(clean)) return { name: 'Mastercard', color: 'bg-amber-600 text-white' };
    if (/^3[47]/.test(clean)) return { name: 'Amex', color: 'bg-emerald-600 text-white' };
    if (/^(60|65|81|82)/.test(clean)) return { name: 'RuPay', color: 'bg-orange-700 text-white' };
    return { name: 'Card', color: 'bg-neutral-800 text-white' };
  };

  const discountAmount = Number(orderData?.discountAmount ?? appliedCoupon?.discountAmount ?? 0);
  const walletAmountDeducted = Number(orderData?.walletAmountUsed ?? 0);
  const currentPayable = Number(orderData?.amount ?? basePrice);
  const catalogPrice = orderData ? currentPayable + discountAmount + walletAmountDeducted : basePrice;

  const taxableTotal = Math.max(0, catalogPrice - discountAmount);

  const [downloadingReceipt, setDownloadingReceipt] = useState(false);

  // Directly downloads receipt as a file to disk on user click
  const handleDownloadReceipt = async () => {
    const orderId = completedOrder?.orderId || orderData?.orderId;
    if (!orderId) return;
    setDownloadingReceipt(true);
    try {
      const { data: blob } = await api.get(
        `/payments/invoices/${encodeURIComponent(orderId)}/download`,
        { responseType: 'blob' }
      );
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Receipt-${completedOrder?.invoiceNumber || orderId || 'INV'}.html`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download error:', err);
      setErrorMessage("Receipt download failed. Please sign in and try again.");
    } finally {
      setDownloadingReceipt(false);
    }
  };

  // Dismiss success modal and notify parent of enrollment completion
  const handleDismissSuccess = () => {
    if (onSuccess && completedOrder) {
      onSuccess(completedOrder);
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-black/60 backdrop-blur-md animate-fadeIn font-sans selection:bg-[#c2410c] selection:text-white">
      {/* Main 2-Column Card (Stripe Payments Demo Style + Focotech Aesthetics) */}
      <div ref={dialogRef} role="dialog" aria-modal="true" aria-label="Course payment checkout" tabIndex={-1} className="relative w-full max-w-4xl bg-white rounded-3xl shadow-2xl border border-[#faeae0] overflow-hidden flex flex-col max-h-[92vh] animate-scaleUp">
        
        {/* Top Navbar */}
        <header className="px-6 py-4 border-b border-[#faeae0] flex items-center justify-between bg-white/95 backdrop-blur-md sticky top-0 z-20">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-[#9a3412] to-[#c2410c] flex items-center justify-center shadow-md shadow-[#ff5a36]/25">
              <span className="material-symbols-outlined text-white text-[19px]">lock</span>
            </div>
            <div>
              <p className="text-xs text-[#6d635c]">Simulation only. Do not use real financial details.</p>
              <div className="flex items-center gap-2">
                <span className="font-extrabold text-[15px] text-[#1e1916] leading-none">
                  Focotech Demo Checkout
                </span>
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={status === 'success' ? handleDismissSuccess : handleCloseCheckout}
            disabled={loadingOrder || status === 'processing'}
            aria-label="Close modal"
            className="w-9 h-9 rounded-full bg-[#fbf7f4] hover:bg-[#fff0eb] text-[#6d635c] hover:text-[#9a3412] flex items-center justify-center transition-all cursor-pointer"
          >
            <span className="material-symbols-outlined text-[19px]">close</span>
          </button>
        </header>

        {/* Modal Main Content */}
        <div className="overflow-y-auto flex-1">
          {status === 'success' ? (
            /* ========================================================
               SUCCESS CONFIRMATION SCREEN
               ======================================================== */
            <div className="py-14 px-6 text-center max-w-md mx-auto space-y-4">
              <div className="w-20 h-20 bg-emerald-50 text-emerald-600 border-2 border-emerald-200 rounded-full flex items-center justify-center mx-auto text-4xl shadow-lg shadow-emerald-500/15 animate-bounce">
                ✓
              </div>
              <div>
                <span className="px-3 py-1 rounded-full bg-emerald-100 text-emerald-800 text-xs font-bold uppercase tracking-wider">
                  Payment Verified
                </span>
                <h3 className="text-2xl font-black text-[#1e1916] mt-2">
                  Enrollment Confirmed!
                </h3>
                <p className="text-xs text-[#6d635c] mt-1.5 leading-relaxed">
                  Cryptographically verified transaction. You have gained full lifetime access to{' '}
                  <strong className="text-[#1e1916]">"{course.title}"</strong>.
                </p>
              </div>

              {completedOrder && (
                <div className="bg-[#fbf7f4] p-4 rounded-2xl border border-[#faeae0] text-left text-xs space-y-2 font-mono">
                  <div className="flex justify-between items-center pb-2 border-b border-[#faeae0]">
                    <span className="text-[#6d635c]">Invoice No:</span>
                    <span className="text-emerald-700 font-bold font-sans">
                      {completedOrder.invoiceNumber || 'INV-202611'}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#6d635c]">Transaction ID:</span>
                    <span className="text-[#9a3412] font-bold font-mono select-all">
                      {completedOrder.paymentId || completedOrder.orderId}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#6d635c]">Order Ref:</span>
                    <span className="text-[#6d635c] font-medium font-mono select-all">
                      {completedOrder.orderId}
                    </span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#6d635c]">Amount Paid:</span>
                    <span className="text-[#1e1916] font-bold text-sm">₹{completedOrder.amount || currentPayable}</span>
                  </div>
                  <div className="flex justify-between items-center">
                    <span className="text-[#6d635c]">Instrument:</span>
                    <span className="text-[#554a43] font-medium font-sans">
                      {completedOrder.paymentMethod || 'QR Payment'}
                    </span>
                  </div>
                </div>
              )}

              <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-2.5 flex-wrap">
                <button
                  type="button"
                  onClick={handleDownloadReceipt}
                  disabled={downloadingReceipt}
                  className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white text-xs font-bold transition shadow-sm flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-75"
                >
                  <span className="material-symbols-outlined text-[16px]">
                    {downloadingReceipt ? 'hourglass_top' : 'download'}
                  </span>
                  <span>{downloadingReceipt ? 'Downloading...' : 'Download Receipt'}</span>
                </button>
                <button
                  type="button"
                  onClick={handleDismissSuccess}
                  className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-[#c2410c] hover:bg-[#e5411d] text-white text-xs font-bold transition shadow-md shadow-[#ff5a36]/25 cursor-pointer flex items-center justify-center gap-1.5"
                >
                  <span>Start Learning Now</span>
                  <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                </button>
              </div>

              <div className="pt-3 border-t border-[#faeae0]/80 space-y-1.5">
                <p className="text-[11px] text-[#6d635c]">
                  A confirmation receipt has also been emailed to your registered address.
                </p>
                <div>
                  <button
                    type="button"
                    onClick={handleDismissSuccess}
                    className="text-[12px] text-[#6d635c] hover:text-[#1e1916] underline underline-offset-4 font-semibold cursor-pointer py-1 px-3 rounded-lg hover:bg-[#fbf7f4] transition"
                  >
                    Done / Close Window
                  </button>
                </div>
              </div>
            </div>
          ) : status === 'failed' ? (
            /* ========================================================
               FAILED / CANCELLED STATE SCREEN (RETURN TO ORIGINAL STATE)
               ======================================================== */
            <div className="py-14 px-6 text-center max-w-md mx-auto space-y-4">
              <div className="w-20 h-20 bg-rose-50 text-rose-600 border-2 border-rose-200 rounded-full flex items-center justify-center mx-auto text-3xl shadow-lg shadow-rose-500/15">
                ✕
              </div>
              <div>
                <span className="px-3 py-1 rounded-full bg-rose-100 text-rose-800 text-xs font-bold uppercase tracking-wider">
                  Payment Unsuccessful
                </span>
                <h3 className="text-2xl font-black text-[#1e1916] mt-2">
                  Transaction Cancelled
                </h3>
                <p className="text-xs text-[#6d635c] mt-1.5 leading-relaxed">
                  {errorMessage || 'The payment was not completed. No charges were made, and no enrollment was added to the database.'}
                </p>
              </div>

              <div className="bg-[#fbf7f4] p-3.5 rounded-2xl border border-[#faeae0] text-xs text-[#6d635c]">
                System state preserved: <strong className="text-[#1e1916]">Course remains unenrolled</strong>
              </div>

              <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
                <button
                  type="button"
                  onClick={handleCloseCheckout}
                  disabled={loadingOrder}
                  className="w-full sm:w-auto px-6 py-2.5 rounded-xl bg-neutral-900 hover:bg-black text-white text-xs font-bold transition shadow-sm cursor-pointer"
                >
                  Return to Course (Original State)
                </button>
                <button
                  type="button"
                  onClick={initCheckout}
                  disabled={loadingOrder}
                  className="w-full sm:w-auto px-5 py-2.5 rounded-xl border border-[#faeae0] bg-white hover:bg-[#fbf7f4] text-[#1e1916] text-xs font-bold transition cursor-pointer"
                >
                  Try Again
                </button>
              </div>
            </div>
          ) : status === 'processing' ? (
            /* ========================================================
               PROCESSING LOADER SCREEN
               ======================================================== */
            <div className="py-24 text-center px-6 space-y-4 max-w-sm mx-auto">
              <div className="w-14 h-14 border-4 border-[#ff5a36] border-t-transparent rounded-full animate-spin mx-auto shadow-sm"></div>
              <div>
                <h4 className="font-extrabold text-base text-[#1e1916]">{stepMessage}</h4>
                <p className="text-xs text-[#6d635c] mt-1 font-mono">
                  Real-time Database Settlement
                </p>
              </div>
            </div>
          ) : (
            /* ========================================================
               2-COLUMN SPLIT CHECKOUT (STRIPE PAYMENTS DEMO STYLE)
               ======================================================== */
            <div className="grid grid-cols-1 lg:grid-cols-12 min-h-[520px]">
              
              {/* ──────────────────────────────────────────────────────────
                  LEFT COLUMN: ORDER SUMMARY & ITEMIZED LEDGER
                  ────────────────────────────────────────────────────────── */}
              <aside className="lg:col-span-5 bg-[#fbf7f4] p-6 lg:p-7 border-b lg:border-b-0 lg:border-r border-[#faeae0] flex flex-col justify-between gap-6">
                <div className="flex flex-col gap-5">
                  {/* Summary Header */}
                  <div>
                    <span className="text-[11px] font-bold uppercase tracking-wider text-[#685b53] block">
                      PURCHASE SUMMARY
                    </span>
                    <h3 className="font-black text-xl text-[#1e1916] mt-0.5">
                      Order Breakdown
                    </h3>
                  </div>

                  {/* Course Item Card */}
                  <div className="bg-white p-4 rounded-2xl border border-[#faeae0] shadow-xs flex items-start gap-3.5">
                    <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-[#fff0eb] to-[#ffe5dc] text-[#9a3412] flex items-center justify-center shrink-0 font-bold text-lg shadow-2xs">
                      🎓
                    </div>
                    <div className="flex-1 min-w-0">
                      <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[10px] font-bold uppercase">
                        Verified Track
                      </span>
                      <h4 className="font-extrabold text-sm text-[#1e1916] mt-1 truncate">
                        {course.title}
                      </h4>
                      <p className="text-[11px] text-[#6d635c] mt-0.5">
                        Lifetime Access • assignments &amp; Certificate
                      </p>
                    </div>
                  </div>

                  {/* Promo Coupon Box */}
                  <div className="bg-white p-3.5 rounded-2xl border border-[#faeae0]">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-bold text-[#554a43]">Promotional Coupon</span>
                      <span className="text-[10px] font-mono text-[#9a3412] font-semibold">
                        Try: DIWALI50
                      </span>
                    </div>

                    {appliedCoupon ? (
                      <div className="flex items-center justify-between bg-emerald-50 border border-emerald-200 px-3 py-2 rounded-xl text-xs">
                        <span className="text-emerald-800 font-bold flex items-center gap-1.5">
                          <span>🏷️</span>
                          <span>Code '{appliedCoupon.code}' applied</span>
                        </span>
                        <button
                          type="button"
                          onClick={removeCoupon}
                          disabled={loadingOrder || couponLoading || !policyAccepted}
                          className="text-[#6d635c] hover:text-rose-600 font-bold text-xs cursor-pointer ml-2"
                        >
                          ✕
                        </button>
                      </div>
                    ) : (
                      <form onSubmit={handleApplyCoupon} className="flex gap-2">
                        <input
                          type="text"
                          placeholder="e.g. DIWALI50, FOCOTECH20"
                          value={couponCode}
                          onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                          className="flex-1 bg-[#fbf7f4] border border-[#faeae0] rounded-xl px-3 py-1.5 text-xs text-[#1e1916] uppercase font-mono font-semibold focus:outline-none focus:border-[#ff5a36] focus:bg-white transition"
                        />
                        <button
                          type="submit"
                          disabled={loadingOrder || couponLoading || !couponCode.trim()}
                          className="px-3.5 py-1.5 bg-[#1e1916] hover:bg-black text-white rounded-xl text-xs font-bold transition disabled:opacity-50 cursor-pointer"
                        >
                          {couponLoading ? '...' : 'Apply'}
                        </button>
                      </form>
                    )}
                    {couponError && <p className="text-[11px] text-rose-600 font-medium mt-1.5">{couponError}</p>}
                  </div>

                  {/* Virtual Wallet Split Deduction */}
                  {walletBalance > 0 && (
                    <div className="bg-white p-3.5 rounded-2xl border border-[#faeae0] flex items-center justify-between text-xs">
                      <label className="flex items-center gap-2.5 cursor-pointer select-none">
                        <input
                          type="checkbox"
                          checked={useWallet}
                          disabled={loadingOrder || couponLoading || !policyAccepted}
                          onChange={(e) => {
                            setLoadingOrder(true);
                            setUseWallet(e.target.checked);
                          }}
                          className="w-4 h-4 rounded border-[#faeae0] text-[#9a3412] focus:ring-0 cursor-pointer"
                        />
                        <div>
                          <span className="font-bold text-[#1e1916] block">Use Virtual Wallet Balance</span>
                          <span className="text-[11px] text-[#6d635c]">Available credit: ₹{walletBalance.toFixed(2)}</span>
                        </div>
                      </label>
                      {useWallet && (
                        <span className="font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-lg border border-emerald-200">
                          - ₹{walletAmountDeducted.toFixed(2)}
                        </span>
                      )}
                    </div>
                  )}

                  <section aria-label="Refund and cancellation policy" className="rounded-xl border p-3 text-xs space-y-2">
                    <h3 className="font-bold">Refund and cancellation policy</h3>
                    <p>{orderData?.refundPolicy || 'Full or partial refunds may be requested within 30 days. Approved simulated refunds restore the original sources and revoke course access. Exceptional refunds require admin approval. No real money moves.'}</p>
                    <p>Policy version: {orderData?.refundPolicyVersion || 'refund-2026-11-v1'}</p>
                    <label className="flex items-start gap-2"><input type="checkbox" checked={policyAccepted} disabled={status === 'processing'} onChange={e => setPolicyAccepted(e.target.checked)} /> I have read the refund policy and approve the displayed total.</label>
                  </section>
                  <section aria-label="Currency estimate" className="rounded-xl border p-3 text-xs space-y-2">
                    <label>Simulated gateway currency <select aria-label="Simulated gateway currency" disabled={useWallet || loadingOrder || status === 'processing'} value={displayCurrency} onChange={e => { setDisplayCurrency(e.target.value); setPolicyAccepted(false); }}>
                      {['INR','USD','EUR','GBP','AED','SGD'].map(code => <option key={code}>{code}</option>)}
                    </select></label>
                    <p>{fx ? `${displayCurrency} ${(Number(orderData?.totalAmount ?? basePrice) / Number(fx.ratesAgainstInr[displayCurrency])).toFixed(2)}` : 'Loading estimate…'}</p>
                    <p>Demo gateway charge: {orderData?.settlementCurrency || 'INR'} {Number(orderData?.settlementAmount ?? 0).toFixed(2)}</p>
                    <p>FX rounding in INR: {Number(orderData?.fxRoundingInr ?? 0).toFixed(4)}</p>
                    <p>Rates are simulated. The course ledger, wallet and invoice remain in INR. The quoted demo gateway charge below is in your selected currency.</p>
                  </section>
                  {/* Itemized Price Ledger */}
                  <div className="space-y-2 pt-2 border-t border-[#faeae0] text-xs">
                    <div className="flex justify-between text-[#6d635c]">
                      <span>Course List Price:</span>
                      <span className="font-semibold text-[#1e1916]">₹{catalogPrice.toFixed(2)}</span>
                    </div>

                    {discountAmount > 0 && (
                      <div className="flex justify-between text-emerald-700 font-semibold">
                        <span>Coupon Discount:</span>
                        <span>- ₹{discountAmount.toFixed(2)}</span>
                      </div>
                    )}

                    {walletAmountDeducted > 0 && (
                      <div className="flex justify-between text-emerald-700 font-semibold">
                        <span>Wallet Deduction:</span>
                        <span>- ₹{walletAmountDeducted.toFixed(2)}</span>
                      </div>
                    )}

                    <div className="flex justify-between text-[#6d635c]">
                      <span>Tax ({(Number(orderData?.taxRate ?? 0) * 100).toFixed(2)}% inclusive):</span>
                      <span className="font-mono">{orderData?.taxAmount == null ? 'Loading…' : `₹${Number(orderData.taxAmount).toFixed(2)}`}</span>
                    </div>

                    <div className="flex justify-between items-baseline pt-3 border-t border-[#faeae0] text-[#1e1916]">
                      <span className="font-black text-sm uppercase tracking-wide">Total Payable:</span>
                      <div className="text-right">
                        <span className="text-2xl font-black text-[#9a3412]">₹{currentPayable}</span>
                        <span className="text-[10px] text-[#6d635c] block font-medium">INR Currency</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Trust Seals */}
                <div className="pt-4 border-t border-[#faeae0] flex items-center justify-end text-[11px] text-[#6d635c]">
                  <span className="flex items-center gap-1 font-semibold text-[#6d635c]">
                    <span>Local payment simulation</span>
                  </span>
                </div>
              </aside>

              {/* ──────────────────────────────────────────────────────────
                  RIGHT COLUMN: PAYMENT INSTRUMENTS & STRIPE CHECKOUT
                  ────────────────────────────────────────────────────────── */}
              <main className="lg:col-span-7 p-6 lg:p-7 flex flex-col justify-between gap-6 bg-white">
                <div className="flex flex-col gap-5">
                  {/* Right Header */}
                  <div>
                    <span className="text-[11px] font-bold uppercase tracking-wider text-[#685b53] block">
                      PAYMENT SELECTION
                    </span>
                    <h3 className="font-black text-xl text-[#1e1916] mt-0.5">
                      Choose Your Payment Instrument
                    </h3>
                  </div>

                  {/* Top Segmented Navigation Tabs */}
                  <div className="grid grid-cols-4 gap-1.5 p-1 bg-[#fbf7f4] rounded-2xl border border-[#faeae0] text-xs font-bold">
                    <button
                      type="button"
                      onClick={() => setActiveTab('stripe')}
                      className={`py-2 px-1 rounded-xl transition flex flex-col items-center gap-1 cursor-pointer ${
                        activeTab === 'stripe'
                          ? 'bg-[#c2410c] text-white shadow-sm'
                          : 'text-[#6d635c] hover:text-[#9a3412]'
                      }`}
                    >
                      <span className="text-[14px]">⚡</span>
                      <span className="text-[11px]">Demo provider</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setActiveTab('upi_qr')}
                      className={`py-2 px-1 rounded-xl transition flex flex-col items-center gap-1 cursor-pointer ${
                        activeTab === 'upi_qr'
                          ? 'bg-[#c2410c] text-white shadow-sm'
                          : 'text-[#6d635c] hover:text-[#9a3412]'
                      }`}
                    >
                      <span className="text-[14px]">📱</span>
                      <span className="text-[11px]">UPI / QR</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setActiveTab('card')}
                      className={`py-2 px-1 rounded-xl transition flex flex-col items-center gap-1 cursor-pointer ${
                        activeTab === 'card'
                          ? 'bg-[#c2410c] text-white shadow-sm'
                          : 'text-[#6d635c] hover:text-[#9a3412]'
                      }`}
                    >
                      <span className="text-[14px]">💳</span>
                      <span className="text-[11px]">Card</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => setActiveTab('netbanking')}
                      className={`py-2 px-1 rounded-xl transition flex flex-col items-center gap-1 cursor-pointer ${
                        activeTab === 'netbanking'
                          ? 'bg-[#c2410c] text-white shadow-sm'
                          : 'text-[#6d635c] hover:text-[#9a3412]'
                      }`}
                    >
                      <span className="text-[14px]">🏦</span>
                      <span className="text-[11px]">NetBank</span>
                    </button>
                  </div>

                  {/* Error Notification */}
                  {errorMessage && (
                    <div className="p-3 bg-rose-50 border border-rose-200 text-rose-700 rounded-2xl text-xs font-medium flex items-center gap-2">
                      <span className="material-symbols-outlined text-[18px]">error</span>
                      <span>{errorMessage}</span>
                    </div>
                  )}

                  {/* ──────────────────────────────────────────────────────────
                      TAB 1: SIMULATED CHECKOUT
                      ────────────────────────────────────────────────────────── */}
                  {activeTab === 'stripe' && (
                    <div className="bg-[#fdfaf8] p-5 rounded-2xl border border-[#faeae0] flex flex-col gap-4">
                      <div>
                        <label className="block text-[11px] font-bold text-[#554a43] uppercase tracking-wider mb-2">
                          Accepted Payment Instruments
                        </label>
                        <div className="grid grid-cols-3 gap-2.5">
                          <div className="p-3 rounded-xl border border-[#faeae0] bg-white text-center flex flex-col items-center justify-center gap-1 shadow-2xs">
                            <span className="text-xl">💳</span>
                            <span className="text-[11px] font-bold text-[#1e1916]">All Cards</span>
                            <span className="text-[10px] text-[#6d635c]">Visa, MC, RuPay</span>
                          </div>
                          <div className="p-3 rounded-xl border border-[#faeae0] bg-white text-center flex flex-col items-center justify-center gap-1 shadow-2xs">
                            <span className="text-xl">🍎</span>
                            <span className="text-[11px] font-bold text-[#1e1916]">Apple Pay</span>
                            <span className="text-[10px] text-[#6d635c]">1-Touch ID</span>
                          </div>
                          <div className="p-3 rounded-xl border border-[#faeae0] bg-white text-center flex flex-col items-center justify-center gap-1 shadow-2xs">
                            <span className="text-xl">🌐</span>
                            <span className="text-[11px] font-bold text-[#1e1916]">Google Pay</span>
                            <span className="text-[10px] text-[#6d635c]">Instant Pay</span>
                          </div>
                        </div>
                      </div>

                      <div className="bg-white p-3 rounded-xl border border-[#faeae0] flex items-center justify-between text-xs">
                        <div className="flex items-center gap-2">
                          <span className="material-symbols-outlined text-emerald-600 text-[18px]">verified</span>
                          <span className="text-[#554a43] font-medium">Local provider simulation</span>
                        </div>
                        <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 text-[10px] font-bold">
                          Ready
                        </span>
                      </div>

                      <button
                        type="button"
                        onClick={handleStripeCheckout}
                        disabled={loadingOrder || status === 'processing' || !policyAccepted}
                        className="w-full py-3 rounded-2xl bg-[#c2410c] hover:bg-[#e5411d] text-white font-extrabold text-sm shadow-lg shadow-[#ff5a36]/25 transition flex items-center justify-center gap-2 cursor-pointer hover:scale-[1.01] active:scale-[0.99]"
                      >
                        <span className="material-symbols-outlined text-[18px]">lock</span>
                        <span>Simulate payment (₹{currentPayable})</span>
                      </button>
                    </div>
                  )}

                  {/* ──────────────────────────────────────────────────────────
                      TAB 2: DYNAMIC NPCI UPI QR CODE
                      ────────────────────────────────────────────────────────── */}
                  {activeTab === 'upi_qr' && (
                    <div className="bg-[#fdfaf8] p-5 rounded-2xl border border-[#faeae0] text-center flex flex-col items-center gap-3">
                      {loadingOrder ? (
                        <div className="py-12 flex flex-col items-center gap-2">
                          <div className="w-8 h-8 border-2 border-[#ff5a36] border-t-transparent rounded-full animate-spin"></div>
                          <p className="text-xs text-[#6d635c]">Generating demo UPI QR code...</p>
                        </div>
                      ) : qrCodeData?.qrBase64 ? (
                        <>
                          <div className="bg-white p-3 rounded-2xl border border-[#faeae0] shadow-sm inline-block">
                            <img
                              src={qrCodeData.qrBase64}
                              alt="Simulated UPI payment QR code"
                              className="w-44 h-44 object-contain rounded-xl"
                            />
                          </div>

                          <div>
                            <div className="flex items-center justify-center gap-2">
                              <span className="text-xs font-extrabold text-[#1e1916]">Demo UPI intent — no real payment</span>
                              <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-800 border border-amber-200 text-[10px] font-bold">
                                10m Timer
                              </span>
                            </div>
                            <div className="text-center mt-1">
                              <span className="text-base font-black text-[#9a3412]">₹{currentPayable}</span>
                              {discountAmount > 0 && (
                                <span className="ml-2 text-xs font-medium text-[#6d635c] line-through">₹{catalogPrice.toFixed(2)}</span>
                              )}
                            </div>
                            <p className="text-[11px] text-[#6d635c] font-mono mt-0.5">
                              Payee: <strong className="text-[#1e1916]">Online Learning Platform</strong> • Ref: {orderData?.orderId}
                            </p>
                          </div>

                          <div className="w-full max-w-sm flex flex-col gap-2">
                            <button
                              type="button"
                              onClick={handleSimulateQrScan}
                              disabled={loadingOrder || !policyAccepted}
                              className="w-full py-2.5 px-4 rounded-xl bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs shadow-md shadow-emerald-600/20 transition flex items-center justify-center gap-1.5 cursor-pointer hover:scale-[1.01]"
                            >
                              <span className="material-symbols-outlined text-[16px]">check_circle</span>
                              <span>Simulate Payment Success (Add to DB &amp; Return)</span>
                            </button>

                            <button
                              type="button"
                              onClick={handleSimulateFailure}
                              className="w-full py-2 px-4 rounded-xl bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-700 font-bold text-xs transition flex items-center justify-center gap-1.5 cursor-pointer"
                            >
                              <span className="material-symbols-outlined text-[16px]">cancel</span>
                              <span>Simulate Payment Failure (Return to Original)</span>
                            </button>
                          </div>
                        </>
                      ) : (
                        <p className="text-xs text-[#6d635c] py-8">QR Code currently unavailable.</p>
                      )}
                    </div>
                  )}

                  {/* ──────────────────────────────────────────────────────────
                      TAB 3: CREDIT / DEBIT CARD (STRIPE PAYMENTS DEMO STYLE)
                      ────────────────────────────────────────────────────────── */}
                  {activeTab === 'card' && (
                    <div className="bg-[#fdfaf8] p-5 rounded-2xl border border-[#faeae0] flex flex-col gap-4">
                      {/* Single-line styled Stripe Elements Card container */}
                      <div className="flex flex-col gap-3">
                        <div>
                          <label className="block text-[11px] font-bold text-[#554a43] uppercase tracking-wider mb-1">
                            Card Information
                          </label>
                          <div className="bg-white border border-[#faeae0] rounded-xl overflow-hidden shadow-2xs focus-within:border-[#ff5a36] focus-within:ring-2 focus-within:ring-[#ff5a36]/10 transition">
                            {/* Card Number */}
                            <div className="flex items-center px-3.5 py-2.5 border-b border-[#faeae0]">
                              <input
                                type="text"
                                placeholder="1234 5678 9012 3456"
                                aria-label="Demo card number"
                                value={cardNumber}
                                readOnly
                                disabled
                                onChange={(e) => setCardNumber(e.target.value)}
                                className="w-full text-xs font-mono font-semibold text-[#1e1916] focus:outline-none bg-transparent"
                              />
                              <span className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${getCardBrand(cardNumber).color}`}>
                                {getCardBrand(cardNumber).name}
                              </span>
                            </div>

                            {/* Expiry & CVC */}
                            <div className="grid grid-cols-2 divide-x divide-[#faeae0]">
                              <input
                                type="text"
                                placeholder="MM / YY"
                                aria-label="Demo card expiry"
                                value={cardExpiry}
                                readOnly
                                disabled
                                onChange={(e) => setCardExpiry(e.target.value)}
                                className="px-3.5 py-2.5 text-xs font-mono font-semibold text-[#1e1916] focus:outline-none bg-transparent"
                              />
                              <input
                                type="password"
                                placeholder="CVC"
                                maxLength={4}
                                aria-label="Demo CVC"
                                value={cardCvv}
                                readOnly
                                disabled
                                onChange={(e) => setCardCvv(e.target.value)}
                                className="px-3.5 py-2.5 text-xs font-mono font-semibold text-[#1e1916] focus:outline-none bg-transparent"
                              />
                            </div>
                          </div>
                        </div>

                        {/* Name and Postal Code */}
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                          <div>
                            <label className="block text-[11px] font-bold text-[#554a43] uppercase tracking-wider mb-1">
                              Cardholder Name
                            </label>
                            <input
                              type="text"
                              aria-label="Demo cardholder name"
                                value={cardName}
                                readOnly
                                disabled
                              onChange={(e) => setCardName(e.target.value)}
                              className="w-full bg-white border border-[#faeae0] rounded-xl px-3 py-2 text-xs font-semibold text-[#1e1916] focus:outline-none focus:border-[#ff5a36] shadow-2xs"
                            />
                          </div>
                          <div>
                            <label className="block text-[11px] font-bold text-[#554a43] uppercase tracking-wider mb-1">
                              PIN / Postal Code
                            </label>
                            <input
                              type="text"
                              aria-label="Demo postal code"
                                value={postalCode}
                                readOnly
                                disabled
                              onChange={(e) => setPostalCode(e.target.value)}
                              className="w-full bg-white border border-[#faeae0] rounded-xl px-3 py-2 text-xs font-mono font-semibold text-[#1e1916] focus:outline-none focus:border-[#ff5a36] shadow-2xs"
                            />
                          </div>
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={handleGatewayPay}
                        disabled={loadingOrder || status === 'processing' || !policyAccepted}
                        className="w-full py-3 rounded-2xl bg-[#c2410c] hover:bg-[#e5411d] text-white font-extrabold text-sm shadow-lg shadow-[#ff5a36]/25 transition flex items-center justify-center gap-2 cursor-pointer hover:scale-[1.01] active:scale-[0.99]"
                      >
                        <span className="material-symbols-outlined text-[18px]">lock</span>
                        <span>Pay ₹{currentPayable} &amp; Enroll</span>
                      </button>
                    </div>
                  )}

                  {/* ──────────────────────────────────────────────────────────
                      TAB 4: INDIAN NETBANKING GRID
                      ────────────────────────────────────────────────────────── */}
                  {activeTab === 'netbanking' && (
                    <div className="bg-[#fdfaf8] p-5 rounded-2xl border border-[#faeae0] flex flex-col gap-4">
                      <div>
                        <label className="block text-[11px] font-bold text-[#554a43] uppercase tracking-wider mb-2">
                          Select Verified Bank
                        </label>
                        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2.5">
                          {['HDFC Bank', 'ICICI Bank', 'State Bank of India', 'Axis Bank', 'Kotak Mahindra', 'Punjab National'].map((b) => (
                            <button
                              key={b}
                              type="button"
                              onClick={() => setSelectedBank(b)}
                              className={`p-2.5 rounded-xl border text-left transition font-semibold text-xs cursor-pointer flex items-center gap-2 ${
                                selectedBank === b
                                  ? 'bg-[#fff0eb] border-[#ff5a36] text-[#9a3412] shadow-xs'
                                  : 'bg-white border-[#faeae0] text-[#554a43] hover:bg-[#fbf7f4]'
                              }`}
                            >
                              <span className="material-symbols-outlined text-[16px]">account_balance</span>
                              <span className="truncate">{b}</span>
                            </button>
                          ))}
                        </div>
                      </div>

                      <button
                        type="button"
                        onClick={handleGatewayPay}
                        disabled={loadingOrder || status === 'processing' || !policyAccepted}
                        className="w-full py-3 rounded-2xl bg-[#c2410c] hover:bg-[#e5411d] text-white font-extrabold text-sm shadow-lg shadow-[#ff5a36]/25 transition flex items-center justify-center gap-2 cursor-pointer hover:scale-[1.01] active:scale-[0.99]"
                      >
                        <span className="material-symbols-outlined text-[18px]">lock</span>
                        <span>Pay via {selectedBank} (₹{currentPayable})</span>
                      </button>
                    </div>
                  )}
                </div>


              </main>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
