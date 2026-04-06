FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# 1. Install platform-shared into local Maven repo
COPY platform-shared/pom.xml platform-shared/pom.xml
COPY platform-shared/src platform-shared/src
RUN mvn -f platform-shared/pom.xml install -DskipTests

# 2. Build auth-service
COPY auth-service/pom.xml auth-service/pom.xml
RUN mvn -f auth-service/pom.xml dependency:go-offline -q
COPY auth-service/src auth-service/src
RUN mvn -f auth-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
