# Credit Lens API contract

This document describes the current HTTP boundary for the frontend and the
monitoring service. It is intended for API consumers, reviewers, and developers
checking implementation behaviour.

- Document revision: 3
- HTTP API version: v1
- Base path: `/api/v1`
- Error media type: `application/problem+json`

## Operations

| Method and path | Request DTO | Response DTO | Purpose |
| --- | --- | --- | --- |
| `POST /api/v1/financing-requests` | `CreateFinancingRequestRequest` | `CreateFinancingRequestResponse` | Request and store one PCR extract |
| `POST /api/v1/financing-requests/search` | `SearchFinancingRequestsRequest` | `SearchFinancingRequestsResponse` | Search successful history by identity code |
| `GET /api/v1/financing-requests/{id}` | Path UUID | `GetFinancingRequestDetailsResponse` | Read one stored extract in full |
| `GET /api/v1/monitoring/financing-requests` | `ListMonitoringFinancingRequestsRequest` | `ListMonitoringFinancingRequestsResponse` | List active-ban requests for monitoring |

## Create a financing request

`POST /api/v1/financing-requests`

```json
{
  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
  "personalIdentityCode": "010190-123A",
  "creditRegisterExtractPurposes": ["NewConsumerCredit"]
}
```

The backend calls the Positive Credit Register (PCR) synchronously and outside
a database transaction. After a valid response, one short transaction stores
the consumer, request, and immutable extract.

A new result returns `201 Created`, a `Location` header, and the response body.
A repeated matching result returns `200 OK`. The response contains request
identifiers, a masked consumer, purposes, timestamps, and an extract summary.
It never contains the full identity code, status, error, or internal
`newlyCreated` flag.

### Idempotency

Reusing `clientRequestId` returns an already persisted matching result without
another PCR call. It does not guarantee exactly-once PCR execution when the
first call fails, times out, or overlaps with another call.

The same ID with a different personal identity code or purpose list returns
`409 Conflict`. The backend does not reserve an ID before calling PCR.

### PCR failures

No consumer, request, or extract is persisted when PCR fails.

| PCR condition | HTTP status |
| --- | ---: |
| Timeout | `504 Gateway Timeout` |
| Upstream rejection | `422 Unprocessable Content` |
| Unavailable service or connection failure | `502 Bad Gateway` |
| Invalid upstream response | `502 Bad Gateway` |

The response uses a generic safe detail. It does not expose the upstream
payload or personal identity code.

## Search request history

`POST /api/v1/financing-requests/search`

The identity code stays in the request body and never appears in a URL. `page`
defaults to `0`; `size` defaults to `20` and accepts `1..100`.

```json
{
  "personalIdentityCode": "010190-123A",
  "page": 0,
  "size": 20
}
```

Results include only requests joined to a stored extract. They are ordered by
`requestedAt DESC, id DESC`. An unknown consumer returns an empty page.

```json
{
  "items": [
    {
      "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
      "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
      "maskedPersonalIdentityCode": "******-123A",
      "requestedAt": "2026-09-18T10:15:29Z",
      "completedAt": "2026-09-18T10:15:30Z",
      "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
      "voluntaryCreditBanActive": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

## Get request details

`GET /api/v1/financing-requests/{id}`

This operation reads a stored result and never calls PCR. Its response has the
following shape:

| Field | Content |
| --- | --- |
| `id`, `clientRequestId` | Request identifiers |
| `consumer` | Consumer ID and masked identity code |
| `creditRegisterExtractPurposes` | One or more purposes |
| `requestedAt`, `completedAt` | Request timing |
| `creditExtract` | Reference, creation time, ban, summary, loans, and income data |

`creditExtract.creditInformationSummary` contains the three counts plus
repayment and monthly leasing currency totals. Each loan can contain collateral,
payment-plan, subtype, and delayed-amount data.

A malformed UUID returns `400 Bad Request`. An unknown UUID returns `404 Not
Found`. A request without an extract is an internal invariant violation, not a
not-found result.

## List requests for monitoring

`GET /api/v1/monitoring/financing-requests`

Required query parameters `completedFrom` and `completedTo` are ISO-8601
instants. The interval is half-open: `[completedFrom, completedTo)`. `page`
defaults to `0`; `size` defaults to `100` and accepts `1..500`.

Example:

```text
/api/v1/monitoring/financing-requests?completedFrom=2026-09-18T10:00:00Z&completedTo=2026-09-18T10:05:00Z&page=0&size=100
```

Results include only stored extracts with `voluntary_ban_active = TRUE`. They
are ordered by `completedAt ASC, financingRequestId ASC`. Each item contains
request and extract references, timestamps, the masked identity code, ban
reason, and the three summary counts.

An empty result is `200 OK` with `items: []`, `totalItems: 0`, and
`totalPages: 0`. This endpoint is an integration boundary for the monitoring
service; the frontend does not call it.

## Validation summary

| Input | Rule |
| --- | --- |
| `clientRequestId` | Required UUID |
| `personalIdentityCode` | Required supported format; full Finnish checksum validation is not implemented |
| `creditRegisterExtractPurposes` | Non-empty list of known enum values |
| History `page` / `size` | `page >= 0`; `size` in `1..100` |
| Detail `id` | UUID path value |
| Monitoring interval | Both timestamps required; `completedFrom < completedTo` |
| Monitoring `page` / `size` | `page >= 0`; `size` in `1..500` |

Invalid JSON, enum values, formats, pagination, timestamps, or intervals return
`400 Bad Request`.

## Problem details

Errors have these fields: `type`, `title`, `status`, `detail`, `instance`, and
`correlationId`. The same correlation ID is returned in the
`X-Correlation-Id` header. An invalid supplied correlation ID is replaced.

Error content is generic by design. Responses never echo the personal identity
code or raw PCR response.

## Create sequence

![Credit Lens create-request sequence](images/sequence.svg)
