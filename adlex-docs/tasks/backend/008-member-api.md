# Task 3.2: 회원 관리 API

## INPUT
- adlex-api/.../domain/entity/Tenant.kt
- adlex-api/.../infra/security/JwtProvider.kt

## OUTPUT 경로
- adlex-api/.../api/controller/AuthController.kt
- adlex-api/.../api/dto/AuthRequest.kt
- adlex-api/.../api/dto/AuthResponse.kt
- adlex-api/.../domain/service/AuthService.kt

## 상세 스펙
POST /api/auth/register → Tenant 생성 + API Key 1개 자동 발급 + JWT 반환.
POST /api/auth/login → JWT 발급.
GET /api/auth/me → 현재 사용자 + plan + 이번 달 사용량.
PUT /api/auth/me → 프로필 수정.
POST /api/auth/refresh → JWT 갱신.
인증 경로: /api/** → JWT, /v1/** → API Key, 예외: register/login/swagger/health.

## 완료 조건
.\gradlew.bat build && Swagger에서 register → login 테스트

## 커밋
feat(api): implement auth API with JWT
