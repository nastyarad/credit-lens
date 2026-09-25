# Credit Lens data model

This document describes database ownership, constraints, indexes, and
transaction boundaries. It is intended for reviewers and developers checking
the persistence model against the domain contract.

Document revision: 3

## Backend database

The backend owns three tables. A financing request is written only after a
valid Positive Credit Register response.

![Credit Lens backend data model](images/data-model.svg)

### `consumer`

Required columns are `id`, unique `personal_identity_code`, and `created_at`.
The PoC stores the full identity code for lookup. It must not be exposed in
logs, URLs, API output, errors, metrics, or monitoring data.

### `financing_request`

Required columns are `id`, `consumer_id`, unique `client_request_id`, non-empty
`extract_purposes`, `requested_at`, and `completed_at`. A check constraint
requires completion not to precede the request.

The table contains no status, error code, error message, or state-transition
columns. Failed and in-progress requests are not persisted.

### `credit_extract`

The table stores its ID, a unique request foreign key, a unique extract
reference, creation and persistence times, voluntary-ban fields, and three
non-negative summary counts. Nested data uses four JSONB arrays:

- `repayment_amounts`;
- `leasing_instalment_amounts`;
- `loans`;
- `income_data`.

Checks require each JSONB value to be an array and enforce the relationship
between the ban flag and reason.

The unique foreign key prevents more than one extract for a request. The
database does not guarantee that every financing request has an extract.
That direction of the one-to-one invariant is enforced by the domain model and
the application transaction.

## Backend indexes

| Index | Purpose |
| --- | --- |
| `ix_financing_request_consumer_history` on `(consumer_id, requested_at DESC, id DESC)` | Stable history lookup |
| `ix_financing_request_completed` on `(completed_at, id)` | Monitoring intervals and ordering |
| `ix_credit_extract_active_ban` on `(financing_request_id) WHERE voluntary_ban_active = TRUE` | Partial active-ban join support |

Unique constraints also index personal identity code, client request ID,
extract reference, and the request-to-extract foreign key.

## Backend transaction boundary

1. Look up `clientRequestId` without opening a write transaction.
2. Call PCR synchronously with no database transaction.
3. Open one short transaction after a valid response.
4. Save or reuse the consumer, then save the financing request and extract.
5. Commit and return the stored aggregate.

The request ID is not reserved before PCR. Concurrent calls can both reach PCR;
the unique constraint prevents two stored requests but does not provide
exactly-once upstream execution.

## Monitoring database

The monitoring service owns a separate PostgreSQL database. It reads a masked
snapshot through the backend API and never connects to the backend database.

### `monitoring_report`

Each report stores a unique half-open interval, recipient, rendered plain-text
subject and body, content type, item count, creation and delivery metadata, and
one delivery status: `CREATED`, `SENT`, or `FAILED`. The interval must have
`interval_start < interval_end`; counts are non-negative.

These values describe email delivery only. They are not financing-request
statuses.

### `monitoring_report_item`

Items are immutable ordered snapshots linked to a report. They store only the
masked identity code, request timestamps, voluntary-ban reason, and summary
counts. They do not store the full identity code, financing-request ID, or
extract reference.

The unique `(report_id, ordinal)` constraint preserves item order. The
`monitoring_report_status_created_at_idx` index supports finding undelivered
reports.

## Monitoring transaction boundaries

1. Fetch and validate all backend pages outside a transaction.
2. Persist the report, ordered items, and rendered content in a short
   transaction.
3. Send SMTP outside a transaction.
4. Update only delivery metadata in another short transaction.

An unavailable or invalid backend response creates no report and does not
advance the interval. SMTP failure retains the report for retry. Delivery is
at-least-once because SMTP acceptance can occur before the `SENT` update.

## Constraint ownership

Database constraints protect local shape, uniqueness, references, counts, and
intervals. The application remains responsible for aggregate completeness,
immutable result semantics, cross-table creation in one transaction, masking,
and preventing failed requests from being stored.
