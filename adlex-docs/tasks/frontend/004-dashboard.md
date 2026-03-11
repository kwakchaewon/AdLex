# Task 3.7: 대시보드 화면

## INPUT
- adlex-dashboard/src/api/client.ts

## OUTPUT 경로
- adlex-dashboard/src/views/DashboardView.vue
- adlex-dashboard/src/api/usage.ts
- adlex-dashboard/src/types/usage.ts

## 상세 스펙
상단: 요약 카드 4개 (총 검사, 준수율, 위반, 남은 쿼터).
중앙: 일별 검사 건수 라인 차트 (최근 30일).
하단: 주요 위반 유형 Top 5 파이 차트.
API: GET /api/usage, GET /api/usage/daily.

## 완료 조건
npm run dev → /dashboard 정상 렌더링 + API 연동

## 커밋
feat(dashboard): implement dashboard view with charts
