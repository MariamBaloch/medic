INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at,
                               updated_at)
SELECT 1,
       CURRENT_DATE,
       CURRENT_DATE + INTERVAL '1 year',
       '10:00:00',
       '14:00:00',
       30,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_rule ar WHERE ar.doctor_id = 1);

INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at,
                               updated_at)
SELECT 2,
       CURRENT_DATE,
       CURRENT_DATE + INTERVAL '6 months',
       '10:00:00',
       '14:00:00',
       30,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_rule ar WHERE ar.doctor_id = 2);

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day
FROM availability_rule ar
         CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS v(day)
WHERE ar.doctor_id = 1
  AND NOT EXISTS (SELECT 1 FROM availability_rule_days ard WHERE ard.rule_id = ar.id AND ard.days_of_week = v.day);

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day
FROM availability_rule ar
         CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS v(day)
WHERE ar.doctor_id = 2
  AND NOT EXISTS (SELECT 1 FROM availability_rule_days ard WHERE ard.rule_id = ar.id AND ard.days_of_week = v.day);

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 1, CURRENT_DATE + INTERVAL '10 days', '10:00:00', '14:00:00', 'Vacation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = 1 AND ae.reason = 'Vacation');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 1, CURRENT_DATE + INTERVAL '20 days', '10:00:00', '12:00:00', 'Conference', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = 1 AND ae.reason = 'Conference');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT 2, CURRENT_DATE + INTERVAL '5 days', '10:00:00', '14:00:00', 'Sick Leave', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = 2 AND ae.reason = 'Sick Leave');

INSERT INTO appointment (doctor_id, patient_id, patient_user_id, doctor_user_id, patient_name, patient_email,
                         doctor_name, doctor_email, doctor_specialization_id, availability_rule_id,
                         appointment_date, start_time, end_time, booking_fee, status, reason, created_at, updated_at)
SELECT 1,
       1,
       4,
       2,
       'John Doe',
       'patient1@medic.com',
       'Sarah Smith',
       'doctor1@medic.com',
       1,
       (SELECT id FROM availability_rule WHERE doctor_id = 1 LIMIT 1),
       CURRENT_DATE + INTERVAL '1 day',
       '10:00:00',
       '10:30:00',
       150.00,
       'BOOKED',
       'General Checkup',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM appointment a
                  WHERE a.doctor_id = 1
                    AND a.appointment_date = CURRENT_DATE + INTERVAL '1 day'
                    AND a.start_time = '10:00:00');

INSERT INTO appointment (doctor_id, patient_id, patient_user_id, doctor_user_id, patient_name, patient_email,
                         doctor_name, doctor_email, doctor_specialization_id, availability_rule_id,
                         appointment_date, start_time, end_time, booking_fee, status, reason, created_at, updated_at)
SELECT 1,
       2,
       5,
       2,
       'Jane Clark',
       'patient2@medic.com',
       'Sarah Smith',
       'doctor1@medic.com',
       1,
       (SELECT id FROM availability_rule WHERE doctor_id = 1 LIMIT 1),
       CURRENT_DATE + INTERVAL '2 days',
       '10:00:00',
       '10:30:00',
       150.00,
       'BOOKED',
       'Follow up',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM appointment a
                  WHERE a.doctor_id = 1
                    AND a.appointment_date = CURRENT_DATE + INTERVAL '2 days'
                    AND a.start_time = '10:00:00');

INSERT INTO appointment (doctor_id, patient_id, patient_user_id, doctor_user_id, patient_name, patient_email,
                         doctor_name, doctor_email, doctor_specialization_id, availability_rule_id,
                         appointment_date, start_time, end_time, booking_fee, status, reason, created_at, updated_at)
SELECT 2,
       1,
       4,
       3,
       'John Doe',
       'patient1@medic.com',
       'Robert Brown',
       'doctor2@medic.com',
       4,
       (SELECT id FROM availability_rule WHERE doctor_id = 2 LIMIT 1),
       CURRENT_DATE + INTERVAL '3 days',
       '10:00:00',
       '10:15:00',
       200.00,
       'BOOKED',
       'Headache',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1
                  FROM appointment a
                  WHERE a.doctor_id = 2
                    AND a.appointment_date = CURRENT_DATE + INTERVAL '3 days'
                    AND a.start_time = '10:00:00');
