# Task 6.2: 산업별 규칙 팩

## INPUT
- adlex-api/.../domain/entity/Rule.kt

## OUTPUT 경로
- adlex-api/src/main/resources/db/migration/V5__industry_rules.sql
- adlex-api/.../domain/service/IndustryRuleService.kt

## 상세 스펙
의료/금융/식품 산업 특화 규칙. 카테고리별 법적 근거 포함.

## 완료 조건
.\gradlew.bat build

## 커밋
feat(engine): add industry-specific compliance rules
