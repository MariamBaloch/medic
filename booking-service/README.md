# booking-service

Appointments, availability, and the waitlist. Port **8081**. Database role `medic_booking`, schema `booking`.

## What this service owns

Booking is the only service that decides whether a slot can be taken.

- A doctor defines a weekly schedule and exceptions here.
- A patient books, reschedules, or cancels here, and joins the waitlist when the slot is already taken.
- The lock that stops two patients from taking the same slot, and the lock that stops one patient from holding two
  overlapping appointments, both run in this database.

Booking does not own accounts. The name, email, specialization, and fee on an appointment are a copy taken when the row
is written. Later profile edits arrive on `identity.user.changed` and `identity.doctor.changed`. When an appointment is
opened, the live profile is loaded from identity. Booking never queries identity's tables.

### How changes are announced

- **Schedule edits** do not go to Kafka. They are an in-process event that bumps the Redis version of that doctor's
  cached week.
- **Appointment and waitlist changes** go to Kafka through the transactional outbox, as `booking.appointment.changed`
  and `booking.waitlist.changed`. Notification and audit consume those. Booking does not send mail.

### Public paths

Through the gateway at `/api/booking`:

| Path                    | What it covers                                      |
|-------------------------|-----------------------------------------------------|
| `/patient/appointments` | List, book, reschedule, cancel, doctor availability |
| `/patient/waitlist`     | Join, list, withdraw, accept                        |
| `/doctors/availability` | Rules, exceptions, calendar, upcoming flags         |
| `/doctors/appointments` | Doctor history and outcome                          |
| `/appointments`         | Statuses and appointment details                    |

## Why rule-based availability

A stored row for every 15–30 minute slot, for every doctor, for months ahead, would be a very large table and a slow
query. Availability is modeled instead:

- **`AvailabilityRule`** is a recurring schedule, for example Monday and Wednesday, 9:00 to 17:00, 30-minute slots, for
  six months.
- **`AvailabilityException`** removes time from that schedule, such as a sick day or a vacation.
- **`Appointment`** is a slot that was actually booked.

When a patient asks for a date range, the service calculates the slots at that moment:

1. Load the doctor's rules that cover the range.
2. Build the slot list in memory.
3. Remove times covered by an exception.
4. Label each remaining slot `AVAILABLE`, `BOOKED`, or `WAITLIST_OFFERED`, based on appointments and active waitlist
   offers.
5. Return booked slots as well, so the patient can join the waitlist.

## Redis schedule cache

The generated week of slots, before booking state is applied, is cached in Redis as `doctor-weekly-schedule`. The key
is the doctor, the Monday of the week, and a per-doctor version. Entries last 24 hours.

- A new appointment does not clear the cache. The booked and waitlist labels are applied after the cached week is read.
- When a rule or exception change commits, booking increments `doctor-schedule-version:{doctorId}`. That is an
  in-process event, not a Kafka message. Older week keys are no longer read.
- Times already in the past are dropped when the week is shown to a patient.

Tests set `app.schedule-cache.enabled=false` and use a no-op cache, so H2 runs do not need Redis to build slots.

## Concurrency and the waitlist

- **Booking.** The `patient-booking:{patientId}` advisory lock is taken first, so one patient cannot keep two
  overlapping appointments. A slot advisory lock then makes requests for the same doctor, date, and start time run one
  at a time. A partial unique index rejects a second active row for that slot.
- **Rescheduling.** `Appointment.version` rejects a stale update. The same locks cover the old slot and the new one. A
  failed move leaves the original appointment in place.
- **Waitlist.** Partial unique indexes block a second active entry for the same patient and slot, and a second offer for
  one slot. A released slot is offered to the earliest eligible patient for 15 minutes. An active offer blocks a normal
  booking. If the patient declines or the offer expires, it moves to the next patient. ShedLock runs the expiry job on
  one booking instance.

See the [Concurrency Guide](../docs/CONCURRENCY.md).

## Configuration

Settings come from environment variables. Compose and Beanstalk provide them, and no secret is stored in the code.

| Variable                                     | Purpose                                              |
|----------------------------------------------|------------------------------------------------------|
| `BOOKING_DB_URL`                             | JDBC URL of the `medic` database                     |
| `BOOKING_DB_USERNAME`, `BOOKING_DB_PASSWORD` | The `medic_booking` database role                    |
| `KAFKA_BOOTSTRAP_SERVERS`                    | Kafka address                                        |
| `REDIS_HOST`, `REDIS_PORT`                   | Redis, used for the schedule cache and token checks  |
| `IDENTITY_URL`                               | Where booking loads live profiles from               |
| `JWT_SECRET`                                 | Validates the caller's token                         |
| `INTERNAL_API_KEY`                           | Authenticates calls to identity's internal endpoints |
| `APP_TIME_ZONE`                              | Time zone used for slot calculation                  |
| `APP_OUTBOX_PUBLISHER_ENABLED`               | Turns the outbox publisher on or off                 |
| `SERVER_PORT`                                | HTTP port, `8081`                                    |

## Run it

The easiest way is the full stack from the repository root: `docker compose up --build`.

To run only this service for development, start its dependencies, install the shared libraries once, then run it:

```bash
docker compose up postgres redis kafka kafka-init
./mvnw -DskipTests install
./mvnw -pl booking-service spring-boot:run
```

The service needs the environment variables above (see `.env.example`). Booking also calls identity for live profiles,
so start `identity-service` as well if you use those endpoints.

Health check: `http://localhost:8081/actuator/health`.

## Tests

Cucumber, from the repository root:

```bash
./mvnw -pl booking-service -am test -Dtest=CucumberTest,ConcurrencyCucumberTest -Dsurefire.failIfNoSpecifiedTests=false
```

`CucumberTest` uses H2. `ConcurrencyCucumberTest` uses Postgres and needs Docker.