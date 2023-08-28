
FROM openjdk:11





COPY target/appfor-0.0.1-SNAPSHOT.jar appfor-0.0.1-SNAPSHOT.jar


EXPOSE 8082


CMD ["java", "-jar", "appfor-0.0.1-SNAPSHOT.jar"]