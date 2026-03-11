# Task 1.4: 핵심 API POST /v1/check

## INPUT
- adlex-api/.../domain/service/ComplianceCheckService.kt

## OUTPUT 경로
- adlex-api/.../api/controller/CheckController.kt
- adlex-api/.../api/dto/CheckRequest.kt
- adlex-api/.../api/dto/CheckResponse.kt
- adlex-api/.../api/advice/GlobalExceptionHandler.kt

## 상세 스펙
POST /v1/check 엔드포인트.
CheckRequest: message(@NotBlank), channel(@NotNull), sender(SenderInfo?), scheduledAt?, options?
CheckResponse: compliant, violationCount, violations[], checkedAt, processingMs
ViolationDto: ruleCode, severity, message, legalBasis?, suggestion?
@RestController @RequestMapping("/v1") @Tag(name="Compliance Check")
HTTP: 200(검사완료), 400(validation), 401(인증), 429(rate limit), 500(서버에러)

## 완료 조건
.\gradlew.bat build && Swagger UI에서 POST /v1/check 테스트

## 커밋
feat(api): implement POST /v1/check endpoint
