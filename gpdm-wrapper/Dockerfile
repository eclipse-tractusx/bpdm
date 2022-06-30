FROM maven:3.8.5-jdk-11-slim AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl gpdm-wrapper -am -DskipTests

FROM amazoncorretto:11-alpine-jdk
COPY --from=build /home/app/gpdm-wrapper/target/bpdm-wrapper.jar /usr/local/lib/bpdm-wrapper/bpdm-wrapper.jar
RUN addgroup -S bpdm && adduser -S bpdm -G bpdm
USER bpdm
WORKDIR /usr/local/lib/bpdm-wrapper
EXPOSE 8080
ENTRYPOINT ["java","-jar","bpdm-wrapper.jar"]