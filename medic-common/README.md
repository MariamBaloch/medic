# medic-common

Shared types. This module is a plain jar. It has no `main` class, no database, and no HTTP server. Services depend on it so a booking event and an identity snapshot use the same field names in every process.

## Event contracts

`EventEnvelope` is the JSON wrapper on every Kafka message: `eventId`, `eventType`, `occurredAt`, `producer`, `aggregateType`, `aggregateId`, `correlationId`, and `payload`. The payload is a map. It is never a JPA entity.

`EventTypes` holds the dotted strings, such as `booking.appointment.created` and `identity.doctor.updated`. Those names are not renamed to words like `AppointmentBooked`.

`TopicNames` is the topic list. `forEventType` maps a dotted event type onto one topic:

| Event type prefix | Topic |
|---|---|
| `booking.appointment.` | `booking.appointment.changed` |
| `booking.waitlist.` | `booking.waitlist.changed` |
| `identity.user.` | `identity.user.changed` |
| `identity.doctor.` | `identity.doctor.changed` |
| `audit.event.recorded` | `audit.event.recorded` |

`deadLetter` appends `.DLT` to a topic name. Several event types share one topic. The event type inside the envelope says what happened.

## Identity contracts

Booking and identity do not share tables. They share these records instead.

- `InternalPatientSnapshotResponse` and `InternalDoctorSnapshotResponse` are the fields copied onto an appointment or waitlist row: ids, names, emails, and for a doctor the specialization and fee.
- `InternalPatientProfileResponse` and `InternalDoctorProfileResponse` are the live profiles, including photo, qualification, and biography. Booking asks identity for these when a user opens an appointment.

## JWT claim names

`JwtClaims` is the set of claim keys identity writes and every other service reads: `uid`, `roles`, `email`, `ver`, `patientId`, `doctorId`, `name`, and `jti`. The constants live here so a producer and a consumer cannot spell the same claim two ways.

`CorrelationId` is the MDC key. The gateway filter in `medic-web` sets it. The outbox copies it onto the envelope.
