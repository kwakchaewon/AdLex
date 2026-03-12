# Task 1.3: 규칙 엔진 개발

공통 타입: adlex-docs/shared-types.md

## 의존성
- backend/002 (Entity, Repository)

## INPUT
- adlex-api/.../domain/entity/Rule.kt
- adlex-api/.../domain/repository/RuleRepository.kt

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
- adlex-api/src/test/.../engine/evaluator/RegexEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/KeywordEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/TimeRangeEvaluatorTest.kt
- adlex-api/src/test/.../engine/evaluator/FieldPresentEvaluatorTest.kt
- adlex-api/src/test/.../domain/service/ComplianceCheckServiceTest.kt

## 상세 스펙

### Enums
- `Channel` enum: SMS, KAKAO, EMAIL (패키지: engine.model)
- `Severity` enum: HIGH, MEDIUM, LOW (패키지: engine.model)

### EvaluationContext (data class)
```
message: String              // 검사 대상 메시지 본문
channel: Channel             // 발송 채널
sender: SenderInfo?          // 발신자 정보 (shared-types.md §5)
scheduledAt: Instant?        // 예약 발송 시각 (null이면 현재 시각)
options: Map<String, Any>    // 추가 옵션 (subject, unsubscribeUrl 등)
```

### EvaluationResult (data class)
```
ruleCode: String
severity: Severity
message: String              // 위반 설명 (한국어)
legalBasis: String?          // 법적 근거
suggestion: String?          // 수정 제안
```

### RuleEvaluator (interface)
```kotlin
interface RuleEvaluator {
    fun supports(type: RuleType): Boolean
    fun evaluate(context: EvaluationContext, rule: Rule): EvaluationResult?
    // null 반환 = 위반 없음, non-null = 위반 발견
}
```

### Evaluator 구현 (각각 @Component)
1. **RegexEvaluator** (supports: REGEX)
   - `rule.config` JSON에서 `matchMode` 읽기
   - REQUIRE: 패턴 미매칭 시 violation (예: 광고 표기 누락)
   - DENY: 패턴 매칭 시 violation (예: 금지 표현 사용)
   - `rule.config`에 `target` 필드 있으면 options[target]에서 검사 (예: subject)
   - 기본: context.message에서 검사

2. **KeywordEvaluator** (supports: KEYWORD)
   - `rule.pattern`에 키워드 목록 (콤마 구분)
   - matchMode=DENY: 키워드 포함 시 violation

3. **TimeRangeEvaluator** (supports: TIME_RANGE)
   - `rule.config` JSON: `denyStart`, `denyEnd`, `timezone` (default: Asia/Seoul)
   - scheduledAt (또는 Instant.now()) → ZonedDateTime 변환
   - deny 시간대에 포함되면 violation

4. **FieldPresentEvaluator** (supports: FIELD_PRESENT)
   - `rule.config` JSON: `field`
   - sender?.{field} 또는 options[field] 존재 + 비어있지 않은지 확인
   - 없으면 violation

### RuleRegistry (@Service)
```kotlin
@Service
class RuleRegistry(
    private val ruleRepository: RuleRepository,
    private val redisTemplate: StringRedisTemplate
) {
    fun getActiveRules(): List<Rule>
    // Redis 캐시: key="rules:active", TTL=10분
    // miss 시 DB 조회 → Redis 저장 → 반환

    fun getRulesByChannel(channel: Channel): List<Rule>
    // getActiveRules() → filter { channel.name in it.channels() }
}
```

### ComplianceCheckService (@Service)
```kotlin
@Service
class ComplianceCheckService(
    private val ruleRegistry: RuleRegistry,
    private val evaluators: List<RuleEvaluator>,
    private val checkLogRepository: CheckLogRepository
) {
    fun check(tenantId: Long, context: EvaluationContext): CheckResultDto
    // 1. ruleRegistry.getRulesByChannel(context.channel)
    // 2. options.skipRules 있으면 해당 rule 제외
    // 3. 각 rule → evaluators.find { it.supports(rule.type) }?.evaluate(context, rule)
    // 4. non-null 결과 수집 → violations
    // 5. CheckLog 저장 (tenant_id, message, channel, compliant, violations JSON)
    // 6. CheckResultDto 반환
}
```

## 완료 조건
.\gradlew.bat test 전체 통과 (Evaluator 4개 + ComplianceCheckService 단위테스트)

## 커밋
feat(engine): implement rule evaluators with strategy pattern
