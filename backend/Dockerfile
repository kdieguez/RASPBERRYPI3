FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN apt-get update && apt-get install -y maven

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build /app/target/aerolineas-backend-1.0.0.jar app.jar

ENV ORACLE_USER=APP_DEV
ENV ORACLE_PASSWORD=Dev123
ENV ORACLE_CONNECT_STRING=oracle-xe:1521/XEPDB1
ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
