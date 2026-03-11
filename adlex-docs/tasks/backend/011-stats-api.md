# Task 3.5: 통계 API

## INPUT
- adlex-api/.../domain/entity/CheckLog.kt

## OUTPUT 경로
- adlex-api/.../api/controller/UsageController.kt
- adlex-api/.../api/dto/UsageResponse.kt
- adlex-api/.../domain/service/UsageService.kt

## 상세 스펙
GET /api/usage: 이번 달 사용량 (총 검사, 준수율, 위반 건수, 남은 쿼터).
GET /api/usage/daily: 일별 검사 건수 (최근 30일, 차트용).

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement usage statistics API
