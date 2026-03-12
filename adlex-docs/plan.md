# AdLex — Development Plan
> AI-powered API for checking marketing and advertising compliance

## 진행 요약
- 전체: 24/71 (33%)
- 현재: Phase 3 진행중 (1/16)

---

## Phase 1: 프로토타입 (4주)
- [x] 1.1 프로젝트 셋업 `backend/001`
- [x] 1.2 DB 스키마 + Flyway `backend/002`
- [x] 1.3 규칙 엔진 `backend/003` ← 1.2
- [x] 1.4 POST /v1/check `backend/004` ← 1.3
- [x] 1.5 Batch + Rules API `backend/005` ← 1.4
- [x] 1.6 인증 + Rate Limit `backend/006` ← 1.2
- [x] 1.7 테스트 `backend/007` ← 1.4,1.6

## Phase 2: 화면 + API (3주)
- [x] 2.1 Vue 셋업 `frontend/001`
- [x] 2.2 회원 API `backend/008` ← 1.6
- [x] 2.3 API Key API `backend/009` ← 2.2
- [x] 2.4 히스토리 API `backend/010` ← 1.2
- [x] 2.5 통계 API `backend/011` ← 1.2
- [x] 2.6 로그인 화면 `frontend/002` ← 2.1,2.2
- [x] 2.7 레이아웃 `frontend/003` ← 2.1
- [x] 2.8 대시보드 `frontend/004` ← 2.7,2.5
- [x] 2.9 API Key 화면 `frontend/005` ← 2.7,2.3
- [x] 2.10 히스토리 화면 `frontend/006` ← 2.7,2.4
- [x] 2.11 Playground `frontend/007` ← 2.7
- [x] 2.12 결제 API `backend/012` ← 2.2
- [x] 2.13 결제 화면 `frontend/009` ← 2.7,2.12
- [x] 2.14 Settings `frontend/008` ← 2.2
- [x] 2.15 규칙 관리 API (CRUD) `backend/013` ← 1.3
- [x] 2.16 규칙 관리 화면 `frontend/010` ← 2.7,2.15

## Phase 3: AI/RAG 고도화 (3주)
> 차별점: Layer 1(규칙 엔진, 결정론적·법조문 추적) + Layer 2(LLM+RAG, 맥락 보조)
> LLM은 독립 판단자가 아니라 규칙 엔진 통과 후 과장·허위 표현 등 2차 검토 보조.
> RAG: pgvector로 최신 법령 조문·판례를 LLM 컨텍스트에 주입 → 법조문 인용 가능.
> 규칙 DB 자체가 핵심 자산 → 크롤러로 법령 변경 자동 반영.

### 3-A: RAG 인프라
- [x] 3.1 pgvector 마이그레이션 + 법령 청크 테이블 `backend/014` ← 1.2
- [ ] 3.2 법령 시드 데이터 50개 + 임베딩 파이프라인 `backend/015` ← 3.1
- [ ] 3.3 판례 시드 데이터 + 임베딩 파이프라인 `backend/016` ← 3.1
- [ ] 3.4 RAG 검색 서비스 (EmbeddingService + VectorSearchService) `backend/017` ← 3.1

### 3-B: LLM Layer 2
- [ ] 3.5 Claude API 클라이언트 (ClaudeApiClient) `backend/018` ← 3.4
- [ ] 3.6 LLM Layer 2 보조 분석기 (RAG 컨텍스트 주입) `backend/019` ← 3.5,3.4
- [ ] 3.7 수정 제안 API `backend/020` ← 3.6
- [ ] 3.8 Layer 2 플랜별 제어 (PRO 이상만 호출) `backend/021` ← 3.6

### 3-C: 법령 자동화
- [ ] 3.9 법령 크롤러 (국가법령정보센터 변경 감지) `backend/022` ← 1.3
- [ ] 3.10 크롤러 → 청크 분할 → 임베딩 자동 업데이트 `backend/023` ← 3.9,3.4
- [ ] 3.11 크롤러 → 규칙 DB 자동 변환 파이프라인 (LLM 보조) `backend/024` ← 3.9,3.6

### 3-D: 기타 고도화
- [ ] 3.12 채널별 규칙 세분화 (LengthCheck/ContentRatio Evaluator) `backend/025` ← 1.3
- [ ] 3.13 Webhook `backend/026` ← 1.4
- [ ] 3.14 리포트 (유사 판례 포함) `backend/027` ← 4.4,3.3
- [ ] 3.15 SDK Python `backend/028` ← 1.4
- [ ] 3.16 SDK JS `backend/029` ← 1.4

