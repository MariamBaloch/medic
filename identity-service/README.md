# identity-service

Accounts and profiles. Port **8082**. Database role `medic_identity`, schema `identity`.

## What this service owns

Identity is the source of users. A person registers here as a patient or a doctor, verifies their email, logs in, and
receives a JWT. Password reset and password change also happen here. Other services never look up the user table. They
only check the token.

The service stores:

- the account,
- the patient or doctor profile,
- the avatar file,
- the specialization list.

Admins verify a doctor or deactivate a user from here. The doctor directory is a search of those profiles. It does not
know which slots are free. That question belongs to booking.

### Tokens and Redis

Logout and a password change write the token blacklist and the token version in Redis. The other services read those
keys when they validate a request. Identity is the only writer.

### Email

Verification and password-reset emails are sent by this service. They contain a token, so they are not published to
Kafka.

### Events

Profile changes that other services need are published through the outbox as `identity.user.changed` and
`identity.doctor.changed`. Booking uses them to refresh the name, email, specialization, and fee it stored on an
appointment.

## Public paths

Through the gateway at `/api/identity`:

| Path                           | What it covers                            |
|--------------------------------|-------------------------------------------|
| `/auth/**`                     | Register, verify, login, logout, password |
| `/user/**`                     | Account and avatar                        |
| `/profile/**`                  | Patient and doctor profile edits          |
| `/doctors`, `/specializations` | Doctor directory and specialization list  |
| `/admin/**`                    | Verify a doctor, deactivate a user        |
| `/uploads/**`                  | Avatar files                              |

## Internal paths

Used only by other services, never by browsers. Callers must send the `X-Internal-Key` header. The gateway does not
route these paths.

| Path                            | Returns                                                                   |
|---------------------------------|---------------------------------------------------------------------------|
| `/internal/patients/{id}`       | The stored snapshot of the patient (what booking keeps on an appointment) |
| `/internal/doctors/{id}`        | The stored snapshot of the doctor                                         |
| The `/profile` variants of both | The live profile, including photo, qualification, and biography           |

Booking calls these over the Docker network when it does not already have a snapshot, or when it opens an appointment
and needs the live profile.

## Avatar storage

Avatars are saved as files under `UPLOAD_BASE_DIR` (`/app/uploads` in the container), which is a Docker volume. The
volume lives on the host, so files survive a container restart but not a replaced Elastic Beanstalk instance.

## Configuration

Settings come from environment variables. Compose and Beanstalk provide them, and no secret is stored in the code.

| Variable                                                                                           | Purpose                                           |
|----------------------------------------------------------------------------------------------------|---------------------------------------------------|
| `IDENTITY_DB_URL`                                                                                  | JDBC URL of the `medic` database                  |
| `IDENTITY_DB_USERNAME`, `IDENTITY_DB_PASSWORD`                                                     | The `medic_identity` database role                |
| `KAFKA_BOOTSTRAP_SERVERS`                                                                          | Kafka address                                     |
| `REDIS_HOST`, `REDIS_PORT`                                                                         | Redis, for the token blacklist and token versions |
| `JWT_SECRET`, `JWT_EXPIRATION`                                                                     | Signs the tokens, and how long they last          |
| `INTERNAL_API_KEY`                                                                                 | Protects the internal paths                       |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS` | Email server for verification and reset mail      |
| `UPLOAD_BASE_DIR`                                                                                  | Where avatar files are saved                      |
| `SERVER_PORT`                                                                                      | HTTP port, `8082`                                 |

## Run it

The easiest way is the full stack from the repository root: `docker compose up --build`.

To run only this service for development, start its dependencies, install the shared libraries once, then run it:

```bash
docker compose up postgres redis kafka kafka-init mailpit
./mvnw -DskipTests install
./mvnw -pl identity-service spring-boot:run
```

The service needs the environment variables above (see `.env.example`). Emails sent locally appear in Mailpit at
`http://localhost:8025`.

Health check: `http://localhost:8082/actuator/health`.