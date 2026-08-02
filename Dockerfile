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
# 저사양 컨테이너(Railway 등)에서 OOM 재시작 루프를 막기 위한 JVM 메모리 제한:
# 힙/메타스페이스/코드캐시/스택을 전부 고정값으로 묶어 총 사용량을 ~450MB 이내로 유지
ENTRYPOINT ["java", \
    "-Xms128m", "-Xmx224m", \
    "-XX:MaxMetaspaceSize=128m", \
    "-XX:ReservedCodeCacheSize=48m", \
    "-XX:MaxDirectMemorySize=32m", \
    "-Xss512k", \
    "-XX:+UseSerialGC", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
