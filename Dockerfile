# ==============================================================================
# Multi-Stage Dockerfile for Disaster Relief Resource Allocation System
# Stage 1: Build using Maven 3.9 and Eclipse Temurin JDK 17
# Stage 2: Minimal Runtime using Eclipse Temurin JRE 17
# ==============================================================================

# --- STAGE 1: BUILD ---
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable shaded fat JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# --- STAGE 2: RUNTIME ---
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create unprivileged application user for security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy shaded fat JAR from build stage
COPY --from=builder /build/target/disaster-relief-system-1.0.0.jar /app/app.jar

# Configuration environment defaults
ENV PORT=7070
ENV MONGODB_DB=relief
ENV MONGODB_TEST_DB=relief_test

# Expose default HTTP port
EXPOSE 7070

# Switch to non-root user
USER appuser

# Run application jar with PORT passed to environment
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
