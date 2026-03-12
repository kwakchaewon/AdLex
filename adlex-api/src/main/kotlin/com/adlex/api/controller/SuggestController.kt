package com.adlex.api.controller

import com.adlex.api.dto.SuggestRequest
import com.adlex.api.dto.SuggestResponse
import com.adlex.domain.service.LlmPlanGuard
import com.adlex.domain.service.SuggestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
@Tag(name = "Suggestion", description = "광고 메시지 법규 준수 수정 제안 API (PRO 이상)")
class SuggestController(
    private val suggestionService: SuggestionService,
    private val llmPlanGuard: LlmPlanGuard
) {

    @PostMapping("/suggest")
    @Operation(
        summary = "광고 메시지 수정 제안 [PRO+]",
        description = "RAG + LLM을 활용하여 법규 위반 가능성이 있는 광고 메시지의 수정안을 제안합니다. PRO 이상 플랜 전용."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 제안 완료"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "402", description = "플랜 업그레이드 필요 (PRO 이상)"),
        ApiResponse(responseCode = "429", description = "Rate limit 초과")
    )
    fun suggest(@RequestBody @Valid request: SuggestRequest): SuggestResponse {
        val tenantId = resolveTenantId()
        llmPlanGuard.requireLlmAccess(tenantId)

        val result = suggestionService.suggest(
            message = request.message,
            channel = request.channel,
            hint = request.hint
        )
        return SuggestResponse(
            original = request.message,
            suggested = result.suggested,
            rationale = result.rationale,
            citedLawCount = result.citedLawCount,
            citedPrecedentCount = result.citedPrecedentCount
        )
    }

    private fun resolveTenantId(): Long =
        (SecurityContextHolder.getContext().authentication?.principal as? Long) ?: 1L
}
