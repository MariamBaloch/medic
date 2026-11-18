# audit-service

Append-only business audit log. Port **8085**. Database role `medic_audit`, schema `audit`.

## What this service owns

Audit keeps a history of business actions: who did it, what they did, which kind of record it was, and when. It does
not approve or perform the action. Booking, identity, and the other services do the work and publish an
`audit.event.recorded` event. This service saves that event as a new row and never changes it afterwards.

An admin reads the log through the gateway at `/api/audit`, newest first. Patients and doctors cannot call it. Audit
does not send mail and does not change appointments or accounts.

## How events arrive

1. A service records a business action and publishes `audit.event.recorded` to Kafka (through its transactional
   outbox, so the event is not lost if the service stops right after saving).
2. Audit consumes the event and appends a row to its own `audit` schema.
3. If the same event arrives twice, the second copy is ignored. Audit remembers processed event ids.
4. If an event cannot be processed, it is moved to `audit.event.recorded.DLT` (the dead-letter topic) for inspection
   instead of blocking the rest.

## Configuration

Settings come from environment variables. Compose and Beanstalk provide them, and no secret is stored in the code.

| Variable                                 | Purpose                          |
|------------------------------------------|----------------------------------|
| `AUDIT_DB_URL`                           | JDBC URL of the `medic` database |
| `AUDIT_DB_USERNAME`, `AUDIT_DB_PASSWORD` | The `medic_audit` database role  |
| `KAFKA_BOOTSTRAP_SERVERS`                | Kafka address                    |
| `REDIS_HOST`, `REDIS_PORT`               | Redis, used for token checks     |
| `JWT_SECRET`                             | Validates the admin's token      |
| `INTERNAL_API_KEY`                       | Protects internal-only endpoints |
| `SERVER_PORT`                            | HTTP port, `8085`                |

## Run it

The easiest way is the full stack from the repository root: `docker compose up --build`.

To run only this service for development, start its dependencies, install the shared libraries once, then run it:

```bash
docker compose up postgres redis kafka kafka-init
./mvnw -DskipTests install
./mvnw -pl audit-service spring-boot:run
```

The service needs the environment variables above (see `.env.example`).

Health check: `http://localhost:8085/actuator/health`.