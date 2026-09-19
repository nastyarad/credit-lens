CREATE TABLE consumer (
    id UUID PRIMARY KEY,
    personal_identity_code VARCHAR(11) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_consumer_personal_identity_code UNIQUE (personal_identity_code)
);

CREATE TABLE financing_request (
    id UUID PRIMARY KEY,
    consumer_id UUID NOT NULL,
    client_request_id UUID NOT NULL,
    extract_purposes VARCHAR[] NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_financing_request_consumer
        FOREIGN KEY (consumer_id) REFERENCES consumer (id),
    CONSTRAINT uq_financing_request_client_request_id UNIQUE (client_request_id),
    CONSTRAINT ck_financing_request_extract_purposes_not_empty
        CHECK (cardinality(extract_purposes) > 0),
    CONSTRAINT ck_financing_request_completion_time
        CHECK (completed_at >= requested_at)
);

CREATE TABLE credit_extract (
    id UUID PRIMARY KEY,
    financing_request_id UUID NOT NULL,
    extract_reference UUID NOT NULL,
    creation_time_utc TIMESTAMPTZ NOT NULL,
    voluntary_ban_active BOOLEAN NOT NULL,
    voluntary_ban_reason VARCHAR(64),
    lenders_count INTEGER NOT NULL,
    loan_contracts_count INTEGER NOT NULL,
    guaranteed_loan_contracts_count INTEGER NOT NULL,
    repayment_amounts JSONB NOT NULL DEFAULT '[]'::jsonb,
    leasing_instalment_amounts JSONB NOT NULL DEFAULT '[]'::jsonb,
    loans JSONB NOT NULL DEFAULT '[]'::jsonb,
    income_data JSONB NOT NULL DEFAULT '[]'::jsonb,
    persisted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_credit_extract_financing_request
        FOREIGN KEY (financing_request_id) REFERENCES financing_request (id),
    CONSTRAINT uq_credit_extract_financing_request_id UNIQUE (financing_request_id),
    CONSTRAINT uq_credit_extract_extract_reference UNIQUE (extract_reference),
    CONSTRAINT ck_credit_extract_voluntary_ban
        CHECK (
            (voluntary_ban_active = FALSE AND voluntary_ban_reason IS NULL)
            OR
            (voluntary_ban_active = TRUE AND voluntary_ban_reason IN (
                'RiskOfIdentityTheft',
                'ControlOfPersonalFinances',
                'Other'
            ))
        ),
    CONSTRAINT ck_credit_extract_counts_non_negative
        CHECK (
            lenders_count >= 0
            AND loan_contracts_count >= 0
            AND guaranteed_loan_contracts_count >= 0
        ),
    CONSTRAINT ck_credit_extract_repayment_amounts_array
        CHECK (jsonb_typeof(repayment_amounts) = 'array'),
    CONSTRAINT ck_credit_extract_leasing_instalment_amounts_array
        CHECK (jsonb_typeof(leasing_instalment_amounts) = 'array'),
    CONSTRAINT ck_credit_extract_loans_array
        CHECK (jsonb_typeof(loans) = 'array'),
    CONSTRAINT ck_credit_extract_income_data_array
        CHECK (jsonb_typeof(income_data) = 'array')
);

CREATE INDEX ix_financing_request_consumer_history
    ON financing_request (consumer_id, requested_at DESC, id DESC);

CREATE INDEX ix_financing_request_completed
    ON financing_request (completed_at, id);

CREATE INDEX ix_credit_extract_active_ban
    ON credit_extract (financing_request_id)
    WHERE voluntary_ban_active = TRUE;
