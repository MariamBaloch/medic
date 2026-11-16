-- Descriptions match the @AuditLogger text the services record for these actions.

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'doctor1@medic.com', 'CREATE', 'AVAILABILITY_RULE', 'Doctor created an availability rule', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'doctor1@medic.com' AND action = 'CREATE' AND entity_type = 'AVAILABILITY_RULE'
      AND description = 'Doctor created an availability rule'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'doctor2@medic.com', 'CREATE', 'AVAILABILITY_RULE', 'Doctor created an availability rule', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'doctor2@medic.com' AND action = 'CREATE' AND entity_type = 'AVAILABILITY_RULE'
      AND description = 'Doctor created an availability rule'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'doctor1@medic.com', 'CREATE', 'AVAILABILITY_EXCEPTION', 'Doctor created an availability exception', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'doctor1@medic.com' AND action = 'CREATE' AND entity_type = 'AVAILABILITY_EXCEPTION'
      AND description = 'Doctor created an availability exception'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'doctor2@medic.com', 'CREATE', 'AVAILABILITY_EXCEPTION', 'Doctor created an availability exception', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'doctor2@medic.com' AND action = 'CREATE' AND entity_type = 'AVAILABILITY_EXCEPTION'
      AND description = 'Doctor created an availability exception'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'patient1@medic.com', 'BOOK', 'APPOINTMENT', 'Patient booked an appointment', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'patient1@medic.com' AND action = 'BOOK' AND entity_type = 'APPOINTMENT'
      AND description = 'Patient booked an appointment'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'patient2@medic.com', 'BOOK', 'APPOINTMENT', 'Patient booked an appointment', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'patient2@medic.com' AND action = 'BOOK' AND entity_type = 'APPOINTMENT'
      AND description = 'Patient booked an appointment'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'patient1@medic.com', 'CANCEL', 'APPOINTMENT', 'Patient cancelled an appointment', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'patient1@medic.com' AND action = 'CANCEL' AND entity_type = 'APPOINTMENT'
      AND description = 'Patient cancelled an appointment'
);

INSERT INTO audit_log (username, action, entity_type, description, timestamp)
SELECT 'admin@medic.com', 'LOGIN', 'USER', 'Admin logged in', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log
    WHERE username = 'admin@medic.com' AND action = 'LOGIN' AND entity_type = 'USER'
      AND description = 'Admin logged in'
);
