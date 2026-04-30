# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-jammy
COPY --from=build /target/*.jar app.jar

# Port must match your application.properties server.port
EXPOSE 1001

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]