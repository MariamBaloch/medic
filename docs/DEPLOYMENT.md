# Deployment

This document describes how Medic runs on AWS.

## 1. Addresses

| What                   | Address                                |
|------------------------|----------------------------------------|
| Frontend (the website) | https://d4405qjwlfxlp.cloudfront.net/  |
| Backend API            | https://d27j77oo959eqd.cloudfront.net/ |

The frontend source code is in [MariamBaloch/medic_frontend](https://github.com/MariamBaloch/medic_frontend).

Both addresses are CloudFront addresses. Nobody calls the `elasticbeanstalk.com` address directly. CloudFront gives the
backend an HTTPS address, which browsers require when the website itself is served over HTTPS.

## 2. The whole picture

```text
                         Browser
                            |
          +-----------------+------------------+
          |                                    |
  1. load the website                  2. call the API
          |                                    |
          v                                    v
  CloudFront (frontend)               CloudFront (backend)
          |                                    |
          v                                    v
  S3 bucket                            Elastic Beanstalk (one EC2 instance)
  (the built React files)              running these containers:
                                         gateway  <- the only public entry
                                         identity, booking,
                                         notification, audit
                                         redis, kafka
                                                |
                                +---------------+----------------+
                                |                                |
                                v                                v
                       RDS PostgreSQL                  ECR (image storage)
                       (private database)              the instance pulls the
                                                       service images from here
```

In plain words:

1. The browser downloads the React app from the **frontend CloudFront**, which reads the files from **S3**.
2. The React app then sends its API requests to the **backend CloudFront**, which forwards them to the **Elastic
   Beanstalk** instance.
3. On the instance, the **gateway** receives every request and passes it to the right service (identity, booking,
   notification or audit).
4. The services store data in **RDS PostgreSQL**, use **Redis** for short-lived data, and talk to each other through
   **Kafka**.

## 3. Frontend

The React app is built into plain static files and uploaded to an S3 bucket. The frontend CloudFront distribution serves
those files to users. The app is configured to send its API calls to the backend address in section 1.

## 4. Backend

### 4.1 Where it runs

The backend runs on **one EC2 instance** managed by Elastic Beanstalk, in the `us-east-1` region. Key facts:

- **Platform:** Docker on Amazon Linux 2023.
- **Network:** the instance is in a public subnet of the default VPC and has a public IP. It needs the public IP to
  download images from ECR.
- **Security group:** `medic-app-sg`. This group is also the only thing allowed to reach the database (see section 5).
- **Access for administration:** the instance has a role that lets it pull images from ECR and lets us open a shell with
  AWS Session Manager (no SSH keys needed).

### 4.2 How the services are started

Beanstalk does not run each service by hand. It unpacks a zip file whose root contains a `docker-compose.yml`, and
Docker Compose starts every container listed in it.

That `docker-compose.yml` is the repository file `deploy/docker-compose.prod.yml`. The deployment copies it to the root
of the zip.

### 4.3 The containers

| Container              | What it does                                                                                                                                                        | Port (inside the Compose network) |
|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| `gateway-service`      | Receives every API request and forwards it to the right service. Handles rate limiting. This is the only container reachable from outside the instance, on port 80. | 80 on the instance                |
| `identity-service`     | Accounts, profiles and login tokens (JWT).                                                                                                                          | 8082                              |
| `booking-service`      | Appointments, availability and the waitlist.                                                                                                                        | 8081                              |
| `notification-service` | Notifications (including live updates over SSE) and email.                                                                                                          | 8084                              |
| `audit-service`        | The audit log.                                                                                                                                                      | 8085                              |
| `redis`                | Short-lived data: the token blacklist and token versions, the schedule-cache version, and the gateway's rate-limit counters.                                        | none                              |
| `kafka`                | The message broker the services use to send events to each other (single KRaft broker).                                                                             | none                              |
| `kafka-init`           | Runs once at start-up. Creates the Medic topics and their `.DLT` (dead-letter) topics, then exits.                                                                  | none                              |

"No port" means the container is only reachable by the other containers on the same Docker network, never from the
internet.

**Memory limits.** One instance runs everything, so each container has a limit: 512 MB for each application container,
768 MB for Kafka and 192 MB for Redis. The Java services start with `-XX:MaxRAMPercentage=70`, which makes each one use
at most 70% of its own limit. Without this, every Java process would try to use the whole machine's memory. All services
run with `SPRING_PROFILES_ACTIVE=prod`.

### 4.4 Where the images come from

Images are not built on the instance. Jenkins builds them and stores them in ECR (AWS's private image storage). The
instance downloads them when it deploys:

`566914242591.dkr.ecr.us-east-1.amazonaws.com/medic/<service>:<tag>`

There is one repository per service: `medic/gateway-service`, `medic/identity-service`, `medic/booking-service`,
`medic/notification-service` and `medic/audit-service`.

The Compose file takes the registry host from `IMAGE_REGISTRY` and the version from `IMAGE_TAG`. The tag is the
7-character Git commit hash, so you can always tell which code an image contains.

## 5. Database

The database is **RDS PostgreSQL**. It has one database called `medic`, and it is private: it has no public address.

**Who can connect.** The database's security group, `medic-db-sg`, allows connections on port 5432 only from
`medic-app-sg`. In practice, only the Beanstalk instance can reach it. Connections use TLS (`sslmode=require` in the
JDBC URLs).

**How data is separated.** Instead of one RDS instance per service, there is one database split into four **schemas**
(folders for tables): `identity`, `booking`, `notification` and `audit`. Each service has its **own database user** that
can use only its own schema:

| Service      | Database user        |
|--------------|----------------------|
| identity     | `medic_identity`     |
| booking      | `medic_booking`      |
| notification | `medic_notification` |
| audit        | `medic_audit`        |

The RDS master user is not used by the services. It is only used to create these users and schemas, which is done once
from a shell on the instance.

## 6. Pipeline (Jenkins)

A push to `main` starts the pipeline defined in `Jenkinsfile`. It uses the Jenkins credential `aws-deployer` (a limited
AWS user) and the region `us-east-1`.

| Stage                  | What it does                                                                                                                                                                                                                                                                                                          | What you get                                                                       |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| 1. Build and test      | Runs `./mvnw verify`: compiles every module and runs all tests.                                                                                                                                                                                                                                                       | The commit hash is saved as the image tag. A failing test stops the pipeline here. |
| 2. Build images        | Runs `docker compose build` to build an image for each service on the Jenkins machine.                                                                                                                                                                                                                                | Five local images tagged with the commit hash.                                     |
| 3. Push to ECR         | Logs in to ECR with `aws ecr get-login-password` and uploads each image as `medic/<service>:<commit>`.                                                                                                                                                                                                                | The five images are stored in ECR.                                                 |
| 4. Deploy to Beanstalk | Zips the production Compose file and the Kafka topic script, uploads the zip to `s3://elasticbeanstalk-us-east-1-566914242591/medic/`, creates a new Beanstalk application version and updates the environment. It also sets `IMAGE_REGISTRY` and `IMAGE_TAG` so the instance pulls the images that were just pushed. | The instance runs the new version.                                                 |
| 5. Verify              | Checks the environment's health URL repeatedly until `/actuator/health` answers.                                                                                                                                                                                                                                      | A green build means the new version is up.                                         |

If any stage fails, the later stages do not run.

## 7. Configuration and secrets

No secrets are committed to Git. Where each kind of setting lives:

| Setting                                                                                                                                                                   | Where it lives                                                                                                                                     |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Secrets and per-environment values (JWT and internal API secrets, SMTP settings, `APP_BASE_URL`, `CORS_ALLOWED_ORIGINS`, the four database URLs, usernames and passwords) | **Beanstalk environment properties.** Beanstalk passes them to the containers, and the Compose file refuses to start if a required one is missing. |
| Image registry and tag                                                                                                                                                    | Set by the pipeline on each deploy (`IMAGE_REGISTRY`, `IMAGE_TAG`).                                                                                |
| Redis and Kafka host names                                                                                                                                                | Fixed in the Compose file (`redis`, `kafka:9092`), because they only exist inside the instance.                                                    |
| AWS keys used by Jenkins                                                                                                                                                  | Stored in Jenkins credentials (`aws-deployer`), never in the repository.                                                                           |

## 8. Security summary

- **Database:** private, reachable only from the application security group, encrypted connections, and one limited user
  per service.
- **Internet exposure:** only the gateway on port 80 is reachable. Redis, Kafka and the other services have no published
  ports.
- **HTTPS:** browsers talk to CloudFront over HTTPS. The step from CloudFront to the Beanstalk instance uses plain HTTP,
  so traffic on that leg is not encrypted. A load balancer with a certificate would encrypt it, at extra cost.
- **Jenkins access to AWS:** a dedicated IAM user that can only push to the `medic/*` ECR repositories, upload to the
  Beanstalk bucket and update the Beanstalk environment.
- **Secrets:** kept in Beanstalk and Jenkins, not in Git.

## 9. How to check that it works

1. Open `https://d27j77oo959eqd.cloudfront.net/actuator/health`. It should answer `{"status":"UP"...}`.
2. In the Elastic Beanstalk console, the environment `medic-env-1` should show health **Ok** and the latest commit hash
   as its running version.
3. In Jenkins, the latest `main` build should be green, with all five stages passed.
4. Open the frontend and log in, then make a booking. The booking should appear, and the notification and audit records
   should be created.

## 10. Names at a glance

| Item                       | Name                                                                                                                            |
|----------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| AWS region                 | `us-east-1`                                                                                                                     |
| Beanstalk application      | `medic`                                                                                                                         |
| Beanstalk environment      | `medic-env-1`                                                                                                                   |
| Beanstalk origin host      | `medic-env-1.eba-mkijgmpf.us-east-1.elasticbeanstalk.com`                                                                       |
| ECR repositories           | `medic/gateway-service`, `medic/identity-service`, `medic/booking-service`, `medic/notification-service`, `medic/audit-service` |
| Application security group | `medic-app-sg`                                                                                                                  |
| Database security group    | `medic-db-sg`                                                                                                                   |
| Deployment bucket          | `elasticbeanstalk-us-east-1-566914242591`                                                                                       |
| Jenkins AWS credential     | `aws-deployer`                                                                                                                  |
 