# Step 1: Use an official OpenJDK base image from Docker Hub
FROM azul/zulu-openjdk:21
# Step 2: Set the working directory inside the container
WORKDIR /app
# Step 3: Copy the Spring Boot JAR file into the container
COPY applicationBookings/target/applicationBookings-1.0.0.jar /app/app.jar
# Step 3: Copy the DB into the container
COPY applicationBookings/bookings.db /app/bookings.db
VOLUME /app
# Step 4: Expose the port your application runs on
EXPOSE 8080
# Step 5: Define the command to run your Spring Boot application
CMD ["java", "-jar", "/app/app.jar"]
