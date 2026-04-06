FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# api-gateway has no dependency on platform-shared — builds standalone
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -f api-gateway/pom.xml dependency:go-offline -q
COPY api-gateway/src api-gateway/src
RUN mvn -f api-gateway/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
