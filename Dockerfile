# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build the application, skipping tests for faster builds in CI/CD
RUN mvn clean package -DskipTests

# Stage 2: Create the final, slim production image
FROM openjdk:17-jdk-slim
WORKDIR /app

# Define arguments for user/group to run as non-root
ARG UID=1001
ARG GID=1001

# Create a non-root user and group
RUN groupadd --gid ${GID} qod-user && \
    useradd --uid ${UID} --gid ${GID} --shell /bin/bash --create-home qod-user

# Copy the application JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership of the app directory and JAR file
RUN chown ${UID}:${GID} /app && \
    chown ${UID}:${GID} app.jar

# Switch to the non-root user
USER qod-user

# Expose the port the application runs on
EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
