FROM openjdk:11
ADD ./target/*.jar bpdm.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","bpdm.jar"]