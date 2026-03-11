# Task 1.1: AdLex 프로젝트 초기 셋업
 
## INPUT
- (없음)
 
## OUTPUT 경로
- adlex-api/build.gradle.kts
- adlex-api/settings.gradle.kts
- adlex-api/src/main/kotlin/com/adlex/AdLexApplication.kt
- adlex-api/src/main/resources/application.yml
- adlex-api/src/main/resources/application-local.yml
- adlex-api/src/main/resources/application-prod.yml
- adlex-infra/docker-compose.yml
 
## 상세 스펙
### build.gradle.kts
group="com.adlex". Kotlin 1.9+, Spring Boot 3.3+, JDK 21.
dependencies: starter-web, data-jpa, data-redis, security, validation, actuator, springdoc-openapi:2.3+, flyway, postgresql, jjwt:0.12+, mockk(test)
 
### docker-compose.yml
postgres:16-alpine (5432, DB=adlex) + redis:7-alpine (6379)
 
### application.yml
DB=localhost:5432/adlex, Redis=localhost:6379, port=8080, swagger paths
 
## 완료 조건 (PowerShell)
- docker-compose up -d
- cd adlex-api; .\gradlew.bat build
- localhost:8080/swagger-ui
 
## 커밋
chore(api): initialize AdLex Spring Boot project