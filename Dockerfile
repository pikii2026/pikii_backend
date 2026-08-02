# ---- Build stage ----
FROM gradle:8-jdk17 AS build
WORKDIR /app

# 의존성 레이어 캐싱: 빌드 스크립트만 먼저 복사해 의존성을 내려받는다
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
