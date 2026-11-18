# medic-common

Shared contracts. This module is a plain jar: it has no `main` class, no database, and no HTTP server, and it does not
depend on Spring, JPA, Kafka, or Redis. The gateway depends on this module only, so it must stay light.

Services depend on it so that an event or a snapshot uses the same field names in every process.

## Event contracts

**`EventEnvelope`** is the JSON wrapper on every Kafka message. Its fields are `eventId`, `eventType`, `occurredAt`,
`producer`, `aggregateType`, `aggregateId`, `correlationId`, and `payload`. The payload is a map. It is never a JPA
entity.

**`EventTypes`** holds the dotted names, such as `booking.appointment.created` and `identity.doctor.updated`. These are
kept as dotted strings and are not renamed to words like `AppointmentBooked`.

**`TopicNames`** is the list of topics. `forEventType` maps an event type to its topic by prefix:

| Event type prefix      | Topic                         |
|------------------------|-------------------------------|
| `booking.appointment.` | `booking.appointment.changed` |
| `booking.waitlist.`    | `booking.waitlist.changed`    |
| `identity.user.`       | `identity.user.changed`       |
| `identity.doctor.`     | `identity.doctor.changed`     |
| `audit.event.recorded` | `audit.event.recorded`        |

Several event types share one topic, so the event type inside the envelope says what happened. `deadLetter` adds
`.DLT` to a topic name to get its dead-letter topic.

## Identity contracts

Booking and identity do not share tables. They share these records instead:

- **`InternalPatientSnapshotResponse`** and **`InternalDoctorSnapshotResponse`** hold the fields copied onto an
  appointment or waitlist row: ids, names, emails, and for a doctor the specialization and fee.
- **`InternalPatientProfileResponse`** and **`InternalDoctorProfileResponse`** are the live profiles, including photo,
  qualification, and biography. Booking asks identity for these when a user opens an appointment.

## JWT claim names

**`JwtClaims`** lists the claim keys that identity writes and every other service reads: `uid`, `roles`, `email`, `ver`,
`patientId`, `doctorId`, `name`, and `jti`. The names live here so a producer and a consumer cannot spell the same
claim two ways.

## Correlation id

**`CorrelationId`** holds the header name (`X-Correlation-Id`), the logging key, and the rule for accepting an incoming
id or creating a new one. The gateway creates the id at the edge, the filter in