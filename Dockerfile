FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/api-spring-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
