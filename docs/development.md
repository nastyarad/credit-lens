# Credit Lens development guide

This guide covers manual local startup, configuration, test fixtures, seed
data, real SMTP, and common failures. Use the root [README](../README.md) for
the complete Docker Compose demo and assignment overview.

## Prerequisites

- Docker with Docker Compose
- Java 21
- Node.js 22.12 or newer
- npm 11 or newer

## Manual multi-terminal startup

Start the supporting containers from the repository root:

```bash
docker compose up postgres monitoring-postgres wiremock mailpit
```

In a second terminal, start the backend. Flyway creates the backend schema:

```bash
cd backend
./gradlew bootRun
```

In a third terminal, start the monitoring service. Flyway creates its separate
schema, and the scheduler begins using the default five-minute interval:

```bash
cd monitoring-service
./gradlew bootRun
```

In a fourth terminal, start the frontend:

```bash
cd frontend
npm ci
npm run dev
```

Vite serves `http://localhost:5173` and proxies `/api` to
`http://localhost:8080`. WireMock listens on `8081`; Mailpit accepts SMTP on
`1025` and serves its inbox on `http://localhost:8025`.

## Environment variables

Copy `.env.example` to `.env` only when overriding Compose defaults. Do not
commit credentials.

### Local container ports and databases

| Variable | Default | Purpose |
| --- | --- | --- |
| `FRONTEND_PORT` | `5173` | Compose frontend host port |
| `BACKEND_PORT` | `8080` | Compose backend host port |
| `POSTGRES_DB` | `credit_lens_backend` | Backend database name |
| `POSTGRES_USER` | `credit_lens` | Backend database user |
| `POSTGRES_PASSWORD` | `credit_lens` | Backend database password |
| `POSTGRES_PORT` | `5432` | Backend database host port |
| `WIREMOCK_PORT` | `8081` | PCR mock host port |
| `MONITORING_POSTGRES_DB` | `credit_lens_monitoring` | Monitoring database name |
| `MONITORING_POSTGRES_USER` | `credit_lens_monitoring` | Monitoring database user |
| `MONITORING_POSTGRES_PASSWORD` | `credit_lens_monitoring` | Monitoring database password |
| `MONITORING_POSTGRES_PORT` | `5433` | Monitoring database host port |
| `MAILPIT_SMTP_PORT` | `1025` | Mailpit SMTP host port |
| `MAILPIT_WEB_PORT` | `8025` | Mailpit web host port |

### Backend

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/credit_lens_backend` | Backend JDBC URL for manual startup |
| `DB_USER` | `credit_lens` | Backend database user |
| `DB_PASSWORD` | `credit_lens` | Backend database password |
| `PCR_BASE_URL` | `http://localhost:8081` | PCR endpoint for manual startup |
| `PCR_TARGET_ENVIRONMENT` | `Test` | PCR target environment field |
| `PCR_OWNER_ID_CODE_TYPE` | `BusinessId` | Request owner identifier type |
| `PCR_OWNER_ID_CODE` | `1234567-8` | Synthetic request owner identifier |
| `PCR_OWNER_COUNTRY_CODE` | `FI` | Request owner country |
| `PCR_CONNECT_TIMEOUT` | `2s` | PCR connection timeout |
| `PCR_READ_TIMEOUT` | `5s` | PCR response timeout |

Compose sets the backend database and PCR URLs to container host names. The
manual defaults point to published localhost ports.

### Monitoring and SMTP

| Variable | Default | Purpose |
| --- | --- | --- |
| `BACKEND_BASE_URL` | `http://localhost:8080` | Backend URL for manual startup |
| `BACKEND_CONNECT_TIMEOUT` | `2s` | Backend connection timeout |
| `BACKEND_READ_TIMEOUT` | `5s` | Backend response timeout |
| `MONITORING_DB_URL` | `jdbc:postgresql://localhost:5433/credit_lens_monitoring` | Monitoring JDBC URL |
| `MONITORING_DB_USER` | `credit_lens_monitoring` | Monitoring database user |
| `MONITORING_DB_PASSWORD` | `credit_lens_monitoring` | Monitoring database password |
| `MONITORING_CRON` | `0 */5 * * * *` | UTC Spring cron schedule |
| `MONITORING_INITIAL_LOOKBACK` | `PT5M` | First interval lookback |
| `MONITORING_RECIPIENT` | `pcr_monitoring@dansketest.dk` | Fixed report recipient by default |
| `SMTP_HOST` | `localhost` | SMTP host for manual startup |
| `SMTP_PORT` | `1025` | SMTP port |
| `SMTP_USERNAME` | empty | Optional SMTP user |
| `SMTP_PASSWORD` | empty | Optional SMTP password |
| `SMTP_AUTH_ENABLED` | `false` | Enable SMTP authentication |
| `SMTP_STARTTLS_ENABLED` | `false` | Enable SMTP STARTTLS |
| `SMTP_SENDER` | `credit-lens-monitoring@localhost` | From address |
| `SMTP_CONNECT_TIMEOUT` | `2000` | Connection timeout in milliseconds |
| `SMTP_READ_TIMEOUT` | `5000` | Read timeout in milliseconds |

