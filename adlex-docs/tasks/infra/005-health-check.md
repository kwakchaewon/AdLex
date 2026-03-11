# Task 2.5: Health Check + 기본 모니터링

## INPUT
- adlex-api/src/main/resources/application.yml

## OUTPUT 경로
- adlex-api/src/main/resources/application-prod.yml (수정)
- adlex-api/.../infra/config/CorsConfig.kt

## 상세 스펙
Spring Actuator: /health, /health/liveness, /health/readiness.
application-prod.yml: RDS + ElastiCache 엔드포인트.
CORS: https://app.adlex.kr 허용.
로그: JSON 포맷 (CloudWatch 파싱용).

## 완료 조건
.\gradlew.bat build && /actuator/health 200 OK

## 커밋
chore(infra): configure health checks and production settings
