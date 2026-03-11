# Task 1.3: 규칙 엔진 개발
 
## INPUT
- adlex-api/.../domain/entity/Rule.kt
- adlex-api/.../db/migration/V2__seed_rules.sql
 
## OUTPUT 경로
- adlex-api/.../engine/model/EvaluationContext.kt
- adlex-api/.../engine/model/EvaluationResult.kt
- adlex-api/.../engine/model/Channel.kt
- adlex-api/.../engine/model/Severity.kt
- adlex-api/.../engine/evaluator/RuleEvaluator.kt (인터페이스)
- adlex-api/.../engine/evaluator/RegexEvaluator.kt
- adlex-api/.../engine/evaluator/KeywordEvaluator.kt
- adlex-api/.../engine/evaluator/TimeRangeEvaluator.kt
- adlex-api/.../engine/evaluator/FieldPresentEvaluator.kt
- adlex-api/.../engine/RuleRegistry.kt
- adlex-api/.../domain/service/ComplianceCheckService.kt
- adlex-api/src/test/.../engine/evaluator/{각 Evaluator}Test.kt
 
## 상세 스펙
Strategy 패턴. RuleEvaluator IF → 4개 Evaluator. RuleRegistry(Redis 캐시). ComplianceCheckService.
 
## 완료 조건
.\gradlew.bat test 전체 통과
 
## 커밋
feat(engine): implement rule evaluators with strategy pattern