Compose replaces the database, backend, and SMTP hosts with service names.
Recipient overrides are accepted only with the `local`, `test`, or `real-mail`
Spring profile.

## WireMock fixtures

WireMock emulates `POST /GetCreditRegisterExtract`. These identity codes are
synthetic and deterministic:

| Identity code | Behaviour |
| --- | --- |
| `010190-123A` | Complete extract without a voluntary ban |
| `020290-123A` | Active `RiskOfIdentityTheft` ban |
| `070790-123A` | Active `ControlOfPersonalFinances` ban |
| `080890-123A` | Active `Other` ban |
| `030390-123A` | PCR-shaped HTTP 400 rejection; backend returns 422 |
| `040490-123A` | HTTP 503 upstream response; backend returns 502 |
| `050590-123A` | HTTP 200 with an invalid extract; backend returns 502 |
| `060690-123A` | Ten-second response delay; backend normally returns 504 |

Other supported-format values receive the general success fixture. Input that
does not match a higher-priority mapping reaches the PCR-shaped fallback.
Mappings are in `wiremock/mappings`; response bodies are in
`wiremock/__files`.

## Seed data

The seed script creates fictional historical requests for pagination and detail
testing. Start the backend database and backend first so Flyway has created the
tables, then run from the repository root:

```bash
docker compose exec -T postgres \
  psql -U credit_lens -d credit_lens_backend \
  < scripts/seed-test-data.sql
```

The script replaces only rows that use its reserved synthetic IDs. Use the
identity codes listed in its final masked verification output to exercise
history. Fresh API requests are better for testing the current monitoring
interval.

## Real SMTP profile

The default setup uses Mailpit and never sends outside the local environment.
To test an SMTP provider deliberately, start the monitoring service with the
`real-mail` profile and provide all relevant values:

```bash
cd monitoring-service
SPRING_PROFILES_ACTIVE=real-mail \
MONITORING_RECIPIENT=your.email@example.com \
SMTP_HOST=smtp.example.com \
SMTP_PORT=587 \
SMTP_USERNAME=your.smtp.username \
SMTP_PASSWORD='your-app-password' \
SMTP_AUTH_ENABLED=true \
SMTP_STARTTLS_ENABLED=true \
SMTP_SENDER=monitoring@example.com \
./gradlew bootRun
```

Do not commit SMTP credentials. Real delivery is a local verification option,
not the supported Compose demo or a production deployment.

## Troubleshooting

### A port is already in use

Override the matching host port in `.env`, or stop the process already using
it. Manual application URLs must be updated when backend or support-service
ports change.

### The backend does not start

Confirm PostgreSQL and WireMock are running, then check that `DB_URL` and the
database credentials match the container settings. Flyway applies the schema
before JPA validation.

### Backend `check` reports a Spotless JVM-version error

Launch Gradle with JDK 21, as required by the project. Compilation uses a Java
21 toolchain, but the formatter also depends on the JVM that launches Gradle;
the configured backend formatter is not compatible with a JDK 25 launcher.

### A request is rejected before PCR

The API validates the supported identity-code format and requires at least one
known purpose. It does not implement the full Finnish checksum. Use a listed
fixture when testing a specific upstream outcome.

### Mail does not appear immediately

Monitoring runs on its UTC cron schedule, not when the request is submitted.
Check that the request completed inside the next interval and has an active
ban. If SMTP fails, the stored `CREATED` or `FAILED` report is retried on a
later run before a new interval is created.

### History is empty after a failed request

This is expected. Only valid PCR responses are persisted, so rejected,
unavailable, invalid, and timed-out calls do not appear in history.
