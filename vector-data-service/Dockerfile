FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1. Install platform-shared into the local Maven repo
COPY platform-shared/pom.xml platform-shared/pom.xml
COPY platform-shared/src platform-shared/src
RUN mvn -f platform-shared/pom.xml install -DskipTests

# 2. Build vector-data-service
COPY vector-data-service/pom.xml vector-data-service/pom.xml
RUN mvn -f vector-data-service/pom.xml dependency:go-offline -q
COPY vector-data-service/src vector-data-service/src
RUN mvn -f vector-data-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/vector-data-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
