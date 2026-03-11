# Task 3.11: 레이아웃 (Sidebar + Header)

## INPUT
- adlex-dashboard/src/router/index.ts

## OUTPUT 경로
- adlex-dashboard/src/components/layout/AppLayout.vue
- adlex-dashboard/src/components/layout/AppSidebar.vue
- adlex-dashboard/src/components/layout/AppHeader.vue

## 상세 스펙
AppLayout: 좌측 Sidebar + 상단 Header + 메인 콘텐츠.
Sidebar: AdLex 로고, 네비 (대시보드/API Key/히스토리/Playground/설정).
Header: 사용자명, plan Badge, 로그아웃.
반응형: md 이상 Sidebar, 모바일 햄버거.
PrimeVue 컴포넌트 우선. TailwindCSS 유틸리티.

## 완료 조건
npm run dev → 사이드바 + 헤더 정상 렌더링

## 커밋
feat(dashboard): implement app layout with sidebar and header
