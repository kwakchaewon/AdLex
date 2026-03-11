# Task 3.10: Playground 화면

## INPUT
- adlex-dashboard/src/api/client.ts

## OUTPUT 경로
- adlex-dashboard/src/views/PlaygroundView.vue
- adlex-dashboard/src/api/check.ts
- adlex-dashboard/src/types/check.ts

## 상세 스펙
좌측: 메시지 Textarea, 채널 선택, 발신자명/연락처, 발송 시간, 검사 버튼.
우측: 준수/위반 아이콘, 위반 그룹 카드 리스트, 처리 시간.
POST /v1/check (사용자의 API Key).

## 완료 조건
npm run dev → /playground 정상 렌더링 + 검사 실행

## 커밋
feat(dashboard): implement playground view
