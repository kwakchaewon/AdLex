---
name: rule-engine-extension
description: 규칙 엔진에 Evaluator+SQL+테스트 추가. "규칙 추가" 요청 시 사용.
---
# Rule Engine Extension
## 생성: Evaluator.kt + V{N}__.sql + Test.kt
## RuleEvaluator IF 구현. @Component.
## SQL 필수: code, name, rule_type, definition(JSONB), legal_basis(JSONB), suggestion_template