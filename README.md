# Credit Lens

Credit Lens is a proof-of-concept system for requesting Finnish Positive
Credit Register extracts, viewing request history and reporting financing
requests made for consumers with an active voluntary credit ban.

## Current status

The repository contains a Spring Boot backend, a separate Spring Boot
monitoring service, service-owned PostgreSQL schemas with Flyway migrations,
local WireMock and Mailpit support, and a React/TypeScript frontend bootstrap.

## Local frontend

Requirements: Node.js 22.12 or newer and npm 11 or newer.

Start the Vite development server from the repository root:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Requests under `/api` are proxied to the backend
at `http://localhost:8080` during local development.

## Local backend and monitoring environment

Requirements: Docker with Compose and Java 21.

Start PostgreSQL from the repository root:

```bash
docker compose up -d postgres
```

Start the local PCR 2.1 WireMock together with PostgreSQL:

```bash
docker compose up -d postgres wiremock
```

WireMock listens on `http://localhost:8081` and emulates
`POST /GetCreditRegisterExtract`. The backend HTTP adapter is configured with
`PCR_BASE_URL`, `PCR_TARGET_ENVIRONMENT`, `PCR_OWNER_ID_CODE_TYPE`,
`PCR_OWNER_ID_CODE`, `PCR_OWNER_COUNTRY_CODE`, `PCR_CONNECT_TIMEOUT` and
`PCR_READ_TIMEOUT` (see `.env.example`). The mock does not implement mTLS or
certificate authentication; those are required by the real PCR service.

Example request through Credit Lens:

```bash
curl -X POST http://localhost:8080/api/v1/financing-requests \
  -H 'Content-Type: application/json' \
  -d '{"clientRequestId":"ec2364ec-b4ed-4af7-805c-2bd44c42b9d5","personalIdentityCode":"010190-123A","creditRegisterExtractPurposes":["NewConsumerCredit"]}'
```

The WireMock identity-code scenarios are deterministic:

| Identity code | Scenario |
| --- | --- |
| `010190-123A` | Full successful extract, no voluntary ban |
| `020290-123A` | Successful extract with active `RiskOfIdentityTheft` ban |
| `030390-123A` | PCR-shaped HTTP 400 rejection |
| `040490-123A` | HTTP 503 unavailable |
| `050590-123A` | HTTP 200 with missing extract |
| `060690-123A` | Fixed delay beyond the default read timeout |

Unknown identity codes use the PCR-shaped `E20` HTTP 400 fallback. Fixtures
are under `wiremock/mappings` and `wiremock/__files`.

Then start the backend. Flyway applies the database migration automatically:

```bash
cd backend
./gradlew bootRun
```

The local defaults are database `credit_lens_backend`, user `credit_lens`,
password `credit_lens` and port `5432`. To override the Docker settings, copy
`.env.example` to `.env`. Supply matching backend settings with `DB_URL`,
`DB_USER` and `DB_PASSWORD`.

Check the container or stop it while preserving data:

```bash
docker compose ps
docker compose down
```

To intentionally remove the local database data as well, run
`docker compose down --volumes`.

Run the backend tests with `./gradlew test` from `backend/`. The database
integration test starts a disposable PostgreSQL container and verifies that
Flyway applies the schema successfully.

Start the monitoring database and Mailpit (SMTP on `1025`, web UI on
`http://localhost:8025`) from the repository root:

```bash
docker compose up -d monitoring-postgres mailpit
```

Then, with the backend running on port `8080`, start the independently built
monitoring service. It has no frontend endpoint and only calls the backend
monitoring API.

```bash
cd monitoring-service
./gradlew bootRun
```

