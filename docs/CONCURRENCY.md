# Appointment Concurrency

Medic uses database transactions and locks to prevent simultaneous requests from booking the same slot or creating
conflicting appointments. Database constraints provide an extra safeguard if requests happen at the same time.

## Booking and Rescheduling

Before checking for overlapping appointments, booking and rescheduling lock the patient's database row using
`PESSIMISTIC_WRITE`. If the same patient sends two requests at once, one must wait for the other to finish before
checking the latest data.

Medic also locks the specific doctor, date, and start time using a PostgreSQL transaction-level advisory lock. Requests
for the same slot must wait for one another, while requests for different slots can proceed independently. Rescheduling
locks both the old and new slots in a consistent order to reduce deadlocks. These locks require an active transaction.

Before booking, the service checks the doctor's schedule, whether a waitlist offer reserves the slot, whether another
appointment already occupies it, and whether the patient has an overlapping appointment with another doctor. This
partial unique index prevents two active appointments from using the same doctor, date, and start time:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_active_doctor_slot
    ON appointment (doctor_id, appointment_date, start_time)
    WHERE status <> 'CANCELLED';
```

Cancelled appointments do not prevent the slot from being booked again. The index protects against duplicate start
times, while the service checks the schedule and overlapping appointments. Booking conflicts return `409 Conflict`.

Rescheduling checks that the appointment belongs to the patient, is booked and in the future, and has the version the
patient last saw (`expectedVersion`). It locks both slots, reloads the appointment, repeats the necessary checks, and
updates the existing record. If validation fails, the original appointment remains unchanged. After a successful
reschedule, Medic checks the old slot and offers it to the next eligible patient on its waitlist.

Booking and rescheduling also check for expired waitlist offers before deciding whether a slot is reserved, so they can
expire outdated offers and notify the next eligible patient immediately without waiting for the scheduled
AppointmentWaitlistExpiryProcessor to run every 30 seconds.

## Appointment Waitlist

Patients can join the waitlist for a valid future slot that cannot be booked normally, provided they have no overlapping
appointment or active waitlist entry for that slot. Patients are considered in the order they joined, using `createdAt`
and then ID. Entries can be `WAITING`, `OFFERED`, `BOOKED`, `CANCELLED`, or `EXPIRED`.

Multiple patients can wait for the same slot, but only one can hold an active offer at a time. The database enforces the
following constraints:

- **`uq_waitlist_active_patient_slot`**: Prevents a patient from having duplicate active waitlist entries (`WAITING` or
  `OFFERED`) for the same doctor, date, and start time.

- **`uq_waitlist_active_offer_slot`**: Ensures only one waitlist entry can have an `OFFERED` status for a particular
  doctor, date, and start time.

The slot-level advisory lock ensures that concurrent operations on the same slot are processed one at a time.

When a slot becomes available, Medic offers it to the earliest eligible patient. The offer lasts 15 minutes. Patients
who already have an overlapping appointment are skipped for that offer but remain in `WAITING`. Accepting an offer
creates the appointment and marks the waitlist entry `BOOKED` in the same transaction. Patients can leave the waitlist
while waiting; withdrawing an active offer allows Medic to offer the slot to the next eligible patient.

`AppointmentWaitlistExpiryProcessor` checks for expired offers every 30 seconds, processing up to 100 entries per run.
For each entry, the service locks the slot, checks whether the offer has expired, updates its status, and offers the
slot to the next eligible patient. Booking and rescheduling also check for expired offers when someone requests a slot.

## Notifications

Medic saves notifications in the business transaction and sends them after commit. When a slot is offered, the selected
patient is notified. When an offer is accepted, both the patient and doctor are notified.

SSE updates are sent after commit, and emails are submitted to a bounded executor with two core threads, up to four
threads, and a queue of 100 tasks. Email failures are logged and do not undo a completed booking. If the executor is
full, `CallerRunsPolicy` may send the email on the request thread, slowing the response. There is no persistent email
outbox, so an email can be lost if the application stops after the transaction commits but before the email task is
submitted.

## Tests

- `AppointmentWaitlistH2Test`: checks queue priority after cancellation, the offer window, declining an offer, handoff
  to
  the next patient, and booking after acceptance.
- `AppointmentRescheduleH2Test`: checks rescheduling updates and versions, past and completed appointments, and rollback
  behavior.
- `AppointmentConcurrencyIntegrationTest` (PostgreSQL/Testcontainers): covers same-slot booking races, the advisory lock
  blocking on the same slot while allowing a different slot for the same doctor, patient-overlap and rescheduling races,
  cancellation/rebooking, and rejection of ordinary booking while an offer is active.
- `AppointmentConstraintIntegrationTest` (PostgreSQL/Testcontainers): verifies the appointment active-slot index maps to
  `409 Conflict`, multiple patients can wait for one slot, and the partial indexes reject duplicate active entries and
  simultaneous offers.

H2 tests cover business behavior. PostgreSQL tests verify database constraints, advisory locking, and races.

Run the full test suite with the Maven wrapper:

```bash
./mvnw test
```

In Windows PowerShell, use `.\mvnw.cmd test`.

PostgreSQL tests use Testcontainers and require Docker. Run all PostgreSQL appointment integration tests with:

```powershell
.\mvnw.cmd '-Dtest=AppointmentConcurrencyIntegrationTest,AppointmentConstraintIntegrationTest' test
```
