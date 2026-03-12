package com.adlex.api.controller

import com.adlex.api.dto.CheckRequest
import com.adlex.api.dto.CheckResponse
import com.adlex.api.dto.ViolationDto
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
    fun check(@RequestBody @Valid request: CheckRequest): CheckResponse {
        val tenantId = resolveTenantId()

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
                ViolationDto(
                    ruleCode = it.ruleCode,
                    severity = it.severity,
                    message = it.message,
                    legalBasis = it.legalBasis,
                    suggestion = it.suggestion
                )
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
