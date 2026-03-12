# AdLex 공통 타입 정의
> 모든 태스크 파일의 상세 스펙이 참조하는 단일 소스

---

## 1. Enums

| Enum | Values |
|------|--------|
| Channel | `SMS`, `KAKAO`, `EMAIL` |
| Severity | `HIGH`, `MEDIUM`, `LOW` |
| Plan | `FREE`, `STARTER`, `PRO`, `ENTERPRISE` |
| RuleType | `REGEX`, `KEYWORD`, `TIME_RANGE`, `FIELD_PRESENT`, `LLM_JUDGE` |
| ApiKeyStatus | `ACTIVE`, `REVOKED` |
| SubscriptionStatus | `ACTIVE`, `CANCELED`, `PAST_DUE` |
| ErrorCode | `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `RATE_LIMIT_EXCEEDED`, `QUOTA_EXCEEDED`, `INTERNAL_ERROR` |

---

## 2. Entity 컬럼 정의

### BaseEntity (@MappedSuperclass)
| Column | Kotlin | SQL | Nullable | Default |
|--------|--------|-----|----------|---------|
| id | Long | BIGSERIAL | N | PK auto |
| created_at | Instant | TIMESTAMPTZ | N | now() |
| updated_at | Instant | TIMESTAMPTZ | N | now() |

### tenants
| Column | Kotlin | SQL | Nullable | Default | Constraint |
|--------|--------|-----|----------|---------|------------|
| email | String | VARCHAR(255) | N | - | UNIQUE |
| password_hash | String | VARCHAR(255) | N | - | - |
| company_name | String? | VARCHAR(100) | Y | null | - |
| plan | Plan | VARCHAR(20) | N | 'FREE' | - |
| monthly_quota | Int | INTEGER | N | 100 | - |
| monthly_used | Int | INTEGER | N | 0 | - |

### api_keys
| Column | Kotlin | SQL | Nullable | Default | Constraint |
|--------|--------|-----|----------|---------|------------|
| tenant_id | Long | BIGINT | N | - | FK → tenants |
| name | String | VARCHAR(50) | N | - | - |
| key_hash | String | VARCHAR(64) | N | - | UNIQUE |
| key_prefix | String | VARCHAR(12) | N | - | - |
| status | ApiKeyStatus | VARCHAR(10) | N | 'ACTIVE' | - |
| last_used_at | Instant? | TIMESTAMPTZ | Y | null | - |

### rules
| Column | Kotlin | SQL | Nullable | Default | Constraint |
|--------|--------|-----|----------|---------|------------|
| code | String | VARCHAR(50) | N | - | UNIQUE |
| name | String | VARCHAR(100) | N | - | - |
| description | String? | TEXT | Y | null | - |
| type | RuleType | VARCHAR(20) | N | - | - |
| channel | String | VARCHAR(50) | N | - | CSV (SMS,KAKAO) |
| severity | Severity | VARCHAR(10) | N | - | - |
| pattern | String? | TEXT | Y | null | - |
| config | String? | JSONB | Y | null | matchMode 등 |
| legal_basis | String? | TEXT | Y | null | - |
| active | Boolean | BOOLEAN | N | true | - |

### check_logs (PARTITION BY RANGE(created_at) 월별)
| Column | Kotlin | SQL | Nullable | Default | Constraint |
|--------|--------|-----|----------|---------|------------|
| tenant_id | Long | BIGINT | N | - | FK → tenants |
| message | String | TEXT | N | - | - |
| channel | Channel | VARCHAR(10) | N | - | - |
| compliant | Boolean | BOOLEAN | N | - | - |
| violation_count | Int | INTEGER | N | 0 | - |
| violations | String | JSONB | N | '[]' | - |
| processing_ms | Long | BIGINT | N | - | - |

### 인덱스
```
idx_api_keys_hash ON api_keys(key_hash)
idx_api_keys_tenant ON api_keys(tenant_id)
idx_check_logs_tenant_created ON check_logs(tenant_id, created_at)
idx_rules_active ON rules(active) WHERE active = true
```

---

## 3. Plan 제한

| Plan | quota/월 | rate/분 | key 개수 | features |
|------|---------|---------|---------|----------|
| FREE | 100 | 10 | 1 | basic check |
| STARTER | 3,000 | 60 | 3 | + history |
| PRO | 30,000 | 300 | 10 | + suggest, LLM |
| ENTERPRISE | 100,000 | 1,000 | unlimited | + webhook, custom rules |

가격: STARTER 29,000 KRW/월, PRO 99,000 KRW/월, ENTERPRISE 별도 협의

---

## 4. Seed Rules (V2__seed_rules.sql)

| code | name | type | channel | severity | pattern / config |
|------|------|------|---------|----------|------------------|
| AD_LABEL | (광고) 표기 | REGEX | SMS,KAKAO | HIGH | `^\(광고\)` matchMode=REQUIRE |
| OPT_OUT_080 | 080 수신거부 | REGEX | SMS | HIGH | `080-\d{3,4}-\d{4}` matchMode=REQUIRE |
| OPT_OUT_LINK | 수신거부 링크 | FIELD_PRESENT | EMAIL | HIGH | field=unsubscribeUrl |
| NIGHT_SMS | 야간발송 제한 | TIME_RANGE | SMS | HIGH | deny 21:00-08:00 KST |
| NIGHT_KAKAO | 카카오 시간제한 | TIME_RANGE | KAKAO | HIGH | deny 20:50-08:00 KST |
| EMAIL_SUBJECT | 이메일 제목 (광고) | REGEX | EMAIL | HIGH | subject contains `\(광고\)` matchMode=REQUIRE |
| SENDER_ID | 발신자 표기 | FIELD_PRESENT | SMS,KAKAO | MEDIUM | field=senderName |

---

## 5. 공통 DTO

### SenderInfo
```
name: String?
phoneNumber: String?
email: String?
```

### CheckOptions
```
skipRules: List<String>? = null   // 건너뛸 규칙 code 목록
```

### ViolationDto
```
ruleCode: String
severity: Severity
message: String
legalBasis: String?
suggestion: String?
```

### ErrorResponse
```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 45 seconds.",
    "details": {}
  }
}
```

---

## 6. 인증

### API Key (/v1/**)
- 형식: `al_live_` + 32자 Base62 (A-Za-z0-9)
- 저장: SHA-256 해시 → `api_keys.key_hash`
- 헤더: `X-API-Key`
- 캐시: Redis `apikey:{hash}` TTL 5분

### JWT (/api/**)
- Algorithm: HS256
- Secret: `adlex.jwt.secret` (application.yml)
- Claims: `sub`=tenantId(Long), `email`, `plan`
- Access Token: 30분
- Refresh Token: 7일
- 비밀번호 해싱: BCryptPasswordEncoder

### 공개 경로 (인증 불요)
- `/api/auth/register`, `/api/auth/login`
- `/swagger-ui/**`, `/v3/api-docs/**`
- `/actuator/health`

---

## 7. Rate Limit

- 알고리즘: Fixed Window (Redis INCR + EXPIRE, 1분 윈도우)
- 키: `ratelimit:{tenantId}:{minute}`
- 응답 헤더: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- 초과 시: HTTP 429 + `Retry-After` 헤더

### Quota (월간)
- Redis: `quota:{tenantId}:{yyyy-MM}` INCR
- 주기적 DB 동기화 (tenants.monthly_used)
- 초과 시: HTTP 402
- 리셋: 매월 1일 00:00 KST
