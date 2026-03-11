# Task 5.1: LLM 기반 기만광고 검사

## INPUT
- adlex-api/.../engine/evaluator/RuleEvaluator.kt

## OUTPUT 경로
- adlex-api/.../engine/evaluator/LlmJudgeEvaluator.kt
- adlex-api/.../infra/llm/ClaudeApiClient.kt
- adlex-api/.../infra/config/ClaudeConfig.kt

## 상세 스펙
LlmJudgeEvaluator: Claude Haiku API로 기만광고 표현 판별.
confidence < 0.8이면 pass (보수적 판단). Circuit Breaker 패턴 적용.
LLM 타임아웃/실패 시 해당 규칙 스킵 (전체 검사 실패 방지).

## 완료 조건
.\gradlew.bat build

## 커밋
feat(engine): add LLM-based deceptive ad detection
