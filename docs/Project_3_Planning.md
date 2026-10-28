# Project 3 — Planning

## Day 1 — Scope, database baseline, and concurrency analysis

- Used the existing ten-entity model and dynamic availability calculation as the Project 3 baseline.
- Verified the Project 2 Flyway V1 baseline and configured the H2 and PostgreSQL/Testcontainers profiles.
- Identified the booking check-then-insert race: separate requests can both see an open doctor slot before either
  appointment is saved.

## Day 2 — Appointment concurrency controls

- Added `Appointment.version`, patient-profile row locking, and the PostgreSQL partial unique index for active exact
  doctor/date/start-time slots.
- Mapped the named index conflict to `409 Conflict` and retained cancellation-based slot reuse.

## Day 3 — Rescheduling backend and frontend

- Implement the transactional rescheduling API with ownership, version, availability, and overlap checks; preserve the
  original appointment when rescheduling fails.
- Build the appointment booking and rescheduling UI using the existing APIs.
- Display available slots and show clear outcomes for successful bookings, reschedules, and conflicts.

## Day 4 — Waitlist functionality

- Support joining a waitlist for an exact doctor, date, and start time.
- Offer released slots to the earliest eligible patient, reserve the slot during the 15-minute offer window, and advance
  the queue after a decline or expiry.

## Day 5 And Day 6 — Waitlist frontend and tests

- Add UI to search and filter a patient's waitlist by doctor, date range, and status, with pagination; allow joining,
  withdrawing, and accepting an offer.
- Cover the H2 business workflow and PostgreSQL partial-index, advisory-lock, and offer-priority behavior with tests.
- Update documentations and prepare to presentation.