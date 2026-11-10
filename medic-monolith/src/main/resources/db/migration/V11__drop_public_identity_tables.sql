-- Identity data lives in identity-service (medic_identity schema / DB).
DROP TABLE IF EXISTS patient_profile CASCADE;
DROP TABLE IF EXISTS doctor_profiles CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP TABLE IF EXISTS specializations CASCADE;
