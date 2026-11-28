# ============================================================
# Stage 1 – BUILD
# Use the official Maven image with Java 21 (Eclipse Temurin)
# to compile the project and produce the fat JAR.
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set a consistent working directory inside the build container
WORKDIR /app

# --- Dependency caching layer ---
# Copy only the POM first so that Docker can cache the downloaded
# dependencies as a separate layer.  They are re-downloaded only
# when pom.xml itself changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# --- Source copy & build ---
# Now copy the rest of the source and build the JAR.
# If application.properties is absent (due to .gitignore),
# copy application.properties.example to populate baseline keys.
COPY src ./src
RUN if [ ! -f src/main/resources/application.properties ]; then \
        cp src/main/resources/application.properties.example src/main/resources/application.properties; \
    fi
RUN mvn package -DskipTests -B

# ============================================================
# Stage 2 – RUNTIME
# Use a minimal Eclipse Temurin 21 JRE-only image.
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Create a non-root user for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR produced in the build stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to the non-root user
RUN chown appuser:appgroup app.jar

USER appuser

# Expose the port the Spring Boot app listens on.
# The actual port is controlled by the PORT env var (default 8080).
EXPOSE 8080

# ============================================================
# Environment variables – override ALL sensitive values at
# runtime.  Default values below are safe placeholders only;
# NEVER use them in production.
# ============================================================
ENV SPRING_DATASOURCE_URL=""
ENV SPRING_DATASOURCE_USERNAME=""
ENV SPRING_DATASOURCE_PASSWORD=""
ENV JWT_SECRET=""
ENV JWT_EXPIRATION_MS="86400000"
ENV PORT="8080"
ENV FRONTEND_URL=""
ENV PUBLIC_API_URL=""

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

