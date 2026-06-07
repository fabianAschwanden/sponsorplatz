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

# Runtime-Pakete:
#  - postgresql-client-17 für BackupService (pg_dump). Offizielles PostgreSQL-
#    APT-Repo, weil Ubuntu Jammy nur Postgres 14 als Default hat und der Server
#    in cloud-free Postgres 17 ist (matched version vermeidet pg_dump-Warnungen).
#  - fontconfig + fonts-dejavu-core: das jre-jammy-Image bringt KEINE Fonts mit.
#    Die Swiss-QR-Bill (net.codecrete.qrbill, PNGCanvas) rendert Text über
#    Java2D/AWT und wirft ohne fontconfig einen InternalError → bisher rohe 500
#    auf jeder QR-Route (Rechnungs-Detail + Rechnungs-PDF). Vertrags-PDF (ohne
#    QR-Bill) war nicht betroffen — exakt das Beta-Fehlerbild BETA-V09.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl ca-certificates gnupg lsb-release \
 && install -d /usr/share/postgresql-common/pgdg \
 && curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
        -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
 && echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
        > /etc/apt/sources.list.d/pgdg.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends postgresql-client-17 fontconfig fonts-dejavu-core \
 && apt-get purge -y curl gnupg lsb-release \
 && apt-get autoremove -y \
 && rm -rf /var/lib/apt/lists/*

# Non-root User
RUN addgroup --system --gid 1001 sponsor && \
    adduser  --system --uid 1001 --gid 1001 sponsor

WORKDIR /app

COPY --from=build /workspace/target/sponsorplatz.jar app.jar

# Upload-Verzeichnis für MedienAssets (LokalerStorageService default ./uploads)
# muss vor USER sponsor angelegt werden, sonst verweigert das non-root-User-Image
# das Erstellen unter WORKDIR /app
RUN mkdir -p /app/uploads && chown -R sponsor:sponsor /app

# Healthcheck nutzt /actuator/health auf dem Management-Port.
# In prod laufen Actuator-Endpoints auf Port 9090 (loopback-bind via
# application-prod.properties), in dev fallen sie auf 8080 zurück.
# Single-line CMD mit Shell-OR — Dockerfile-Parser akzeptiert nur
# zusammenhängende Zeilen für die CMD-Direktive der HEALTHCHECK-Instruktion.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

USER sponsor

EXPOSE 8080
# Management-Port nur intern: nicht via EXPOSE freigeben, damit der
# Container-Operator ihn nicht aus Versehen public mappt. Prometheus
# muss innerhalb des Compose-Netzes oder via Sidecar scrapen.

# JVM-Tuning sinnvoll fürs Container-Setup
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
