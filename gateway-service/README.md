# gateway-service

Public edge for Medic. Port **8080**.

## What this service owns

The gateway is the only process browsers and the frontend call. It does not store users, appointments, notifications, or
audit rows, and it does not issue JWTs. A request is matched to one service, the `/api/<service>` prefix is removed, and
the rest of the path is forwarded. The downstream service checks the bearer token.

`/api/identity` goes to port 8082, `/api/booking` to 8081, `/api/notification` to 8084, and `/api/audit` to 8085.
`/internal/**` is not a route. Booking reaches identity on the Docker network, not through this proxy.

CORS is applied here so each service does not keep its own allowed-origin list. A correlation id is added to the request
so the same id appears in service logs and on Kafka events. Auth attempts are rate limited with Redis and Bucket4j. The
read timeout stays long because notification subscriptions are a server-sent event stream.

```bash
./mvnw -pl gateway-service -am spring-boot:run
```

Health: `http://localhost:8080/actuator/health`.

Production Compose publishes this service on port 80.