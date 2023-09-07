FROM maven:3.8.7-eclipse-temurin-17 AS build
ENV HOME=/home/app
COPY .  $HOME
WORKDIR  $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -B -U clean package -pl bpdm-gate -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
ENV HOME=/home/app
COPY --from=build $HOME/bpdm-gate/target/bpdm-gate.jar /usr/local/lib/bpdm/app.jar
RUN apk update && apk upgrade libssl3 libcrypto3
ARG USERNAME=bpdm
ARG USERID=10001
ARG GID=3000
RUN addgroup -g $GID -S $USERNAME
RUN adduser -u $USERID -S $USERNAME $USERNAME
USER $USERNAME
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT java $JAVA_OPTIONS -jar app.jar