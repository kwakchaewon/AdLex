# Task 1.5: Batch API + Rules API

공통 타입: adlex-docs/shared-types.md

## 의존성
- backend/004 (CheckController, CheckRequest, CheckResponse)

## INPUT
- adlex-api/.../api/controller/CheckController.kt
- adlex-api/.../api/dto/CheckRequest.kt
- adlex-api/.../api/dto/CheckResponse.kt
- adlex-api/.../engine/RuleRegistry.kt

## OUTPUT 경로
- adlex-api/.../api/controller/CheckController.kt (batch 엔드포인트 추가)
- adlex-api/.../api/dto/BatchCheckRequest.kt
- adlex-api/.../api/dto/BatchCheckResponse.kt
- adlex-api/.../api/controller/RuleController.kt
- adlex-api/.../api/dto/RuleResponse.kt

## 상세 스펙

### BatchCheckRequest
```kotlin
data class BatchCheckRequest(
    @field:NotEmpty @field:Size(max = 100)
    val messages: List<@Valid CheckRequest>
)
```

### BatchCheckResponse
```kotlin
data class BatchCheckResponse(
    val results: List<CheckResponse>,
    val summary: BatchSummary
)

data class BatchSummary(
    val total: Int,
    val compliant: Int,
    val violated: Int,
    val processingMs: Long
)
```

### CheckController에 추가
```kotlin
@PostMapping("/check/batch")
@Operation(summary = "메시지 일괄 법규 검사")
fun batchCheck(@RequestBody @Valid request: BatchCheckRequest): BatchCheckResponse
// 각 message를 순차 check → results 수집
// summary 계산: total=results.size, compliant=count{it.compliant}, violated=total-compliant
```

### RuleResponse
```kotlin
data class RuleResponse(
    val code: String,
    val name: String,
    val description: String?,
    val type: RuleType,
    val channels: List<Channel>,
    val severity: Severity,
    val legalBasis: String?,
    val active: Boolean
)
```

### RuleController
```kotlin
@RestController
@RequestMapping("/v1/rules")
@Tag(name = "Rules")
class RuleController(private val ruleRegistry: RuleRegistry) {

    @GetMapping
    @Operation(summary = "활성 규칙 목록 조회")
    fun getRules(@RequestParam channel: Channel?): List<RuleResponse>
    // channel 파라미터 있으면 해당 채널 규칙만, 없으면 전체
    // RuleRegistry 캐시 활용 (TTL 10분)

    @GetMapping("/{code}")
    @Operation(summary = "규칙 상세 조회")
    fun getRule(@PathVariable code: String): RuleResponse
    // 없으면 BusinessException(NOT_FOUND)
}
```

인증: /v1/rules는 API Key 인증 필요 (CheckController와 동일)

## 완료 조건
.\gradlew.bat build && Swagger UI에서 batch, rules 테스트

## 커밋
feat(api): add batch check and rules endpoints
