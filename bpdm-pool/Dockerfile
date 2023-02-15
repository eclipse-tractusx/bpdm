FROM maven:3.8.7-eclipse-temurin-17 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl bpdm-pool -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /home/app/bpdm-pool/target/bpdm-pool.jar /usr/local/lib/bpdm/app.jar
RUN apk update
RUN apk upgrade --no-cache libssl3 libcrypto3
RUN addgroup -S bpdm && adduser -S bpdm -G bpdm
USER bpdm
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]