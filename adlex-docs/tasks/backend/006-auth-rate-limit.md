# Task 1.6: 인증 (API Key) + Rate Limiting

공통 타입: adlex-docs/shared-types.md (§6 인증, §7 Rate Limit)

## 의존성
- backend/002 (Tenant, ApiKey Entity, Repository)

## INPUT
- adlex-api/.../infra/security/SecurityConfig.kt
- adlex-api/.../domain/entity/ApiKey.kt
- adlex-api/.../domain/repository/ApiKeyRepository.kt

## OUTPUT 경로
- adlex-api/.../infra/security/ApiKeyAuthFilter.kt
- adlex-api/.../infra/security/JwtProvider.kt
- adlex-api/.../infra/security/SecurityConfig.kt (수정)
- adlex-api/.../infra/cache/RateLimiter.kt
- adlex-api/.../domain/service/ApiKeyService.kt

## 상세 스펙

### ApiKeyAuthFilter (OncePerRequestFilter)
적용 경로: `/v1/**`
```
1. X-API-Key 헤더 읽기 → 없으면 401 UNAUTHORIZED
2. SHA-256 해시 계산 (MessageDigest, hex encoding)
3. Redis 캐시 조회: key="apikey:{hash}", TTL=5분
4. cache miss → ApiKeyRepository.findByKeyHash(hash)
5. 없거나 status=REVOKED → 401
6. 찾으면 Redis 캐시 저장 (tenantId, plan 직렬화)
7. SecurityContext에 Authentication 설정:
   - principal = tenantId (Long)
   - details = mapOf("plan" to plan, "tenantId" to tenantId)
8. ApiKey.lastUsedAt 업데이트 (비동기/배치)
```

### ApiKeyService (@Service)
```kotlin
fun generateKey(tenantId: Long, name: String): Pair<String, ApiKey>
// 1. rawKey = "al_live_" + SecureRandom 32자 Base62 (A-Za-z0-9)
// 2. keyHash = SHA-256(rawKey) → hex string (64자)
// 3. keyPrefix = rawKey.take(12)
// 4. ApiKey 엔티티 저장 (tenant_id, name, key_hash, key_prefix)
// 5. return Pair(rawKey, savedEntity) — rawKey는 이때만 반환

fun validateKey(rawKey: String): ApiKey?
// SHA-256 해시 → findByKeyHash → status=ACTIVE 확인
```

### JwtProvider (@Component)
```kotlin
@Component
class JwtProvider(@Value("\${adlex.jwt.secret}") private val secret: String) {
    fun generateAccessToken(tenantId: Long, email: String, plan: Plan): String
    // claims: sub=tenantId, email, plan. exp=30분

    fun generateRefreshToken(tenantId: Long): String
    // claims: sub=tenantId. exp=7일

    fun validateToken(token: String): Claims?
    // 유효하면 Claims 반환, 만료/위조면 null

    // Algorithm: HS256, io.jsonwebtoken:jjwt-api 사용
}
```

### RateLimiter (@Component)
```kotlin
@Component
class RateLimiter(private val redisTemplate: StringRedisTemplate) {
    fun isAllowed(tenantId: Long, plan: Plan): RateLimitResult
    // 알고리즘: Fixed Window (1분 단위)
    // Redis key: "ratelimit:{tenantId}:{epochMinute}"
    // INCR → 첫 요청이면 EXPIRE 60초
    // plan별 limit: shared-types.md §3 참조
    // return RateLimitResult(allowed, limit, remaining, resetEpochSecond)
}

data class RateLimitResult(
    val allowed: Boolean,
    val limit: Int,
    val remaining: Int,
    val resetAt: Long  // epoch seconds
)
```

### SecurityConfig 수정
```kotlin
// 기존 공개 경로 유지 + 필터 체인 추가:
// /v1/** → ApiKeyAuthFilter
// /api/** → JWT Filter (Task 3.2에서 추가)
// 공개: /api/auth/register, /api/auth/login, /swagger-ui/**, /v3/api-docs/**, /actuator/health
```

### Rate Limit 응답 헤더
모든 /v1/** 응답에 추가:
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 42
X-RateLimit-Reset: 1741795260
```
초과 시: HTTP 429 + `Retry-After: {seconds}` + ErrorResponse(RATE_LIMIT_EXCEEDED)

### Quota 체크
- Redis key: `quota:{tenantId}:{yyyy-MM}` INCR
- shared-types.md §3 Plan별 monthly_quota 참조
- 초과 시: HTTP 402 + ErrorResponse(QUOTA_EXCEEDED)

## 완료 조건
.\gradlew.bat build && API Key 없이 /v1/check 호출 시 401, 유효 키로 호출 시 200 + Rate Limit 헤더 확인

## 커밋
feat(auth): implement API key authentication and rate limiting
