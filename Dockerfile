# =============================================================================
# FactoryFlow Backend Dockerfile (Render / Railway / Cloud PaaS 배포용)
# Multi-stage build: Gradle 빌드(JDK 25) -> 경량 실행 이미지(JRE 25)
# =============================================================================

# Stage 1: Build Spring Boot executable JAR
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

# Gradle 래퍼 및 빌드 스크립트 먼저 복사 (의존성 레이어 캐시 활용)
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 코드 복사 및 실행 JAR 빌드 (테스트는 CI/로컬에서 수행)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test \
    && find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} /workspace/app.jar \;

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:25-jre
WORKDIR /app

# 보안을 위해 non-root 전용 사용자로 실행
RUN groupadd -r factoryflow && useradd -r -g factoryflow factoryflow

COPY --from=builder /workspace/app.jar ./app.jar
RUN chown factoryflow:factoryflow /app/app.jar

USER factoryflow

# 클라우드 기본 포트 안내 (실제 바인딩은 server.port=${PORT:8080} 사용)
EXPOSE 8080

# 한국 표준시(KST) 고정 및 컨테이너 메모리 비율 제한(소규모 클라우드 OOM 방지)
ENTRYPOINT ["java", \
  "-Duser.timezone=Asia/Seoul", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "/app/app.jar"]
