# Task 1.4: 핵심 API POST /v1/check

공통 타입: adlex-docs/shared-types.md

## 의존성
- backend/003 (ComplianceCheckService, EvaluationContext, Channel)

## INPUT
- adlex-api/.../domain/service/ComplianceCheckService.kt
- adlex-api/.../engine/model/EvaluationContext.kt

## OUTPUT 경로
- adlex-api/.../api/controller/CheckController.kt
- adlex-api/.../api/dto/CheckRequest.kt
- adlex-api/.../api/dto/CheckResponse.kt
- adlex-api/.../api/dto/SenderInfo.kt
- adlex-api/.../api/dto/ViolationDto.kt
- adlex-api/.../api/advice/GlobalExceptionHandler.kt
- adlex-api/.../api/advice/ErrorResponse.kt

## 상세 스펙

### CheckRequest (data class)
```kotlin
data class CheckRequest(
    @field:NotBlank @field:Size(max = 2000)
    val message: String,
    @field:NotNull
    val channel: Channel,
    val sender: SenderInfo? = null,
    val scheduledAt: Instant? = null,
    val options: CheckOptions? = null
)

data class SenderInfo(
    val name: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
)

data class CheckOptions(
    val skipRules: List<String>? = null
)
```

### CheckResponse (data class)
```kotlin
data class CheckResponse(
    val compliant: Boolean,
    val violationCount: Int,
    val violations: List<ViolationDto>,
    val checkedAt: Instant,
    val processingMs: Long
)

data class ViolationDto(
    val ruleCode: String,
    val severity: Severity,
    val message: String,
    val legalBasis: String? = null,
    val suggestion: String? = null
)
```

### CheckController
```kotlin
@RestController
@RequestMapping("/v1")
@Tag(name = "Compliance Check")
class CheckController(private val complianceCheckService: ComplianceCheckService) {

    @PostMapping("/check")
    @Operation(summary = "메시지 법규 준수 검사")
    @ApiResponse(responseCode = "200", description = "검사 완료")
    @ApiResponse(responseCode = "400", description = "Validation 실패")
    @ApiResponse(responseCode = "401", description = "인증 실패")
    @ApiResponse(responseCode = "429", description = "Rate limit 초과")
    fun check(@RequestBody @Valid request: CheckRequest): CheckResponse
    // SecurityContext에서 tenantId 추출 (ApiKeyAuthFilter가 설정)
    // request → EvaluationContext 변환
    // complianceCheckService.check(tenantId, context) → CheckResponse 변환
    // processingMs = System.currentTimeMillis() 차이
}
```

### GlobalExceptionHandler (@RestControllerAdvice)
```kotlin
data class ErrorResponse(
    val error: ErrorDetail
)
data class ErrorDetail(
    val code: String,    // ErrorCode enum name
    val message: String,
    val details: Map<String, Any>? = null
)
```
처리할 예외:
- `MethodArgumentNotValidException` → 400 + field errors in details
- `BusinessException(ErrorCode, message)` → ErrorCode별 HTTP status 매핑
- `Exception` → 500 INTERNAL_ERROR

### BusinessException
```kotlin
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String
) : RuntimeException(message)

enum class ErrorCode(val status: Int) {
    VALIDATION_ERROR(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    RATE_LIMIT_EXCEEDED(429),
    QUOTA_EXCEEDED(402),
    INTERNAL_ERROR(500)
}
```

## 완료 조건
.\gradlew.bat build && Swagger UI에서 POST /v1/check 테스트

## 커밋
feat(api): implement POST /v1/check endpoint
