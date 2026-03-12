# CLAUDE.md — AdLex 프로젝트 지시서
 
> AI-powered API for checking marketing and advertising compliance
 
## 개발 환경
- OS: Windows
- 터미널: PowerShell
- IDE: VS Code + Claude 확장 (기본 모델: Sonnet)
- 컨테이너: Docker Desktop for Windows
- Gradle 실행: .\gradlew.bat (bash의 ./gradlew 대신)
 
## 프로젝트 개요
AdLex — 마케팅/광고 메시지(SMS/카카오/이메일) 법규 준수 자동 검사 API 서비스.
 
## 기술 스택
- Backend: Kotlin 1.9+ / Spring Boot 3.3+ / JDK 21 / Gradle (Kotlin DSL)
- Frontend: Vue.js 3 / Vite / TypeScript / PrimeVue 4 (Aura) / TailwindCSS
- DB: PostgreSQL 16 / Redis 7
- ORM: Spring Data JPA + QueryDSL. Migration: Flyway.
- Auth: Spring Security + JWT + API Key
- Infra: Docker / AWS ECS Fargate / RDS / ElastiCache
- CI/CD: GitHub Actions
- 패키지 루트: com.adlex
 
## 디렉토리 맵 (절대 경로표)
> Claude는 가급적 파일 탐색(ls, find, tree)을 하지 않는다. 아래 맵을 따른다.
 
### 백엔드 (adlex-api/)
| 경로 | 내용 |
|------|------|
| adlex-api/build.gradle.kts | Gradle 빌드 설정 |
| adlex-api/Dockerfile | Docker 이미지 |
| adlex-api/src/main/kotlin/com/adlex/ | 메인 소스 루트 |
| adlex-api/.../api/controller/ | REST Controller |
| adlex-api/.../api/dto/ | Request/Response DTO |
| adlex-api/.../api/advice/ | GlobalExceptionHandler |
| adlex-api/.../domain/entity/ | JPA Entity |
| adlex-api/.../domain/repository/ | Repository 인터페이스 |
| adlex-api/.../domain/service/ | 비즈니스 로직 Service |
| adlex-api/.../engine/evaluator/ | 규칙 평가기 (Strategy 패턴) |
| adlex-api/.../engine/model/ | EvaluationContext, EvaluationResult |
| adlex-api/.../engine/RuleRegistry.kt | 규칙 로딩/캐싱 |
| adlex-api/.../infra/config/ | Spring Config 클래스 |
| adlex-api/.../infra/security/ | ApiKeyAuthFilter, JwtProvider |
| adlex-api/.../infra/cache/ | RedisCacheService, RateLimiter |
| adlex-api/.../infra/llm/ | ClaudeApiClient (Phase 5) |
| adlex-api/src/main/resources/application.yml | 공통 설정 |
| adlex-api/src/main/resources/application-local.yml | 로컬 설정 |
| adlex-api/src/main/resources/application-prod.yml | 프로덕션 설정 |
| adlex-api/src/main/resources/db/migration/ | Flyway SQL 파일 |
| adlex-api/src/test/kotlin/com/adlex/ | 테스트 (동일 패키지 구조) |
 
(adlex-api/... = adlex-api/src/main/kotlin/com/adlex/)
 
### 프론트엔드 (adlex-dashboard/)
| 경로 | 내용 |
|------|------|
| adlex-dashboard/src/views/ | 페이지 컴포넌트 |
| adlex-dashboard/src/components/layout/ | AppHeader, AppSidebar |
| adlex-dashboard/src/api/ | Axios API 함수 |
| adlex-dashboard/src/stores/ | Pinia stores |
| adlex-dashboard/src/router/index.ts | Vue Router |
| adlex-dashboard/src/types/ | TypeScript 타입 |
 
### 인프라·문서·Skills
| 경로 | 내용 |
|------|------|
| adlex-infra/docker-compose.yml | 로컬 PG + Redis |
| adlex-infra/terraform/ | AWS 리소스 |
| .github/workflows/ | CI/CD |
| adlex-docs/plan.md | 진행 현황 |
| adlex-docs/tasks/{backend,frontend,infra}/*.md | 태스크 |
| adlex-docs/decisions/*.md | ADR |
| .claude/skills/{skill-name}/SKILL.md | Skills 7개 |
| .claude/settings.json | 모델 설정 (Sonnet) |
 
## 파일 경로 자동 계산
도메인={Domain}일 때:
- Controller → adlex-api/.../api/controller/{Domain}Controller.kt
- DTO → adlex-api/.../api/dto/{Domain}Request.kt, {Domain}Response.kt
- Service → adlex-api/.../domain/service/{Domain}Service.kt
- Entity → adlex-api/.../domain/entity/{Domain}.kt
- Repository → adlex-api/.../domain/repository/{Domain}Repository.kt
- Test → adlex-api/src/test/.../domain/service/{Domain}ServiceTest.kt
- Vue Page → adlex-dashboard/src/views/{Page}View.vue
- Vue API → adlex-dashboard/src/api/{domain}.ts
- Vue Store → adlex-dashboard/src/stores/{domain}.ts
 
## 코드 규칙
 
### Kotlin (백엔드)
- data class 적극 사용.
- 모든 Controller에 @Tag, @Operation (Swagger) 어노테이션 필수.
- 모든 Service에 단위 테스트 필수 (JUnit 5 + MockK).
- 에러: BusinessException(ErrorCode, message) → GlobalExceptionHandler.
 
### Vue.js (프론트엔드)
- <script setup lang="ts"> + Composition API 필수.
- PrimeVue 4 컴포넌트 우선. TailwindCSS 유틸리티.
- Pinia 상태 관리. Axios (src/api/client.ts).
 
### Git 커밋
형식: <type>(<scope>): <description>
type: feat | fix | refactor | test | docs | chore
scope: api | engine | auth | dashboard | infra | db | docs
scope 자동 추론:
  engine/ → engine, api/ → api, infra/security/ → auth,
  adlex-dashboard/ → dashboard, terraform/ → infra, db/migration/ → db
subject: 50자 이내. 명령형. 마침표 없음.
 
## 토큰 최적화 규칙
1. 가급적 파일 경로 탐색을 하지 않는다. 디렉토리 맵 또는 OUTPUT 경로를 따른다.
2. 반복 설명 금지. 이 CLAUDE.md에 있는 내용을 재설명하지 않는다.
3. 출력 최적화. 코드 자체에 집중. import문 완전 포함.
4. 1 태스크 = 1 대화. 20턴 넘으면 새 대화.
 
## 작업 프로세스
1. 태스크 파일(adlex-docs/tasks/)을 읽고 OUTPUT 경로에 파일 생성
2. 코드 생성 후 빌드/테스트 명령 안내 (PowerShell: .\gradlew.bat)
3. 코드 생성 완료 후 /auto-commit 스킬을 자동 실행하여 커밋
4. plan.md의 해당 태스크를 [x] 완료로 업데이트