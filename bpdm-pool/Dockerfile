FROM maven:3.8.5-openjdk-17-slim AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn -B -U clean package -pl bpdm-pool -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /home/app/bpdm-pool/target/bpdm-pool.jar /usr/local/lib/bpdm/app.jar
RUN addgroup -S bpdm && adduser -S bpdm -G bpdm
USER bpdm
WORKDIR /usr/local/lib/bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]