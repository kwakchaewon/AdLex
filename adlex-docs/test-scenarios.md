# AdLex Phase 1~2 테스트 시나리오

> 사전 조건: Docker (`docker-compose up -d`), API 서버 (`.\gradlew.bat bootRun`), Vite (`npm run dev`) 실행 중

---

## 우선순위 권장 순서

```
A-1 → A-4 → B-1 → C-1 → C-2 → E-1 → F-1 → H-2
```

---

## A. 인증 (회원가입 / 로그인)

| # | 동작 | 기대 결과 |
|---|------|-----------|
| A-1 | `POST /api/auth/register` — 정상 이메일 + 비밀번호(8자↑) | 201, `accessToken` / `refreshToken` 반환 |
| A-2 | A-1 동일 이메일 재가입 | 409 `DUPLICATE_EMAIL` |
| A-3 | `POST /api/auth/register` — 비밀번호 7자 이하 | 400 `VALIDATION_ERROR` |
| A-4 | `POST /api/auth/login` — 올바른 자격증명 | 200, 토큰 반환 |
| A-5 | `POST /api/auth/login` — 틀린 비밀번호 | 401 `UNAUTHORIZED` |
| A-6 | `POST /api/auth/refresh` — 유효한 refreshToken | 200, 새 accessToken 반환 |
| A-7 | 만료/위조 토큰으로 `GET /api/users/me` 호출 | 401, 대시보드에서 로그인 화면 리다이렉트 |

---

## B. API Key 관리

| # | 동작 | 기대 결과 |
|---|------|-----------|
| B-1 | `POST /api/api-keys` (JWT 헤더 포함) | 201, `key` 평문 1회 노출 |
| B-2 | `GET /api/api-keys` | 키 목록 반환 (key 값 마스킹) |
| B-3 | `DELETE /api/api-keys/{id}` | 204, 이후 해당 키로 검사 시 401 |
| B-4 | 대시보드 `/api-keys` 화면 → "새 API Key 생성" 버튼 | 모달에 키 표시, 복사 버튼 동작 |

---

## C. 광고 검사 핵심 API

> `X-API-Key: {B-1에서 발급받은 키}` 헤더 필요

**단건 검사 curl 예시:**

```bash
curl -X POST http://localhost:8080/v1/check \
  -H "X-API-Key: {your-key}" \
  -H "Content-Type: application/json" \
  -d '{"channel":"SMS","message":"지금 무료로 받으세요! 클릭하세요"}'
```

| # | 메시지 | 기대 결과 |
|---|--------|-----------|
| C-1 | `"지금 바로 무료로 받으세요!"` | violations 포함 (스팸성 표현) |
| C-2 | `"AdLex 서비스 이용 안내입니다."` | `compliant: true`, `violations: []` |
| C-3 | `X-API-Key` 헤더 없이 `POST /v1/check` | 401 `X-API-Key 헤더가 필요합니다` |
| C-4 | `channel: "SMS"`, message 90자 초과 | violations에 글자수 규칙 위반 포함 |
| C-5 | 새벽 시간대(0~8시) 조건, `channel: "SMS"` | 야간 발송 규칙 위반 |

---

## D. Batch 검사

| # | 동작 | 기대 결과 |
|---|------|-----------|
| D-1 | `POST /v1/check/batch` — items 5개 | results 배열 5개 반환 |
| D-2 | `POST /v1/check/batch` — items 11개 (한도 초과) | 400 `VALIDATION_ERROR` |

---

## E. 히스토리 / 통계

| # | 동작 | 기대 결과 |
|---|------|-----------|
| E-1 | C-1 검사 후 `GET /api/history` | 방금 결과 목록에 포함 |
| E-2 | `GET /api/history?channel=SMS` | SMS 검사 결과만 필터링 |
| E-3 | `GET /api/stats/summary` | totalChecks, violationRate 등 수치 반환 |
| E-4 | 대시보드 화면 수치 카드 + 차트 확인 | NaN / undefined 없어야 함 |
| E-5 | 히스토리 화면 → 행 클릭 → 상세 모달 | violations 목록, legalBasis 표시 |

---

## F. Playground

| # | 동작 | 기대 결과 |
|---|------|-----------|
| F-1 | API Key 입력 → 메시지 작성 → "검사 실행" | 결과 패널에 준수/위반 표시 |
| F-2 | API Key 없이 실행 | 에러 메시지 표시 |
| F-3 | 채널 SMS → KAKAO → EMAIL 전환 후 재검사 | 채널별 규칙 차이 확인 |
| F-4 | 200자 이상 입력 | 글자 수 카운터 색 변화 (경고) |

---

## G. 설정 — 계정 탭

| # | 동작 | 기대 결과 |
|---|------|-----------|
| G-1 | `/settings` → 계정 탭 → 회사명 수정 → 저장 | 성공 배너 표시, 새로고침 후 유지 |
| G-2 | 이메일 필드 편집 시도 | 수정 불가 (read-only) 확인 |

---

## H. 규칙 관리

| # | 동작 | 기대 결과 |
|---|------|-----------|
| H-1 | `GET /api/rules` | 전체 규칙 목록 반환 |
| H-2 | `/rules` 화면 → "새 규칙" → 코드/이름/타입/채널/심각도 입력 → 저장 | 테이블에 즉시 추가 |
| H-3 | 중복 코드 재등록 | 400 에러 배너 표시 |
| H-4 | 규칙 수정 (이름 변경) → 저장 | 테이블에 즉시 반영 |
| H-5 | 규칙 행 삭제 버튼 → 인라인 확인 → 확인 | 행 사라짐, `active=false` |
| H-6 | H-5 이후 `GET /v1/rules` | 비활성 규칙 미포함 확인 |
| H-7 | 심각도 필터 HIGH 클릭 | HIGH 규칙만 표시 |

---

## I. Rate Limit

| # | 동작 | 기대 결과 |
|---|------|-----------|
| I-1 | FREE 플랜 API Key로 `/v1/check` 빠르게 반복 호출 | `X-RateLimit-Remaining` 헤더 감소 후 429 반환 |
