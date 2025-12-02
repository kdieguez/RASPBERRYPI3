FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/aerolineas-backend-1.0.0.jar app.jar

ENV ORACLE_USER=APP_UAT
ENV ORACLE_PASSWORD=Uat123
ENV ORACLE_CONNECT_STRING=oracle-xe:1521/XEPDB1
ENV SERVER_PORT=8082

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
