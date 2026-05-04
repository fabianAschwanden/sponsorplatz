# =============================================================================
# Sponsorplatz — Multi-Stage Dockerfile
# =============================================================================

# ------------------ Build Stage ------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache-friendly: zuerst pom für Dependency-Resolve, dann Source
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ------------------ Runtime Stage ------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Non-root User
RUN addgroup --system --gid 1001 sponsor && \
    adduser  --system --uid 1001 --gid 1001 sponsor

WORKDIR /app

COPY --from=build /workspace/target/sponsorplatz.jar app.jar

# Healthcheck nutzt /actuator/health
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER sponsor

EXPOSE 8080

# JVM-Tuning sinnvoll fürs Container-Setup
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
