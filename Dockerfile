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
# 힙은 컨테이너 메모리의 65%까지만, 스레드 스택 512KB, GC는 메모리를 적게 쓰는 SerialGC
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=65", \
    "-XX:+UseSerialGC", \
    "-Xss512k", \
    "-XX:MaxMetaspaceSize=160m", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
