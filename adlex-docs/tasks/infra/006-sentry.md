# Task 4.1: Sentry 에러 추적

## OUTPUT 경로
- adlex-api/build.gradle.kts (sentry-spring-boot-starter-jakarta 추가)
- adlex-api/src/main/resources/application.yml (sentry 설정 추가)
- adlex-dashboard/package.json (@sentry/vue 추가)
- adlex-dashboard/src/main.ts (Sentry init 추가)

## 상세 스펙
백엔드: sentry-spring-boot-starter-jakarta. tenantId 태그 추가.
프론트: @sentry/vue. Vue Router 연동.

## 완료 조건
.\gradlew.bat build && npm run build

## 커밋
chore(infra): integrate Sentry error tracking
