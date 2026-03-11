# Task 6.5: 커스텀 규칙 (백엔드)

## INPUT
- adlex-api/.../domain/entity/Rule.kt

## OUTPUT 경로
- adlex-api/.../api/controller/CustomRuleController.kt
- adlex-api/.../api/dto/CustomRuleRequest.kt
- adlex-api/.../api/dto/CustomRuleResponse.kt
- adlex-api/.../domain/service/CustomRuleService.kt
- adlex-api/src/main/resources/db/migration/V7__custom_rules.sql

## 상세 스펙
Enterprise 전용. 테넌트별 자체 규칙 CRUD. 규칙 엔진에 자동 등록.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(api): implement custom rules for Enterprise plan
