# Task 3.9: 히스토리 화면

## INPUT
- adlex-dashboard/src/api/client.ts

## OUTPUT 경로
- adlex-dashboard/src/views/HistoryView.vue
- adlex-dashboard/src/api/history.ts
- adlex-dashboard/src/types/history.ts

## 상세 스펙
DataTable (서버 페이징): 일시, 채널, 결과 Badge, 위반수, 상세보기.
필터: 채널, 결과, 날짜 범위.
상세: Dialog로 위반 그룹 표시 (규칙명, severity, 법적 근거, 수정 제안).

## 완료 조건
npm run dev → /history 정상 렌더링 + 필터 + 페이징

## 커밋
feat(dashboard): implement check history view
