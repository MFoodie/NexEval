FROM gradle:8.10.2-jdk21-alpine AS build

WORKDIR /workspace

COPY server/build.gradle server/settings.gradle ./
COPY server/src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY server/fig ./fig
COPY --from=build /workspace/build/libs/*.jar /app/app.jar

ENV SERVER_PORT=8080 \
    SSL_ENABLED=false \
    DB_HOST=mysql \
    DB_PORT=3306 \
    DB_USERNAME=root \
    ALLOWED_ORIGINS=http://localhost,http://127.0.0.1

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
