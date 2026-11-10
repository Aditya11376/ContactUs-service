# Stage 1: Build JAR
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build project
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for cloud security
RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8083
ENV SPRING_DOCKER_COMPOSE_ENABLED=false
EXPOSE 8083

ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xms128m", "-Xmx350m", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-Dspring.jmx.enabled=false", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
