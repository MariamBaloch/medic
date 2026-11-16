# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY medic-common medic-common
COPY medic-security medic-security
COPY medic-messaging medic-messaging
COPY medic-web medic-web
COPY notification-service notification-service
COPY booking-service/pom.xml booking-service/pom.xml
COPY gateway-service/pom.xml gateway-service/pom.xml
COPY audit-service/pom.xml audit-service/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml

RUN chmod +x mvnw \
    && ./mvnw -B -pl notification-service -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN printf 'Acquire::By-Hash "false";\nAcquire::http::Pipeline-Depth "0";\nAcquire::Retries "5";\n' > /etc/apt/apt.conf.d/99docker \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system medic \
    && useradd --system --gid medic --home-dir /app --shell /usr/sbin/nologin medic \
    && chown -R medic:medic /app

COPY --from=build /workspace/notification-service/target/notification-service-*.jar /app/app.jar

USER medic
EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8084/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
