# Stage 1: Build JAR
FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /app
COPY . .
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Stage 2: Run JAR
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
