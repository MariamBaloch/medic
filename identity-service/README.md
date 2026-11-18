# identity-service

Accounts and profiles. Port **8082**. Database role `medic_identity`, schema `identity`.

## What this service owns

Identity is the source of users. A person registers here as a patient or a doctor, verifies their email, logs in, and receives a JWT. Password reset and password change also happen here. Other services never look up the user table. They only check the token.

The service stores the account, the patient or doctor profile, the avatar file, and the specialization list. Admins verify a doctor or deactivate a user from here. The doctor directory is a search of those profiles. It does not know which slots are free. That question belongs to booking.

Logout and a password change write the token blacklist and the token version in Redis. The other services read those keys when they validate a request. Identity is the only writer.

Verification and password-reset emails are sent by this service. They contain a token, so they are not published to Kafka. Profile changes that other services need are published as `identity.user.changed` and `identity.doctor.changed`. Booking uses those events to refresh the name, email, specialization, and fee it stored on an appointment.

`/internal/patients/{id}` and `/internal/doctors/{id}` return that stored snapshot. The `/profile` variants return the live profile, including photo, qualification, and biography. Callers must send `X-Internal-Key`. The gateway does not route these paths. Booking calls them on the Docker network when it does not already have a snapshot.

Public paths, through the gateway at `/api/identity`:

- `/auth/**` — register, verify, login, logout, password
- `/user/**` — account and avatar
- `/profile/**` — patient and doctor profile edits
- `/doctors`, `/specializations`
- `/admin/**` — verify a doctor, deactivate a user
- `/uploads/**` — avatar files

`/internal/patients/{id}` and `/internal/doctors/{id}` return the snapshot booking stores. The `/profile` variants
return the live profile. Callers must send `X-Internal-Key`. The gateway does not expose these routes.

Identity publishes `identity.user.changed` and `identity.doctor.changed`. Verification and reset mail are sent from this
service.

```bash
./mvnw -pl identity-service -am spring-boot:run
```
