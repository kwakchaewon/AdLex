# Task 1.5: Batch API + Rules API

## INPUT
- adlex-api/.../api/controller/CheckController.kt

## OUTPUT 경로
- adlex-api/.../api/controller/CheckController.kt (batch 추가)
- adlex-api/.../api/dto/BatchCheckRequest.kt
- adlex-api/.../api/dto/BatchCheckResponse.kt
- adlex-api/.../api/controller/RuleController.kt
- adlex-api/.../api/dto/RuleResponse.kt

## 상세 스펙
POST /v1/check/batch: messages max 100. BatchCheckResponse: results[], summary(total, compliant, violations, ms).
GET /v1/rules: 활성 규칙 목록 (Redis 캐시 10분).
GET /v1/rules/{code}: 특정 규칙 상세 + 법적 근거.

## 완료 조건
.\gradlew.bat build && Swagger UI에서 batch, rules 테스트

## 커밋
feat(api): add batch check and rules endpoints
