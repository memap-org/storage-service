FROM maven:3.9.11-amazoncorretto-21-al2023 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -Pdev -DskipTests

FROM eclipse-temurin:21-jdk-alpine-3.22
WORKDIR /app
COPY --from=builder /app/target/*.jar storage-service.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/storage-service.jar"]
