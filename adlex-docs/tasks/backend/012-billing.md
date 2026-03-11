# Task 3.12: 결제 연동 (백엔드)

## INPUT
- adlex-api/.../domain/entity/Tenant.kt

## OUTPUT 경로
- adlex-api/.../api/controller/BillingController.kt
- adlex-api/.../api/dto/BillingRequest.kt
- adlex-api/.../api/dto/BillingResponse.kt
- adlex-api/.../domain/service/BillingService.kt
- adlex-api/.../domain/entity/Subscription.kt
- adlex-api/.../domain/repository/SubscriptionRepository.kt
- adlex-api/src/main/resources/db/migration/V3__subscription.sql

## 상세 스펙
POST /api/billing/subscribe: 구독 시작. POST /api/billing/confirm: 결제 확인.
POST /api/billing/cancel: 구독 취소. GET /api/billing/status: 현재 구독 상태.
PortOne(구 아임포트) 연동. 플랜 변경: 업그레이드/다운그레이드/취소.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement billing with PortOne integration
