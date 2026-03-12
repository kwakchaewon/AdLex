# AdLex 서비스 플로우

> AI-powered 마케팅/광고 법규 준수 검사 API 서비스

---

## 1. 전체 서비스 구조

```
[고객사 시스템]                [AdLex 플랫폼]                    [외부]
      │                              │
      │  ① 회원가입/로그인            │
      │ ─────────────────────────── ▶│  Tenant 생성 (FREE 플랜)
      │                              │
      │  ② API Key 발급              │
      │ ─────────────────────────── ▶│  ApiKey 생성 → SHA-256 해시 저장
      │                              │
      │  ③ 광고 메시지 검사           │
      │  X-API-Key 헤더              │                         [규칙 DB]
      │ ─────────────────────────── ▶│ ── 규칙 엔진 평가 ────▶ Rules
      │                              │ ◀─ violations 반환 ─────
      │  ④ 결과 수신                  │
      │ ◀─────────────────────────── │  CheckHistory 저장
      │                              │
      │  ⑤ (선택) 플랜 업그레이드     │
      │ ─────────────────────────── ▶│ ── PortOne 결제 ───────▶ KG이니시스
      │                              │ ◀─ 구독 활성화 ──────────
```

---

## 2. 사용자 온보딩 플로우

```
[신규 사용자]
      │
      ▼
┌─────────────┐
│  회원가입    │  POST /api/auth/register
│  이메일+PW   │  → Tenant 생성 (plan=FREE, quota=100)
└──────┬──────┘
       │ accessToken + refreshToken 발급
       ▼
┌─────────────┐
│  대시보드     │  /dashboard
│  진입        │  통계 카드, 최근 히스토리 표시
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  API Key    │  /api-keys
│  발급        │ POST /api/api-keys → key 평문 1회 노출
└──────┬──────┘
       │ key 복사 후 고객사 시스템에 등록
       ▼
┌─────────────┐
│  검사 시작   │  /v1/check (X-API-Key 헤더)
│  (API 호출)  │
└─────────────┘
```

---

## 3. 광고 검사 상세 플로우

```
고객사 시스템
      │
      │ POST /v1/check
      │ { channel, message, senderInfo?, sendAt? }
      │
      ▼
┌─────────────────────────────────────────┐
│ ApiKeyAuthFilter                        │
│  1. X-API-Key 헤더 추출                  │
│  2. SHA-256 해시 → Redis 캐시 조회        │
│     (miss) DB 조회 → 캐시 저장 (5분)      │
│  3. Rate Limit 체크 (플랜별 분당 한도)     │
│     → 초과 시 429 반환                   │
└──────────────────┬──────────────────────┘
                   │ tenantId, plan 설정
                   ▼
┌─────────────────────────────────────────┐
│ CheckService.check()                     │
│                                          │
│  1. EvaluationContext 생성                │
│     { channel, message, senderInfo,      │
│       sendAt, tenantId }                 │
│                                          │
│  2. RuleRegistry.getActiveRules()        │
│     → Redis 캐시 (10분) or DB             │
│                                          │
│  3. 규칙 엔진 평가 (병렬)                   │
│     ┌──────────────────────────────┐     │
│     │ REGEX      → 정규식 매칭     │      │
│     │ KEYWORD    → 키워드 포함     │      │
│     │ TIME_RANGE → 발송 시간 제한  │       │
│     │ FIELD_PRESENT → 필수 필드    │      │
│     └──────────────────────────────┘     │
│                                          │
│  4. 위반 규칙 → ViolationDto 생성          │
│     { ruleCode, severity, message,       │
│       legalBasis, suggestion }           │
│                                          │
│  5. CheckHistory 저장 (비동기)            │
│  6. monthlyUsed++ (비동기)               │
└──────────────────┬──────────────────────┘
                   │
                   ▼
        ┌──────────────────┐
        │  CheckResponse   │
        │  compliant: bool │
        │  violations: []  │
        │  channel, ts     │
        └──────────────────┘
```

