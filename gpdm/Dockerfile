FROM maven:3.8.5-jdk-11-slim AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl gpdm -am -DskipTests

FROM amazoncorretto:11-alpine-jdk
COPY --from=build /home/app/gpdm/target/bpdm.jar /usr/local/lib/bpdm/bpdm.jar
RUN addgroup -S bpdm && adduser -S bpdm -G bpdm
USER bpdm
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","bpdm.jar"]