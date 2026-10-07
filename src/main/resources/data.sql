-- Seed Roles
INSERT INTO roles (name, created_at, updated_at) VALUES
('ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DOCTOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PATIENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Seed Specializations
INSERT INTO specializations (name, description, created_at, updated_at) VALUES
('Cardiology', 'Heart and blood vessels', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dermatology', 'Skin conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pediatrics', 'Children and infants', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Neurology', 'Nervous system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Orthopedics', 'Bones and muscles', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Seed Users
-- Admin
INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'admin@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'System', 'Admin', '1980-01-01', '1234567890', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;

-- Doctor 1
INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'doctor1@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Sarah', 'Smith', '1985-05-15', '9876543211', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'DOCTOR'
ON CONFLICT (email) DO NOTHING;

-- Doctor 2
INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'doctor2@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Robert', 'Brown', '1978-08-22', '9876543212', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'DOCTOR'
ON CONFLICT (email) DO NOTHING;

-- Patient 1
INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'patient1@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'John', 'Doe', '1990-10-20', '5551234561', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'PATIENT'
ON CONFLICT (email) DO NOTHING;

-- Patient 2
INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'patient2@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Jane', 'Clark', '1992-12-10', '5551234562', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'PATIENT'
ON CONFLICT (email) DO NOTHING;

-- Seed Doctor Profiles
INSERT INTO doctor_profiles (user_id, specialization_id, license_number, qualification, years_of_experience, consultation_fee, hospital_affiliation, bio, is_verified, created_at, updated_at)
SELECT u.id, s.id, 'MD10001', 'MBBS, MD', 10, 150.00, 'City Hospital', 'Experienced cardiologist.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, specializations s
WHERE u.email = 'doctor1@medic.com' AND s.name = 'Cardiology'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO doctor_profiles (user_id, specialization_id, license_number, qualification, years_of_experience, consultation_fee, hospital_affiliation, bio, is_verified, created_at, updated_at)
SELECT u.id, s.id, 'MD10002', 'MBBS, DO', 15, 200.00, 'General Hospital', 'Expert in neurology.', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u, specializations s
WHERE u.email = 'doctor2@medic.com' AND s.name = 'Neurology'
ON CONFLICT (user_id) DO NOTHING;

-- Seed Patient Profiles
INSERT INTO patient_profile (user_id, address, city, country, blood_type, height_cm, weight_kg, allergies, chronic_conditions, created_at, updated_at)
SELECT u.id, '123 Main St', 'Metropolis', 'Countryland', 'O_POSITIVE', 180, 75, 'Peanuts', 'None', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'patient1@medic.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO patient_profile (user_id, address, city, country, blood_type, height_cm, weight_kg, allergies, chronic_conditions, created_at, updated_at)
SELECT u.id, '456 Elm St', 'Gotham', 'Countryland', 'A_NEGATIVE', 165, 60, 'Dust', 'Asthma', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'patient2@medic.com'
ON CONFLICT (user_id) DO NOTHING;

-- Seed Availability Rules
-- Doctor 1: Weekdays 9-5
INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at, updated_at)
SELECT dp.id, CURRENT_DATE, CURRENT_DATE + INTERVAL '1 year', '09:00:00', '17:00:00', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor1@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_rule ar WHERE ar.doctor_id = dp.id);

-- Doctor 2: MWF 10-2
INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at, updated_at)
SELECT dp.id, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 months', '10:00:00', '14:00:00', 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor2@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_rule ar WHERE ar.doctor_id = dp.id AND start_time = '10:00:00');

-- Doctor 2: TTh 14-18
INSERT INTO availability_rule (doctor_id, start_date, end_date, start_time, end_time, slot_minutes, created_at, updated_at)
SELECT dp.id, CURRENT_DATE, CURRENT_DATE + INTERVAL '6 months', '14:00:00', '18:00:00', 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor2@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_rule ar WHERE ar.doctor_id = dp.id AND start_time = '14:00:00');

-- Seed Availability Days
INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day FROM availability_rule ar JOIN doctor_profiles dp ON dp.id = ar.doctor_id JOIN users u ON u.id = dp.user_id
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS v(day)
WHERE u.email = 'doctor1@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_rule_days ard WHERE ard.rule_id = ar.id AND ard.days_of_week = v.day);

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day FROM availability_rule ar JOIN doctor_profiles dp ON dp.id = ar.doctor_id JOIN users u ON u.id = dp.user_id
CROSS JOIN (VALUES ('MONDAY'), ('WEDNESDAY'), ('FRIDAY')) AS v(day)
WHERE u.email = 'doctor2@medic.com' AND ar.start_time = '10:00:00'
AND NOT EXISTS (SELECT 1 FROM availability_rule_days ard WHERE ard.rule_id = ar.id AND ard.days_of_week = v.day);

INSERT INTO availability_rule_days (rule_id, days_of_week)
SELECT ar.id, day FROM availability_rule ar JOIN doctor_profiles dp ON dp.id = ar.doctor_id JOIN users u ON u.id = dp.user_id
CROSS JOIN (VALUES ('TUESDAY'), ('THURSDAY')) AS v(day)
WHERE u.email = 'doctor2@medic.com' AND ar.start_time = '14:00:00'
AND NOT EXISTS (SELECT 1 FROM availability_rule_days ard WHERE ard.rule_id = ar.id AND ard.days_of_week = v.day);

