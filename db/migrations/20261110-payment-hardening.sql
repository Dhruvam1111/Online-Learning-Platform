-- Apply only after 20261110-payment-claims.sql. No automatic Hibernate DDL.
-- Indefinite retention: no scheduled deletion. Operators must maintain verified backups.
BEGIN;
CREATE TABLE IF NOT EXISTS financial_ledger (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name text NOT NULL,
    operation text NOT NULL,
    database_actor text NOT NULL DEFAULT session_user,
    recorded_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    old_record jsonb,
    new_record jsonb
);
CREATE INDEX IF NOT EXISTS idx_financial_ledger_recorded_at ON financial_ledger(recorded_at);
CREATE OR REPLACE FUNCTION protect_financial_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Financial history is append-only; corrections require new entries';
END;
$$;
DROP TRIGGER IF EXISTS protect_financial_ledger ON financial_ledger;
CREATE TRIGGER protect_financial_ledger BEFORE UPDATE OR DELETE OR TRUNCATE ON financial_ledger
FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history();
DROP TRIGGER IF EXISTS protect_webhook_history ON webhook_events;
CREATE TRIGGER protect_webhook_history BEFORE UPDATE OR DELETE OR TRUNCATE ON webhook_events
FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history();
CREATE OR REPLACE FUNCTION record_financial_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO financial_ledger(table_name, operation, old_record, new_record)
    VALUES(TG_TABLE_NAME,TG_OP,
      CASE WHEN TG_OP IN ('UPDATE','DELETE') THEN to_jsonb(OLD) - 'signature' ELSE NULL END,
      CASE WHEN TG_OP IN ('INSERT','UPDATE') THEN to_jsonb(NEW) - 'signature' ELSE NULL END);
    RETURN NULL;
END;
$$;
DO $$ DECLARE item text; BEGIN
    FOREACH item IN ARRAY ARRAY['payments','refunds','instructor_earnings','wallets','wallet_transactions','coupons','coupon_usages'] LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS financial_history ON %I',item);
        EXECUTE format('CREATE TRIGGER financial_history AFTER INSERT OR UPDATE OR DELETE ON %I FOR EACH ROW EXECUTE FUNCTION record_financial_change()',item);
        EXECUTE format('DROP TRIGGER IF EXISTS prevent_financial_truncate ON %I',item);
        EXECUTE format('CREATE TRIGGER prevent_financial_truncate BEFORE TRUNCATE ON %I FOR EACH STATEMENT EXECUTE FUNCTION protect_financial_history()',item);
    END LOOP;
END $$;
COMMIT;
