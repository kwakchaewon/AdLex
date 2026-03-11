# Task 5.2: 자동 수정 제안 API

## INPUT
- adlex-api/.../infra/llm/ClaudeApiClient.kt

## OUTPUT 경로
- adlex-api/.../api/controller/SuggestController.kt
- adlex-api/.../api/dto/SuggestRequest.kt
- adlex-api/.../api/dto/SuggestResponse.kt
- adlex-api/.../domain/service/SuggestService.kt

## 상세 스펙
POST /v1/suggest: 위반 메시지 → Claude Haiku로 수정본 생성.
Pro 플랜 이상 전용. 플랜 미달 시 403.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement auto-correction suggestion API
