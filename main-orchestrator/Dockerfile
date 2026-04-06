FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1. Install platform-shared into the local Maven repo
COPY platform-shared/pom.xml platform-shared/pom.xml
COPY platform-shared/src platform-shared/src
RUN mvn -f platform-shared/pom.xml install -DskipTests

# 2. Build main-orchestrator (resolves platform-shared from local repo above)
COPY main-orchestrator/pom.xml main-orchestrator/pom.xml
RUN mvn -f main-orchestrator/pom.xml dependency:go-offline -q
COPY main-orchestrator/src main-orchestrator/src
RUN mvn -f main-orchestrator/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/main-orchestrator/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
