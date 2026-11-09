ALTER TABLE appointment
    ADD COLUMN IF NOT EXISTS patient_user_id          BIGINT,
    ADD COLUMN IF NOT EXISTS doctor_user_id           BIGINT,
    ADD COLUMN IF NOT EXISTS patient_name             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS patient_email            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_name              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_email             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_specialization_id BIGINT;

ALTER TABLE appointment_waitlist_entry
    ADD COLUMN IF NOT EXISTS patient_user_id          BIGINT,
    ADD COLUMN IF NOT EXISTS doctor_user_id           BIGINT,
    ADD COLUMN IF NOT EXISTS patient_name             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS patient_email            VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_name              VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_email             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS doctor_specialization_id BIGINT;

UPDATE appointment AS a
SET patient_user_id          = pu.id,
    doctor_user_id           = du.id,
    patient_name             = TRIM(BOTH FROM CONCAT(COALESCE(pu.first_name, ''), ' ', COALESCE(pu.last_name, ''))),
    patient_email            = pu.email,
    doctor_name              = TRIM(BOTH FROM CONCAT(COALESCE(du.first_name, ''), ' ', COALESCE(du.last_name, ''))),
    doctor_email             = du.email,
    doctor_specialization_id = dp.specialization_id
FROM patient_profile AS pp,
     users AS pu,
     doctor_profiles AS dp,
     users AS du
WHERE a.patient_id = pp.id
  AND pp.user_id = pu.id
  AND a.doctor_id = dp.id
  AND dp.user_id = du.id;

UPDATE appointment_waitlist_entry AS w
SET patient_user_id          = pu.id,
    doctor_user_id           = du.id,
    patient_name             = TRIM(BOTH FROM CONCAT(COALESCE(pu.first_name, ''), ' ', COALESCE(pu.last_name, ''))),
    patient_email            = pu.email,
    doctor_name              = TRIM(BOTH FROM CONCAT(COALESCE(du.first_name, ''), ' ', COALESCE(du.last_name, ''))),
    doctor_email             = du.email,
    doctor_specialization_id = dp.specialization_id
FROM patient_profile AS pp,
     users AS pu,
     doctor_profiles AS dp,
     users AS du
WHERE w.patient_id = pp.id
  AND pp.user_id = pu.id
  AND w.doctor_id = dp.id
  AND dp.user_id = du.id;

ALTER TABLE appointment
    ALTER COLUMN patient_user_id SET NOT NULL,
    ALTER COLUMN doctor_user_id SET NOT NULL,
    ALTER COLUMN patient_name SET NOT NULL,
    ALTER COLUMN patient_email SET NOT NULL,
    ALTER COLUMN doctor_name SET NOT NULL,
    ALTER COLUMN doctor_email SET NOT NULL;

ALTER TABLE appointment_waitlist_entry
    ALTER COLUMN patient_user_id SET NOT NULL,
    ALTER COLUMN doctor_user_id SET NOT NULL,
    ALTER COLUMN patient_name SET NOT NULL,
    ALTER COLUMN patient_email SET NOT NULL,
    ALTER COLUMN doctor_name SET NOT NULL,
    ALTER COLUMN doctor_email SET NOT NULL;

ALTER TABLE appointment
    DROP CONSTRAINT IF EXISTS fk_appointment_doctor;
ALTER TABLE appointment
    DROP CONSTRAINT IF EXISTS fk_appointment_patient;
ALTER TABLE appointment_waitlist_entry
    DROP CONSTRAINT IF EXISTS fk_waitlist_doctor;
ALTER TABLE appointment_waitlist_entry
    DROP CONSTRAINT IF EXISTS fk_waitlist_patient;
ALTER TABLE availability_rule
    DROP CONSTRAINT IF EXISTS fk_availability_rule_doctor;
ALTER TABLE availability_exception
    DROP CONSTRAINT IF EXISTS fk_availability_exception_doctor;

CREATE INDEX IF NOT EXISTS idx_appointment_doctor_name ON appointment (doctor_name);
CREATE INDEX IF NOT EXISTS idx_appointment_patient_name ON appointment (patient_name);
CREATE INDEX IF NOT EXISTS idx_appointment_doctor_specialization ON appointment (doctor_specialization_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_doctor_name ON appointment_waitlist_entry (doctor_name);
