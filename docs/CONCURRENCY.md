# Appointment Concurrency

## Booking

Medic calculates available times from a doctor's availability rules, availability exceptions, and existing appointments.
Booking runs in a
database transaction.

Before checking a patient's overlapping appointments, the service locks that patient's profile row using
`PESSIMISTIC_WRITE`. If the same patient sends two requests at once, one waits for the first transaction to commit or
roll back. The second then checks the current database state. It can also succeed if its request is still valid; if it
conflicts with the first change, it returns `409 Conflict` and saves no changes.

For two different patients trying the same doctor's date and start time, PostgreSQL uses this partial unique index:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_active_doctor_slot
    ON appointment (doctor_id, appointment_date, start_time)
    WHERE status <> 'CANCELLED';
```

Only one active appointment can use that exact doctor/date/start-time combination. If the insert loses the race, its
transaction rolls back and the API returns `409 Conflict`. Cancelled appointments do not occupy the index key, so the
slot can be booked again.

The index checks identical start times. The availability rules generate the normal slots, and the service also checks
overlapping appointment times for the same patient across doctors.

## Rescheduling

Each appointment response includes a `version`. A reschedule request sends that value as `expectedVersion`. The service
locks the patient's profile, loads the appointment, checks version, validates the requested time using
dynamic availability, and updates the existing appointment row.

If the version is stale, the target time is unavailable, or the database detects a slot conflict, the transaction rolls
back and the API returns `409 Conflict`. The original appointment remains unchanged.

## Notifications

The notification is saved in the appointment transaction. After commit, Medic sends the SSE update and queues the email
on a bounded executor with 2 core threads, up to 4 threads, and a queue of 100 tasks. Email errors are logged and do not
undo the appointment.

When the executor is full, `CallerRunsPolicy` sends the email on the request/commit thread, which can delay the
response.
There is no persistent email outbox, so a process crash after commit but before email submission can lose the email. If
a booking or reschedule returns `409 Conflict` and rolls back, no SSE notification or email is sent; the caller receives
the HTTP conflict response.

## Tests

- `AppointmentConcurrencyIntegrationTest` sends authenticated HTTP requests to PostgreSQL. It tests same-slot races,
  independent bookings, patient overlaps across doctors, stale-version reschedules, a booking competing with a
  reschedule, and cancellation/rebooking. Latches start requests together; tests check HTTP responses and final rows.
- `AppointmentConstraintIntegrationTest` checks that a PostgreSQL active-slot index violation returns `409 Conflict`.
- `AppointmentRescheduleH2Test` checks the persisted update/version, past and completed appointments, and
  exception-blocked rollback. H2 tests cover business rules; PostgreSQL tests verify locking and the partial index.

Run all tests with `./mvnw.cmd clean test`. PostgreSQL tests use Testcontainers and require Docker. To run those two
PostgreSQL test classes only:

```bash
./mvnw -Dtest=AppointmentConcurrencyIntegrationTest,AppointmentConstraintIntegrationTest test
```
