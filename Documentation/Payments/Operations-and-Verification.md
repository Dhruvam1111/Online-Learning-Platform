# Payment simulation operations and verification

This subsystem models payment processing. It does not transfer money, certify PCI compliance, or establish real-bank/RBI/GST compliance. Use demo payment methods only. Checkout settles in INR; FX values remain explicitly labelled display estimates.

## Apply schema safely

1. Review `db/migrations/20261110-payment-hardening.sql` with the team.
2. Back up the development database; apply first to a disposable copy, then the approved development branch.
3. Apply the migration with `psql -v ON_ERROR_STOP=1 -f db/migrations/20261110-payment-hardening.sql`. Supply the connection through the normal secure environment, never a committed password.
4. Start Spring Boot with `spring.jpa.hibernate.ddl-auto=validate`. Never use update/create against the shared database.
5. For fresh databases, follow 01, 02, 03, 04 and the incremental payment migrations. The hardening migration is idempotent.

Financial history and webhook journal reject UPDATE, DELETE and TRUNCATE. Corrections must be new entries. Indefinite online retention exceeds the document's minimum periods but is not a backup strategy. Database owners/superusers can bypass triggers: production must use a separately provisioned restricted runtime role, prohibit schema/trigger modification, enable audited operator access and keep encrypted offsite backups. Never run drop-all/seed reset scripts against a populated shared database.

## Financial policy

`GET /api/payments/policy` reads the current policy; administrator-only `PUT` accepts `commissionRate`, `taxRate` (fractions between 0 and 1), and `defaultPrice` in INR. Default commission/tax remain 0.20/0.18. Zero tax models an exemption. Special course tiers retain their tier prices. A journal snapshot is taken when each order is created. Settlement and invoices use that snapshot, even if an administrator later changes the policy. Legacy orders keep the historical 20%/18% assumptions.

## Gateway outage and uncertain outcomes

- Gateway order/refund operations are behind `PaymentGateway`. The local adapter only generates simulated references. A future real adapter must implement provider idempotency, timeouts, status lookup and verified callback translation before enabling real money.
- If order creation fails, let the exception roll back the transaction, including wallet reservation. Do not grant enrollment.
- If the customer abandons a pending order or a provider cannot be reached, keep enrollment absent. The expiry job locks each pending order, expires it after 15 minutes and restores reserved wallet funds once.
- Never blindly repeat a debit for an uncertain real-provider result. Query the provider by idempotent order reference and reconcile; require manual review for unresolved mismatches.
- A late success for an expired simulator order cannot create enrollment. For a future real provider, operator resolution must refund the confirmed external debit; local expiry alone is not evidence that real cash was reversed.
- Run `POST /api/payments/maintenance/run` as administrator to scan expiry, prepare notices and record the current day's reconciliation. Students receive 403.
- Review `/api/payments/reconciliation/report` and the governance audit daily. Resolve discrepancies with compensating entries; never edit historical journal rows.

## Jobs and multi-instance operation

After migration, explicitly configure `payments.jobs.enabled=true`. This enables scheduling, including the existing order-expiry scan. Subscription notices scan once per minute; reconciliation runs at 01:00 UTC. PostgreSQL row locks serialize subscription approval/cancellation and expiry. An advisory transaction lock plus daily journal identity prevents duplicate daily reconciliation reports across application instances.

Configure the same strong `payments.webhook-secret` on every instance. The random per-process fallback is only for a single-instance local demo. TLS termination, secure secrets, connection pooling, deployment health checks and load-balancer configuration require deployment verification.

## Monthly subscriptions

The `#payments` page provides creation, durable notices, quote review, approval and cancellation. A saved simulated method and explicit consent are required. The initial renewal is 30 days later. Notices are persisted to the student's subscription inbox; approving a due renewal requires at least 24 hours since its notice, the current quoted amount, and an idempotent request ID. Each debit requires a fresh authenticated explicit approval. Revoked/expired methods cannot pay. Cancellation is serialized with renewal and stops future renewals. Repeated approval with the same request ID returns the original result. No unapproved unattended debit occurs.

This is recurring simulator billing for course access, not a real e-mandate integration or time-limited membership product. Cancellation stops billing; purchased course access follows the existing course/refund model.

## Retention and restoration acceptance

Retain financial histories online indefinitely until a reviewed retention policy is introduced. Back up daily to encrypted, access-controlled storage with retention matching the requirements document, and test restoration quarterly into a disposable database. Keep audit histories, invoice policy snapshots, payment/refund/payout references and dispute evidence together. Operations must supply actual backup/restore evidence; source code does not prove seven years of retention or one year of immutable storage.

## Evidence still required outside a local demo

Monthly uptime, horizontally scaled deployment, deployed TLS, all required browsers, WCAG AA, 100,000-record dashboard performance and legal/provider certifications require separate evidence. Record measured test results rather than asserting them from code structure. Full configurable tax jurisdiction handling, statutory instructor deductions, verified bank ownership and foreign-currency settlement remain outside this patch.
