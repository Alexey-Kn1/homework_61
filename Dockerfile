FROM eclipse-temurin:24.0.2_12-jre

EXPOSE 8081

COPY build/libs/*.jar app.jar

RUN mkdir -p /storage

CMD ["java", "-jar", "app.jar"]
