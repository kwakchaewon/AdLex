# Task 3.12: 결제 연동 화면

## INPUT
- adlex-dashboard/src/views/SettingsView.vue

## OUTPUT 경로
- adlex-dashboard/src/views/SettingsView.vue (결제 탭 추가)
- adlex-dashboard/src/composables/usePayment.ts

## 상세 스펙
npm i @portone/browser-sdk. 결제창 호출 → 결과 확인.
플랜 변경: 업그레이드/다운그레이드/취소 UI.
POST /api/billing/subscribe → confirm → status.

## 완료 조건
npm run dev → 결제 플로우 테스트

## 커밋
feat(dashboard): integrate PortOne payment in settings
