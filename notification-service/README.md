# notification-service

In-app notifications, unread counts, server-sent events, and email. Port **8084**. Database role `medic_notification`,
schema `notification`.

## What this service owns

Notification is the inbox. It does not decide that an appointment was booked or cancelled. Booking already did that and
published an event. This service turns that event into:

- a row the user can list,
- a count of unread items,
- a live stream (server-sent events, SSE),
- an email.

Marking items as read only changes this database. It does not change the appointment.

## How a booking becomes a notification

1. Booking writes the event to its outbox in the same database transaction as the appointment or waitlist change.
2. The outbox publisher sends it to Kafka on `booking.appointment.changed` or `booking.waitlist.changed`.
3. This service consumes the event and saves the in-app notification. That row is the record.
4. After the row is committed, the service pushes it on the SSE stream and sends the email.

Rules that apply to this flow:

- **Only booking events.** The service listens only to `booking.appointment.changed` and `booking.waitlist.changed`. It
  does not listen to identity, so it never sends verification or password-reset mail. Identity sends those itself.
- **Who is notified.** A new booking notifies the doctor only. The patient is not emailed for that event.
- **No duplicates.** If a Kafka message arrives again with an `eventId` that is already stored, it is ignored.
- **Email sending.** Emails are sent by a small thread pool: 2 core threads, up to 4, and room for 100 waiting tasks.
  The send happens after the notification row is committed, so a failed email never removes the notification.

## Public paths

Through the gateway at `/api/notification`:

| Path                           | What it does                          |
|--------------------------------|---------------------------------------|
| `GET /`                        | List notifications, with `unreadOnly` |
| `GET /unread-count`            | Count of unread notifications         |
| `PATCH /read-all`              | Mark all as read                      |
| `PATCH /{notificationId}/read` | Mark one as read                      |
| `GET /subscribe`               | Live stream (`text/event-stream`)     |

## Configuration

Settings come from environment variables. Compose and Beanstalk provide them, and no secret is stored in the code.

| Variable                                                                                           | Purpose                                |
|----------------------------------------------------------------------------------------------------|----------------------------------------|
| `NOTIFICATION_DB_URL`                                                                              | JDBC URL of the `medic` database       |
| `NOTIFICATION_DB_USERNAME`, `NOTIFICATION_DB_PASSWORD`                                             | The `medic_notification` database role |
| `KAFKA_BOOTSTRAP_SERVERS`                                                                          | Kafka address                          |
| `REDIS_HOST`, `REDIS_PORT`                                                                         | Redis, used for token checks           |
| `JWT_SECRET`                                                                                       | Validates the caller's token           |
| `INTERNAL_API_KEY`                                                                                 | Protects internal-only endpoints       |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Email server                           |
| `SERVER_PORT`                                                                                      | HTTP port, `8084`                      |

Locally, emails go to Mailpit (`http://localhost:8025`). In production there is no Mailpit, so the `MAIL_*` values must
point to a real SMTP server.

## Run it

The easiest way is the full stack from the repository root: `docker compose up --build`.

To run only this service for development, start its dependencies, install the shared libraries once, then run it:

```bash
docker compose up postgres redis kafka kafka-init mailpit
./mvnw -DskipTests install
./mvnw -pl notification-service spring-boot:run
```

The service needs the environment variables above (see `.env.example`).

Health check: `http://localhost:8084/actuator/health`.