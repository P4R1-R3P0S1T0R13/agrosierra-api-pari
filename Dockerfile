FROM gradle:8.14.3-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon -q > /dev/null || true
COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
