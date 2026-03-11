# Task 4.4: 로그 중앙화

## OUTPUT 경로
- adlex-api/src/main/resources/logback-spring.xml
- adlex-infra/terraform/cloudwatch-logs.tf

## 상세 스펙
logback JSON 포맷 → CloudWatch Logs.
Insights 쿼리 저장. 보관 30일.

## 완료 조건
.\gradlew.bat build && JSON 로그 출력 확인

## 커밋
chore(infra): configure centralized JSON logging
