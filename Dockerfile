# Use the official OpenJDK 11 image as the base image
FROM openjdk:11

# Arguments for appname and build version (default values can be set if needed)
ARG APP_NAME=my-app-name
ARG BUILD_VERSION=1.0

# Set labels for appname and build version
LABEL app.name=${APP_NAME}
LABEL build.version=${BUILD_VERSION}

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring application JAR file into the container
COPY target/appfor-0.0.1-SNAPSHOT.jar app-${APP_NAME}-${BUILD_VERSION}.jar

# Expose the port on which your Spring application runs (change it if necessary)
EXPOSE 8082

# Set the command to run your Spring application when the container starts
CMD ["java", "-jar", "app-${APP_NAME}-${BUILD_VERSION}.jar"]
