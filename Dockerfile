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
# 힙은 컨테이너 메모리에 비례해 잡고, 메타스페이스는 넉넉히 상한만 둔다.
# (상한을 너무 낮게 잡으면 Swagger/JPA 프록시 클래스가 쌓이며 Metaspace OOM이 난다)
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=70", \
    "-XX:MaxMetaspaceSize=512m", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
