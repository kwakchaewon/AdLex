# AdLex — Development Plan
> AI-powered API for checking marketing and advertising compliance

## 진행 요약
- 전체: 22/48 (45%)
- 현재: Phase 2 진행중 (15/16)

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
- [ ] 2.16 규칙 관리 화면 `frontend/010` ← 2.7,2.15

## Phase 3: 배포 (2주)
- [ ] 3.1 Docker `infra/001`
- [ ] 3.2 AWS 인프라 `infra/002`
- [ ] 3.3 CI/CD `infra/003` ← 3.1,3.2
- [ ] 3.4 HTTPS + 도메인 `infra/004` ← 3.2
- [ ] 3.5 Health Check `infra/005` ← 3.2

## Phase 4: 모니터링 (1주)
- [ ] 4.1 Sentry `infra/006` ← 2.2
- [ ] 4.2 Grafana `infra/007` ← 2.2
- [ ] 4.3 Alarm + Slack `infra/008` ← 2.2
- [ ] 4.4 로그 `infra/009` ← 2.2

## Phase 5: 고도화 (2주)
> 차별점: Layer 1(규칙 엔진, 결정론적·법조문 추적) + Layer 2(LLM, 맥락 보조)
> LLM은 독립 판단자가 아니라 규칙 엔진 통과 후 과장·허위 표현 등 2차 검토 보조.
> 규칙 DB 자체가 핵심 자산 → 크롤러로 법령 변경 자동 반영.
- [ ] 5.1 LLM Layer 2 보조 분석기 `backend/014` ← 1.3
- [ ] 5.2 수정 제안 `backend/015` ← 5.1
- [ ] 5.3 채널별 규칙 `backend/016` ← 1.3
- [ ] 5.4 Webhook `backend/017` ← 1.4
- [ ] 5.5 리포트 `backend/018` ← 3.4
- [ ] 5.6 SDK Python `backend/019` ← 1.4
- [ ] 5.7 SDK JS `backend/020` ← 1.4
- [ ] 5.8 법령 크롤러 (국가법령정보센터 변경 감지) `backend/021` ← 1.3
- [ ] 5.9 크롤러 → 규칙 DB 자동 변환 파이프라인 (LLM 보조) `backend/022` ← 5.8,5.1

## Phase 6: 추가 (지속)
- [ ] 6.1 백오피스 `frontend/010`
- [ ] 6.2 산업별 규칙 `backend/020` ← 1.3
- [ ] 6.3 멀티유저 API `backend/021` ← 3.2
- [ ] 6.4 멀티유저 화면 `frontend/011` ← 6.3
- [ ] 6.5 Slack 봇 `backend/022`
- [ ] 6.6 커스텀 규칙 API `backend/023` ← 1.3
- [ ] 6.7 커스텀 규칙 화면 `frontend/012` ← 6.6
