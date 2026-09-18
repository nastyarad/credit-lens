# Credit Lens API contract

Version: `v2`

This document defines the REST boundary exposed by the backend to the frontend
and monitoring service. Domain terminology follows
[domain-model.md](domain-model.md).

## Conventions

- Base path: `/api/v1`
- Media type: `application/json`
- Field names: `camelCase`
- Timestamps: ISO-8601 UTC, for example `2026-09-18T10:15:30Z`
- IDs: UUID strings
- Empty collections are returned as `[]`, not `null`
- Errors use `application/problem+json`

The full `personalIdentityCode` is accepted only in request bodies. It is never
placed in a URL or returned in list, detail or monitoring responses. Clients
may send `X-Correlation-Id`; the backend returns the same value or creates one.

## Frontend API

### Create a financing request

`POST /api/v1/financing-requests`

Request:

```json
{
  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
  "personalIdentityCode": "010190-123A",
  "creditRegisterExtractPurposes": [
    "NewConsumerCredit"
  ]
}
```

`clientRequestId` identifies one user submission. Repeating the request with
the same ID and the same body returns the existing `FinancingRequest` and does
not call the Positive Credit Register again. Reusing the ID with different
input returns `409 Conflict`.

The PoC completes the mocked register call synchronously. The backend first
persists an `IN_PROGRESS` request and then returns its terminal representation.

New request: `201 Created` with a `Location` header pointing to
`/api/v1/financing-requests/{id}`.

Repeated request: `200 OK`.

If the original call is still running, a concurrent repeat returns the current
representation with `status: IN_PROGRESS`.

Completed response:

```json
{
  "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
  "consumer": {
    "id": "d2fbb47f-2317-4740-8fa5-50f73b64182d",
    "maskedPersonalIdentityCode": "******-123A"
  },
  "creditRegisterExtractPurposes": [
    "NewConsumerCredit"
  ],
  "status": "COMPLETED",
  "requestedAt": "2026-09-18T10:15:29Z",
  "completedAt": "2026-09-18T10:15:30Z",
  "error": null,
  "creditExtractSummary": {
    "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
    "creationTimeUtc": "2026-09-18T10:15:30Z",
    "voluntaryBanOnCredits": {
      "isInEffect": true,
      "reason": "ControlOfPersonalFinances"
    },
    "lendersCount": 2,
    "loanContractsCount": 3,
    "guaranteedLoanContractsCount": 0
  }
}
```

A register timeout, connection error, rejection or invalid response still
creates a history entry. The API returns `201 Created` with `status: FAILED`,
`creditExtractSummary: null` and an error object:

```json
{
  "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
  "consumer": {
    "id": "d2fbb47f-2317-4740-8fa5-50f73b64182d",
    "maskedPersonalIdentityCode": "******-123A"
  },
  "creditRegisterExtractPurposes": [
    "NewConsumerCredit"
  ],
  "status": "FAILED",
  "requestedAt": "2026-09-18T10:15:29Z",
  "completedAt": "2026-09-18T10:15:35Z",
  "error": {
    "code": "PCR_TIMEOUT",
    "message": "The Positive Credit Register did not respond in time."
  },
  "creditExtractSummary": null
}
```

Validation errors:

- `400 Bad Request` - invalid personal identity code, missing purpose or invalid
  `clientRequestId`;
- `409 Conflict` - `clientRequestId` was already used with different input;
- `500 Internal Server Error` - the request could not be persisted.

### Search financing-request history

`POST /api/v1/financing-requests/search`

A POST search keeps the sensitive personal identity code out of URLs and
access logs.

Request:

```json
{
  "personalIdentityCode": "010190-123A",
  "page": 0,
  "size": 20
}
```

`page` defaults to `0`. `size` defaults to `20` and cannot exceed `100`.

Response: `200 OK`

```json
{
  "items": [
    {
      "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
      "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
      "maskedPersonalIdentityCode": "******-123A",
      "status": "COMPLETED",
      "requestedAt": "2026-09-18T10:15:29Z",
      "completedAt": "2026-09-18T10:15:30Z",
      "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
      "voluntaryCreditBanActive": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1,
  "totalPages": 1
}
```

Items are ordered by `requestedAt DESC, id DESC`. An unknown consumer returns
`200 OK` with an empty `items` array. Failed requests have `null` values for
`extractReference` and `voluntaryCreditBanActive`.

### Get financing-request details

`GET /api/v1/financing-requests/{id}`

Response for a completed request: `200 OK`

