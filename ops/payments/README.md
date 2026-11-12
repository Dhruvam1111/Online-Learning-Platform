# Payment subsystem operations

These tools implement deployment and verification mechanisms. They do not certify a real payment provider, prove a month of uptime from a short test, or install themselves on shared infrastructure.

## Database activation and audit protection

Apply the reviewed payment migrations in date order to a disposable database first, then the approved Neon development branch. Keep Hibernate at `validate`. `20261111-payment-completion.sql` adds pagination indexes and hardens the audit trigger against temporary-table shadowing. The ledger writer runs with its protected owner's permissions and validates the source table. Runtime connections must not own tables or inherit the owner role.

`db/scripts/restrict-financial-runtime.psql` prepares a NOLOGIN privilege role. Review PUBLIC grants, provision a separate application login securely, and give it only that role's privileges. Never put a password into SQL files. This step is deliberately not automatically applied to shared Neon.

## Scheduled work

Set `payments.provider=simulation`, `payments.jobs.enabled=true`, and `payments.require-shared-secret=true`. Supply the same high-entropy `PAYMENTS_WEBHOOK_SECRET` and JWT secret to every instance. Do not use the random per-process signing fallback in a multi-instance deployment.

The application scans order expiry and subscription notices, and records daily simulated reconciliation. PostgreSQL locks make repeated scans and callbacks safe across instances. A verified late payment confirmation for an expired/failed order is reversed to its simulated source without granting enrollment or revoking another purchase's access.

## TLS, rolling updates and scaling

`deployment.example.yaml` defines two backend/frontend replicas, readiness/liveness probes, rolling updates, CPU autoscaling, TLS ingress, daily encrypted backup and minute-by-minute readiness observations. Image names, real DNS, ingress configuration, storage and secret references must be reviewed/provisioned. It is not an already deployed environment. `nginx-tls.example.conf` restricts transport to TLS 1.2/1.3 and redirects HTTP to HTTPS. Use a real certificate for deployment; never bypass a browser certificate warning.

Provision a Kubernetes metrics server for autoscaling and confirm database connection-pool limits across replicas. Configure readiness to include the database. Health endpoints expose status only; other actuator endpoints remain authenticated/admin-restricted. Run rolling-update and load tests on the actual deployment before claiming its availability/scaling guarantees.

## Encrypted backup and restoration

Prerequisites: Python 3.10+, PostgreSQL client tools matching/newer than the server, and OpenSSL. No Python package installation is required. Connections use standard PGHOST, PGPORT, PGDATABASE, PGUSER and optionally PGPASSWORD environment variables from secret storage.

Set `OLP_BACKUP_KEY` to a securely generated base64-encoded 32-byte secret. Keep a separately protected recovery copy; losing it makes restoration impossible. Never commit it, shell-history it, or store it beside the backup. Archives use AES-256-CBC/PBKDF2 plus an independently derived HMAC over metadata and the encrypted-file checksum; authentication is verified before decryption.

```sh
python3 ops/payments/finance_backup.py backup --directory /secure/backups --assets /srv/course-assets --retention-days 2557
python3 ops/payments/finance_backup.py verify --archive /secure/backups/finance-REPLACE.enc --restore-database olp_dr_restore_check --restore-assets /new/empty/restored-assets --report restore-report.json
python3 ops/payments/finance_backup.py prune --directory /secure/backups
```

`verify` is restricted to an explicitly configured local database and a new name ending in `_restore_check`. It never overwrites or drops an existing database. Asset destinations must be new. `prune` rejects unauthenticated manifests and never deletes before the signed retention deadline. Administrators may configure a longer retention period; do not shorten a legal hold. Backups and their matching manifests must keep their filenames.

The daily job is a deployment example. Provision encrypted offsite/durable storage and monitor failures. External video/CDN objects require their provider's backup/export mechanism; backing up lecture URLs is not backing up videos. Test full service restoration, secrets/config recovery and asset playback on representative data. A database/assets restore time alone does not prove the complete platform's four-hour RTO.

## Monthly availability evidence

```sh
python3 ops/payments/availability_probe.py --url https://YOUR-HOST/actuator/health/readiness --log availability.jsonl
python3 ops/payments/availability_probe.py --log availability.jsonl --summary-month 2026-11 --max-gap 120 --report availability-report.json
```

Collect once per minute using the deployment job. Optionally pass `--maintenance` containing approved UTC `start`/`end` windows. Overlapping maintenance windows are merged, monitoring gaps are reported as unknown, and an incomplete month cannot establish the SLA. Synthetic test logs prove the calculation only. Keep actual monitoring logs and alert on readiness failures, missing samples and backup failures.

## Financial workflows

- `/api/payments/policy` and `/courses/{id}` manage commission, course prices and customer tax. Order snapshots preserve historical calculations.
- `/api/payments/governance/policy` configures minimum retention, simulated withholding and independent platform-commission tax rates. Tax/withholding applicability is an administrator-selected simulation policy, not a legal determination.
- Instructors save masked payout accounts; administrators review simulated ownership/identity. Updates invalidate prior verification. Real bank/KYC verification requires a provider implementation.
- Both admin settlement and instructor withdrawal use the verified account and generate transfer references, withholding records and downloadable statements.
- Customer receipts, platform commission invoices and payout statements explicitly identify their simulation scope.
- Checkout quotes server tax, refund policy, selected-currency provider amount and FX rounding. Accounting/wallets/invoices remain INR. Refunds use the original provider-currency quote.
- Dispute evidence retains policy acceptance, enrollment snapshots and successful course-access events; administrative access is required to retrieve it.
- Financial views use bounded server pagination. Historical administrator requests, selected response IDs and outcomes are retained without storing request bodies, raw bank account numbers or credentials.

## Verification scope

Run backend tests, the payment-coverage script, frontend build and `python3 -m unittest discover -s ops/payments -p 'test_*.py'`. Browser/device/accessibility and real-provider certifications require their own evidence. Guardian/DOB handling stays with the shared signup work as previously agreed. Whole-platform privacy/deletion, examination/video/AI requirements are not certified by payment tests.
