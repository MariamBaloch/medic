# medic-messaging

How a service publishes a Kafka event and how a consumer avoids handling the same event twice. The services that need it pick this up through auto-configuration. There is no standalone process.

## Outbox

`OutboxService.append` writes an `outbox_event` row and must run inside the caller's transaction. Booking uses it for an appointment or waitlist change, so the event is stored only if the booking commits. `appendIndependent` opens its own transaction for an audit row recorded after the controller has already returned.

`OutboxPublisher` runs on a schedule when `app.outbox.publisher-enabled` is true. It locks unpublished rows with `FOR UPDATE SKIP LOCKED`, sends them with an idempotent producer (`acks=all`), and sets `published_at`. The Kafka key is the aggregate id. The correlation id is a header. A row stops being retried after `app.outbox.max-attempts` (default 10). Tests set `app.outbox.publisher-enabled=false`.

## Already-processed events

`ProcessedEvent` stores an `eventId` after a consumer handles it. A second delivery of the same id is skipped. `ProcessedEventRetentionJob` deletes old rows. The job follows `spring.task.scheduling.enabled`, which the test profile turns off.

## Dead letters

`KafkaDltAutoConfiguration` retries a failed consumer record, then publishes it to `<topic>.DLT` using `TopicNames.deadLetter`. It is active when the application has a Kafka consumer group. Retry interval and retry count come from `app.kafka.dlt.retry-interval-ms` and `app.kafka.dlt.max-retries`.
