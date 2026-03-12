# AdLex — Development Plan
> AI-powered API for checking marketing and advertising compliance

## 진행 요약
- 전체: 1/44 (2%)
- 현재: Phase 1 진행중 (1/7)

---

## Phase 1: 프로토타입 (4주)
- [x] 1.1 프로젝트 셋업 `backend/001`
- [ ] 1.2 DB 스키마 + Flyway `backend/002`
- [ ] 1.3 규칙 엔진 `backend/003` ← 1.2
- [ ] 1.4 POST /v1/check `backend/004` ← 1.3
- [ ] 1.5 Batch + Rules API `backend/005` ← 1.4
- [ ] 1.6 인증 + Rate Limit `backend/006` ← 1.2
- [ ] 1.7 테스트 `backend/007` ← 1.4,1.6

## Phase 2: 배포 (2주)
- [ ] 2.1 Docker `infra/001`
- [ ] 2.2 AWS 인프라 `infra/002`
- [ ] 2.3 CI/CD `infra/003` ← 2.1,2.2
- [ ] 2.4 HTTPS + 도메인 `infra/004` ← 2.2
- [ ] 2.5 Health Check `infra/005` ← 2.2

## Phase 3: 화면 + API (3주)
- [ ] 3.1 Vue 셋업 `frontend/001`
- [ ] 3.2 회원 API `backend/008` ← 1.6
- [ ] 3.3 API Key API `backend/009` ← 3.2
- [ ] 3.4 히스토리 API `backend/010` ← 1.2
- [ ] 3.5 통계 API `backend/011` ← 1.2
- [ ] 3.6 로그인 화면 `frontend/002` ← 3.1,3.2
- [ ] 3.7 레이아웃 `frontend/003` ← 3.1
- [ ] 3.8 대시보드 `frontend/004` ← 3.7,3.5
- [ ] 3.9 API Key 화면 `frontend/005` ← 3.7,3.3
- [ ] 3.10 히스토리 화면 `frontend/006` ← 3.7,3.4
- [ ] 3.11 Playground `frontend/007` ← 3.7
- [ ] 3.12 결제 API `backend/012` ← 3.2
- [ ] 3.13 결제 화면 `frontend/009` ← 3.7,3.12
- [ ] 3.14 Settings `frontend/008` ← 3.2

## Phase 4: 모니터링 (1주)
- [ ] 4.1 Sentry `infra/006` ← 2.2
- [ ] 4.2 Grafana `infra/007` ← 2.2
- [ ] 4.3 Alarm + Slack `infra/008` ← 2.2
- [ ] 4.4 로그 `infra/009` ← 2.2

## Phase 5: 고도화 (2주)
- [ ] 5.1 LLM `backend/013` ← 1.3
- [ ] 5.2 수정 제안 `backend/014` ← 5.1
- [ ] 5.3 채널별 규칙 `backend/015` ← 1.3
- [ ] 5.4 Webhook `backend/016` ← 1.4
- [ ] 5.5 리포트 `backend/017` ← 3.4
- [ ] 5.6 SDK Python `backend/018` ← 1.4
- [ ] 5.7 SDK JS `backend/019` ← 1.4

## Phase 6: 추가 (지속)
- [ ] 6.1 백오피스 `frontend/010`
- [ ] 6.2 산업별 규칙 `backend/020` ← 1.3
- [ ] 6.3 멀티유저 API `backend/021` ← 3.2
- [ ] 6.4 멀티유저 화면 `frontend/011` ← 6.3
- [ ] 6.5 Slack 봇 `backend/022`
- [ ] 6.6 커스텀 규칙 API `backend/023` ← 1.3
- [ ] 6.7 커스텀 규칙 화면 `frontend/012` ← 6.6