## Phase 4: 배포 (2주)
- [ ] 4.1 Docker `infra/001`
- [ ] 4.2 AWS 인프라 `infra/002`
- [ ] 4.3 CI/CD `infra/003` ← 4.1,4.2
- [ ] 4.4 HTTPS + 도메인 `infra/004` ← 4.2
- [ ] 4.5 Health Check `infra/005` ← 4.2

## Phase 5: 모니터링 (1주)
- [ ] 5.1 Sentry `infra/006` ← 2.2
- [ ] 5.2 Grafana `infra/007` ← 2.2
- [ ] 5.3 Alarm + Slack `infra/008` ← 2.2
- [ ] 5.4 로그 `infra/009` ← 2.2

## Phase 7: 상용화 준비 — 보안 강화 + 서비스 완성도 (별도 일정)
> 기준: Claude Security 가이드라인 + 과학기술부 「주요정보통신기반시설 기술적 취약점 분석·평가 방법(2021)」
> 세부 취약 항목은 별도 문서(adlex-docs/security-checklist.md)로 관리 예정.
> ※ 상세 태스크는 보안 점검 문서 업로드 후 확정

### 7-A: 보안 취약점 분석 및 대응
- [ ] 7.1 보안 취약점 분석 (`security/001`) — 주요정보통신기반시설 기준 항목 점검
- [ ] 7.2 인증/인가 보안 강화 (`security/002`) ← 7.1
  > JWT 알고리즘 검증, 토큰 탈취 대응, API Key 권한 세분화
- [ ] 7.3 입력값 검증 + 인젝션 대응 (`security/003`) ← 7.1
  > SQL Injection, SSRF, Path Traversal, XXE 대응
- [ ] 7.4 API 보안 강화 (`security/004`) ← 7.1
  > CORS 정책, 보안 헤더(CSP/HSTS/X-Frame), 민감 정보 노출 방지
- [ ] 7.5 LLM 보안 (`security/005`) ← 7.1
  > Prompt Injection 방어, Claude API 입출력 검증, 응답 sanitization
- [ ] 7.6 인프라 보안 (`security/006`) ← 4.2,7.1
  > AWS IAM 최소 권한, VPC 설정, 보안 그룹, 시크릿 관리(Secrets Manager)
- [ ] 7.7 데이터 보호 (`security/007`) ← 7.1
  > 개인정보 암호화(AES-256), PII 마스킹, 데이터 보존 정책, GDPR/개인정보보호법 대응
- [ ] 7.8 보안 감사 로그 (`security/008`) ← 5.4,7.1
  > 인증 이벤트, 권한 변경, 이상 접근 패턴 로깅 및 알림

### 7-B: 서비스 상용화 완성도
- [ ] 7.9 API 문서 자동화 (`backend/034`) — Swagger/OpenAPI 정비, 외부 공개용 문서
- [ ] 7.10 온보딩 플로우 개선 (`frontend/013`) — 가입 후 API Key 발급까지 가이드
- [ ] 7.11 이용약관 / 개인정보처리방침 법적 검토 (`legal/001`)
- [ ] 7.12 서비스 SLA 정의 + 상태 페이지 (`infra/010`) ← 5.1
- [ ] 7.13 부하 테스트 + 성능 튜닝 (`infra/011`) ← 4.3
- [ ] 7.14 재해복구(DR) 계획 + 백업 정책 (`infra/012`) ← 4.2
- [ ] 7.15 침투 테스트 (외부 수행) (`security/009`) ← 7.1~7.8 완료 후
- [ ] 7.16 보안 인증 검토 (ISMS-P 또는 ISO 27001) (`security/010`)

## Phase 6: 추가 (지속)
- [ ] 6.1 백오피스 `frontend/010`
- [ ] 6.2 산업별 규칙 `backend/030` ← 1.3
- [ ] 6.3 멀티유저 API `backend/031` ← 4.2
- [ ] 6.4 멀티유저 화면 `frontend/011` ← 6.3
- [ ] 6.5 Slack 봇 `backend/032`
- [ ] 6.6 커스텀 규칙 API `backend/033` ← 1.3
- [ ] 6.7 커스텀 규칙 화면 `frontend/012` ← 6.6
