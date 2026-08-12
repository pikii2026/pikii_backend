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
# OOM 발생 시 원인 분석용으로 힙 덤프를 남기고(HeapDumpOnOutOfMemoryError),
# 이 이미지가 jre라 jmap/jcmd가 없으므로 평소에도 JFR을 저용량으로 계속 돌려서
# 메모리 증가 추이를 나중에 들여다볼 수 있게 한다 (원형 버퍼: 최근 24시간/250MB만 보존).
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=70", \
    "-XX:MaxMetaspaceSize=512m", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/app/heapdump.hprof", \
    "-XX:StartFlightRecording=filename=/app/recording.jfr,maxage=24h,maxsize=250m", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
