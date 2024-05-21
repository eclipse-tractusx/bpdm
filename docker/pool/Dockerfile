FROM maven:3.9-eclipse-temurin-21 AS build
ENV HOME=/home/app
COPY . $HOME
WORKDIR $HOME
RUN --mount=type=cache,target=/root/.m2 mvn -B -U clean package -pl bpdm-pool -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
ENV HOME=/home/app
COPY --from=build $HOME/bpdm-pool/target/bpdm-pool.jar /usr/local/lib/bpdm/app.jar
ARG USERNAME=bpdm
ARG USERID=10001
ARG GID=10001
RUN addgroup -g $GID -S $USERNAME
RUN adduser -u $USERID -S $USERNAME $USERNAME
USER $USERNAME
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
