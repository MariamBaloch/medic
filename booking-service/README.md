# booking-service

Appointments, availability, and the waitlist. Port **8081**. Database role `medic_booking`, schema `booking`.

## What this service owns

Booking is the only service that decides whether a slot can be taken. A doctor defines a weekly schedule and exceptions here. A patient books, reschedules, or cancels here, and joins the waitlist when the slot is already taken. The lock that stops two patients from taking the same slot, and the lock that stops one patient from holding two overlapping appointments, both run in this database.

It does not own accounts. The name, email, specialization, and fee on an appointment are a copy taken when the row is written. Later profile edits arrive on `identity.user.changed` and `identity.doctor.changed`. Opening an appointment loads the live profile from identity. Booking never queries identity's tables.

A schedule edit does not go to Kafka. It is an in-process event that bumps the Redis version of that doctor's cached week. Appointment and waitlist changes do go to Kafka, through the outbox, as `booking.appointment.changed` and `booking.waitlist.changed`. Notification and audit consume those. Booking does not send mail.

Public paths, through the gateway at `/api/booking`:

- `/patient/appointments` — list, book, reschedule, cancel, doctor availability
- `/patient/waitlist` — join, list, withdraw, accept
- `/doctors/availability` — rules, exceptions, calendar, upcoming flags
- `/doctors/appointments` — doctor history and outcome
- `/appointments` — statuses and appointment details

Booking publishes `booking.appointment.changed` and `booking.waitlist.changed`.

## Why rule-based availability

A stored row for every 15–30 minute slot, for every doctor, for months ahead, would be a very large table and a slow query. Availability is modeled instead:

- **`AvailabilityRule`** is a recurring schedule, for example Monday and Wednesday, 9:00 to 17:00, 30-minute slots, for six months.
- **`AvailabilityException`** removes time from that schedule, such as a sick day or a vacation.
- **`Appointment`** is a slot that was actually booked.

When a patient asks for a date range, the service calculates slots then:

1. Load the doctor's rules that cover the range.
2. Build the slot list in memory.
3. Remove times covered by an exception.
4. Label each remaining slot `AVAILABLE`, `BOOKED`, or `WAITLIST_OFFERED` from appointments and active waitlist offers.
5. Return booked slots as well, so the patient can join the waitlist.

## Redis schedule cache

The generated week of slots, before booking state is applied, is cached in Redis as `doctor-weekly-schedule`. The key is the doctor, the Monday of the week, and a per-doctor version. Entries last 24 hours.

A new appointment does not clear the cache. Booked and waitlist labels are applied after the cached week is read. When a rule or exception change commits, booking increments `doctor-schedule-version:{doctorId}`. That is an in-process event, not a Kafka message. Older week keys are no longer read. Times already in the past are dropped when the week is shown to a patient.

Tests set `app.schedule-cache.enabled=false` and use a no-op cache, so H2 runs do not need Redis to build slots.

## Concurrency and the waitlist

- **Booking.** The `patient-booking:{patientId}` advisory lock is taken first, so one patient cannot keep two overlapping appointments. A slot advisory lock then serializes requests for the same doctor, date, and start time. A partial unique index rejects a second active row for that slot.
- **Rescheduling.** `Appointment.version` rejects a stale update. The same locks cover the old slot and the new one. A failed move leaves the original appointment in place.
- **Waitlist.** Partial unique indexes block a second active entry for the same patient and slot, and a second offer for one slot. A released slot is offered to the earliest eligible patient for 15 minutes. An active offer blocks a normal booking. Decline or expiry moves the offer to the next patient. ShedLock runs the expiry job on one booking instance.

See the [Concurrency Guide](../docs/CONCURRENCY.md).

```bash
./mvnw -pl booking-service -am spring-boot:run
```

Cucumber, from the repo root:

```bash
./mvnw -pl booking-service -am test -Dtest=CucumberTest,ConcurrencyCucumberTest -Dsurefire.failIfNoSpecifiedTests=false
```

`CucumberTest` uses H2. `ConcurrencyCucumberTest` uses Postgres and needs Docker.
