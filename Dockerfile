# 빌드 단계 (JAR 생성)
FROM openjdk:17-jdk-slim AS build

WORKDIR /app
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 실행 단계 (최적화된 컨테이너)
FROM openjdk:17-jdk-slim

WORKDIR /app

# 모든 application.yml 파일 복사 (application.yml & application-prod.yml 포함)
COPY build/resources/main/application*.yml /app/config/

# JAR 파일 복사
COPY --from=build /app/app.jar app.jar

# 실행 시 prod 프로파일을 활성화하도록 설정
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.config.location=/app/config/", "-jar", "app.jar"]
