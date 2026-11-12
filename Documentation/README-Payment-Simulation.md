# Payment simulation completion

This implements the professor-approved local simulator. No real card details, banking credentials, provider onboarding or real transfers are needed.

## Using it

Start the backend and active Vite frontend as usual, sign in, then open the frontend with `#payments` appended (normally `http://localhost:5173/#payments`). A Payment simulation link also appears on the signed-in home page.

Students can create a course purchase/renewal, explicitly consent to saving a demo method, pay an order using that method, reuse it on another order, and revoke it. Renewals are explicitly authorized by the user; the simulator does not debit accounts automatically. Tokens and consent/revocation records survive restarts in the existing event log. The saved-method API never asks for PAN, CVV, card expiry or bank details.

Students can request a full or partial asynchronous refund. The UI displays its pending reference. Sign in as admin to simulate processed/failed refund outcomes, payout processing/success/failure/delay, and chargeback opening/resolution. Use the order reference plus the displayed refund reference or instructor earning ID from the existing earnings report. All events are signed on the server; admin controls never expose the secret.

## Implemented behavior

- FR-PG10: opaque saved tokens with explicit consent, owner checks, expiry, revocation and repeat/renewal payment of an existing order. Retrying an order is idempotent.
- FR-PG06/08/13/14: asynchronous refund/payout/dispute state updates, terminal-state protection, refund wallet restoration, payout holds and dispute resolution. A lost dispute revokes course access. Original event payloads remain in the event log.
- FR-PG15 / NFR-16: HMAC-SHA256 verification of exact request bytes and timestamp; missing/forged/expired signatures are rejected before database writes. Invalid event schemas are rejected. Conflicting event-ID reuse is rejected; identical delivery is idempotent.
- Payment ownership checks on confirmation/failure/status/QR routes; authenticated transaction history; admin-only reward and event controls; demo-account impersonation removed.
- Wallet debit/reservation and credit serialize using the student row, including first wallet creation. Callback/status/checkout paths lock the payment row. Scheduled stale-order cleanup runs every 60 seconds and releases the wallet once.
- Wallet-funded invoice totals and earnings, full/partial refunds, and previous regression fixes are retained.

## API reference

Authenticated student routes:

- GET/POST `/api/payments/methods`; creation body `{"method":"card","consent":true}`. Methods: card, upi, netbanking, wallet. These are demo instruments.
- DELETE `/api/payments/methods/{token}`.
- POST `/api/payments/methods/{token}/pay` with `{"orderId":"OLP-..."}`.
- POST `/api/payments/refunds/request` with `{"paymentId":"OLP-...","amount":null,"reason":"Cancellation"}`. Null amount requests a full refund.

Admin route: POST `/api/payments/simulator/events` with a JSON event containing unique `event_id`, `order_id` and `event`.
Supported events: `payment.confirmed`, `payment.failed`, `payment.pending`; `refund.pending`, `refund.processed`, `refund.failed`; `payout.processing`, `payout.successful`, `payout.failed`, `payout.delayed`; `chargeback.opened`, `chargeback.resolved`.
Refund events also require `refund_id`; payout events require numeric `earning_id`; chargeback resolution requires `outcome` equal to `won` or `lost`.

External local test senders can POST the same JSON to `/api/payments/webhook`, providing `X-Razorpay-Signature: t=<unix-seconds>,v1=<hex-hmac>`. Sign `<unix-seconds>.<exact-raw-body>` using HMAC-SHA256. The accepted timestamp window is five minutes. Despite the legacy header name, this is the simulator protocol, not a claim of Razorpay protocol compatibility.

Set `PAYMENTS_WEBHOOK_SECRET` to a private value of at least 32 bytes for a stable shared local signing key. If unset, a secure ephemeral key is generated at application startup and the in-process admin simulator still works. Never put this key into frontend code or commit it.

## Validation and database safety

Full backend tests run against disposable H2, including the earlier regression cases, genuine database concurrency checks, signed events, token consent/ownership/revocation, and Spring Security route checks. The frontend build is checked. Live Neon and real banking systems are not used for validation. Browser interaction has not been exhaustively tested.

No new tables/columns or shared database migration are introduced by this payment work. Existing local OTP changes and their separate migration requirements are unchanged. The existing database must already contain the payment, wallet, refund, earnings and event tables.

These features complete the identified simulated-payment feature gaps; this is not a certification of every platform FR/NFR, real provider compliance, or production deployment readiness. Backend logs and exact test counts are in the accompanying completion report.
