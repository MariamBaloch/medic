# medic-messaging

Shared code for sending and receiving Kafka events safely. This module is a library, not a running service. A service
picks it up through Spring Boot auto-configuration, so there is nothing to start.

It solves three problems:

1. **Publishing without losing events** (the outbox).
2. **Not handling the same event twice** (processed events).
3. **Not blocking on a bad message** (dead letters).

## Outbox

The outbox makes sure an event is sent only if the database change that caused it was saved.

- `OutboxService.append` writes a row to the `outbox_event` table. It must run inside the caller's transaction. Booking
  uses it for an appointment or waitlist change, so the event is stored only if the booking commits.
- `OutboxService.appendIndependent` opens its own transaction. It is used for an audit row recorded after the controller
  has already returned.
- `OutboxPublisher` runs on a schedule and sends the stored rows to Kafka:
    1. It locks unpublished rows with `FOR UPDATE SKIP LOCKED`, so two instances never send the same row.
    2. It sends each one with an idempotent producer (`acks=all`). The Kafka key is the aggregate id, and the
       correlation id travels as a header.
    3. It sets `published_at` on the row once Kafka has accepted it.
- A row stops being retried after `app.outbox.max-attempts` failures (default 10).

## Already-processed events

Kafka can deliver a message more than once. To handle this safely:

- `ProcessedEvent` stores an `eventId` after a consumer has handled the event. A second delivery of the same id is
  skipped.
- `ProcessedEventRetentionJob` deletes old rows so the table does not grow forever. It follows
  `spring.task.scheduling.enabled`, which the test profile turns off.

## Dead letters

When a consumer cannot process a message, it must not block the messages behind it.

`KafkaDltAutoConfiguration` retries a failed record a few times, then publishes it to `<topic>.DLT` (the dead-letter
topic) using `TopicNames.deadLetter`. It is active when the application has a Kafka consumer group.

## Settings

| Property                             | Purpose                                                         |
|--------------------------------------|-----------------------------------------------------------------|
| `app.outbox.publisher-enabled`       | Turns the outbox publisher on or off. Tests set it to `false`.  |
| `app.outbox.publish-interval-ms`     | How often the publisher looks for unsent rows                   |
| `app.outbox.max-attempts`            | How many times a row is tried before it is left alone           |
| `app.processed-event.retention-days` | How long processed event ids are kept                           |
| `app.processed-event.retention-cron` | When the cleanup job runs                                       |
| `app.kafka.dlt.retry-interval-ms`    | Wait between retries of a failed record                         |
| `app.kafka.dlt.max-retries`          | Number of retries before a record goes to the dead-letter topic |

## What a service must provide

- **Tables.** This library does not ship database migrations. Each service creates `outbox_event` and `processed_event`
  in its own schema with its own Flyway migration.
- **Parts it uses.** A service that publishes events (booking, identity) uses the outbox. A service that only consumes
  (notification, audit) needs the processed-event and dead-letter parts, plus the outbox only if it also publishes.