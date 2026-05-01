FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
COPY --from=build /target/*.jar app.jar

EXPOSE 10000

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
