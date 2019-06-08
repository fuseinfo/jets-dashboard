FROM openjdk:8-jdk-alpine
COPY target/jets-dashboard-0.2.0.jar /app/jets-dashboard.jar
WORKDIR /app
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/jets-dashboard.jar"]

