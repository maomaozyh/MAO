FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8123
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
