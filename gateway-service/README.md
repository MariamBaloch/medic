# gateway-service

Public edge for Medic. Port **8080**. No database.

## What this service owns

The gateway is the only process that browsers and the frontend call. It does not store users, appointments,
notifications, or audit rows, and it does not issue JWTs. The downstream service checks the bearer token, not the
gateway.

For each request, the gateway:

1. Matches the path to one service by its `/api/<service>` prefix.
2. Removes that prefix.
3. Forwards the rest of the path to the service.

| Public path            | Forwarded to           | Port |
|------------------------|------------------------|------|
| `/api/identity/**`     | `identity-service`     | 8082 |
| `/api/booking/**`      | `booking-service`      | 8081 |
| `/api/notification/**` | `notification-service` | 8084 |
| `/api/audit/**`        | `audit-service`        | 8085 |

For example, `/api/booking/patient/appointments` reaches booking as `/patient/appointments`.

`/internal/**` is blocked at the gateway and returns 404. Those endpoints are only for service-to-service calls: booking
reaches identity directly on the Docker network, not through this proxy.

## What else it does

- **CORS.** Allowed origins are set here once, so each service does not keep its own list.
- **Correlation id.** Each request gets an `X-Correlation-Id`, so the same id appears in service logs and on Kafka
  events.
- **Rate limiting.** Authentication attempts are limited with Redis and Bucket4j.
- **Long read timeout.** The read timeout stays long (1 hour by default) because notification subscriptions are a
  server-sent event stream that stays open.

The gateway has no Swagger UI. Each service's Swagger is reached through its own `/api/<service>/swagger-ui.html` path
in local runs.

## Configuration

Settings come from environment variables. Compose and Beanstalk provide them, and no secret is stored in the code.

| Variable                                                       | Purpose                                     |
|----------------------------------------------------------------|---------------------------------------------|
| `GATEWAY_PORT`                                                 | HTTP port, `8080`                           |
| `IDENTITY_URL`, `BOOKING_URL`, `NOTIFICATION_URL`, `AUDIT_URL` | Where each service is reached               |
| `REDIS_HOST`, `REDIS_PORT`                                     | Redis, used for rate limiting               |
| `CORS_ALLOWED_ORIGINS`                                         | Comma-separated list of allowed web origins |
| `GATEWAY_READ_TIMEOUT`                                         | Read timeout, `1h` by default               |

## Run it

The easiest way is the full stack from the repository root: `docker compose up --build`.

To run only the gateway for development, start Redis and the services it forwards to, install the shared library once,
then run it:

```bash
docker compose up redis
./mvnw -DskipTests install
./mvnw -pl gateway-service spring-boot:run
```

Health check: `http://localhost:8080/actuator/health`.

## Production

In production, the gateway is the only container reachable from outside the instance. Compose publishes it on port 80 of
the instance, mapped to port 8080 inside the container. The frontend and the backend CloudFront address both end up
here. See [docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md).