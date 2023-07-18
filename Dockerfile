# Use the official OpenJDK 11 image as the base image
FROM openjdk:11

# Set the working directory inside the container


# Copy the Spring application JAR file into the container
COPY target/appfor-0.0.1-SNAPSHOT.jar appfor-0.0.1-SNAPSHOT.jar

# Expose the port on which your Spring application runs (change it if necessary)
EXPOSE 8082

# Set the command to run your Spring application when the container starts
CMD ["java", "-jar", "/appfor-0.0.1-SNAPSHOT.jar"]
