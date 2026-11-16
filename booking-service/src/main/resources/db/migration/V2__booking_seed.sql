INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at, updated_at)
SELECT 1, CURRENT_DATE, CURRENT_DATE + 365, TIME '10:00:00', TIME '14:00:00', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM availability_rule
    WHERE doctor_id = 1 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30
);

INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at, updated_at)
SELECT 2, CURRENT_DATE, CURRENT_DATE + 183, TIME '10:00:00', TIME '14:00:00', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM availability_rule
    WHERE doctor_id = 2 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30
);

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day
FROM availability_rule ar
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS v(day)
WHERE ar.doctor_id = 1
  AND ar.start_time = TIME '10:00:00'
  AND ar.end_time = TIME '14:00:00'
  AND ar.slot_minutes = 30
ON CONFLICT (rule_id, days_of_week) DO NOTHING;

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day
FROM availability_rule ar
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS v(day)
WHERE ar.doctor_id = 2
  AND ar.start_time = TIME '10:00:00'
  AND ar.end_time = TIME '14:00:00'
  AND ar.slot_minutes = 30
ON CONFLICT (rule_id, days_of_week) DO NOTHING;

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 1, CURRENT_DATE + 10, TIME '10:00:00', TIME '14:00:00', 'Vacation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception WHERE doctor_id = 1 AND reason = 'Vacation');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 1, CURRENT_DATE + 20, TIME '10:00:00', TIME '12:00:00', 'Conference', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception WHERE doctor_id = 1 AND reason = 'Conference');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 2, CURRENT_DATE + 5, TIME '10:00:00', TIME '14:00:00', 'Sick Leave', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception WHERE doctor_id = 2 AND reason = 'Sick Leave');

INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, created_at, updated_at
)
SELECT
    1, 1, 4, 2,
    'John Doe', 'patient1@medic.com', 'Sarah Smith', 'doctor1@medic.com',
    1, 'Cardiology',
    (SELECT id FROM availability_rule WHERE doctor_id = 1 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1),
    CURRENT_DATE + 1, TIME '10:00:00', TIME '10:30:00',
    150.00, 'BOOKED', 'General Checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM appointment
    WHERE doctor_id = 1 AND appointment_date = CURRENT_DATE + 1 AND start_time = TIME '10:00:00' AND status <> 'CANCELLED'
);

INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, created_at, updated_at
)
SELECT
    1, 2, 5, 2,
    'Jane Clark', 'patient2@medic.com', 'Sarah Smith', 'doctor1@medic.com',
    1, 'Cardiology',
    (SELECT id FROM availability_rule WHERE doctor_id = 1 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1),
    CURRENT_DATE + 2, TIME '10:00:00', TIME '10:30:00',
    150.00, 'BOOKED', 'Follow up', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM appointment
    WHERE doctor_id = 1 AND appointment_date = CURRENT_DATE + 2 AND start_time = TIME '10:00:00' AND status <> 'CANCELLED'
);

INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, created_at, updated_at
)
SELECT
    2, 1, 4, 3,
    'John Doe', 'patient1@medic.com', 'Robert Brown', 'doctor2@medic.com',
    4, 'Neurology',
    (SELECT id FROM availability_rule WHERE doctor_id = 2 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1),
    CURRENT_DATE + 3, TIME '10:00:00', TIME '10:15:00',
    200.00, 'BOOKED', 'Headache', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM appointment
    WHERE doctor_id = 2 AND appointment_date = CURRENT_DATE + 3 AND start_time = TIME '10:00:00' AND status <> 'CANCELLED'
);

INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, created_at, updated_at
)
SELECT
    2, 2, 5, 3,
    'Jane Clark', 'patient2@medic.com', 'Robert Brown', 'doctor2@medic.com',
    4, 'Neurology',
    (SELECT id FROM availability_rule WHERE doctor_id = 2 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1),
    CURRENT_DATE + 4, TIME '13:00:00', TIME '13:30:00',
    200.00, 'BOOKED', 'Migraine', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM appointment
    WHERE doctor_id = 2 AND appointment_date = CURRENT_DATE + 4 AND start_time = TIME '13:00:00' AND status <> 'CANCELLED'
);

INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, cancelled_reason, created_at, updated_at
)
SELECT
    1, 1, 4, 2,
    'John Doe', 'patient1@medic.com', 'Sarah Smith', 'doctor1@medic.com',
    1, 'Cardiology',
    (SELECT id FROM availability_rule WHERE doctor_id = 1 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1),
    CURRENT_DATE + v.day_offset, v.start_time, v.end_time,
    150.00, v.status, v.reason, v.cancelled_reason, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    (-4, TIME '10:00:00', TIME '10:30:00', 'COMPLETED', 'General Checkup', NULL),
    (-3, TIME '10:30:00', TIME '11:00:00', 'COMPLETED', 'Follow up', NULL),
    (-2, TIME '11:00:00', TIME '11:30:00', 'NO_SHOW', 'Consultation', NULL),
    (-1, TIME '11:30:00', TIME '12:00:00', 'CANCELLED', 'Review', 'Review')
) AS v(day_offset, start_time, end_time, status, reason, cancelled_reason)
WHERE NOT EXISTS (
    SELECT 1 FROM appointment a
    WHERE a.doctor_id = 1
      AND a.patient_id = 1
      AND a.appointment_date = CURRENT_DATE + v.day_offset
      AND a.start_time = v.start_time
);

WITH completed_counts AS (
    SELECT appointment.patient_id, COUNT(*) AS completed_count
    FROM appointment
    WHERE appointment.patient_id IN (1, 2, 3)
      AND appointment.status = 'COMPLETED'
      AND appointment.appointment_date < CURRENT_DATE
    GROUP BY appointment.patient_id
),
seed_patients AS (
    SELECT patients.patient_id, COALESCE(completed_counts.completed_count, 0) AS completed_count
    FROM (VALUES (1), (2), (3)) AS patients(patient_id)
    LEFT JOIN completed_counts ON completed_counts.patient_id = patients.patient_id
),
seed_doctor AS (
    SELECT 1 AS doctor_id,
           2 AS doctor_user_id,
           150.00 AS consultation_fee,
           (SELECT id FROM availability_rule WHERE doctor_id = 1 AND start_time = TIME '10:00:00' AND end_time = TIME '14:00:00' AND slot_minutes = 30 ORDER BY id LIMIT 1) AS availability_rule_id
),
candidate_slots AS (
    SELECT seed_patients.patient_id,
           CASE seed_patients.patient_id
               WHEN 1 THEN 4
               WHEN 2 THEN 5
               ELSE 6
           END AS patient_user_id,
           CASE seed_patients.patient_id
               WHEN 1 THEN 'John Doe'
               WHEN 2 THEN 'Jane Clark'
               ELSE 'Alex Morgan'
           END AS patient_name,
           CASE seed_patients.patient_id
               WHEN 1 THEN 'patient1@medic.com'
               WHEN 2 THEN 'patient2@medic.com'
               ELSE 'patient3@medic.com'
           END AS patient_email,
           seed_doctor.doctor_id,
           seed_doctor.doctor_user_id,
           seed_doctor.availability_rule_id,
           seed_doctor.consultation_fee,
           CURRENT_DATE - (30 + seed_patients.patient_id * 10 + slot_number.candidate_number) AS appointment_date,
           TIME '10:00:00' AS start_time,
           TIME '10:30:00' AS end_time,
           slot_number.candidate_number,
           seed_patients.completed_count
    FROM seed_patients
    CROSS JOIN (VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)) AS slot_number(candidate_number)
    CROSS JOIN seed_doctor
    WHERE seed_patients.completed_count < 3
      AND seed_doctor.availability_rule_id IS NOT NULL
),
available_candidates AS (
    SELECT candidate_slots.*
    FROM candidate_slots
    WHERE NOT EXISTS (
        SELECT 1
        FROM appointment
        WHERE appointment.doctor_id = candidate_slots.doctor_id
          AND appointment.appointment_date = candidate_slots.appointment_date
          AND appointment.start_time = candidate_slots.start_time
          AND appointment.status <> 'CANCELLED'
    )
),
ranked_candidates AS (
    SELECT available_candidates.*,
           ROW_NUMBER() OVER (
               PARTITION BY available_candidates.patient_id
               ORDER BY available_candidates.candidate_number
           ) AS candidate_rank
    FROM available_candidates
)
INSERT INTO appointment (
    doctor_id, patient_id, patient_user_id, doctor_user_id,
    patient_name, patient_email, doctor_name, doctor_email,
    doctor_specialization_id, doctor_specialization_name,
    availability_rule_id, appointment_date, start_time, end_time,
    booking_fee, status, reason, created_at, updated_at
)
SELECT doctor_id, patient_id, patient_user_id, doctor_user_id,
       patient_name, patient_email, 'Sarah Smith', 'doctor1@medic.com',
       1, 'Cardiology',
       availability_rule_id, appointment_date, start_time, end_time,
       consultation_fee, 'COMPLETED', 'Historical checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM ranked_candidates
WHERE candidate_rank <= 3 - completed_count;