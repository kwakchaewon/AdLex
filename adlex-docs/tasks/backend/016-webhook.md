# Task 5.4: Webhook 알림

## INPUT
- adlex-api/.../domain/entity/Tenant.kt

## OUTPUT 경로
- adlex-api/.../api/controller/WebhookController.kt
- adlex-api/.../api/dto/WebhookRequest.kt
- adlex-api/.../api/dto/WebhookResponse.kt
- adlex-api/.../domain/entity/Webhook.kt
- adlex-api/.../domain/repository/WebhookRepository.kt
- adlex-api/.../domain/service/WebhookService.kt

## 상세 스펙
POST /api/webhooks: url, events 등록.
HMAC-SHA256 서명. 3회 재시도 exponential backoff.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement webhook notification system