```json
{
  "id": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
  "clientRequestId": "ec2364ec-b4ed-4af7-805c-2bd44c42b9d5",
  "consumer": {
    "id": "d2fbb47f-2317-4740-8fa5-50f73b64182d",
    "maskedPersonalIdentityCode": "******-123A"
  },
  "creditRegisterExtractPurposes": [
    "NewConsumerCredit"
  ],
  "status": "COMPLETED",
  "requestedAt": "2026-09-18T10:15:29Z",
  "completedAt": "2026-09-18T10:15:30Z",
  "error": null,
  "creditExtract": {
    "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
    "creationTimeUtc": "2026-09-18T10:15:30Z",
    "voluntaryBanOnCredits": {
      "isInEffect": true,
      "reason": "ControlOfPersonalFinances"
    },
    "creditInformationSummary": {
      "lendersCount": 2,
      "loanContractsCount": 3,
      "guaranteedLoanContractsCount": 0,
      "repaymentsPaidLastAmount": [
        {
          "currencyCode": "EUR",
          "sum": 450.00
        }
      ],
      "sumOfMonthlyLeasingInstalments": []
    },
    "loans": [
      {
        "loanType": "LumpSumLoan",
        "contractDate": "2024-01-15",
        "isLoanWithCollateral": false,
        "collateralType": [],
        "borrowersCount": 1,
        "currencyCode": "EUR",
        "paymentPlan": {
          "isInDebtArrangement": false,
          "isInBusinessRestructuringProgram": false
        },
        "accuracyIsDenied": false,
        "lumpSumLoan": {
          "amountIssued": 5000.00,
          "amountPaid": 1800.00,
          "balance": 3200.00,
          "plannedFinalDueDate": "2028-01-15",
          "amortizationFrequency": 1
        },
        "runningAccountLoan": null,
        "leasingContract": null,
        "delayedAmount": []
      }
    ],
    "incomeData": [
      {
        "year": 2026,
        "months": [
          {
            "month": 8,
            "wagesGrossAmount": 4200.00,
            "wagesNetAmount": 3100.00,
            "benefitsGrossAmount": 0.00,
            "benefitsNetAmount": 0.00
          }
        ]
      }
    ]
  }
}
```

Optional object and scalar fields are returned as `null`; optional collections
are returned as empty arrays. A failed request returns the same top-level shape
with an `error` object and `creditExtract: null`.

Errors:

- `400 Bad Request` - malformed request ID;
- `404 Not Found` - financing request does not exist.

## Monitoring API

### Get report candidates

`GET /api/v1/monitoring/financing-requests`

Required query parameters:

- `completedFrom` - inclusive interval start;
- `completedTo` - exclusive interval end.

Optional query parameters:

- `page` - zero-based page number, default `0`;
- `size` - page size, default `100`, maximum `500`.

Example:

```http
GET /api/v1/monitoring/financing-requests?completedFrom=2026-09-18T10:00:00Z&completedTo=2026-09-18T10:05:00Z&page=0&size=100
```

Response: `200 OK`

```json
{
  "items": [
    {
      "financingRequestId": "d46c2b84-d979-43e2-b0e4-bc7de12d2454",
      "extractReference": "2fdc1e0b-91d8-4d7c-9d4c-82df9c3b2a10",
      "requestedAt": "2026-09-18T10:04:40Z",
      "completedAt": "2026-09-18T10:04:42Z",
      "maskedPersonalIdentityCode": "******-123A",
      "voluntaryCreditBanReason": "ControlOfPersonalFinances",
      "lendersCount": 2,
      "loanContractsCount": 3,
      "guaranteedLoanContractsCount": 0
    }
  ],
  "page": 0,
  "size": 100,
  "totalItems": 1,
  "totalPages": 1
}
```

The backend returns only completed requests with an active voluntary credit
ban and `completedAt` inside `[completedFrom, completedTo)`. Results are ordered
by `completedAt ASC, financingRequestId ASC`. An interval with no matches
returns an empty page.

Errors:

- `400 Bad Request` - missing timestamps, `completedFrom >= completedTo`, or
  invalid pagination;
- `500 Internal Server Error` - query could not be completed.

## Error format

Validation and infrastructure errors use RFC 9457-style problem details:

```json
{
  "type": "https://credit-lens.local/problems/invalid-personal-identity-code",
  "title": "Invalid personal identity code",
  "status": 400,
  "detail": "The supplied personal identity code has an invalid format.",
  "instance": "/api/v1/financing-requests",
  "correlationId": "0b1bc2e7-8f84-47dd-8326-a8bfef0e08af"
}
```

Error responses never include the personal identity code or a raw upstream
response.

## Mock Positive Credit Register contract

The mock is based on the official
[Requesting a credit register extract API description](https://www.vero.fi/globalassets/pore/dokumentaatio-2026/requesting-a-credit-register-extract---api-description_2.1.pdf).
It uses the register field names needed by the PoC rather than the frontend API
names.

Request:

```json
{
  "targetEnvironment": "Test",
  "owner": {
    "idCodeType": "BusinessId",
    "idCode": "1234567-8"
  },
  "request": {
    "idCodeType": "PersonalIdentityCode",
    "idCode": "010190-123A",
    "creditRegisterExtractPurpose": [
      "NewConsumerCredit"
    ]
  }
}
```

A successful mock response contains `statusMessage` and
`creditRegisterExtract`. The extract includes:

- `extractReference` and `creationTimeUtc`;
- optional `voluntaryBanOnCredits`;
- `creditInformationSummary`;
- `loans`, preserving the documented type-specific groups;
- `incomeData`.

The mock can also return the documented HTTP errors and an `errorResponses`
list. The backend maps outcomes as follows:

| Mock outcome | Financing-request result |
| --- | --- |
| Valid `200` response | `COMPLETED` with an immutable extract |
| Timeout | `FAILED` / `PCR_TIMEOUT` |
| Connection error, `500`, `502` or `503` | `FAILED` / `PCR_UNAVAILABLE` |
| Rejected request, `400` or `403` | `FAILED` / `PCR_REJECTED` |
| Invalid successful response | `FAILED` / `PCR_INVALID_RESPONSE` |

The raw mock response and requester organisation data are not exposed through
the frontend or monitoring APIs.
