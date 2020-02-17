FROM openjdk:8-alpine

COPY target/uberjar/hash-blob-server.jar /hash-blob-server/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/hash-blob-server/app.jar"]
