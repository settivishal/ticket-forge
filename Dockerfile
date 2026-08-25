# Stage 1: Build the application with Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Java 21 JRE Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
VOLUME /tmp

# Add non-root application user for production container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar
EXPOSE 8080

# Lightweight JVM memory settings optimized for 512MB RAM containers
ENV JAVA_OPTS="-XX:+UseSerialGC -Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -Xss512k"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
