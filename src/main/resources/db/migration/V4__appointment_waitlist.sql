CREATE TABLE appointment_waitlist_entry (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    preferred_start_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    offered_at TIMESTAMPTZ,
    offer_expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_waitlist_patient FOREIGN KEY (patient_id) REFERENCES patient_profile (id),
    CONSTRAINT fk_waitlist_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles (id),
    CONSTRAINT ck_waitlist_status CHECK (status IN ('WAITING', 'OFFERED', 'BOOKED', 'CANCELLED', 'EXPIRED'))
);

CREATE UNIQUE INDEX uq_waitlist_active_patient_slot
    ON appointment_waitlist_entry (patient_id, doctor_id, appointment_date, preferred_start_time)
    WHERE status IN ('WAITING', 'OFFERED');

CREATE UNIQUE INDEX uq_waitlist_active_offer_slot
    ON appointment_waitlist_entry (doctor_id, appointment_date, preferred_start_time)
    WHERE status = 'OFFERED';

CREATE INDEX ix_waitlist_patient_created
    ON appointment_waitlist_entry (patient_id, created_at DESC, id DESC);

CREATE INDEX ix_waitlist_slot_queue
    ON appointment_waitlist_entry (doctor_id, appointment_date, preferred_start_time, created_at, id)
    WHERE status = 'WAITING';

CREATE INDEX ix_waitlist_expired_offers
    ON appointment_waitlist_entry (offer_expires_at)
    WHERE status = 'OFFERED';

ALTER TABLE notification ALTER COLUMN message TYPE TEXT;
