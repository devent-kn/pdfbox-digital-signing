FROM gradle:8.9-jdk17-alpine AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean build -x test

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]