Its local defaults are database `credit_lens_monitoring`, user and password
`credit_lens_monitoring`, port `5433`, backend `http://localhost:8080`, and
Mailpit. Configuration is supplied through `BACKEND_BASE_URL`,
`BACKEND_CONNECT_TIMEOUT`, `BACKEND_READ_TIMEOUT`, `MONITORING_DB_URL`,
`MONITORING_DB_USER`, `MONITORING_DB_PASSWORD`, `MONITORING_CRON`,
`MONITORING_INITIAL_LOOKBACK`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`,
`SMTP_PASSWORD`, `SMTP_AUTH_ENABLED`, `SMTP_STARTTLS_ENABLED`, and
`SMTP_SENDER`. The fixed recipient is `pcr_monitoring@dansketest.dk`; override
`MONITORING_RECIPIENT` only for local or test runs (set
`SPRING_PROFILES_ACTIVE=local` for a local override).

Run its checks with `./gradlew check` from `monitoring-service/`.

## Assignment coverage

The planned solution covers the three required components:

- a frontend where a user starts a request and views history and details;
- a backend that calls a mocked Positive Credit Register API and stores the
  results;
- a monitoring service that periodically finds completed requests with an
  active voluntary credit ban and sends an immutable email report to
  `pcr_monitoring@dansketest.dk`.

## Architecture

![Credit Lens architecture](docs/images/architecture.svg)

The monitoring service uses the backend API and never reads the backend
database directly. Each service owns its data. The monitoring endpoint is for
the monitoring service only: there is no frontend navigation, dashboard,
report preview, or user-facing monitoring API.

## Target technology

| Component | Planned technology | Responsibility |
| --- | --- | --- |
| Frontend | React and TypeScript | Start requests and display history and details |
| Backend | Java 21 and Spring Boot | Own consumers, financing requests and credit extracts |
| Monitoring service | Java 21 and Spring Boot | Build reports and retry email delivery |
| Persistence | PostgreSQL and Flyway | Service-owned data and schema migrations |
| Register mock | WireMock | Documentation-based PCR responses and failures |
| Email | Mailpit or test SMTP adapter | Local email delivery and inspection |
| Local environment | Docker Compose | Start the complete PoC |

Library choices may be adjusted during implementation without changing the
service boundaries or domain contracts.

## Main flow

### Request and view a credit extract

1. The frontend assigns a `clientRequestId` to one user submission.
2. The backend validates the request and checks for an existing successful
   request with that ID.
3. For a new request, it calls the register mock synchronously without an open
   database transaction.
4. After a valid response, one short transaction stores Consumer,
   FinancingRequest and immutable CreditExtract.
5. A PCR error returns problem details and leaves no database record.
6. The frontend can view only successfully stored fetches in history/details.

A technical retry reuses the same `clientRequestId`, so the register is not
called twice for one submission.

### Build and send a monitoring report

1. The monitoring scheduler closes a half-open interval `[start, end)`.
2. It calls the backend monitoring API for completed requests with an active
   voluntary credit ban.
3. It stores an immutable `MonitoringReport` and its report items.
4. It sends the stored report by email.
5. If SMTP is unavailable, only delivery state changes; a later retry sends the
   same report content.

## Design documentation

- [Domain model](docs/domain-model.md) - business vocabulary, aggregates,
  states and invariants.
- [API contract](docs/api-contract.md) - frontend, monitoring and mock PCR
  HTTP boundaries.
- [Data model](docs/data-model.md) - PostgreSQL tables, constraints, indexes
  and transaction boundaries.

The official PCR reference used for the mock is
[Requesting a credit register extract - API description, version 2.1](https://www.vero.fi/globalassets/pore/dokumentaatio-2026/requesting-a-credit-register-extract---api-description_2.1.pdf).

The adapter currently maps the PCR 2.1 fields represented by the Credit Lens
domain: extract reference/time, requested person, voluntary ban, summary,
repayment and leasing totals, loans/collaterals/delayed amounts/foreclosure,
and income data. PCR 2.1 `businessInformation`, `defermentPeriods`,
`repaymentMethod` and `purposeOfUse` are intentionally ignored because no
corresponding public Credit Lens domain/API fields exist yet.

## Resilience and error handling

- Connection and read timeouts are configured for the register client.
- Register failures are mapped to safe problem details: timeout is `504`,
  unavailable/invalid responses are `502`, and PCR rejection is `422`.
- Failed calls are not stored and do not appear in history.
- No database transaction stays open during an HTTP or SMTP call.
- Report creation is unique per interval.
- Email delivery is retried from persisted state without rebuilding a report.
- SMTP delivery is at-least-once: if the process stops after SMTP accepts a
  message but before the short `SENT` database update commits, the next run can
  deliver the stored message again.
- API errors use problem details and correlation IDs without exposing sensitive
  values.

## Testing strategy

Backend tests use explicit boundaries:

- controller contract tests exercise JSON, validation, HTTP statuses and
  problem details with mocked services;
- unit tests exercise domain rules, mapping and service branches with mocked
  ports;
- component tests call a real controller, service, mapper and PCR HTTP adapter
  against WireMock while repositories remain mocked;
- PCR client integration tests cover the HTTP request/response contract,
  rejection, invalid response and timeout behavior without starting Spring MVC
  or PostgreSQL;
- API integration tests load the complete Spring context and run against one
  shared PostgreSQL Testcontainer and WireMock. Successful persistence is
  verified through subsequent public API reads; direct SQL assertions are kept
  for migrations and the no-partial-write failure invariant.

The API integration tests are split by use case (create, details, history and
monitoring) rather than collected in one application test class. History and
monitoring tests cover their native PostgreSQL queries, including stable
pagination, successful-extract joins, active-ban filtering and half-open time
intervals. Separate repository tests are intentionally omitted while those
same query semantics are already covered through the full API boundary.

Monitoring-service tests cover interval selection, persisted-content retry
orchestration, backend pagination/error handling, PostgreSQL migrations and
SMTP delivery. Frontend component tests use React Testing Library and a mocked
backend API. The remaining broader goal is one system end-to-end happy path
through the frontend, backend, PCR mock and monitoring service.

## Security and privacy

The personal identity code is sensitive. It must not be written to logs,
metrics, traces, URLs, error responses or monitoring reports. The PoC stores it
as plain text only to keep the exercise focused. Production use requires
encryption or tokenization, a deterministic lookup value, access control,
auditing and an agreed retention policy.

Authentication and authorization are intentionally outside the PoC scope.
They are required before any production use.

## PoC scope

Included:

- the standard successful register response for a living consumer;
- request history containing only successfully persisted fetches;
- one scheduled email report per interval;
- local mocks for the register and email provider.

Outside scope:

- the separate deceased-person register response;
- production-grade identity and access management;
- event streaming or an outbox;
- notification channels other than email;
- production retention and deletion policies.
