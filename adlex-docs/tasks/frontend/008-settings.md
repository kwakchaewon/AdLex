# Task 3.11b: Settings + 프로필 화면

## INPUT
- adlex-dashboard/src/api/client.ts
- adlex-dashboard/src/stores/auth.ts

## OUTPUT 경로
- adlex-dashboard/src/views/SettingsView.vue
- adlex-dashboard/src/api/billing.ts
- adlex-dashboard/src/types/billing.ts

## 상세 스펙
프로필 탭: 회사명 수정, 비밀번호 변경.
사용량 탭: 현재 플랜, 플랜 별 카드(FREE/STARTER/PRO), 업그레이드/취소.

## 완료 조건
npm run dev → /settings 정상 렌더링

## 커밋
feat(dashboard): implement settings and profile view
