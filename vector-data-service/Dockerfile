FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY platform-shared/pom.xml platform-shared/pom.xml
COPY main-orchestrator/pom.xml main-orchestrator/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY vector-data-service/pom.xml vector-data-service/pom.xml
COPY platform-shared/src platform-shared/src
COPY vector-data-service/src vector-data-service/src

RUN mvn -pl vector-data-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/vector-data-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
