# Credit Lens data model

Version: `v2`

The backend owns three tables. A FinancingRequest is created only after a
successful PCR response, so every stored request is already successful.

```mermaid
erDiagram
    CONSUMER ||--o{ FINANCING_REQUEST : has
    FINANCING_REQUEST ||--|| CREDIT_EXTRACT : produces
    CONSUMER { uuid id PK; varchar personal_identity_code UK; timestamptz created_at }
    FINANCING_REQUEST { uuid id PK; uuid consumer_id FK; uuid client_request_id UK; varchar_array extract_purposes; timestamptz requested_at; timestamptz completed_at }
    CREDIT_EXTRACT { uuid id PK; uuid financing_request_id FK_UK; uuid extract_reference UK; timestamptz creation_time_utc; jsonb register_data; timestamptz persisted_at }
```

## `consumer`

`id`, unique `personal_identity_code`, and `created_at` are required. The PoC
stores the identity code for lookup, but it must never appear in logs, URLs,
responses or database errors.

## `financing_request`

Required columns are `id`, `consumer_id`, unique `client_request_id`, non-empty
`extract_purposes`, `requested_at` and `completed_at`. The database retains no
status, error code, error message or state-transition constraints. The unique
client request ID provides simple idempotency.

## `credit_extract`

`financing_request_id` is a unique foreign key. PCR data is immutable and
stored in regular summary columns plus JSON arrays for nested register data.
The extract and its request are inserted in the same transaction.

## Indexes

- `(consumer_id, requested_at DESC, id DESC)` supports history;
- `(completed_at, id)` supports monitoring intervals;
- the unique request/extract relationship supports successful-fetch joins.

## Transaction boundary

1. Read `find(clientRequestId)` without opening a write transaction.
2. Call PCR synchronously with no database transaction.
3. Open one short `TransactionTemplate` transaction.
4. Save or reuse Consumer, then save FinancingRequest and CreditExtract.
5. Commit and return the response.

The PoC intentionally does not reserve `clientRequestId` before PCR. Two
concurrent calls may both call PCR; the unique constraint prevents two stored
requests. Exactly-once PCR execution is outside scope.