---

## 4. 인증 토큰 플로우

```
로그인
  │ POST /api/auth/login
  │
  ▼
accessToken (JWT, 30분)  +  refreshToken (JWT, 7일)
  │                               │
  │ API 호출 시 Bearer 헤더        │ 만료 후 재발급
  │                               │
  ▼                               ▼
JwtAuthFilter                POST /api/auth/refresh
  │ claims.subject → tenantId     │
  │ SecurityContext 설정           │ 새 accessToken 발급
  ▼
컨트롤러 @AuthenticationPrincipal tenantId: Long
```

---

## 5. 결제/구독 플로우

```
사용자 → 플랜 & 결제 탭 (/settings)
  │
  │ "결제하기" 클릭 (STARTER / PRO / ENTERPRISE)
  │
  ▼
POST /api/billing/subscribe
  { plan, customerUid }      ← PortOne 빌링키
  │
  │ Subscription(status=PENDING) 저장
  │
  ▼
window.IMP.request_pay()     ← PortOne 결제창 호출
  │ 결제 완료 콜백
  │ { imp_uid, merchant_uid }
  ▼
POST /api/billing/confirm
  { impUid, merchantUid }
  │
  │ PortOne REST API 검증 (GET /payments/{impUid})
  │ status == "paid" 확인
  │
  ├─ 성공 → Subscription(status=ACTIVE)
  │         currentPeriodEnd = now + 30일
  │         Tenant.plan, monthlyQuota 업데이트
  │
  └─ 실패 → 400 에러 반환

구독 취소 흐름:
  POST /api/billing/cancel
  → Subscription(status=CANCELLED, cancelledAt=now)
  → Tenant.plan=FREE, monthlyQuota=100 복원
```

---

## 6. 규칙 엔진 캐싱 구조

```
규칙 수정 (POST/PUT/DELETE /api/rules)
  │
  │ DB 저장
  │
  ▼
RuleRegistry.invalidateCache()
  │ Redis "rules:active" 키 삭제
  │
  ▼
다음 검사 요청 시
  │ Redis miss → DB 재조회 → 캐시 저장 (TTL 10분)
```

---

## 7. 화면 라우팅 맵

```
/login              로그인 / 회원가입
/dashboard          대시보드 (통계 카드, 최근 히스토리, 차트)
/api-keys           API Key 목록 및 생성/삭제
/history            검사 히스토리 (필터, 상세 모달)
/playground         실시간 검사 테스트
/rules              규칙 관리 CRUD
/settings
  └─ 플랜 & 결제    구독 현황, 플랜 카드, 결제/취소
  └─ 계정           프로필 수정 (이메일, 회사명)
  └─ 보안           (준비 중)
```

---

## 8. 플랜별 제한

| 플랜 | 월 검사 한도 | Rate Limit | 가격 |
|------|------------|------------|------|
| FREE | 100건 | 분당 10회 | 무료 |
| STARTER | 1,000건 | 분당 60회 | ₩29,000/월 |
| PRO | 10,000건 | 분당 300회 | ₩99,000/월 |
| ENTERPRISE | 무제한 | 분당 1,000회 | ₩299,000/월 |

> 월 한도 초과 시 402 `QUOTA_EXCEEDED` 반환
> Rate Limit 초과 시 429 + `Retry-After` 헤더 반환

---

## 9. 에러 코드 일람

| 코드 | HTTP | 발생 상황 |
|------|------|-----------|
| `VALIDATION_ERROR` | 400 | 입력값 오류 |
| `UNAUTHORIZED` | 401 | 인증 실패 (토큰/API Key 없거나 유효하지 않음) |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일 |
| `QUOTA_EXCEEDED` | 402 | 월 검사 한도 초과 |
| `RATE_LIMIT_EXCEEDED` | 429 | 분당 요청 한도 초과 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |
