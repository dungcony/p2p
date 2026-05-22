# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY bootstrap-server/pom.xml bootstrap-server/pom.xml
COPY peer-node/pom.xml peer-node/pom.xml
RUN mvn -pl bootstrap-server -am dependency:go-offline

COPY bootstrap-server/src bootstrap-server/src
RUN mvn -pl bootstrap-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

ENV BOOTSTRAP_SERVER_PORT=9000 \
    BOOTSTRAP_DATABASE_PATH=/data/bootstrap-server.db

RUN mkdir -p /data

COPY --from=build /workspace/bootstrap-server/target/bootstrap-server-1.0-SNAPSHOT.jar /app/bootstrap-server.jar

VOLUME ["/data"]
EXPOSE 9000

ENTRYPOINT ["java", "-jar", "/app/bootstrap-server.jar"]
