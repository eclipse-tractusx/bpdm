FROM maven:3.8.7-eclipse-temurin-17 AS build
ENV HOME=/home/app
COPY .  $HOME
WORKDIR  $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -B -U clean package -pl bpdm-gate -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
# Adding wget for the health check
RUN apk --no-cache add wget
ENV HOME=/home/app
COPY --from=build $HOME/bpdm-gate/target/bpdm-gate.jar /usr/local/lib/bpdm/app.jar
RUN apk update && apk upgrade --no-cache libssl3 libcrypto3
ARG USERNAME=bpdm
ARG USERID=10001
ARG GID=10001
RUN addgroup -g $GID -S $USERNAME
RUN adduser -u $USERID -S $USERNAME $USERNAME
USER $USERNAME
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
# Health check instruction
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
