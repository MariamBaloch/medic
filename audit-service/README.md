# audit-service

Append-only business audit log. Port **8085**. Database role `medic_audit`, schema `audit`.

## What this service owns

Audit keeps a history of business actions: who did it, what they did, which kind of record it was, and when. It does not approve the action. Booking, identity, and the other services perform the work and publish `audit.event.recorded`. This service appends that event and does not update it later.

An admin reads the log through the gateway at `/api/audit`. The list is newest first. Patients and doctors cannot call it. Audit does not send mail and does not change appointments or accounts.

```bash
./mvnw -pl audit-service -am spring-boot:run
```
