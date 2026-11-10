-- Identity seed (roles, users, profiles only; no booking tables)
SET search_path TO identity;

INSERT INTO roles (name, created_at, updated_at) VALUES
('ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DOCTOR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PATIENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO specializations (name, description, created_at, updated_at) VALUES
('Cardiology', 'Heart and blood vessels', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dermatology', 'Skin conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pediatrics', 'Children and infants', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Neurology', 'Nervous system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Orthopedics', 'Bones and muscles', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'admin@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'System', 'Admin', '1980-01-01', '1234567890', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'ADMIN'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'doctor1@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Sarah', 'Smith', '1985-05-15', '9876543211', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'DOCTOR'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'doctor2@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Robert', 'Brown', '1978-08-22', '9876543212', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'DOCTOR'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'patient1@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'John', 'Doe', '1990-10-20', '5551234561', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'PATIENT'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'patient2@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Jane', 'Clark', '1992-12-10', '5551234562', 'FEMALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'PATIENT'
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (role_id, email, password, first_name, last_name, date_of_birth, phone, gender, status, created_at, updated_at)
SELECT id, 'patient3@medic.com', '$2a$10$7Dasjt9P2q.4nWy3VLPjYed2xO/BcO6npvpNeUmt0gCT8dao2/HQq', 'Alex', 'Morgan', '1995-03-15', '5551234563', 'MALE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM roles WHERE name = 'PATIENT'
ON CONFLICT (email) DO NOTHING;

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

INSERT INTO patient_profile (user_id, address, city, country, blood_type, height_cm, weight_kg, allergies, chronic_conditions, created_at, updated_at)
SELECT u.id, '123 Main St', 'Metropolis', 'Countryland', 'O_POSITIVE', 180, 75, 'Peanuts', 'None', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'patient1@medic.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO patient_profile (user_id, address, city, country, blood_type, height_cm, weight_kg, allergies, chronic_conditions, created_at, updated_at)
SELECT u.id, '456 Elm St', 'Gotham', 'Countryland', 'A_NEGATIVE', 165, 60, 'Dust', 'Asthma', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'patient2@medic.com'
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO patient_profile (user_id, address, city, country, blood_type, height_cm, weight_kg, allergies, chronic_conditions, created_at, updated_at)
SELECT u.id, '789 Oak St', 'Star City', 'Countryland', 'B_POSITIVE', 172, 68, 'None', 'None', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u WHERE u.email = 'patient3@medic.com'
ON CONFLICT (user_id) DO NOTHING;
