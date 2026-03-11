# Task 5.5: 정기 리포트

## INPUT
- adlex-api/.../domain/entity/CheckLog.kt

## OUTPUT 경로
- adlex-api/.../domain/service/ReportService.kt
- adlex-api/.../infra/config/SchedulerConfig.kt

## 상세 스펙
@Scheduled 매주 1회. check_logs 집계 → HTML 형태 이메일 발송.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement weekly compliance report
