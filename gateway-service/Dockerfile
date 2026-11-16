# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY medic-common medic-common
COPY gateway-service gateway-service
COPY booking-service/pom.xml booking-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY audit-service/pom.xml audit-service/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY medic-security/pom.xml medic-security/pom.xml
COPY medic-messaging/pom.xml medic-messaging/pom.xml
COPY medic-web/pom.xml medic-web/pom.xml

RUN chmod +x mvnw \
    && ./mvnw -B -pl gateway-service -am -DskipTests package

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

COPY --from=build /workspace/gateway-service/target/gateway-service-*.jar /app/app.jar

USER medic
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
