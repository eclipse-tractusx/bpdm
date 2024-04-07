FROM maven:3.9-eclipse-temurin-21 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl bpdm-orchestrator -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
# Adding wget for the health check
RUN apk --no-cache add wget
COPY --from=build /home/app/bpdm-orchestrator/target/bpdm-orchestrator.jar /usr/local/lib/bpdm/app.jar
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
  CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
