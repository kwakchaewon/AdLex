package com.adlex.api.controller

import com.adlex.api.dto.SuggestRequest
import com.adlex.api.dto.SuggestResponse
import com.adlex.domain.service.SuggestionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
@Tag(name = "Suggestion", description = "광고 메시지 법규 준수 수정 제안 API")
class SuggestController(
    private val suggestionService: SuggestionService
) {

    @PostMapping("/suggest")
    @Operation(
        summary = "광고 메시지 수정 제안",
        description = "RAG + LLM을 활용하여 법규 위반 가능성이 있는 광고 메시지의 수정안을 제안합니다."
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "수정 제안 완료"),
        ApiResponse(responseCode = "400", description = "Validation 실패"),
        ApiResponse(responseCode = "401", description = "인증 실패"),
        ApiResponse(responseCode = "429", description = "Rate limit 초과"),
        ApiResponse(responseCode = "503", description = "LLM 서비스 오류")
    )
    fun suggest(@RequestBody @Valid request: SuggestRequest): SuggestResponse {
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
}
