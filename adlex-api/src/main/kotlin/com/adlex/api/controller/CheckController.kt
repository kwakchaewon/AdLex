package com.adlex.api.controller

import com.adlex.api.dto.*
import com.adlex.domain.service.ComplianceCheckService
import com.adlex.engine.model.EvaluationContext
import com.adlex.engine.model.SenderInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1")
@Tag(name = "Compliance Check", description = "광고 메시지 법규 준수 검사 API")
class CheckController(
    private val complianceCheckService: ComplianceCheckService
) {

    @PostMapping("/check")
    @Operation(summary = "메시지 법규 준수 검사")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "검사 완료"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "429", description = "Rate limit 초과")
    )
    fun check(@RequestBody @Valid request: CheckRequest): CheckResponse =
        check(request, resolveTenantId())

    @PostMapping("/check/batch")
    @Operation(summary = "메시지 일괄 법규 검사 (최대 100건)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "일괄 검사 완료"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패")
    )
    fun batchCheck(@RequestBody @Valid request: BatchCheckRequest): BatchCheckResponse {
        val startMs = System.currentTimeMillis()
        val tenantId = resolveTenantId()

        val results = request.messages.map { check(it, tenantId) }
        val compliantCount = results.count { it.compliant }

        return BatchCheckResponse(
            results = results,
            summary = BatchSummary(
                total = results.size,
                compliant = compliantCount,
                violated = results.size - compliantCount,
                processingMs = System.currentTimeMillis() - startMs
            )
        )
    }

    private fun check(request: CheckRequest, tenantId: Long): CheckResponse {
        val context = EvaluationContext(
            message = request.message,
            channel = request.channel,
            sender = request.sender?.let {
                SenderInfo(name = it.name, phoneNumber = it.phoneNumber, email = it.email)
            },
            scheduledAt = request.scheduledAt,
            options = buildOptions(request)
        )
        val result = complianceCheckService.check(tenantId, context)
        return CheckResponse(
            compliant = result.compliant,
            violationCount = result.violations.size,
            violations = result.violations.map {
                ViolationDto(ruleCode = it.ruleCode, severity = it.severity, message = it.message,
                    legalBasis = it.legalBasis, suggestion = it.suggestion)
            },
            checkedAt = result.checkedAt,
            processingMs = result.processingMs
        )
    }

    /** SecurityContext에서 tenantId 추출 (ApiKeyAuthFilter 설정 전 fallback = 1L) */
    private fun resolveTenantId(): Long =
        (SecurityContextHolder.getContext().authentication?.principal as? Long) ?: 1L

    private fun buildOptions(request: CheckRequest): Map<String, Any> {
        val opts = mutableMapOf<String, Any>()
        request.options?.skipRules?.let { opts["skipRules"] = it }
        return opts
    }
}
