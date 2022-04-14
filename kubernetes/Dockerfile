FROM openjdk:11
ADD ./target/*.jar bpdm.jar
RUN chmod 777 bpdm.jar
RUN useradd -u 1000 bpdm
USER bpdm
EXPOSE 8080
ENTRYPOINT ["java","-jar","bpdm.jar"]