ALTER TABLE appointment
    DROP CONSTRAINT IF EXISTS uc_appointment_doctor_date_time;

CREATE UNIQUE INDEX IF NOT EXISTS uq_appointment_active_doctor_slot
    ON appointment (doctor_id, appointment_date, start_time)
    WHERE status <> 'CANCELLED';
