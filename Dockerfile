FROM maven:3.8.5-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
WORKDIR /home/app
RUN mvn -B -U clean package

FROM openjdk:11-jre-slim
RUN apt-get update
COPY --from=build /home/app/target/bpdm.jar /usr/local/lib/bpdm/bpdm.jar
RUN adduser bpdm
USER bpdm
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","bpdm.jar"]