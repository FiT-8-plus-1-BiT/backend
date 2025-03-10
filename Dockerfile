# 기본 이미지 설정
FROM openjdk:17-jdk-slim

# 로케일 및 타임존 설정을 위해 필요한 패키지 설치
RUN apt-get update && \
    apt-get install -y locales tzdata && \
    echo "ko_KR.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen && \
    ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apt-get clean

# 환경 변수 설정 (한글, 시간대)
ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8
ENV TZ=Asia/Seoul

# 작업 디렉토리 설정
WORKDIR /app

# JAR 파일 복사
COPY build/libs/app.jar app.jar

# 컨테이너 실행 시 Spring Boot 실행
CMD ["java", "-jar", "app.jar"]
