# Project 4 — Planning

Two weeks. The running system is the Project 3 booking product, split into services that share one Postgres database. Each day uses what the previous day left in place.

## Week 1

### Day 1 — Shared modules and the gateway

- Split the build into `medic-common`, `medic-security`, `medic-messaging`, and `medic-web`.
- Moved event envelopes, topic names, and dotted event types (`booking.appointment.created` and the rest) into `medic-common`.
- Moved the JWT filter, claims principal, token blacklist, and token version checks into `medic-security`.
- Moved the transactional outbox, processed-event store, and Kafka dead-letter handling into `medic-messaging`.
- Added `gateway-service` on port 8080 as the public edge: `/api/identity`, `/api/booking`, `/api/notification`, and `/api/audit`, each with `StripPrefix=2`. CORS, correlation ids, and Redis rate limiting stay on the gateway. `/internal/**` is not routed.

### Day 2 — Outbox, before any service is extracted

- Domain events are written to `outbox_event` in the same transaction as the booking or waitlist change.
- A publisher sends unpublished rows with `FOR UPDATE SKIP LOCKED`, then marks them published. A duplicate `eventId` is ignored by consumers. Failed messages go to the topic's `.DLT`.
- Appointment mail is no longer sent inside the booking process. The outbox row is what notification will consume once that service exists.

### Day 3 — Compose and Kafka, before the first extract

- Compose starts Postgres, Redis, Kafka in KRaft, topic creation, Mailpit, and the services.
- Init creates one `medic` database, the four service roles, and the `identity`, `booking`, `notification`, and `audit` schemas.
- Topics created here are `identity.user.changed`, `identity.doctor.changed`, `booking.appointment.changed`, `booking.waitlist.changed`, and `audit.event.recorded`, each with a `.DLT`.
- Nothing is extracted yet. The broker and the database layout have to exist first, or the next services have nowhere to run.

### Day 4 — Claims, snapshots, and locks, still in one database

- The JWT is issued with user id, roles, email, name, token version, and patient or doctor id. Other processes only validate it. They no longer load a user row on every request.
- Logout and password change still bump Redis. An expired token returns 401 from the shared filter.
- Appointment and waitlist rows gain snapshot columns, including specialization name and fee, so booking will not need a foreign key into identity.
- Patient booking uses the `patient-booking:{patientId}` advisory lock, taken before the slot lock. ShedLock is ready for the waitlist expiry job.

### Day 5 — Notification service, the first Kafka consumer

- Moved in-app notifications, unread counts, SSE, and email to `notification-service` on port 8084, role `medic_notification`, schema `notification`.
- It consumes `booking.appointment.changed` and `booking.waitlist.changed`. A new booking notifies the doctor. The patient is not notified for that event. Email is sent after the notification row commits.
- Verification and password-reset mail stay in identity and are not published. Those messages carry tokens, and notification does not listen to identity topics.

## Week 2

### Day 6 — Audit service

- Moved the admin audit log to `audit-service` on port 8085, role `medic_audit`, schema `audit`.
- It consumes `audit.event.recorded`. `GET /` returns the log newest first and requires the admin role.
- This is a small consumer, so it lands before identity and booking are split out.

### Day 7 — Identity service

- Moved registration, login, email verification, password reset, profiles, avatars, specializations, admin actions, and the doctor directory to `identity-service` on port 8082, role `medic_identity`, schema `identity`.
- Identity is the only service that issues JWTs. It publishes `identity.user.changed` and `identity.doctor.changed`.
- Added `/internal/**` snapshot and live-profile reads, protected by `X-Internal-Key`. The gateway does not expose them. Booking will call these on a snapshot miss.

### Day 8 — Booking service

- Booking is the last extract. It keeps availability, appointments, waitlist, and the slot locks, on role `medic_booking` and schema `booking`.
- It reads identity only through the internal client. Party data on an appointment is the snapshot from Day 4, not a live identity row.
- Cached each doctor's generated week of slots in Redis (`doctor-weekly-schedule`). The key is doctor, week, and a per-doctor version. Entries live 24 hours. The cache stores schedule slots only. Booked and waitlist state is applied after the read, so a new appointment does not invalidate the week.
- After a rule or exception change commits, booking increments `doctor-schedule-version:{doctorId}`. That bump is an in-process event, not a Kafka topic. Older week keys stop being read. Past times are filtered when the week is shown to a patient.
- Tests set `app.schedule-cache.enabled=false` and use a no-op cache, so H2 runs do not need Redis to build slots.

### Day 9 — Configuration, Cucumber, and the missing API docs

- Removed the unused Docker Spring profiles. Compose sets container hostnames. Local defaults stay in `application.properties`.
- Added `prod` profiles where deployment behavior changes: required settings, no API docs, no health details, and no localhost fallback.
- Booking no longer depends on mail. Its mail health check is off. The production gateway sets forwarded headers to none, because it is the public edge.
- Scheduled jobs follow `spring.task.scheduling.enabled`, so the H2 tests do not run the waitlist job.
- Cucumber on booking covers an open-slot booking, a rejected second booking, a cancel that reopens the slot, anonymous access (401), and a health body whose status is exactly `UP`. The ten-patient race runs on Postgres: one success and nine conflicts.
- Added Swagger summaries only where they were missing: upcoming availability, the audit list, the notification read and subscribe routes, and the internal profile reads.

### Day 10 — Production deploy and docs

- Added `deploy/docker-compose.prod.yml` and `deploy/.env.example`. Only the gateway is published, on port 80. Postgres, Mailpit, and Kafka UI are outside the file. Redis and Kafka have no host ports. Each JVM uses `MaxRAMPercentage=70` inside a 512 MB limit.
- Jenkins builds images, pushes `566914242591.dkr.ecr.us-east-1.amazonaws.com/medic/<service>:<commit>`, and updates Elastic Beanstalk environment `medic-env-1`.
- The Beanstalk zip is `docker-compose.yml` plus `docker/kafka/create-topics.sh`. Image registry and tag are environment properties.
- RDS keeps the private `medic` database. Service roles and schemas are created from inside the VPC, not by the local Postgres entrypoint script.
- Updated this plan, the module READMEs, and the repository README.