-- Seed Availability Exceptions
INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT dp.id, CURRENT_DATE + INTERVAL '10 days', '09:00:00', '17:00:00', 'Vacation', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor1@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = dp.id AND reason = 'Vacation');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT dp.id, CURRENT_DATE + INTERVAL '20 days', '09:00:00', '12:00:00', 'Conference', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor1@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = dp.id AND reason = 'Conference');

INSERT INTO availability_exception (doctor_id, exception_date, start_time, end_time, reason, created_at, updated_at)
SELECT dp.id, CURRENT_DATE + INTERVAL '5 days', '10:00:00', '14:00:00', 'Sick Leave', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users u ON u.id = dp.user_id WHERE u.email = 'doctor2@medic.com'
AND NOT EXISTS (SELECT 1 FROM availability_exception ae WHERE ae.doctor_id = dp.id AND reason = 'Sick Leave');

-- Seed Appointments (Assume constraints on appointment_date and start_time to avoid unique constraint violation)
INSERT INTO appointment (doctor_id, patient_id, availability_rule_id, appointment_date, start_time, end_time, status, reason, created_at, updated_at)
SELECT dp.id, pp.id, (SELECT id FROM availability_rule WHERE doctor_id = dp.id LIMIT 1), CURRENT_DATE + INTERVAL '1 day', '09:00:00', '09:30:00', 'BOOKED', 'General Checkup', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users du ON du.id = dp.user_id
CROSS JOIN patient_profile pp JOIN users pu ON pu.id = pp.user_id
WHERE du.email = 'doctor1@medic.com' AND pu.email = 'patient1@medic.com'
AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.doctor_id = dp.id AND a.appointment_date = CURRENT_DATE + INTERVAL '1 day' AND a.start_time = '09:00:00');

INSERT INTO appointment (doctor_id, patient_id, availability_rule_id, appointment_date, start_time, end_time, status, reason, created_at, updated_at)
SELECT dp.id, pp.id, (SELECT id FROM availability_rule WHERE doctor_id = dp.id LIMIT 1), CURRENT_DATE + INTERVAL '2 days', '10:00:00', '10:30:00', 'BOOKED', 'Follow up', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users du ON du.id = dp.user_id
CROSS JOIN patient_profile pp JOIN users pu ON pu.id = pp.user_id
WHERE du.email = 'doctor1@medic.com' AND pu.email = 'patient2@medic.com'
AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.doctor_id = dp.id AND a.appointment_date = CURRENT_DATE + INTERVAL '2 days' AND a.start_time = '10:00:00');

INSERT INTO appointment (doctor_id, patient_id, availability_rule_id, appointment_date, start_time, end_time, status, reason, created_at, updated_at)
SELECT dp.id, pp.id, (SELECT id FROM availability_rule WHERE doctor_id = dp.id AND start_time = '10:00:00' LIMIT 1), CURRENT_DATE + INTERVAL '3 days', '10:00:00', '10:15:00', 'BOOKED', 'Headache', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users du ON du.id = dp.user_id
CROSS JOIN patient_profile pp JOIN users pu ON pu.id = pp.user_id
WHERE du.email = 'doctor2@medic.com' AND pu.email = 'patient1@medic.com'
AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.doctor_id = dp.id AND a.appointment_date = CURRENT_DATE + INTERVAL '3 days' AND a.start_time = '10:00:00');

INSERT INTO appointment (doctor_id, patient_id, availability_rule_id, appointment_date, start_time, end_time, status, reason, created_at, updated_at)
SELECT dp.id, pp.id, (SELECT id FROM availability_rule WHERE doctor_id = dp.id AND start_time = '14:00:00' LIMIT 1), CURRENT_DATE + INTERVAL '4 days', '14:00:00', '14:15:00', 'BOOKED', 'Migraine', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM doctor_profiles dp JOIN users du ON du.id = dp.user_id
CROSS JOIN patient_profile pp JOIN users pu ON pu.id = pp.user_id
WHERE du.email = 'doctor2@medic.com' AND pu.email = 'patient2@medic.com'
AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.doctor_id = dp.id AND a.appointment_date = CURRENT_DATE + INTERVAL '4 days' AND a.start_time = '14:00:00');

-- Seed Notifications
INSERT INTO notification (user_id, type, action, title, message, read, created_at, updated_at)
SELECT id, 'APPOINTMENT', 'BOOKED', 'Appointment Booked', 'Your appointment is booked', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE email = 'patient1@medic.com'
AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = users.id AND title = 'Appointment Booked');

INSERT INTO notification (user_id, type, action, title, message, read, created_at, updated_at)
SELECT id, 'APPOINTMENT', 'BOOKED', 'New Appointment', 'A new patient has booked an appointment', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE email = 'doctor1@medic.com'
AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = users.id AND title = 'New Appointment');

INSERT INTO notification (user_id, type, action, title, message, read, created_at, updated_at)
SELECT id, 'APPOINTMENT', 'CANCELLED', 'Appointment Cancelled', 'Your appointment was cancelled', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users WHERE email = 'patient2@medic.com'
AND NOT EXISTS (SELECT 1 FROM notification n WHERE n.user_id = users.id AND title = 'Appointment Cancelled');

-- Seed Audit Logs
INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'admin@medic.com', 'LOGIN', 'USER', 'Admin logged in', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE description = 'Admin logged in');

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'doctor1@medic.com', 'CREATE', 'AVAILABILITY_RULE', 'Doctor created availability rule', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE description = 'Doctor created availability rule');

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'patient1@medic.com', 'BOOK', 'APPOINTMENT', 'Patient booked appointment', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM audit_log WHERE description = 'Patient booked appointment');