FROM eclipse-temurin:26-jre
WORKDIR /app
COPY taskboard-worker/target/taskboard-worker-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]