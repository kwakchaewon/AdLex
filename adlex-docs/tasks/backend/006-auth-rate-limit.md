# Task 1.6: 인증 (API Key) + Rate Limiting

## INPUT
- adlex-api/.../infra/security/SecurityConfig.kt

## OUTPUT 경로
- adlex-api/.../infra/security/ApiKeyAuthFilter.kt
- adlex-api/.../infra/security/JwtProvider.kt
- adlex-api/.../infra/security/SecurityConfig.kt (수정)
- adlex-api/.../infra/cache/RateLimiter.kt
- adlex-api/.../domain/service/ApiKeyService.kt

## 상세 스펙
ApiKeyAuthFilter(OncePerRequestFilter): /v1/** X-API-Key → SHA-256 해시 → DB 조회 (Redis 캐시 5분).
API Key 형식: al_live_ + 32자 랜덤 (SecureRandom, Base62). 생성 시에만 키 반환, SHA-256 해시로 DB 저장.
RateLimiter(Redis Sliding Window): FREE=10/min, STARTER=60/min, PRO=300/min. 초과 시 429 + Retry-After + X-RateLimit-* 헤더.
쿼터 초과: Redis INCR, 초과 시 402.

## 완료 조건
.\gradlew.bat build && API Key 없이 /v1/check 호출 시 401

## 커밋
feat(auth): implement API key authentication and rate limiting
