FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B clean verify
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system bloodbridge && useradd --system --gid bloodbridge bloodbridge
COPY --from=build /app/target/bloodbridge-1.0.0.jar app.jar
USER bloodbridge
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
