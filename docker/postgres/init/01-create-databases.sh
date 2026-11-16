#!/bin/sh
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
  -v medic_password="$MEDIC_DB_PASSWORD" \
  -v identity_password="$IDENTITY_DB_PASSWORD" \
  -v booking_password="$BOOKING_DB_PASSWORD" \
  -v notification_password="$NOTIFICATION_DB_PASSWORD" \
  -v audit_password="$AUDIT_DB_PASSWORD" <<'EOSQL'
CREATE USER medic WITH PASSWORD :'medic_password';
CREATE USER medic_identity WITH PASSWORD :'identity_password';
CREATE USER medic_booking WITH PASSWORD :'booking_password';
CREATE USER medic_notification WITH PASSWORD :'notification_password';
CREATE USER medic_audit WITH PASSWORD :'audit_password';

CREATE DATABASE medic OWNER medic;

GRANT CONNECT ON DATABASE medic TO medic;
GRANT CONNECT ON DATABASE medic TO medic_identity;
GRANT CONNECT ON DATABASE medic TO medic_booking;
GRANT CONNECT ON DATABASE medic TO medic_notification;
GRANT CONNECT ON DATABASE medic TO medic_audit;
EOSQL
