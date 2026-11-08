-- ============================================================
-- Online Learning Platform — Enterprise Auth, Payments & Enhancements
-- Schema migration for two_factor_tokens, payments, refunds, wallets, coupons
-- ============================================================

-- =========================
-- TABLE: two_factor_tokens
-- =========================
CREATE TABLE IF NOT EXISTS two_factor_tokens (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  INTEGER NOT NULL,
    otp_code                 VARCHAR(6) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    expires_at               TIMESTAMP NOT NULL,
    is_used                  BOOLEAN NOT NULL DEFAULT false,
    attempt_count            INTEGER NOT NULL DEFAULT 0,
    purpose                  VARCHAR(50) NOT NULL DEFAULT 'PASSWORD_RESET'::character varying,
    CONSTRAINT two_factor_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: coupons
-- =========================
CREATE TABLE IF NOT EXISTS coupons (
    id                       SERIAL PRIMARY KEY,
    code                     VARCHAR(50) NOT NULL,
    discount_percentage      NUMERIC(5, 2) NOT NULL,
    max_discount_amount      NUMERIC(10, 2),
    min_order_amount         NUMERIC(10, 2) DEFAULT 0,
    valid_until              TIMESTAMP NOT NULL,
    usage_limit              INTEGER DEFAULT 1000,
    times_used               INTEGER DEFAULT 0,
    is_active                BOOLEAN DEFAULT true,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT coupons_code_key UNIQUE (code)
);

-- =========================
-- TABLE: payments
-- =========================
CREATE TABLE IF NOT EXISTS payments (
    id                       SERIAL PRIMARY KEY,
    order_id                 VARCHAR(100) NOT NULL,
    payment_id               VARCHAR(100),
    signature                VARCHAR(255),
    student_id               INTEGER NOT NULL,
    course_id                INTEGER NOT NULL,
    amount                   NUMERIC(10, 2) NOT NULL,
    currency                 VARCHAR(10) NOT NULL DEFAULT 'INR'::character varying,
    payment_method           VARCHAR(50) DEFAULT 'UPI'::character varying,
    status                   VARCHAR(20) NOT NULL DEFAULT 'created'::character varying,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    refund_status            VARCHAR(20) DEFAULT 'none'::character varying,
    refund_amount            NUMERIC(10, 2) DEFAULT 0,
    refund_reason            TEXT,
    invoice_number           VARCHAR(100),
    failure_reason           TEXT,
    platform_fee             NUMERIC(10, 2) DEFAULT 0,
    instructor_earning       NUMERIC(10, 2) DEFAULT 0,
    risk_score               INTEGER DEFAULT 10,
    coupon_code              VARCHAR(50),
    discount_amount          NUMERIC(10, 2) DEFAULT 0,
    wallet_amount_used       NUMERIC(10, 2) DEFAULT 0,
    CONSTRAINT payments_order_id_key UNIQUE (order_id),
    CONSTRAINT payments_course_id_fkey FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT payments_student_id_fkey FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: refunds
-- =========================
CREATE TABLE IF NOT EXISTS refunds (
    id                       SERIAL PRIMARY KEY,
    payment_id               INTEGER NOT NULL,
    razorpay_refund_id       VARCHAR(100) NOT NULL,
    amount                   NUMERIC(10, 2) NOT NULL,
    status                   VARCHAR(20) NOT NULL DEFAULT 'processed'::character varying,
    reason                   TEXT,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT refunds_razorpay_refund_id_key UNIQUE (razorpay_refund_id),
    CONSTRAINT refunds_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: coupon_usages
-- =========================
CREATE TABLE IF NOT EXISTS coupon_usages (
    id                       SERIAL PRIMARY KEY,
    coupon_id                INTEGER NOT NULL,
    student_id               INTEGER NOT NULL,
    order_id                 VARCHAR(100) NOT NULL,
    discount_applied         NUMERIC(10, 2) NOT NULL,
    used_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT coupon_usages_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT coupon_usages_student_id_fkey FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: wallets
-- =========================
CREATE TABLE IF NOT EXISTS wallets (
    id                       SERIAL PRIMARY KEY,
    student_id               INTEGER NOT NULL,
    balance                  NUMERIC(10, 2) NOT NULL DEFAULT 0,
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT wallets_student_id_key UNIQUE (student_id),
    CONSTRAINT wallets_student_id_fkey FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: wallet_transactions
-- =========================
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id                       SERIAL PRIMARY KEY,
    wallet_id                INTEGER NOT NULL,
    amount                   NUMERIC(10, 2) NOT NULL,
    type                     VARCHAR(20) NOT NULL,
    description              VARCHAR(255) NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT wallet_transactions_wallet_id_fkey FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: instructor_earnings
-- =========================
CREATE TABLE IF NOT EXISTS instructor_earnings (
    id                       SERIAL PRIMARY KEY,
    instructor_id            INTEGER NOT NULL,
    course_id                INTEGER NOT NULL,
    payment_id               INTEGER NOT NULL,
    gross_amount             NUMERIC(10, 2) NOT NULL,
    platform_commission      NUMERIC(10, 2) NOT NULL,
    net_instructor_earning   NUMERIC(10, 2) NOT NULL,
    payout_status            VARCHAR(20) NOT NULL DEFAULT 'unpaid'::character varying,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT instructor_earnings_course_id_fkey FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT instructor_earnings_instructor_id_fkey FOREIGN KEY (instructor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT instructor_earnings_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: lecture_materials
-- =========================
CREATE TABLE IF NOT EXISTS lecture_materials (
    id                       SERIAL PRIMARY KEY,
    lecture_id               INTEGER NOT NULL,
    title                    VARCHAR(255) NOT NULL,
    file_url                 VARCHAR(500) NOT NULL,
    file_type                VARCHAR(50) NOT NULL DEFAULT 'pdf'::character varying,
    is_downloadable          BOOLEAN NOT NULL DEFAULT true,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT lecture_materials_lecture_id_fkey FOREIGN KEY (lecture_id) REFERENCES lectures(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: lecture_bookmarks
-- =========================
CREATE TABLE IF NOT EXISTS lecture_bookmarks (
    id                       SERIAL PRIMARY KEY,
    student_id               INTEGER NOT NULL,
    lecture_id               INTEGER NOT NULL,
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_student_lecture_bookmark UNIQUE (student_id, lecture_id),
    CONSTRAINT lecture_bookmarks_lecture_id_fkey FOREIGN KEY (lecture_id) REFERENCES lectures(id) ON DELETE CASCADE,
    CONSTRAINT lecture_bookmarks_student_id_fkey FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =========================
-- TABLE: webhook_events
-- =========================
CREATE TABLE IF NOT EXISTS webhook_events (
    id                       SERIAL PRIMARY KEY,
    event_id                 VARCHAR(100) NOT NULL,
    event_type               VARCHAR(50) NOT NULL,
    payload                  TEXT,
    processed_at             TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT webhook_events_event_id_key UNIQUE (event_id)
);

-- =========================
-- INDEXES
-- =========================
CREATE INDEX IF NOT EXISTS idx_two_factor_tokens_user_id ON public.two_factor_tokens USING btree (user_id);
CREATE UNIQUE INDEX IF NOT EXISTS coupons_code_key ON public.coupons USING btree (code);
CREATE INDEX IF NOT EXISTS idx_payments_course_id ON public.payments USING btree (course_id);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON public.payments USING btree (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_student_id ON public.payments USING btree (student_id);
CREATE UNIQUE INDEX IF NOT EXISTS payments_order_id_key ON public.payments USING btree (order_id);
CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON public.refunds USING btree (payment_id);
CREATE UNIQUE INDEX IF NOT EXISTS refunds_razorpay_refund_id_key ON public.refunds USING btree (razorpay_refund_id);
CREATE UNIQUE INDEX IF NOT EXISTS wallets_student_id_key ON public.wallets USING btree (student_id);
CREATE INDEX IF NOT EXISTS idx_instructor_earnings_instructor ON public.instructor_earnings USING btree (instructor_id);
CREATE INDEX IF NOT EXISTS idx_materials_lecture_id ON public.lecture_materials USING btree (lecture_id);
CREATE INDEX IF NOT EXISTS idx_lecture_bookmarks_student ON public.lecture_bookmarks USING btree (student_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_student_lecture_bookmark ON public.lecture_bookmarks USING btree (student_id, lecture_id);
CREATE INDEX IF NOT EXISTS idx_webhook_events_id ON public.webhook_events USING btree (event_id);
CREATE UNIQUE INDEX IF NOT EXISTS webhook_events_event_id_key ON public.webhook_events USING btree (event_id);

-- =========================
-- SEED DATA: Coupons
-- =========================
INSERT INTO coupons (code, discount_percentage, max_discount_amount, min_order_amount, valid_until, usage_limit, times_used, is_active)
VALUES
    ('WELCOME10', 10.00, 50.00, 199.00, NOW() + INTERVAL '1 year', 1000, 0, true),
    ('FOCOTECH20', 20.00, 100.00, 0.00, NOW() + INTERVAL '1 year', 500, 0, true),
    ('DIWALI50', 50.00, 250.00, 499.00, NOW() + INTERVAL '1 year', 200, 0, true)
ON CONFLICT (code) DO NOTHING;
