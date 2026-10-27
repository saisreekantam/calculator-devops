# syntax=docker/dockerfile:1

# ---------- Stage 1: build the JAR with Maven + full JDK ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy only the POM first so the dependency layer is cached
# and re-downloaded only when pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests already ran in the Jenkins Test stage
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: slim JRE runtime ----------
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="calculator-api" \
      org.opencontainers.image.description="Calculator REST API (CSE 816 SPE mini-project)" \
      org.opencontainers.image.source="https://github.com/saisreekantam/calculator-devops"

# Run as an unprivileged user, never root
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/calculator-api.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
