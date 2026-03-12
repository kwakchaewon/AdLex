# AdLex

마케팅/광고 메시지(SMS·카카오·이메일)의 법규 준수 여부를 자동 검사하는 AI API 서비스.

규칙 엔진(결정론적 법조문 추적) + LLM Layer 2(RAG 기반 맥락 보조) 이중 구조.

## 기술 스택

| 영역 | 스택 |
|------|------|
| Backend | Kotlin / Spring Boot 3 / JDK 21 |
| Frontend | Vue.js 3 / Vite / TypeScript / PrimeVue 4 |
| Database | PostgreSQL 16 + pgvector / Redis 7 |
| AI | Claude API / RAG (pgvector) |
| Infra | Docker / AWS ECS Fargate |

## 프로젝트 구조

```
adlex-api/          # Spring Boot API 서버
adlex-dashboard/    # Vue.js 관리 대시보드
adlex-infra/        # Docker Compose, Terraform
adlex-docs/         # 문서, 태스크, 진행 계획
```

## 로컬 실행

```powershell
# 1. DB + Redis
cd adlex-infra; docker-compose up -d

# 2. API 서버
cd adlex-api; .\gradlew.bat bootRun

# 3. 대시보드
cd adlex-dashboard; npm install; npm run dev
```

## 주요 엔드포인트

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/v1/check` | API Key | 메시지 법규 검사 |
| POST | `/v1/check/batch` | API Key | 일괄 검사 (최대 10건) |
| GET | `/v1/rules` | API Key | 활성 규칙 목록 |
| POST | `/api/auth/register` | - | 회원가입 |
| POST | `/api/auth/login` | - | 로그인 |
| GET | `/api/api-keys` | JWT | API Key 관리 |
| GET | `/api/history` | JWT | 검사 히스토리 |
| GET | `/api/stats/summary` | JWT | 통계 |

## 플랜

| 플랜 | 월 한도 | Rate Limit |
|------|--------|------------|
| FREE | 100건 | 분당 10회 |
| STARTER | 1,000건 | 분당 60회 |
| PRO | 10,000건 + LLM Layer 2 | 분당 300회 |
| ENTERPRISE | 무제한 + 전체 기능 | 분당 1,000회 |

## 진행 현황

[adlex-docs/plan.md](adlex-docs/plan.md) 참고.

## 라이선스

Private
