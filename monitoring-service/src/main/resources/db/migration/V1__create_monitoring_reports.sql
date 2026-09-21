CREATE TABLE monitoring_report (
    id UUID PRIMARY KEY,
    interval_start TIMESTAMPTZ NOT NULL,
    interval_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(998) NOT NULL,
    body TEXT NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    item_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    last_attempt_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(512),
    CONSTRAINT monitoring_report_interval_valid CHECK (interval_start < interval_end),
    CONSTRAINT monitoring_report_interval_unique UNIQUE (interval_start, interval_end),
    CONSTRAINT monitoring_report_status_valid CHECK (status IN ('CREATED', 'SENT', 'FAILED')),
    CONSTRAINT monitoring_report_counts_nonnegative CHECK (item_count >= 0 AND attempt_count >= 0)
);

CREATE TABLE monitoring_report_item (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES monitoring_report(id),
    ordinal INTEGER NOT NULL,
    masked_personal_identity_code VARCHAR(32) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    voluntary_credit_ban_reason VARCHAR(128) NOT NULL,
    lenders_count INTEGER NOT NULL,
    loan_contracts_count INTEGER NOT NULL,
    guaranteed_loan_contracts_count INTEGER NOT NULL,
    CONSTRAINT monitoring_report_item_ordinal_unique UNIQUE (report_id, ordinal),
    CONSTRAINT monitoring_report_item_counts_nonnegative CHECK (
      ordinal >= 0 AND lenders_count >= 0 AND loan_contracts_count >= 0
      AND guaranteed_loan_contracts_count >= 0
    )
);

CREATE INDEX monitoring_report_status_created_at_idx ON monitoring_report(status, created_at);
