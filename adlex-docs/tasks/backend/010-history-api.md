# Task 3.4: 히스토리 API

## INPUT
- adlex-api/.../domain/entity/CheckLog.kt

## OUTPUT 경로
- adlex-api/.../api/controller/HistoryController.kt
- adlex-api/.../api/dto/HistoryResponse.kt
- adlex-api/.../domain/service/HistoryService.kt
- adlex-api/.../domain/repository/CheckLogRepository.kt (수정)

## 상세 스펙
GET /api/history: 페이징, 필터(channel, compliant, date 범위).
GET /api/history/{id}: 검사 상세 (violations JSONB 포함).

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement check history API
