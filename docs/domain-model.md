# Credit Lens domain model

This document defines the business vocabulary and invariants shared by the
backend and monitoring flows. It is intended for developers and reviewers who
need the model without HTTP or database detail.

## Business vocabulary

- **Consumer** identifies the person whose register data is requested.
- **Financing request** records one successful request to the Positive Credit
  Register (PCR), its business purposes, and its timing.
- **Credit extract** is the immutable PCR snapshot returned for that request.
- **Voluntary credit ban** is a register fact. Credit Lens displays it but does
  not make a lending decision.
- **Monitoring report** is an immutable delivery snapshot for one time
  interval. It belongs to the monitoring service, not the financing aggregate.

## Backend aggregate

`FinancingRequest` is the aggregate root. A stored aggregate contains:

- one `clientRequestId` used for simple idempotency;
- one `Consumer`;
- one or more extract purposes;
- `requestedAt` and `completedAt` timestamps;
- exactly one immutable `CreditExtract`.

A stored financing request is always successful. The backend deliberately has
no failed or in-progress request status, error fields, or lifecycle transition.

`PersonalIdentityCode` validates the supported input format. It does not
perform the complete Finnish identity-code checksum validation. The value
object redacts `toString()` and exposes a masked value for API output.

The extract preserves the register reference, creation time, voluntary ban,
summary counts, repayment and leasing totals, loans, and income data. An active
ban requires a reason; an inactive ban has no reason.

## Create request flow

![Backend create-request components](images/components.svg)

The controller validates `CreateFinancingRequestRequest` and delegates the
operation. The service checks `clientRequestId`, calls PCR outside a database
transaction, and opens one short transaction only after receiving a valid
response. That transaction saves or reuses the consumer and stores the request
with its extract.

A repeated matching request returns the stored aggregate. Reusing the same
request ID with a different identity code or purpose list is a conflict.

## Monitoring model

Monitoring selects successful extracts with an active voluntary ban from a
half-open completion interval `[completedFrom, completedTo)`. It reads them
through the backend API and stores an immutable report plus ordered item
snapshots in its own database.

Monitoring values `CREATED`, `SENT`, and `FAILED` describe report delivery.
They are not financing-request statuses. A failed email can be retried without
rebuilding the report or rereading the backend interval.

## Intentional PoC trade-offs

- `clientRequestId` is not reserved before PCR. Concurrent identical requests
  can both call PCR, although a unique constraint prevents duplicate storage.
- Exactly-once PCR execution and recovery of an abandoned call are outside
  scope.
- SMTP delivery is at-least-once because a crash can occur after acceptance
  but before the `SENT` update commits.
- The full personal identity code is stored for lookup. Production-grade
  protection, access control, auditing, and retention are outside this PoC.
