# AdLex

마케팅/광고 메시지의 법규 준수 여부를 자동 검사하는 API 서비스.

## 기술 스택

| 영역 | 스택 |
|------|------|
| Backend | Kotlin / Spring Boot 3 / JDK 21 |
| Frontend | Vue.js 3 / Vite / TypeScript / PrimeVue 4 |
| Database | PostgreSQL 16 / Redis 7 |
| Infra | Docker / AWS ECS Fargate |

## 프로젝트 구조

```
adlex-api/          # Spring Boot API 서버
adlex-dashboard/    # Vue.js 관리 대시보드
adlex-infra/        # Docker Compose, Terraform
adlex-docs/         # 문서 및 태스크
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

| Method | Path | 설명 |
|--------|------|------|
| POST | `/v1/check` | 메시지 법규 검사 |
| POST | `/v1/check/batch` | 일괄 검사 |
| GET | `/v1/rules` | 규칙 목록 조회 |

## 라이선스

Private
