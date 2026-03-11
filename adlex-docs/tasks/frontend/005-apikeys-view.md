# Task 3.8: API Key 관리 화면

## INPUT
- adlex-dashboard/src/api/client.ts

## OUTPUT 경로
- adlex-dashboard/src/views/ApiKeysView.vue
- adlex-dashboard/src/api/keys.ts
- adlex-dashboard/src/types/apikey.ts

## 상세 스펙
DataTable: 이름, Prefix, 상태, 마지막 사용, 생성일, 액션(수정/복사/삭제).
생성 버튼 → 이름 입력 Dialog → 새 키 표시 + 복사 버튼.
PrimeVue DataTable + Dialog + Button.

## 완료 조건
npm run dev → /keys 정상 렌더링 + CRUD

## 커밋
feat(dashboard): implement API key management view
