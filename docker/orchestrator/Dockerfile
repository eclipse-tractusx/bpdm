FROM maven:3.9-eclipse-temurin-21 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl bpdm-orchestrator -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk update && \
    apk add --upgrade "libpng>=1.6.54-r0" && \
    apk add --upgrade "gnutls=3.8.12-r0" && \
    rm -rf /var/cache/apk/*
COPY --from=build /home/app/bpdm-orchestrator/target/bpdm-orchestrator.jar /usr/local/lib/bpdm/app.jar
ARG USERNAME=bpdm
ARG USERID=10001
ARG GID=10001
RUN addgroup -g $GID -S $USERNAME
RUN adduser -u $USERID -S $USERNAME $USERNAME
USER $USERNAME
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
