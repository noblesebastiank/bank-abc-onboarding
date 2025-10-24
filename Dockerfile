# Multi-stage build: Use Eclipse Temurin OpenJDK 21 for building
FROM --platform=linux/amd64 eclipse-temurin:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage: Use Eclipse Temurin OpenJDK 21 JRE on Ubuntu Jammy (x86_64 emulation)
FROM --platform=linux/amd64 eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create directory for file storage
RUN mkdir -p /app/file-storage

# Expose port
EXPOSE 8080

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
