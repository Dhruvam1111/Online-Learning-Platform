BEGIN;

-- Stable finance pagination under equal timestamps.
CREATE INDEX IF NOT EXISTS idx_payment_created_id ON payments(created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS idx_refund_created_id ON refunds(created_at DESC,id DESC);
CREATE INDEX IF NOT EXISTS idx_earning_owner_created_id ON instructor_earnings(instructor_id,created_at DESC,id DESC);
CREATE OR REPLACE FUNCTION public.record_financial_change() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,public AS $$
BEGIN
    IF TG_TABLE_SCHEMA <> 'public' OR TG_RELID NOT IN ('public.payments'::regclass,'public.refunds'::regclass,'public.instructor_earnings'::regclass,'public.wallets'::regclass,'public.wallet_transactions'::regclass,'public.coupons'::regclass,'public.coupon_usages'::regclass) THEN
        RAISE EXCEPTION 'Unexpected financial audit source';
    END IF;
    INSERT INTO public.financial_ledger(table_name, operation, old_record, new_record)
    VALUES(TG_TABLE_NAME,TG_OP,
      CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) - 'signature' ELSE NULL END,
      CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) - 'signature' ELSE NULL END);
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION public.record_financial_change() FROM PUBLIC;
COMMIT;
