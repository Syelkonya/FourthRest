FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts settings.gradle.kts ./
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
